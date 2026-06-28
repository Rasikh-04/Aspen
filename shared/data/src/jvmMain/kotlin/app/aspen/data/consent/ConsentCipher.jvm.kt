package app.aspen.data.consent

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * JVM [ConsentCipher]: real AES-256/GCM, but with a process-EPHEMERAL key (test/dev grade — the key
 * is not persisted, so ciphertext is only readable within the same process by the same instance).
 * This is what the JVM unit tests exercise; production durable keys are platform-specific (Keystore/
 * Keychain) and a tracked Phase-2 leftout.
 */
private class JvmConsentCipher : ConsentCipher {
    private val key: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val rng = SecureRandom()

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val iv = ByteArray(IV_BYTES).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return iv + cipher.doFinal(plaintext)
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        val iv = ciphertext.copyOfRange(0, IV_BYTES)
        val body = ciphertext.copyOfRange(IV_BYTES, ciphertext.size)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(body)
    }

    private companion object {
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}

actual fun platformConsentCipher(): ConsentCipher = JvmConsentCipher()
