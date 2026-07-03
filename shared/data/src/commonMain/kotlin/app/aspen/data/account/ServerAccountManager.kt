package app.aspen.data.account

import app.aspen.api.ApiError
import app.aspen.api.AspenApi
import app.aspen.api.AuthResponse
import app.aspen.api.LoginRequest
import app.aspen.api.RegisterRequest
import app.aspen.domain.account.AccountManager
import app.aspen.domain.account.AccountResult
import app.aspen.domain.account.AccountState
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * [AccountManager] over the Aspen server (docs/08 §1; wire contract in `:shared:server-api`).
 * Total and calm: every transport failure maps to [AccountResult.Unavailable]; error bodies map
 * by machine code, never by displaying server text (CLAUDE.md #11 — copy stays in UI resources).
 * The session persists encrypted via [PersistentSessionStore]; the password is used in the one
 * request and never stored anywhere on the device.
 */
class ServerAccountManager(
    private val baseUrl: String,
    private val sessions: PersistentSessionStore,
    private val http: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AccountManager {

    override fun current(): AccountState? = sessions.current()?.let { AccountState(it.accountId) }

    /** The bearer token for other Aspen-server calls (e.g. the AI relay); null when signed out. */
    fun sessionToken(): String? = sessions.current()?.token

    override suspend fun register(password: String, email: String?): AccountResult = authCall {
        http.post(url(AspenApi.REGISTER)) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RegisterRequest.serializer(), RegisterRequest(password, email)))
        }
    }

    override suspend fun signIn(identifier: String, password: String): AccountResult = authCall {
        http.post(url(AspenApi.LOGIN)) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LoginRequest.serializer(), LoginRequest(identifier, password)))
        }
    }

    override suspend fun signOut() {
        val token = sessionToken()
        // Best-effort server revoke; the LOCAL clear below must happen no matter what.
        if (token != null) {
            runCatching { http.post(url(AspenApi.LOGOUT)) { bearer(token) } }
        }
        sessions.clear()
    }

    override suspend fun deleteAccount(): Boolean {
        val token = sessionToken() ?: return false
        val purged = runCatching {
            http.delete(url(AspenApi.ACCOUNT)) { bearer(token) }.status.isSuccess()
        }.getOrDefault(false)
        // Only forget locally once the server purge succeeded — otherwise the user could believe
        // deleted data still exists server-side with no session left to delete it.
        if (purged) sessions.clear()
        return purged
    }

    private suspend fun authCall(request: suspend () -> HttpResponse): AccountResult = runCatching {
        val response = request()
        if (response.status.isSuccess()) {
            val auth = json.decodeFromString(AuthResponse.serializer(), response.bodyAsText())
            sessions.save(StoredSession(auth.accountId, auth.token))
            return@runCatching AccountResult.SignedIn(AccountState(auth.accountId))
        }
        when (errorCode(response)) {
            ApiError.CODE_EMAIL_TAKEN -> AccountResult.EmailTaken
            ApiError.CODE_WEAK_PASSWORD -> AccountResult.WeakPassword
            else -> if (response.status == HttpStatusCode.Unauthorized) {
                AccountResult.Denied
            } else {
                AccountResult.Unavailable
            }
        }
    }.getOrDefault(AccountResult.Unavailable)

    private suspend fun errorCode(response: HttpResponse): String? =
        runCatching { json.decodeFromString(ApiError.serializer(), response.bodyAsText()).code }.getOrNull()

    private fun url(path: String) = baseUrl.trimEnd('/') + path

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }
}
