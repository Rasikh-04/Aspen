package app.aspen.data.ai.cloud

import app.aspen.api.ApiChatMessage
import app.aspen.api.AspenApi
import app.aspen.api.ReflectRequest
import app.aspen.api.ReflectResponse
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
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/** The shipped default: cloud tier compiled but off — provably never touches the network. */
object DisabledAiClient : AiClient {
    override suspend fun reply(userText: String, history: List<AiMessage>): AiClientResult =
        AiClientResult.Disabled
}

/**
 * The Tier-2 [AiClient] over the **Aspen server's stateless relay** (docs/00 decision #11) —
 * replaces the Phase-4 direct-from-device `ClaudeAiClient`. The device holds NO vendor key and
 * knows NO vendor shape: which model answers is server env config (any model behind the
 * Anthropic or OpenAI-compatible adapter). The on-device pipeline around this client
 * (consent → crisis check → output guard, [app.aspen.domain.ai.ReflectionCompanion]) is unchanged.
 *
 * Contracts: on-demand only (one POST per [reply]); no server URL or no signed-in session →
 * [AiClientResult.Disabled] without any network call; every transport/API failure and expired
 * session → [AiClientResult.Unavailable] (calm degradation, never an exception upward).
 */
class AspenServerAiClient(
    private val baseUrl: String?,
    private val sessionToken: () -> String?,
    private val http: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AiClient {

    override suspend fun reply(userText: String, history: List<AiMessage>): AiClientResult {
        val base = baseUrl?.trimEnd('/')?.takeIf { it.isNotBlank() } ?: return AiClientResult.Disabled
        val token = sessionToken() ?: return AiClientResult.Disabled

        return runCatching {
            val response = http.post(base + AspenApi.AI_REFLECT) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    json.encodeToString(
                        ReflectRequest.serializer(),
                        ReflectRequest(text = userText, history = history.map { it.toWire() }),
                    ),
                )
            }
            if (!response.status.isSuccess()) return@runCatching AiClientResult.Unavailable

            val parsed = json.decodeFromString(ReflectResponse.serializer(), response.bodyAsText())
            val text = parsed.text
            if (parsed.status == ReflectResponse.STATUS_REPLY && !text.isNullOrBlank()) {
                AiClientResult.Reply(text)
            } else {
                AiClientResult.Unavailable
            }
        }.getOrElse { AiClientResult.Unavailable }
    }

    private fun AiMessage.toWire() = ApiChatMessage(
        role = if (role == AiRole.USER) ApiChatMessage.ROLE_USER else ApiChatMessage.ROLE_COMPANION,
        text = text,
    )
}
