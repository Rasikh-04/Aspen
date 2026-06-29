package app.aspen.data.consent

import app.aspen.data.local.platformLocalCipher

/**
 * Android [ConsentCipher] — delegates to the unified [app.aspen.data.local.LocalCipher] (AES-256/GCM
 * via the AndroidKeyStore) so consent shares the single audited, key-backed crypto with the rest of
 * the on-device data. The implementation lives in `local/LocalCipher.android.kt`.
 */
actual fun platformConsentCipher(): ConsentCipher {
    val delegate = platformLocalCipher()
    return object : ConsentCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray = delegate.encrypt(plaintext)
        override fun decrypt(ciphertext: ByteArray): ByteArray = delegate.decrypt(ciphertext)
    }
}
