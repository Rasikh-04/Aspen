package app.aspen.server.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Both vendor adapters against a mock engine: request shape (the "documentation differences" —
 * auth header, system-prompt placement, roles, response path) and total, calm failure mapping.
 * No test here touches the network; this is the provider-agnostic contract with no live API.
 */
class ProviderAdapterTest {

    private val config = ProviderConfig(
        baseUrl = "https://vendor.example.com/",
        apiKey = "test-key",
        model = "any-model",
        maxTokens = 128,
    )
    private val history = listOf(
        ChatMessage(ChatRole.USER, "earlier"),
        ChatMessage(ChatRole.ASSISTANT, "reflected"),
    )

    private fun anthropicReply(text: String) =
        """{"content":[{"type":"text","text":"$text"}],"stop_reason":"end_turn"}"""

    private fun openAiReply(text: String) =
        """{"choices":[{"message":{"role":"assistant","content":"$text"},"finish_reason":"stop"}]}"""

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    // ---- Anthropic Messages shape ----

    @Test
    fun `anthropic - url, headers, top-level system, role mapping, text extraction`() = runTest {
        var seenUrl = ""
        var seenHeaders = mapOf<String, String?>()
        var seenBody = ""
        val http = HttpClient(
            MockEngine { request ->
                seenUrl = request.url.toString()
                seenHeaders = mapOf(
                    "x-api-key" to request.headers["x-api-key"],
                    "anthropic-version" to request.headers["anthropic-version"],
                )
                seenBody = String(request.body.toByteArray())
                respond(anthropicReply("a quiet reply"), HttpStatusCode.OK, jsonHeaders)
            },
        )

        val result = AnthropicMessagesProvider(config, http, systemPrompt = "SYSTEM-TEXT")
            .complete("hello", history)

        assertEquals(ProviderResult.Reply("a quiet reply"), result)
        assertEquals("https://vendor.example.com/v1/messages", seenUrl)
        assertEquals("test-key", seenHeaders["x-api-key"])
        assertEquals("2023-06-01", seenHeaders["anthropic-version"])

        val body = Json.parseToJsonElement(seenBody).jsonObject
        assertEquals("SYSTEM-TEXT", body["system"]!!.jsonPrimitive.content)
        assertEquals("any-model", body["model"]!!.jsonPrimitive.content)
        val roles = body["messages"]!!.jsonArray.map { it.jsonObject["role"]!!.jsonPrimitive.content }
        assertEquals(listOf("user", "assistant", "user"), roles)
    }

    @Test
    fun `anthropic - refusal, api error and transport failure all map to Unavailable`() = runTest {
        suspend fun resultFor(engine: MockEngine): ProviderResult =
            AnthropicMessagesProvider(config, HttpClient(engine), "s").complete("hi", emptyList())

        val refusal = MockEngine {
            respond("""{"content":[{"type":"text","text":"x"}],"stop_reason":"refusal"}""", HttpStatusCode.OK, jsonHeaders)
        }
        val apiError = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val thrower = MockEngine { throw RuntimeException("connection reset") }

        assertEquals(ProviderResult.Unavailable, resultFor(refusal))
        assertEquals(ProviderResult.Unavailable, resultFor(apiError))
        assertEquals(ProviderResult.Unavailable, resultFor(thrower))
    }

    // ---- OpenAI-compatible Chat Completions shape ----

    @Test
    fun `openai-compat - url, bearer auth, system as leading message, choices extraction`() = runTest {
        var seenUrl = ""
        var seenAuth: String? = null
        var seenBody = ""
        val http = HttpClient(
            MockEngine { request ->
                seenUrl = request.url.toString()
                seenAuth = request.headers[HttpHeaders.Authorization]
                seenBody = String(request.body.toByteArray())
                respond(openAiReply("heard you"), HttpStatusCode.OK, jsonHeaders)
            },
        )

        val result = OpenAiCompatProvider(config, http, systemPrompt = "SYSTEM-TEXT")
            .complete("hello", history)

        assertEquals(ProviderResult.Reply("heard you"), result)
        assertEquals("https://vendor.example.com/v1/chat/completions", seenUrl)
        assertEquals("Bearer test-key", seenAuth)

        val messages = Json.parseToJsonElement(seenBody).jsonObject["messages"]!!.jsonArray
        val roles = messages.map { it.jsonObject["role"]!!.jsonPrimitive.content }
        assertEquals(listOf("system", "user", "assistant", "user"), roles)
        assertEquals("SYSTEM-TEXT", messages.first().jsonObject["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun `openai-compat - content filter, empty choices, api error, transport failure to Unavailable`() = runTest {
        suspend fun resultFor(engine: MockEngine): ProviderResult =
            OpenAiCompatProvider(config, HttpClient(engine), "s").complete("hi", emptyList())

        val filtered = MockEngine {
            respond(
                """{"choices":[{"message":{"role":"assistant","content":"x"},"finish_reason":"content_filter"}]}""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }
        val empty = MockEngine { respond("""{"choices":[]}""", HttpStatusCode.OK, jsonHeaders) }
        val apiError = MockEngine { respondError(HttpStatusCode.TooManyRequests) }
        val thrower = MockEngine { throw RuntimeException("dns failure") }

        assertEquals(ProviderResult.Unavailable, resultFor(filtered))
        assertEquals(ProviderResult.Unavailable, resultFor(empty))
        assertEquals(ProviderResult.Unavailable, resultFor(apiError))
        assertEquals(ProviderResult.Unavailable, resultFor(thrower))
    }

    // ---- Fake (the no-API mode) ----

    @Test
    fun `fake provider is deterministic, offline and inside copy rules`() = runTest {
        val result = FakeModelProvider().complete("anything", emptyList())
        val reply = assertIs<ProviderResult.Reply>(result)
        assertEquals(FakeModelProvider.FIXED_REPLY, reply.text)
        assertTrue(reply.text.none { it.isDigit() }, "no numbers in the fixed dev line")
    }
}
