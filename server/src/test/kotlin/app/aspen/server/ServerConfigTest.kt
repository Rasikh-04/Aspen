package app.aspen.server

import app.aspen.server.ai.AnthropicMessagesProvider
import app.aspen.server.ai.FakeModelProvider
import app.aspen.server.ai.OpenAiCompatProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ServerConfigTest {

    private val http = HttpClient(MockEngine { respondOk() })

    private fun config(vararg pairs: Pair<String, String>): ServerConfig =
        ServerConfig.fromEnv { key -> pairs.toMap()[key] }

    @Test
    fun `no environment at all means fake provider, in-memory, port 8080 - runs with zero setup`() {
        val c = config()
        assertEquals(8080, c.port)
        assertEquals(null, c.dataDir)
        assertIs<FakeModelProvider>(buildModelProvider(c, http))
    }

    @Test
    fun `anthropic is selected only when url, model and key are all present`() {
        val complete = config(
            "ASPEN_AI_PROVIDER" to "anthropic",
            "ASPEN_AI_BASE_URL" to "https://api.example.com",
            "ASPEN_AI_MODEL" to "some-model",
            "ASPEN_AI_KEY" to "k",
        )
        assertIs<AnthropicMessagesProvider>(buildModelProvider(complete, http))

        val missingKey = config(
            "ASPEN_AI_PROVIDER" to "anthropic",
            "ASPEN_AI_BASE_URL" to "https://api.example.com",
            "ASPEN_AI_MODEL" to "some-model",
        )
        assertIs<FakeModelProvider>(buildModelProvider(missingKey, http))
    }

    @Test
    fun `openai-compatible shape covers any vendor exposing it - selected by name`() {
        val c = config(
            "ASPEN_AI_PROVIDER" to "openai",
            "ASPEN_AI_BASE_URL" to "http://localhost:11434",
            "ASPEN_AI_MODEL" to "any-local-model",
            "ASPEN_AI_KEY" to "unused-but-set",
        )
        assertIs<OpenAiCompatProvider>(buildModelProvider(c, http))
    }

    @Test
    fun `unknown provider name degrades to fake, never a half-configured vendor`() {
        val c = config(
            "ASPEN_AI_PROVIDER" to "mystery",
            "ASPEN_AI_BASE_URL" to "https://api.example.com",
            "ASPEN_AI_MODEL" to "m",
            "ASPEN_AI_KEY" to "k",
        )
        assertIs<FakeModelProvider>(buildModelProvider(c, http))
    }
}
