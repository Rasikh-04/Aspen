package app.aspen.data.companion

import app.aspen.data.local.InMemoryEncryptedBlobStore
import app.aspen.data.local.platformLocalCipher
import app.aspen.domain.companion.model.CompanionPrefs
import app.aspen.domain.companion.model.CompanionSpecies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Runs on JVM against the real AES/GCM [platformLocalCipher]. The safety-bearing property:
 * a failed read can only yield `null` → callers fall back to [CompanionPrefs] defaults →
 * everything OFF. Storage failure can never switch the companion (or its overlay) on.
 */
class PersistentCompanionPrefsStoreTest {

    private fun samplePrefs() = CompanionPrefs(
        enabled = true,
        species = CompanionSpecies.BUNNY,
        overlayEnabled = true,
        notificationsEnabled = false,
    )

    @Test
    fun roundTripsThroughEncryptedBlob() {
        // Arrange
        val store = PersistentCompanionPrefsStore(platformLocalCipher(), InMemoryEncryptedBlobStore())

        // Act
        store.save(samplePrefs())

        // Assert
        assertEquals(samplePrefs(), store.current())
    }

    @Test
    fun blobIsCiphertextNotPlaintext() {
        // Arrange
        val blob = InMemoryEncryptedBlobStore()
        val store = PersistentCompanionPrefsStore(platformLocalCipher(), blob)

        // Act
        store.save(samplePrefs())
        val onDisk = blob.load()!!.decodeToString()

        // Assert
        assertFalse(onDisk.contains("BUNNY"), "prefs must be encrypted at rest")
    }

    @Test
    fun missingBlobReadsAsNull() {
        val store = PersistentCompanionPrefsStore(platformLocalCipher(), InMemoryEncryptedBlobStore())
        assertNull(store.current())
    }

    @Test
    fun corruptBlobReadsAsNullNeverThrows() {
        // Arrange — garbage bytes that will fail decryption/parsing.
        val blob = InMemoryEncryptedBlobStore()
        blob.save("not-a-valid-encrypted-blob".encodeToByteArray())
        val store = PersistentCompanionPrefsStore(platformLocalCipher(), blob)

        // Act / Assert — fail-safe: unreadable → null → defaults (all off).
        assertNull(store.current())
    }

    @Test
    fun unknownSpeciesInStoredDataFailsSafeToNull() {
        // Arrange — a store that wrote a species this build doesn't know (future fast-follow pack).
        val cipher = platformLocalCipher()
        val blob = InMemoryEncryptedBlobStore()
        val futureJson = """{"enabled":true,"species":"OWL","overlayEnabled":true,"notificationsEnabled":true}"""
        blob.save(cipher.encrypt(futureJson.encodeToByteArray()))

        // Act / Assert — unknown species must not crash or guess; null → defaults (all off).
        assertNull(PersistentCompanionPrefsStore(cipher, blob).current())
    }

    @Test
    fun clearRemovesStoredPrefs() {
        // Arrange
        val store = PersistentCompanionPrefsStore(platformLocalCipher(), InMemoryEncryptedBlobStore())
        store.save(samplePrefs())

        // Act
        store.clear()

        // Assert
        assertNull(store.current())
    }
}
