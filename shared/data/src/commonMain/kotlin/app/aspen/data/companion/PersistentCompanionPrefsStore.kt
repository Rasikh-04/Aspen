package app.aspen.data.companion

import app.aspen.data.local.EncryptedBlobStore
import app.aspen.data.local.LocalCipher
import app.aspen.domain.companion.CompanionPrefsStore
import app.aspen.domain.companion.model.CompanionPrefs
import app.aspen.domain.companion.model.CompanionSpecies
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * DTO for [CompanionPrefs]. Species travels as a string so a build that doesn't know a stored
 * species (e.g. after a downgrade) fails safe as a whole rather than guessing a different animal.
 */
@Serializable
internal data class CompanionPrefsDto(
    val enabled: Boolean = false,
    val species: String = CompanionSpecies.ASPEN_SPRITE.name,
    val overlayEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
)

internal fun CompanionPrefs.toDto() = CompanionPrefsDto(
    enabled = enabled,
    species = species.name,
    overlayEnabled = overlayEnabled,
    notificationsEnabled = notificationsEnabled,
)

internal fun CompanionPrefsDto.toDomainOrNull(): CompanionPrefs? {
    val parsedSpecies = CompanionSpecies.entries.firstOrNull { it.name == species } ?: return null
    return CompanionPrefs(
        enabled = enabled,
        species = parsedSpecies,
        overlayEnabled = overlayEnabled,
        notificationsEnabled = notificationsEnabled,
    )
}

/**
 * The encrypted, fail-safe [CompanionPrefsStore] (same pattern as
 * [app.aspen.data.onboarding.PersistentProfileStore]).
 *
 * **Fail-safe:** anything unreadable → `null` → [CompanionPrefs] defaults → companion, overlay and
 * notifications all OFF (docs/05 §3.1). A storage fault can only make the companion quieter.
 */
class PersistentCompanionPrefsStore(
    private val cipher: LocalCipher,
    private val blob: EncryptedBlobStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CompanionPrefsStore {

    override fun save(prefs: CompanionPrefs) {
        val plaintext = json.encodeToString(CompanionPrefsDto.serializer(), prefs.toDto())
        blob.save(cipher.encrypt(plaintext.encodeToByteArray()))
    }

    override fun current(): CompanionPrefs? = try {
        val bytes = blob.load() ?: return null
        val plaintext = cipher.decrypt(bytes).decodeToString()
        json.decodeFromString(CompanionPrefsDto.serializer(), plaintext).toDomainOrNull()
    } catch (t: Throwable) {
        // Fail SAFE: unreadable prefs = no prefs = everything off (see class doc).
        null
    }

    override fun clear() = blob.clear()
}
