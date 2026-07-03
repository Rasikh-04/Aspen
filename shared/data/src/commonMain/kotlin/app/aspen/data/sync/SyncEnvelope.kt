package app.aspen.data.sync

import app.aspen.data.local.EncryptedBlobStore
import app.aspen.data.local.LocalCipher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The ciphertext blob the server stores (docs/08 §2). The random data key K seals the payload;
 * K itself travels ONLY wrapped — once under the passphrase-derived key, once under the
 * recovery-code-derived key — so either secret opens the backup and neither is derivable from
 * the blob. Salts are public by design. Everything the server sees is this envelope's JSON:
 * no field of it is readable without one of the user's two secrets.
 */
@OptIn(ExperimentalEncodingApi::class)
@Serializable
internal data class SyncEnvelope(
    val version: Int = 1,
    val passSalt: String,
    val recoverySalt: String,
    val keyByPass: String,
    val keyByRecovery: String,
    val payload: String,
) {
    companion object {
        fun encode(bytes: ByteArray): String = Base64.encode(bytes)
        fun decode(text: String): ByteArray = Base64.decode(text)
    }
}

/** The header alone (no payload) — persisted locally so later backups reuse the same wrapping. */
@Serializable
internal data class SyncKeyMeta(
    val passSalt: String,
    val recoverySalt: String,
    val keyByPass: String,
    val keyByRecovery: String,
)

/**
 * One-time recovery code: ~100 bits, Crockford-style alphabet (no I/L/O/U — nothing a tired
 * hand misreads), shown as XXXXX-XXXXX-XXXXX-XXXXX. [normalize] forgives case, spaces, dashes.
 */
internal object RecoveryCode {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val LENGTH = 20

    fun generate(crypto: SyncCrypto): String =
        crypto.randomBytes(LENGTH)
            .map { ALPHABET[it.toInt() and 0x1F] }
            .joinToString("")
            .chunked(5)
            .joinToString("-")

    fun normalize(secret: String): String = secret.uppercase().filter { it in ALPHABET }
}

/**
 * What gets backed up: the user's written CONTENT — profile, logs, AI history — as the same
 * plaintext JSON the persistent stores already read/write (schema-stable via `ignoreUnknownKeys`).
 * Device-scoped state (consent grants, sessions, companion/language prefs) stays out on purpose.
 * Export decrypts each blob with the DEVICE key; import re-encrypts with THIS device's key.
 */
class LocalStoreBundle(
    private val cipher: LocalCipher,
    private val stores: Map<String, EncryptedBlobStore>,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    @Serializable
    private data class Bundle(val entries: Map<String, String>)

    @OptIn(ExperimentalEncodingApi::class)
    fun exportPlaintext(): ByteArray {
        val entries = stores.mapNotNull { (name, blob) ->
            val plaintext = runCatching { blob.load()?.let(cipher::decrypt) }.getOrNull()
            plaintext?.let { name to Base64.encode(it) }
        }.toMap()
        return json.encodeToString(Bundle.serializer(), Bundle(entries)).encodeToByteArray()
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun importPlaintext(bytes: ByteArray) {
        val bundle = json.decodeFromString(Bundle.serializer(), bytes.decodeToString())
        bundle.entries.forEach { (name, encoded) ->
            stores[name]?.save(cipher.encrypt(Base64.decode(encoded)))
        }
    }

    companion object {
        /** The backed-up content blobs (names = the logical ids used across the app since P3/P4). */
        val CONTENT_STORE_NAMES = listOf("profile", "logs", "ai_messages")
    }
}
