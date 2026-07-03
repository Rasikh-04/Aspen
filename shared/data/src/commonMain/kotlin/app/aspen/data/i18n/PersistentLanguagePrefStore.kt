package app.aspen.data.i18n

import app.aspen.core.i18n.SupportedLanguage
import app.aspen.data.local.EncryptedBlobStore
import app.aspen.data.local.LocalCipher
import app.aspen.domain.i18n.LanguagePrefStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * DTO for the language choice. The language travels as its BCP-47 primary subtag so a build that
 * doesn't know a stored language (e.g. after a downgrade) fails safe to "follow the device"
 * rather than guessing a different language.
 */
@Serializable
internal data class LanguagePrefDto(val code: String)

/**
 * The encrypted, fail-safe [LanguagePrefStore] (same pattern as
 * [app.aspen.data.companion.PersistentCompanionPrefsStore]).
 *
 * **Fail-safe:** anything unreadable → `null` → the UI follows the device language (docs/12 §4).
 */
class PersistentLanguagePrefStore(
    private val cipher: LocalCipher,
    private val blob: EncryptedBlobStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : LanguagePrefStore {

    override fun save(language: SupportedLanguage) {
        val plaintext = json.encodeToString(LanguagePrefDto.serializer(), LanguagePrefDto(language.code))
        blob.save(cipher.encrypt(plaintext.encodeToByteArray()))
    }

    override fun current(): SupportedLanguage? = try {
        val bytes = blob.load() ?: return null
        val plaintext = cipher.decrypt(bytes).decodeToString()
        SupportedLanguage.fromCode(json.decodeFromString(LanguagePrefDto.serializer(), plaintext).code)
    } catch (t: Throwable) {
        // Fail SAFE: unreadable choice = no choice = follow the device (see class doc).
        null
    }

    override fun clear() = blob.clear()
}
