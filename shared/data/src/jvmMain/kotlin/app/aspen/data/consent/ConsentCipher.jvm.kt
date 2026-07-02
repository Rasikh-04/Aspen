package app.aspen.data.consent

import app.aspen.data.local.platformLocalCipher

/**
 * JVM [ConsentCipher] — delegates to the unified [app.aspen.data.local.LocalCipher] so there is one
 * audited AES/GCM implementation for all on-device data, not a consent-specific copy. The crypto
 * (process-ephemeral AES-256/GCM, test/dev grade) lives in `local/LocalCipher.jvm.kt`.
 */
actual fun platformConsentCipher(): ConsentCipher {
    val delegate = platformLocalCipher()
    return object : ConsentCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray = delegate.encrypt(plaintext)
        override fun decrypt(ciphertext: ByteArray): ByteArray = delegate.decrypt(ciphertext)
    }
}
