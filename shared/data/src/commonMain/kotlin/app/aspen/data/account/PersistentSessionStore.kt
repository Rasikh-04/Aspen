package app.aspen.data.account

import app.aspen.data.local.EncryptedBlobStore
import app.aspen.data.local.LocalCipher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The locally-held session: the account id + the opaque bearer token. Never the password. */
@Serializable
data class StoredSession(val accountId: String, val token: String)

/**
 * Encrypted, fail-safe persistence for the optional account session (same pattern as
 * [app.aspen.data.i18n.PersistentLanguagePrefStore]). **Fail-safe:** anything unreadable → `null`
 * → signed out — a storage fault can only ever sign the user OUT, never in.
 */
class PersistentSessionStore(
    private val cipher: LocalCipher,
    private val blob: EncryptedBlobStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun save(session: StoredSession) {
        val plaintext = json.encodeToString(StoredSession.serializer(), session)
        blob.save(cipher.encrypt(plaintext.encodeToByteArray()))
    }

    fun current(): StoredSession? = try {
        val bytes = blob.load() ?: return null
        json.decodeFromString(StoredSession.serializer(), cipher.decrypt(bytes).decodeToString())
    } catch (t: Throwable) {
        null
    }

    fun clear() = blob.clear()
}
