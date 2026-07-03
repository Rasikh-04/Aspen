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

// ---- OpenAI-compatible Chat Completions wire shapes ----

@Serializable
internal data class OpenAiMessage(val role: String, val content: String)

@Serializable
internal data class OpenAiRequest(
    val model: String,
    val max_tokens: Int,
    val messages: List<OpenAiMessage>,
)

@Serializable
internal data class OpenAiChoice(val message: OpenAiMessage? = null, val finish_reason: String? = null)

@Serializable
internal data class OpenAiResponse(val choices: List<OpenAiChoice> = emptyList())

/**
 * Adapter for the OpenAI-compatible Chat Completions shape (POST /v1/chat/completions) — the de
 * facto interchange format also exposed by Gemini, Mistral, Groq, Together, OpenRouter, and
 * self-hosted Ollama/vLLM. Differences from Anthropic are exactly adapter-sized: `Authorization:
 * Bearer` auth, the system prompt as a leading `system`-role message, and the reply at
 * `choices[0].message.content`. A `content_filter` finish reason maps to
 * [ProviderResult.Unavailable] — filter internals are never relayed.
 */
class OpenAiCompatProvider(
    private val config: ProviderConfig,
    private val http: HttpClient,
    private val systemPrompt: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ModelProvider {

    override suspend fun complete(userText: String, history: List<ChatMessage>): ProviderResult =
        runCatching {
            val response = http.post("${config.baseUrl.trimEnd('/')}/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${config.apiKey}")
                setBody(
                    json.encodeToString(
                        OpenAiRequest.serializer(),
                        OpenAiRequest(
                            model = config.model,
                            max_tokens = config.maxTokens,
                            messages = listOf(OpenAiMessage(role = "system", content = systemPrompt)) +
                                history.map { it.toWire() } +
                                OpenAiMessage(role = "user", content = userText),
                        ),
                    ),
                )
            }
            if (!response.status.isSuccess()) return@runCatching ProviderResult.Unavailable

            val parsed = json.decodeFromString(OpenAiResponse.serializer(), response.bodyAsText())
            val choice = parsed.choices.firstOrNull() ?: return@runCatching ProviderResult.Unavailable
            if (choice.finish_reason == "content_filter") return@runCatching ProviderResult.Unavailable

            choice.message?.content?.takeIf { it.isNotBlank() }
                ?.let { ProviderResult.Reply(it) }
                ?: ProviderResult.Unavailable
        }.getOrDefault(ProviderResult.Unavailable)

    private fun ChatMessage.toWire() = OpenAiMessage(
        role = if (role == ChatRole.USER) "user" else "assistant",
        content = text,
    )
}
