package app.aspen.data.local

/**
 * The single encryption seam for **all on-device local data** (docs/04 §5, docs/08 §2): the support
 * profile, reflections, and numberless logs — alongside consent, which delegates here. Local data
 * uses device/user-held keys (Android Keystore, iOS Keychain), so it is persisted only as ciphertext
 * the platform key store unlocks, never as plaintext.
 *
 * Defined as a common interface with an `expect` factory so all store logic stays platform-free and
 * unit-testable, while each platform binds real key-backed crypto in one audited place (no duplicated
 * cryptography across stores).
 *
 * Contract: [encrypt] then [decrypt] round-trips to the original bytes within a process. Callers treat
 * any thrown error as "cannot read" and fail SAFE (empty → safest default) — see the persistent stores.
 */
interface LocalCipher {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray): ByteArray
}

/**
 * Platform-provided local cipher. Actuals: JVM = AES/GCM with an in-process key (test/dev grade);
 * Android = AES/GCM via the AndroidKeyStore; iOS = passthrough placeholder pending a Keychain +
 * CryptoKit actual. None of the device actuals are hardware-verified yet — a tracked leftout
 * (docs/STATUS.md).
 */
expect fun platformLocalCipher(): LocalCipher
