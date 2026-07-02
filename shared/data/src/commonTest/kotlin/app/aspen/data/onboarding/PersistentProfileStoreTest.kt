package app.aspen.data.onboarding

import app.aspen.data.local.InMemoryEncryptedBlobStore
import app.aspen.data.local.platformLocalCipher
import app.aspen.domain.onboarding.model.OnboardingResult
import app.aspen.domain.onboarding.model.ProtectiveFlag
import app.aspen.domain.onboarding.model.RoutingHints
import app.aspen.domain.onboarding.model.SupportProfile
import app.aspen.domain.onboarding.model.SupportRoutingStrength
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Runs on JVM against the real AES/GCM [platformLocalCipher]. */
class PersistentProfileStoreTest {

    private fun sampleResult() = OnboardingResult(
        profiles = mapOf(SupportProfile.RESTRICTION_LEANING to 2, SupportProfile.BINGE_LEANING to 1),
        dominantProfile = SupportProfile.RESTRICTION_LEANING,
        protectiveFlags = setOf(ProtectiveFlag.SUPPRESS_FOOD_LOGGING),
        routingHints = RoutingHints(SupportRoutingStrength.ELEVATED, offerTrustedContactSetup = true, emphasiseTreatmentFinder = false),
    )

    @Test
    fun roundTripsThroughEncryptedBlob() {
        // Arrange
        val store = PersistentProfileStore(platformLocalCipher(), InMemoryEncryptedBlobStore())

        // Act
        store.save(sampleResult())
        val loaded = store.current()

        // Assert
        assertEquals(sampleResult(), loaded)
    }

    @Test
    fun blobIsCiphertextNotPlaintext() {
        // Arrange
        val blob = InMemoryEncryptedBlobStore()
        val store = PersistentProfileStore(platformLocalCipher(), blob)

        // Act
        store.save(sampleResult())
        val onDisk = blob.load()!!.decodeToString()

        // Assert — the profile enum name must not appear in the stored bytes.
        assertFalse(onDisk.contains("RESTRICTION_LEANING"), "profile must be encrypted at rest")
    }

    @Test
    fun missingBlobReadsAsNull() {
        // Act / Assert
        val store = PersistentProfileStore(platformLocalCipher(), InMemoryEncryptedBlobStore())
        assertNull(store.current())
    }

    @Test
    fun corruptBlobReadsAsNullNeverThrows() {
        // Arrange — garbage bytes that can't decrypt.
        val blob = InMemoryEncryptedBlobStore("not valid ciphertext".encodeToByteArray())
        val store = PersistentProfileStore(platformLocalCipher(), blob)

        // Act / Assert — fail SAFE to "no profile".
        assertNull(store.current())
    }

    @Test
    fun clearRemovesTheProfile() {
        // Arrange
        val store = PersistentProfileStore(platformLocalCipher(), InMemoryEncryptedBlobStore())
        store.save(sampleResult())
        assertTrue(store.current() != null)

        // Act
        store.clear()

        // Assert
        assertNull(store.current())
    }
}
