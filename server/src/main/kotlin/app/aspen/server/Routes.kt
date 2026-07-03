package app.aspen.server

import app.aspen.api.ApiChatMessage
import app.aspen.api.ApiError
import app.aspen.api.AspenApi
import app.aspen.api.AuthResponse
import app.aspen.api.LoginRequest
import app.aspen.api.RecoveryCompleteRequest
import app.aspen.api.RecoveryRequest
import app.aspen.api.ReflectRequest
import app.aspen.api.ReflectResponse
import app.aspen.api.RegisterRequest
import app.aspen.server.ai.ChatMessage
import app.aspen.server.ai.ChatRole
import app.aspen.server.ai.ModelProvider
import app.aspen.server.ai.ProviderResult
import app.aspen.server.auth.AccountService
import app.aspen.server.auth.LoginOutcome
import app.aspen.server.auth.RecoveryOutcome
import app.aspen.server.auth.RegisterOutcome
import app.aspen.server.store.BlobRepository
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

/** Everything the routes need; tests construct this directly with in-memory pieces. */
class ServerDeps(
    val service: AccountService,
    val blobs: BlobRepository,
    val provider: ModelProvider,
    val credentialLimiter: RateLimiter = RateLimiter(maxPerWindow = 20, windowMs = 60_000),
    val aiLimiter: RateLimiter = RateLimiter(maxPerWindow = 30, windowMs = 60_000),
)

/**
 * The Aspen server surface — auth, E2E ciphertext sync, and the stateless AI relay
 * (docs/07 Phase 6; wire contract in `:shared:server-api`).
 *
 * Route-level invariants:
 *  - Sync bodies are opaque bytes: stored and returned verbatim, never parsed (docs/08 §2).
 *  - The AI relay persists NOTHING (there is no content store to write to) and applies no
 *    content judgement — the device's consent gate, crisis check, and output guard wrap every
 *    call (CLAUDE.md #8).
 *  - Denials are undifferentiated (one `unauthorized` code) so account existence never leaks.
 */
fun Application.aspenServer(deps: ServerDeps) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }

    routing {
        get(AspenApi.HEALTH) {
            call.respondText("ok")
        }

        post(AspenApi.REGISTER) {
            if (!deps.credentialLimiter.allow("register")) return@post call.rateLimited()
            val body = call.receiveValid<RegisterRequest>() ?: return@post
            when (val outcome = deps.service.register(body.password, body.email)) {
                is RegisterOutcome.Registered ->
                    call.respond(AuthResponse(outcome.accountId, outcome.token))
                RegisterOutcome.EmailTaken ->
                    call.respond(HttpStatusCode.Conflict, ApiError(ApiError.CODE_EMAIL_TAKEN))
                RegisterOutcome.WeakPassword ->
                    call.respond(HttpStatusCode.BadRequest, ApiError(ApiError.CODE_WEAK_PASSWORD))
            }
        }

        post(AspenApi.LOGIN) {
            val body = call.receiveValid<LoginRequest>() ?: return@post
            if (!deps.credentialLimiter.allow("login:${body.identifier}")) return@post call.rateLimited()
            when (val outcome = deps.service.login(body.identifier, body.password)) {
                is LoginOutcome.LoggedIn ->
                    call.respond(AuthResponse(outcome.accountId, outcome.token))
                LoginOutcome.Denied ->
                    call.respond(HttpStatusCode.Unauthorized, ApiError(ApiError.CODE_UNAUTHORIZED))
            }
        }

        post(AspenApi.LOGOUT) {
            val token = call.bearerToken()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiError(ApiError.CODE_UNAUTHORIZED))
            deps.service.logout(token)
            call.respond(HttpStatusCode.OK)
        }

        delete(AspenApi.ACCOUNT) {
            val accountId = call.requireAccount(deps) ?: return@delete
            deps.service.deleteAccount(accountId)
            call.respond(HttpStatusCode.OK)
        }

        post(AspenApi.RECOVERY_REQUEST) {
            if (!deps.credentialLimiter.allow("recovery")) return@post call.rateLimited()
            val body = call.receiveValid<RecoveryRequest>() ?: return@post
            deps.service.requestRecovery(body.email)
            // Always accepted: whether the email exists is never disclosed.
            call.respond(HttpStatusCode.Accepted)
        }

        post(AspenApi.RECOVERY_COMPLETE) {
            if (!deps.credentialLimiter.allow("recovery-complete")) return@post call.rateLimited()
            val body = call.receiveValid<RecoveryCompleteRequest>() ?: return@post
            when (val outcome = deps.service.completeRecovery(body.recoveryToken, body.newPassword)) {
                is RecoveryOutcome.Recovered ->
                    call.respond(AuthResponse(outcome.accountId, outcome.token))
                RecoveryOutcome.WeakPassword ->
                    call.respond(HttpStatusCode.BadRequest, ApiError(ApiError.CODE_WEAK_PASSWORD))
                RecoveryOutcome.Denied ->
                    call.respond(HttpStatusCode.Unauthorized, ApiError(ApiError.CODE_UNAUTHORIZED))
            }
        }

        put(AspenApi.SYNC_BLOB) {
            val accountId = call.requireAccount(deps) ?: return@put
            val bytes = call.receive<ByteArray>()
            if (bytes.size > MAX_BLOB_BYTES) {
                return@put call.respond(HttpStatusCode.PayloadTooLarge, ApiError(ApiError.CODE_TOO_LARGE))
            }
            deps.blobs.put(accountId, bytes)
            call.respond(HttpStatusCode.OK)
        }

        get(AspenApi.SYNC_BLOB) {
            val accountId = call.requireAccount(deps) ?: return@get
            val bytes = deps.blobs.get(accountId)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiError(ApiError.CODE_NOT_FOUND))
            call.respondBytes(bytes)
        }

        delete(AspenApi.SYNC_BLOB) {
            val accountId = call.requireAccount(deps) ?: return@delete
            if (deps.blobs.delete(accountId)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound, ApiError(ApiError.CODE_NOT_FOUND))
            }
        }

        post(AspenApi.AI_REFLECT) {
            val accountId = call.requireAccount(deps) ?: return@post
            if (!deps.aiLimiter.allow("ai:$accountId")) return@post call.rateLimited()
            val body = call.receiveValid<ReflectRequest>() ?: return@post
            if (body.text.isBlank() || body.text.length > MAX_REFLECT_CHARS || body.history.size > MAX_HISTORY_ENTRIES) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiError(ApiError.CODE_INVALID))
            }
            val result = deps.provider.complete(body.text, body.history.map { it.toChatMessage() })
            call.respond(
                when (result) {
                    is ProviderResult.Reply -> ReflectResponse(ReflectResponse.STATUS_REPLY, result.text)
                    ProviderResult.Unavailable -> ReflectResponse(ReflectResponse.STATUS_UNAVAILABLE)
                },
            )
        }
    }
}

private const val MAX_BLOB_BYTES = 5 * 1024 * 1024
private const val MAX_REFLECT_CHARS = 8_000
private const val MAX_HISTORY_ENTRIES = 64

private fun ApiChatMessage.toChatMessage() = ChatMessage(
    role = if (role == ApiChatMessage.ROLE_USER) ChatRole.USER else ChatRole.ASSISTANT,
    text = text,
)

/** Malformed request bodies are a client error (400), never an unhandled 500. */
private suspend inline fun <reified T : Any> ApplicationCall.receiveValid(): T? =
    runCatching { receive<T>() }.getOrNull()
        ?: run {
            respond(HttpStatusCode.BadRequest, ApiError(ApiError.CODE_INVALID))
            null
        }

private fun ApplicationCall.bearerToken(): String? =
    request.headers[HttpHeaders.Authorization]
        ?.takeIf { it.startsWith(BEARER_PREFIX) }
        ?.removePrefix(BEARER_PREFIX)
        ?.takeIf { it.isNotBlank() }

private suspend fun ApplicationCall.requireAccount(deps: ServerDeps): String? {
    val accountId = bearerToken()?.let { deps.service.accountFor(it) }
    if (accountId == null) respond(HttpStatusCode.Unauthorized, ApiError(ApiError.CODE_UNAUTHORIZED))
    return accountId
}

private suspend fun ApplicationCall.rateLimited() =
    respond(HttpStatusCode.TooManyRequests, ApiError(ApiError.CODE_RATE_LIMITED))

private const val BEARER_PREFIX = "Bearer "
