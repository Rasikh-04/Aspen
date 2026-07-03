package app.aspen.data.ai.cloud

import app.aspen.api.AspenApi
import app.aspen.domain.ai.AiClientResult
import app.aspen.domain.ai.AiMessage
import app.aspen.domain.ai.AiRole
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class AspenServerAiClientTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(
        engine: MockEngine,
        baseUrl: String? = "https://aspen.example.com",
        token: String? = "session-token",
    ) = AspenServerAiClient(baseUrl, { token }, HttpClient(engine))

    @Test
    fun `no server url or no session means Disabled with zero network calls`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }

        assertEquals(AiClientResult.Disabled, client(engine, baseUrl = null).reply("hi", emptyList()))
        assertEquals(AiClientResult.Disabled, client(engine, token = null).reply("hi", emptyList()))
        assertTrue(engine.requestHistory.isEmpty(), "Disabled must never touch the network")
    }

    @Test
    fun `a reply relays with the session bearer on Aspen's own wire shape`() = runTest {
        var seenUrl = ""
        var seenAuth: String? = null
        var seenBody = ""
        val engine = MockEngine { request ->
            seenUrl = request.url.toString()
            seenAuth = request.headers[HttpHeaders.Authorization]
            seenBody = String(request.body.toByteArray())
            respond("""{"status":"reply","text":"heard you"}""", HttpStatusCode.OK, jsonHeaders)
        }

        val history = listOf(
            AiMessage("1", AiRole.USER, "earlier", Instant.fromEpochMilliseconds(0)),
            AiMessage("2", AiRole.COMPANION, "reflected", Instant.fromEpochMilliseconds(1)),
        )
        val result = client(engine).reply("today was hard", history)

        assertEquals(AiClientResult.Reply("heard you"), result)
        assertEquals("https://aspen.example.com" + AspenApi.AI_REFLECT, seenUrl)
        assertEquals("Bearer session-token", seenAuth)
        // Aspen wire shape, not any vendor's: roles are user/companion, no model/system fields.
        assertTrue(seenBody.contains("\"companion\"") && !seenBody.contains("model"))
    }

    @Test
    fun `unavailable status, expired session, server error and offline all degrade calmly`() = runTest {
        suspend fun resultFor(engine: MockEngine) = client(engine).reply("hi", emptyList())

        val unavailable = MockEngine { respond("""{"status":"unavailable"}""", HttpStatusCode.OK, jsonHeaders) }
        val expired = MockEngine { respondError(HttpStatusCode.Unauthorized) }
        val serverError = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val offline = MockEngine { throw RuntimeException("no network") }

        assertEquals(AiClientResult.Unavailable, resultFor(unavailable))
        assertEquals(AiClientResult.Unavailable, resultFor(expired))
        assertEquals(AiClientResult.Unavailable, resultFor(serverError))
        assertEquals(AiClientResult.Unavailable, resultFor(offline))
    }
}
