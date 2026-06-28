package app.aspen.data.consent

/**
 * iOS [ConsentCipher] — **Phase-2 PLACEHOLDER**. This is an in-process passthrough so the iOS target
 * compiles and the encryption seam is in place; it is **NOT secure and NOT device-verified**.
 *
 * Tracked leftout (docs/STATUS.md): the real iOS actual must hold the key in the iOS Keychain and use
 * CryptoKit AES-GCM (via cinterop), mirroring the Android Keystore actual. Aspen is Android-first, so
 * no consent data is persisted on iOS in Phase 2 — but this must be replaced before any iOS release.
 */
private class PlaceholderIosConsentCipher : ConsentCipher {
    override fun encrypt(plaintext: ByteArray): ByteArray = plaintext.copyOf()
    override fun decrypt(ciphertext: ByteArray): ByteArray = ciphertext.copyOf()
}

actual fun platformConsentCipher(): ConsentCipher = PlaceholderIosConsentCipher()
