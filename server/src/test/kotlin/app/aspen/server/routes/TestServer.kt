package app.aspen.server.routes

import app.aspen.api.AspenApi
import app.aspen.api.AuthResponse
import app.aspen.api.RegisterRequest
import app.aspen.server.RateLimiter
import app.aspen.server.ServerDeps
import app.aspen.server.ai.FakeModelProvider
import app.aspen.server.ai.ModelProvider
import app.aspen.server.aspenServer
import app.aspen.server.auth.AccountService
import app.aspen.server.auth.RecoveryMailer
import app.aspen.server.store.BlobRepository
import app.aspen.server.store.InMemoryAccountRepository
import app.aspen.server.store.InMemoryBlobRepository
import app.aspen.server.store.InMemoryRecoveryTokenRepository
import app.aspen.server.store.InMemorySessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json

/** Route-test fixture: a full in-memory server with generous limits unless a test tightens them. */
internal class TestServer(
    val blobs: BlobRepository = InMemoryBlobRepository(),
    provider: ModelProvider = FakeModelProvider(),
    aiLimiter: RateLimiter = RateLimiter(maxPerWindow = 1_000, windowMs = 60_000),
    credentialLimiter: RateLimiter = RateLimiter(maxPerWindow = 1_000, windowMs = 60_000),
) {
    val sentMail = mutableListOf<Pair<String, String>>()

    val deps = ServerDeps(
        service = AccountService(
            accounts = InMemoryAccountRepository(),
            sessions = InMemorySessionRepository(),
            recoveryTokens = InMemoryRecoveryTokenRepository(),
            blobs = blobs,
            mailer = RecoveryMailer { email, token -> sentMail.add(email to token) },
            hashIterations = 1_000,
        ),
        blobs = blobs,
        provider = provider,
        credentialLimiter = credentialLimiter,
        aiLimiter = aiLimiter,
    )

    fun run(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application { aspenServer(deps) }
        block(client)
    }
}

internal val testJson = Json { ignoreUnknownKeys = true }

internal suspend fun HttpClient.registerAccount(password: String = "longenough", email: String? = null): AuthResponse {
    val response = post(AspenApi.REGISTER) {
        contentType(ContentType.Application.Json)
        setBody(testJson.encodeToString(RegisterRequest.serializer(), RegisterRequest(password, email)))
    }
    return testJson.decodeFromString(AuthResponse.serializer(), response.bodyAsText())
}

internal fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
    header(HttpHeaders.Authorization, "Bearer $token")
}

internal suspend inline fun <reified T> HttpResponse.body(deserializer: kotlinx.serialization.DeserializationStrategy<T>): T =
    testJson.decodeFromString(deserializer, bodyAsText())
