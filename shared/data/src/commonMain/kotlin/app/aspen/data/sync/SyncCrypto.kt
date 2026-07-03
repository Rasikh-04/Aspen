package app.aspen.data.sync

/**
 * The passphrase-based crypto seam for E2E backup (docs/08 §2). Distinct from [app.aspen.data.local.LocalCipher]
 * on purpose: local storage uses a DEVICE-held key (Keystore/Keychain), while a backup must be
 * decryptable on a *different* device — so its key derives from something the user carries
 * (passphrase or recovery code) and never exists server-side.
 *
 * Contracts: [deriveKey] is PBKDF2-HMAC-SHA256 at the OWASP-recommended iteration floor,
 * 32-byte output; [seal]/[open] are AES-256-GCM with a random nonce carried in the sealed bytes;
 * [open] returns null on a wrong key or tampered data — it never throws.
 */
interface SyncCrypto {
    fun randomBytes(count: Int): ByteArray
    fun deriveKey(secret: String, salt: ByteArray): ByteArray
    fun seal(key: ByteArray, plaintext: ByteArray): ByteArray
    fun open(key: ByteArray, sealedBytes: ByteArray): ByteArray?
}

/**
 * Null where no REAL implementation exists (iOS, until its CryptoKit actual lands — same
 * placeholder policy as the local cipher, docs/STATUS.md): a passthrough here would upload
 * PLAINTEXT, so absence disables the feature instead (PRE_SHIP §3 pattern).
 */
expect fun platformSyncCrypto(): SyncCrypto?
