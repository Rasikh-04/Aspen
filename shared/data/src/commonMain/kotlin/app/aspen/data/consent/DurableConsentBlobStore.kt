package app.aspen.data.consent

import app.aspen.data.local.EncryptedBlobStore
import app.aspen.data.local.FileEncryptedBlobStore

/**
 * [ConsentBlobStore] over the durable [FileEncryptedBlobStore] (Phase 4 — closes the Phase-2
 * "in-memory only" consent leftout, docs/STATUS.md). A thin adapter, not a second implementation:
 * consent keeps its own interface (and its own blob, so corruption can't cross-contaminate) but the
 * file/crash-safety logic lives once in `data.local`.
 */
class DurableConsentBlobStore(
    private val backing: EncryptedBlobStore = FileEncryptedBlobStore(BLOB_NAME),
) : ConsentBlobStore {
    override fun load(): ByteArray? = backing.load()
    override fun save(bytes: ByteArray) = backing.save(bytes)

    private companion object {
        const val BLOB_NAME = "consent"
    }
}
