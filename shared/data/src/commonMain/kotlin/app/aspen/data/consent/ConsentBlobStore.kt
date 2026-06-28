package app.aspen.data.consent

/**
 * The raw, encrypted-blob persistence seam under [PersistentConsentStore]. Kept deliberately tiny —
 * one opaque ciphertext blob in, one out — so the durable backend (platform file, EncryptedShared-
 * Preferences, Keychain item) can vary without touching consent logic.
 *
 * Phase-2 note: the shipped default is [InMemoryConsentBlobStore]. A durable on-disk implementation
 * (with the platform path/Context plumbing) is a tracked leftout for the next phase; the encryption
 * seam and the fail-safe read/parse logic are what Phase 2 locks in and tests.
 */
interface ConsentBlobStore {
    /** The stored ciphertext, or null if nothing has been written yet. Must not throw. */
    fun load(): ByteArray?

    /** Replace the stored ciphertext. */
    fun save(bytes: ByteArray)
}

/** In-memory blob store — the Phase-2 default and the test double. */
class InMemoryConsentBlobStore(initial: ByteArray? = null) : ConsentBlobStore {
    private var data: ByteArray? = initial
    override fun load(): ByteArray? = data
    override fun save(bytes: ByteArray) {
        data = bytes
    }
}
