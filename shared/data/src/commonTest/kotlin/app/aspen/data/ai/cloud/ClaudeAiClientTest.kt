package app.aspen.data.ai.cloud

import app.aspen.domain.ai.AiClientResult
import app.aspen.domain.ai.AiMessage
import app.aspen.domain.ai.AiRole
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.utils.io.core.toByteArray
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Wire-shape and degradation tests against a mock engine — no network, no key, ever (the shipped
 * DI default is [DisabledAiClient]; this class exists so the live wiring is a config decision, not
 * new code).
 */
class ClaudeAiClientTest {

    private fun config(token: String? = "test-token") = AiEndpointConfig(
        baseUrl = "https://example.invalid",
        authToken = { token },
    )

    private fun okBody(text: String) =
        """{"content":[{"type":"text","text":"$text"}],"stop_reason":"end_turn"}"""

    private fun clientReturning(body: String, status: HttpStatusCode = HttpStatusCode.OK): Pair<ClaudeAiClient, MockEngine> {
        val engine = MockEngine { _ ->
            respond(
                content = body.toByteArray(),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return ClaudeAiClient(config(), HttpClient(engine)) to engine
    }

    @Test
    fun sendsCorrectRequestShapeAndParsesReply() = runTest {
        val (client, engine) = clientReturning(okBody("that sounds heavy"))
        val history = listOf(
            AiMessage("m1", AiRole.USER, "yesterday", Instant.fromEpochMilliseconds(0)),
            AiMessage("m2", AiRole.COMPANION, "mm", Instant.fromEpochMilliseconds(1)),
        )

        val result = client.reply("today was hard", history)

        assertEquals(AiClientResult.Reply("that sounds heavy"), result)
        val request = engine.requestHistory.single()
        assertEquals("https://example.invalid/v1/messages", request.url.toString())
        assertEquals("test-token", request.headers["x-api-key"])
        assertEquals("2023-06-01", request.headers["anthropic-version"])
        val body = (request.body as TextContent).text
        // History rides along in order, user text last; system prompt included.
        assertTrue(body.contains(""""role":"user","content":"yesterday""""))
        assertTrue(body.contains(""""role":"assistant","content":"mm""""))
        assertTrue(body.contains(""""content":"today was hard""""))
        assertTrue(body.contains("supportive notebook"), "system prompt must be sent")
    }

    @Test
    fun missingTokenIsDisabledWithoutAnyNetworkCall() = runTest {
        val engine = MockEngine { error("must never be reached") }
        val client = ClaudeAiClient(config(token = null), HttpClient(engine))

        assertIs<AiClientResult.Disabled>(client.reply("hello", emptyList()))
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun blankBaseUrlIsDisabledWithoutAnyNetworkCall() = runTest {
        val engine = MockEngine { error("must never be reached") }
        val client = ClaudeAiClient(
            AiEndpointConfig(baseUrl = " ", authToken = { "t" }),
            HttpClient(engine),
        )

        assertIs<AiClientResult.Disabled>(client.reply("hello", emptyList()))
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun httpErrorDegradesToUnavailable() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val client = ClaudeAiClient(config(), HttpClient(engine))

        assertIs<AiClientResult.Unavailable>(client.reply("hello", emptyList()))
    }

    @Test
    fun transportFailureDegradesToUnavailable() = runTest {
        val engine = MockEngine { throw RuntimeException("connection reset") }
        val client = ClaudeAiClient(config(), HttpClient(engine))

        assertIs<AiClientResult.Unavailable>(client.reply("hello", emptyList()))
    }

    @Test
    fun malformedResponseDegradesToUnavailable() = runTest {
        val (client, _) = clientReturning("{not json")

        assertIs<AiClientResult.Unavailable>(client.reply("hello", emptyList()))
    }

    @Test
    fun refusalStopReasonIsUnavailableNeverSurfaced() = runTest {
        val (client, _) = clientReturning("""{"content":[],"stop_reason":"refusal"}""")

        assertIs<AiClientResult.Unavailable>(client.reply("hello", emptyList()))
    }

    @Test
    fun emptyContentIsUnavailable() = runTest {
        val (client, _) = clientReturning("""{"content":[],"stop_reason":"end_turn"}""")

        assertIs<AiClientResult.Unavailable>(client.reply("hello", emptyList()))
    }

    @Test
    fun disabledClientDefaultNeverReplies() = runTest {
        assertIs<AiClientResult.Disabled>(DisabledAiClient.reply("anything", emptyList()))
    }
}
