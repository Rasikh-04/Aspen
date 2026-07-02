package app.aspen.data.local

/**
 * Raw byte-file IO under the durable blob store — the ONLY platform-varying part, kept tiny so the
 * fail-safe logic in [FileEncryptedBlobStore] is written and tested once in common code. Actuals:
 * JVM = app-scoped temp dir (dev/test grade, pairs with the process-ephemeral JVM cipher),
 * Android = `Context.filesDir` (private app storage; requires [app.aspen.data.local] init at app
 * start), iOS = Application Support directory.
 *
 * Contract for actuals: [read] returns null for a missing file; [write] must be atomic
 * (temp-then-rename / `atomically:` write) so a crash mid-write can never leave a half-written blob
 * where good data was; [delete] removes the file. Callers ([FileEncryptedBlobStore]) catch, so
 * actuals may throw on genuine IO failure.
 */
interface BlobFileIo {
    fun read(name: String): ByteArray?
    fun write(name: String, bytes: ByteArray)
    fun delete(name: String)
}

/** Platform-provided durable byte-file IO. */
expect fun platformBlobFileIo(): BlobFileIo

/**
 * The durable, file-backed [EncryptedBlobStore] (docs/04 §5) — closes the Phase-2/3 in-memory
 * leftout, so the profile/logs/consent survive a cold start. Stores only the ciphertext produced by
 * [LocalCipher]; encryption stays the callers' concern, exactly as with [InMemoryEncryptedBlobStore].
 *
 * Fail-safe by contract: [load] and [clear] never throw — an unreadable file reads as "nothing
 * stored", which every persistent store above already treats as the safest default. [save] may throw
 * (a failed write must not be silently swallowed as success).
 *
 * [name] is a logical id, never a path: one store per name (profile, logs, consent, ai_messages) so
 * a corrupt blob can't cross-contaminate.
 */
class FileEncryptedBlobStore(
    private val name: String,
    private val io: BlobFileIo = platformBlobFileIo(),
) : EncryptedBlobStore {

    init {
        require(name.isNotEmpty() && name.all { it.isLetterOrDigit() || it == '_' }) {
            "blob name must be a simple identifier (letters/digits/underscore), got: $name"
        }
    }

    override fun load(): ByteArray? = runCatching { io.read(name) }.getOrNull()

    override fun save(bytes: ByteArray) = io.write(name, bytes)

    override fun clear() {
        runCatching { io.delete(name) }
    }
}
