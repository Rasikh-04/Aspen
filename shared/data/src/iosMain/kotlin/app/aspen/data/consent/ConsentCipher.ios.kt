package app.aspen.data.consent

import app.aspen.data.local.platformLocalCipher

/**
 * iOS [ConsentCipher] — delegates to the unified [app.aspen.data.local.LocalCipher], which is itself a
 * **PLACEHOLDER passthrough** on iOS (NOT secure, NOT device-verified). Replacing the iOS local cipher
 * with a Keychain + CryptoKit actual fixes consent and every other on-device store at once. Tracked
 * leftout (docs/STATUS.md).
 */
actual fun platformConsentCipher(): ConsentCipher {
    val delegate = platformLocalCipher()
    return object : ConsentCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray = delegate.encrypt(plaintext)
        override fun decrypt(ciphertext: ByteArray): ByteArray = delegate.decrypt(ciphertext)
    }
}
