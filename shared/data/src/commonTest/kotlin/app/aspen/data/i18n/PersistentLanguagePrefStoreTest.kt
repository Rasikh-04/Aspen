package app.aspen.data.i18n

import app.aspen.core.i18n.SupportedLanguage
import app.aspen.data.local.InMemoryEncryptedBlobStore
import app.aspen.data.local.platformLocalCipher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Runs on JVM against the real AES/GCM [platformLocalCipher]. The property that matters:
 * a failed read can only yield `null` → the UI falls back to following the device language
 * (docs/12 §4). A storage fault can never pin the app to a wrong language.
 */
class PersistentLanguagePrefStoreTest {

    @Test
    fun roundTripsThroughEncryptedBlob() {
        // Arrange
        val store = PersistentLanguagePrefStore(platformLocalCipher(), InMemoryEncryptedBlobStore())

        // Act
        store.save(SupportedLanguage.UR)

        // Assert
        assertEquals(SupportedLanguage.UR, store.current())
    }

    @Test
    fun blobIsCiphertextNotPlaintext() {
        // Arrange
        val blob = InMemoryEncryptedBlobStore()
        val store = PersistentLanguagePrefStore(platformLocalCipher(), blob)

        // Act
        store.save(SupportedLanguage.UR)
        val onDisk = blob.load()!!.decodeToString()

        // Assert — the JSON key must not be readable at rest ("ur" alone is too short to be
        // collision-safe against random ciphertext bytes).
        assertFalse(onDisk.contains("code"), "choice must be encrypted at rest")
    }

    @Test
    fun missingBlobReadsAsNull() {
        val store = PersistentLanguagePrefStore(platformLocalCipher(), InMemoryEncryptedBlobStore())
        assertNull(store.current())
    }

    @Test
    fun corruptBlobReadsAsNullNeverThrows() {
        // Arrange — garbage bytes that will fail decryption/parsing.
        val blob = InMemoryEncryptedBlobStore()
        blob.save("not-a-valid-encrypted-blob".encodeToByteArray())
        val store = PersistentLanguagePrefStore(platformLocalCipher(), blob)

        // Act / Assert — fail-safe: unreadable → null → follow the device.
        assertNull(store.current())
    }

    @Test
    fun unknownLanguageCodeFailsSafeToNull() {
        // Arrange — a store written by a build that supported a language this one doesn't.
        val cipher = platformLocalCipher()
        val blob = InMemoryEncryptedBlobStore()
        blob.save(cipher.encrypt("""{"code":"tlh"}""".encodeToByteArray()))

        // Act / Assert — unknown code must not crash or guess; null → follow the device.
        assertNull(PersistentLanguagePrefStore(cipher, blob).current())
    }

    @Test
    fun clearRemovesStoredChoice() {
        // Arrange
        val store = PersistentLanguagePrefStore(platformLocalCipher(), InMemoryEncryptedBlobStore())
        store.save(SupportedLanguage.EN)

        // Act
        store.clear()

        // Assert
        assertNull(store.current())
    }
}
