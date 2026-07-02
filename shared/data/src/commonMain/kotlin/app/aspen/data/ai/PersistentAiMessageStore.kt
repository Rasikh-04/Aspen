package app.aspen.data.ai

import app.aspen.data.local.EncryptedBlobStore
import app.aspen.data.local.LocalCipher
import app.aspen.domain.ai.AiMessage
import app.aspen.domain.ai.AiMessageStore
import app.aspen.domain.ai.AiRole
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** JSON shape of one stored AI-conversation line (docs/04 §5 `ai_messages`). */
@Serializable
internal data class AiMessageDto(
    val id: String,
    val role: String,
    val text: String,
    val createdAtEpochMs: Long,
)

@Serializable
internal data class AiStateDto(val messages: List<AiMessageDto> = emptyList())

internal fun AiMessage.toDto() = AiMessageDto(
    id = id,
    role = role.name,
    text = text,
    createdAtEpochMs = createdAt.toEpochMilliseconds(),
)

internal fun AiMessageDto.toDomainOrNull(): AiMessage? {
    val parsedRole = AiRole.entries.firstOrNull { it.name == role } ?: return null
    return AiMessage(
        id = id,
        role = parsedRole,
        text = text,
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMs),
    )
}

/**
 * The encrypted, fail-safe [AiMessageStore] (docs/04 §5): exists only for the opt-in cloud tier.
 * Same contracts as every local store — unreadable/corrupt blob reads as EMPTY (never throws), and
 * delete means delete ([clearAll] drops the blob, FR-11). Consent gating is NOT here — it lives in
 * [app.aspen.domain.ai.ReflectionCompanion], the single pipeline.
 */
class PersistentAiMessageStore(
    private val cipher: LocalCipher,
    private val blob: EncryptedBlobStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AiMessageStore {

    override fun append(message: AiMessage) {
        val current = load()
        val next = current.copy(messages = current.messages + message.toDto())
        blob.save(cipher.encrypt(json.encodeToString(AiStateDto.serializer(), next).encodeToByteArray()))
    }

    override fun history(): List<AiMessage> = load().messages.mapNotNull { it.toDomainOrNull() }

    override fun clearAll() = blob.clear()

    private fun load(): AiStateDto = try {
        val bytes = blob.load() ?: return AiStateDto()
        json.decodeFromString(AiStateDto.serializer(), cipher.decrypt(bytes).decodeToString())
    } catch (t: Throwable) {
        AiStateDto() // Fail SAFE: unreadable → empty (docs/04 §5).
    }
}
