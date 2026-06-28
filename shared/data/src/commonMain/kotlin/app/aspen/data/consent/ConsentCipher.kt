package app.aspen.data.consent

/**
 * The encryption seam for consent state (docs/08 §2, docs/09 §3.2). Consent is sensitive, so it is
 * persisted as ciphertext the platform key store unlocks — Android Keystore, iOS Keychain — never as
 * plaintext. Defined as a common interface with an `expect` factory so the domain/store logic stays
 * platform-free and testable, while each platform binds real key-backed crypto.
 *
 * Contract: [encrypt] then [decrypt] round-trips to the original bytes within a process. Callers
 * treat any thrown error as "cannot read" and fail SAFE (deny) — see [PersistentConsentStore].
 */
interface ConsentCipher {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray): ByteArray
}

/**
 * Platform-provided cipher. Actuals: JVM = AES/GCM with an in-process key (test/dev grade); Android =
 * AES/GCM via the AndroidKeyStore; iOS = Keychain-held key (placeholder pending device verification).
 * None of the device actuals are hardware-verified yet — that is a tracked Phase-2 leftout.
 */
expect fun platformConsentCipher(): ConsentCipher
