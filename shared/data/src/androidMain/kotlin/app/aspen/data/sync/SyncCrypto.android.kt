package app.aspen.data.sync

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

actual fun platformSyncCrypto(): SyncCrypto? = AndroidSyncCrypto

/** JDK-crypto implementation (identical to the jvmMain actual — javax.crypto on both). */
internal object AndroidSyncCrypto : SyncCrypto {

    private const val ITERATIONS = 600_000
    private const val KEY_BITS = 256
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128

    private val random = SecureRandom()

    override fun randomBytes(count: Int): ByteArray = ByteArray(count).also(random::nextBytes)

    override fun deriveKey(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    override fun seal(key: ByteArray, plaintext: ByteArray): ByteArray {
        val nonce = randomBytes(NONCE_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        return nonce + cipher.doFinal(plaintext)
    }

    override fun open(key: ByteArray, sealedBytes: ByteArray): ByteArray? = runCatching {
        require(sealedBytes.size > NONCE_BYTES)
        val nonce = sealedBytes.copyOfRange(0, NONCE_BYTES)
        val body = sealedBytes.copyOfRange(NONCE_BYTES, sealedBytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.doFinal(body)
    }.getOrNull()
}
