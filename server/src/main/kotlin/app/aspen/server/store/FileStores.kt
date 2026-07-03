package app.aspen.server.store

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * File-backed repositories for a durable dev server (v1 — a production database replaces these
 * behind the same ports, docs/07 Phase 6.9). Writes are atomic (temp file + rename, the same
 * pattern as the app's `FileEncryptedBlobStore`); reads are fail-safe — a corrupt or unreadable
 * file starts empty rather than crashing the server.
 *
 * Sessions and recovery tokens are deliberately NOT file-backed: they are short-lived secrets,
 * and losing them on restart only means signing in again — safer than leaving them on disk.
 */

@Serializable
private data class AccountsFile(val accounts: List<StoredAccount> = emptyList())

@Serializable
private data class StoredAccount(
    val id: String,
    val email: String? = null,
    val passwordHash: String,
    val createdAtEpochMs: Long,
)

class FileAccountRepository(dataDir: Path) : AccountRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val file: Path = dataDir.resolve("accounts.json")
    private val accounts = LinkedHashMap<String, AccountRecord>()
    private val lock = Any()

    init {
        Files.createDirectories(dataDir)
        val parsed = runCatching {
            json.decodeFromString(AccountsFile.serializer(), Files.readString(file))
        }.getOrNull()
        parsed?.accounts?.forEach {
            accounts[it.id] = AccountRecord(it.id, it.email, it.passwordHash, it.createdAtEpochMs)
        }
    }

    override fun create(record: AccountRecord): Unit = synchronized(lock) {
        accounts[record.id] = record
        persist()
    }

    override fun byId(id: String): AccountRecord? = synchronized(lock) { accounts[id] }

    override fun byEmail(email: String): AccountRecord? = synchronized(lock) {
        accounts.values.firstOrNull { it.email.equals(email, ignoreCase = true) }
    }

    override fun updatePassword(id: String, newHash: String): Boolean = synchronized(lock) {
        val existing = accounts[id] ?: return false
        accounts[id] = existing.copy(passwordHash = newHash)
        persist()
        true
    }

    override fun delete(id: String): Boolean = synchronized(lock) {
        (accounts.remove(id) != null).also { if (it) persist() }
    }

    private fun persist() {
        val snapshot = AccountsFile(
            accounts.values.map { StoredAccount(it.id, it.email, it.passwordHash, it.createdAtEpochMs) },
        )
        atomicWrite(file, json.encodeToString(AccountsFile.serializer(), snapshot).encodeToByteArray())
    }
}

class FileBlobRepository(dataDir: Path) : BlobRepository {
    private val blobDir: Path = dataDir.resolve("blobs")

    init {
        Files.createDirectories(blobDir)
    }

    override fun put(accountId: String, ciphertext: ByteArray) {
        atomicWrite(blobDir.resolve(fileNameFor(accountId)), ciphertext)
    }

    override fun get(accountId: String): ByteArray? =
        runCatching { Files.readAllBytes(blobDir.resolve(fileNameFor(accountId))) }.getOrNull()

    override fun delete(accountId: String): Boolean =
        runCatching { Files.deleteIfExists(blobDir.resolve(fileNameFor(accountId))) }.getOrDefault(false)

    /** Account ids are server-generated UUIDs; sanitise anyway so no id can traverse paths. */
    private fun fileNameFor(accountId: String): String =
        accountId.filter { it.isLetterOrDigit() || it == '-' }.ifEmpty { "invalid" } + ".bin"
}

private fun atomicWrite(target: Path, bytes: ByteArray) {
    val tmp = target.resolveSibling(target.fileName.toString() + ".tmp")
    Files.write(tmp, bytes)
    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
}
