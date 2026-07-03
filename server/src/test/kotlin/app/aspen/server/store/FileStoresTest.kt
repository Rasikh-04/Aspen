package app.aspen.server.store

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class FileStoresTest {

    private fun tempDir(): Path = Files.createTempDirectory("aspen-server-test")

    @Test
    fun `accounts survive a new repository instance - durable across restart`() {
        val dir = tempDir()
        val record = AccountRecord("id-1", "a@b.c", "hash", 42L)
        FileAccountRepository(dir).create(record)

        val reloaded = FileAccountRepository(dir)
        assertEquals(record, reloaded.byId("id-1"))
        assertEquals(record, reloaded.byEmail("A@B.C"))
    }

    @Test
    fun `password update and delete persist`() {
        val dir = tempDir()
        FileAccountRepository(dir).apply {
            create(AccountRecord("id-1", null, "old", 0L))
            updatePassword("id-1", "new")
        }
        assertEquals("new", FileAccountRepository(dir).byId("id-1")!!.passwordHash)

        FileAccountRepository(dir).delete("id-1")
        assertNull(FileAccountRepository(dir).byId("id-1"))
    }

    @Test
    fun `corrupt accounts file starts empty instead of crashing - fail-safe`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("accounts.json"), "{ not json at all")
        assertNull(FileAccountRepository(dir).byId("anything"))
    }

    @Test
    fun `blob round-trips verbatim and deletes`() {
        val dir = tempDir()
        val repo = FileBlobRepository(dir)
        val ciphertext = ByteArray(256) { it.toByte() }

        repo.put("acct-1", ciphertext)
        assertContentEquals(ciphertext, FileBlobRepository(dir).get("acct-1"))

        assertEquals(true, repo.delete("acct-1"))
        assertNull(repo.get("acct-1"))
        assertFalse(repo.delete("acct-1"))
    }

    @Test
    fun `blob file names cannot traverse paths`() {
        val dir = tempDir()
        val repo = FileBlobRepository(dir)
        repo.put("../../escape", byteArrayOf(1))
        // Whatever was written stayed inside the blobs dir.
        assertEquals(1, Files.list(dir.resolve("blobs")).count())
    }
}
