package app.aspen.data.onboarding

import app.aspen.data.local.EncryptedBlobStore
import app.aspen.data.local.LocalCipher
import app.aspen.domain.onboarding.ProfileStore
import app.aspen.domain.onboarding.model.OnboardingResult
import kotlinx.serialization.json.Json

/**
 * The encrypted, fail-safe [ProfileStore] (docs/11 §1.7, docs/04 §5). The profile is serialized to
 * JSON, encrypted via [LocalCipher], and held as one opaque blob in an [EncryptedBlobStore].
 *
 * **Fail-safe:** a missing, unreadable, undecryptable, or malformed blob yields `null` — never an
 * exception — so [app.aspen.domain.onboarding.AppConfigProvider] falls back to the safest config. The
 * broad catch is the intended behaviour (a profile we can't trust = no profile = safest), not a
 * swallowed error.
 */
class PersistentProfileStore(
    private val cipher: LocalCipher,
    private val blob: EncryptedBlobStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ProfileStore {

    override fun save(result: OnboardingResult) {
        val plaintext = json.encodeToString(ProfileStateDto.serializer(), result.toDto())
        blob.save(cipher.encrypt(plaintext.encodeToByteArray()))
    }

    override fun current(): OnboardingResult? = try {
        val bytes = blob.load() ?: return null
        val plaintext = cipher.decrypt(bytes).decodeToString()
        json.decodeFromString(ProfileStateDto.serializer(), plaintext).toDomainOrNull()
    } catch (t: Throwable) {
        // Fail SAFE: anything we cannot read → no profile → safest default (see class doc).
        null
    }

    override fun clear() = blob.clear()
}
