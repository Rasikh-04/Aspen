package app.aspen.server.ai

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Vendor endpoint configuration — values come from env only, never from the repo or the app. */
data class ProviderConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
) {
    companion object {
        /** Replies stay deliberately brief (docs/06 §5 "warm, brief"); small cap = cost + restraint. */
        const val DEFAULT_MAX_TOKENS = 512
    }
}

// ---- Anthropic Messages API wire shapes (moved server-side from the Phase-4 ClaudeAiClient) ----

@Serializable
internal data class AnthropicMessage(val role: String, val content: String)

@Serializable
internal data class AnthropicRequest(
    val model: String,
    val max_tokens: Int,
    val system: String,
    val messages: List<AnthropicMessage>,
)

@Serializable
internal data class AnthropicContentBlock(val type: String = "", val text: String = "")

@Serializable
internal data class AnthropicResponse(
    val content: List<AnthropicContentBlock> = emptyList(),
    val stop_reason: String? = null,
)

/**
 * Anthropic Messages API adapter (POST /v1/messages): `x-api-key` + `anthropic-version` headers,
 * system prompt as a top-level field, reply as content blocks. A `refusal` stop reason maps to
 * [ProviderResult.Unavailable] — refusal internals are never relayed.
 */
class AnthropicMessagesProvider(
    private val config: ProviderConfig,
    private val http: HttpClient,
    private val systemPrompt: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ModelProvider {

    override suspend fun complete(userText: String, history: List<ChatMessage>): ProviderResult =
        runCatching {
            val response = http.post("${config.baseUrl.trimEnd('/')}/v1/messages") {
                contentType(ContentType.Application.Json)
                header("x-api-key", config.apiKey)
                header("anthropic-version", ANTHROPIC_VERSION)
                setBody(
                    json.encodeToString(
                        AnthropicRequest.serializer(),
                        AnthropicRequest(
                            model = config.model,
                            max_tokens = config.maxTokens,
                            system = systemPrompt,
                            messages = history.map { it.toWire() } +
                                AnthropicMessage(role = "user", content = userText),
                        ),
                    ),
                )
            }
            if (!response.status.isSuccess()) return@runCatching ProviderResult.Unavailable

            val parsed = json.decodeFromString(AnthropicResponse.serializer(), response.bodyAsText())
            if (parsed.stop_reason == "refusal") return@runCatching ProviderResult.Unavailable

            parsed.content.firstOrNull { it.type == "text" && it.text.isNotBlank() }
                ?.let { ProviderResult.Reply(it.text) }
                ?: ProviderResult.Unavailable
        }.getOrDefault(ProviderResult.Unavailable)

    private fun ChatMessage.toWire() = AnthropicMessage(
        role = if (role == ChatRole.USER) "user" else "assistant",
        content = text,
    )

    private companion object {
        const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
