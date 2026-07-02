package app.aspen.data.ai.cloud

import app.aspen.domain.ai.AiClient
import app.aspen.domain.ai.AiClientResult
import app.aspen.domain.ai.AiMessage
import app.aspen.domain.ai.AiRole
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

/**
 * Injected Tier-2 endpoint configuration. **No credential exists anywhere in this repository**
 * (CLAUDE.md security rule): [authToken] is a provider the platform binds at runtime, and the
 * shipped default binding is [DisabledAiClient] — the cloud tier is compiled and tested but NOT
 * live-wired until the endpoint/proxy decision is made (docs/PRE_SHIP_VERIFICATION.md).
 */
data class AiEndpointConfig(
    val baseUrl: String,
    val authToken: () -> String?,
    val model: String = DEFAULT_MODEL,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
) {
    companion object {
        /** Current default per Anthropic guidance; revisit at live wiring. */
        const val DEFAULT_MODEL = "claude-opus-4-8"

        /** Replies are deliberately brief (docs/06 §5 "warm, brief"); small cap = cost + restraint. */
        const val DEFAULT_MAX_TOKENS = 512
    }
}

/** The shipped default: cloud tier compiled but off — provably never touches the network. */
object DisabledAiClient : AiClient {
    override suspend fun reply(userText: String, history: List<AiMessage>): AiClientResult =
        AiClientResult.Disabled
}

// ---- Anthropic Messages API wire shapes (api.anthropic.com/v1/messages) ----

@Serializable
internal data class WireMessage(val role: String, val content: String)

@Serializable
internal data class MessagesRequest(
    val model: String,
    val max_tokens: Int,
    val system: String,
    val messages: List<WireMessage>,
)

@Serializable
internal data class WireContentBlock(val type: String = "", val text: String = "")

@Serializable
internal data class MessagesResponse(
    val content: List<WireContentBlock> = emptyList(),
    val stop_reason: String? = null,
)

/**
 * Ktor implementation of the Tier-2 [AiClient] against the Anthropic Messages API shape
 * (POST /v1/messages; raw HTTP because the official SDK doesn't target KMP common code).
 *
 * Contracts (docs/04 ADR-003): on-demand only — one POST per [reply], nothing in the background;
 * missing auth/config → [AiClientResult.Disabled] without any network call; every transport or
 * API failure → [AiClientResult.Unavailable] (calm degradation, never an exception upward); a
 * `refusal` stop reason → [AiClientResult.Unavailable] (never surface refusal internals). The raw
 * reply text is returned for [app.aspen.domain.ai.ReflectionCompanion] to guard — this class never
 * decides what the user sees.
 */
class ClaudeAiClient(
    private val config: AiEndpointConfig,
    private val http: HttpClient,
    private val systemPrompt: String = ReflectionSystemPrompt.text,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AiClient {

    override suspend fun reply(userText: String, history: List<AiMessage>): AiClientResult {
        val token = config.authToken() ?: return AiClientResult.Disabled
        if (config.baseUrl.isBlank()) return AiClientResult.Disabled

        return runCatching {
            val response = http.post("${config.baseUrl.trimEnd('/')}/v1/messages") {
                contentType(ContentType.Application.Json)
                header("x-api-key", token)
                header("anthropic-version", ANTHROPIC_VERSION)
                setBody(
                    json.encodeToString(
                        MessagesRequest.serializer(),
                        MessagesRequest(
                            model = config.model,
                            max_tokens = config.maxTokens,
                            system = systemPrompt,
                            messages = history.map { it.toWire() } + WireMessage(role = "user", content = userText),
                        ),
                    ),
                )
            }
            if (!response.status.isSuccess()) return@runCatching AiClientResult.Unavailable

            val parsed = json.decodeFromString(MessagesResponse.serializer(), response.bodyAsText())
            if (parsed.stop_reason == "refusal") return@runCatching AiClientResult.Unavailable

            parsed.content.firstOrNull { it.type == "text" && it.text.isNotBlank() }
                ?.let { AiClientResult.Reply(it.text) }
                ?: AiClientResult.Unavailable
        }.getOrElse { AiClientResult.Unavailable }
    }

    private fun AiMessage.toWire() = WireMessage(
        role = if (role == AiRole.USER) "user" else "assistant",
        content = text,
    )

    private companion object {
        const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
