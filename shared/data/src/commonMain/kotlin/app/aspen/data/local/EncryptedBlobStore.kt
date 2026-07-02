package app.aspen.data.local

/**
 * The raw, encrypted-blob persistence seam for an on-device store (docs/04 §5). Deliberately tiny —
 * one opaque ciphertext blob in, one out — so the durable backend (platform file,
 * EncryptedSharedPreferences, Keychain item) can vary without touching store logic. One instance backs
 * one logical store (profile, logs, …); keep them separate so a corrupt blob can't cross-contaminate.
 *
 * The shipped default is the durable [FileEncryptedBlobStore] (Phase 4); [InMemoryEncryptedBlobStore]
 * remains as the test double.
 */
interface EncryptedBlobStore {
    /** The stored ciphertext, or null if nothing has been written yet. Must not throw. */
    fun load(): ByteArray?

    /** Replace the stored ciphertext. */
    fun save(bytes: ByteArray)

    /** Permanently remove the blob (FR-11 "delete means delete"). Must not throw. */
    fun clear()
}

/** In-memory blob store — the test double (the shipped default is [FileEncryptedBlobStore]). */
class InMemoryEncryptedBlobStore(initial: ByteArray? = null) : EncryptedBlobStore {
    private var data: ByteArray? = initial
    override fun load(): ByteArray? = data
    override fun save(bytes: ByteArray) {
        data = bytes
    }
    override fun clear() {
        data = null
    }
}
