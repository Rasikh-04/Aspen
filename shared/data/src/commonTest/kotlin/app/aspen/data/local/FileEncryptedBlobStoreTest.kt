package app.aspen.data.local

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

/**
 * Contract tests for the durable, file-backed [EncryptedBlobStore] (closes the Phase-2/3 in-memory
 * leftout — docs/STATUS.md). Runs on every target through [platformBlobFileIo], so each platform's
 * file IO honours the same fail-safe contract the in-memory default established:
 * load/clear never throw; a fresh instance over the same name sees what was saved (real durability).
 */
class FileEncryptedBlobStoreTest {

    private fun uniqueName() = "test_blob_${Random.nextLong().toULong().toString(16)}"

    @Test
    fun missingFileLoadsAsNull() {
        // Arrange
        val store = FileEncryptedBlobStore(uniqueName())

        // Act / Assert
        assertNull(store.load())
    }

    @Test
    fun roundTripsAcrossStoreInstances() {
        // Arrange — two independent instances over the same logical name = durability, not memory.
        val name = uniqueName()
        val bytes = "opaque ciphertext bytes".encodeToByteArray()
        try {
            // Act
            FileEncryptedBlobStore(name).save(bytes)
            val reloaded = FileEncryptedBlobStore(name).load()

            // Assert
            assertContentEquals(bytes, reloaded)
        } finally {
            FileEncryptedBlobStore(name).clear()
        }
    }

    @Test
    fun saveOverwritesPreviousBlob() {
        // Arrange
        val name = uniqueName()
        try {
            FileEncryptedBlobStore(name).save("first".encodeToByteArray())

            // Act
            FileEncryptedBlobStore(name).save("second".encodeToByteArray())

            // Assert
            assertContentEquals("second".encodeToByteArray(), FileEncryptedBlobStore(name).load())
        } finally {
            FileEncryptedBlobStore(name).clear()
        }
    }

    @Test
    fun clearRemovesTheBlobDurably() {
        // Arrange
        val name = uniqueName()
        FileEncryptedBlobStore(name).save("to be deleted".encodeToByteArray())

        // Act — FR-11: delete means delete, visible to a fresh instance.
        FileEncryptedBlobStore(name).clear()

        // Assert
        assertNull(FileEncryptedBlobStore(name).load())
    }

    @Test
    fun clearOnMissingBlobIsANoOpNeverThrows() {
        // Act / Assert — must not throw.
        FileEncryptedBlobStore(uniqueName()).clear()
    }

    @Test
    fun loadFailsSafeToNullWhenIoThrows() {
        // Arrange — an IO backend that always fails (worst-case platform behaviour).
        val store = FileEncryptedBlobStore("any", ThrowingBlobFileIo)

        // Act / Assert — fail SAFE: unreadable = "nothing stored", never an exception upward.
        assertNull(store.load())
    }

    @Test
    fun clearFailsSafeWhenIoThrows() {
        // Act / Assert — must not throw even when the backend does.
        FileEncryptedBlobStore("any", ThrowingBlobFileIo).clear()
    }

    private object ThrowingBlobFileIo : BlobFileIo {
        override fun read(name: String): ByteArray? = throw IllegalStateException("io failure")
        override fun write(name: String, bytes: ByteArray) = throw IllegalStateException("io failure")
        override fun delete(name: String) = throw IllegalStateException("io failure")
    }
}
