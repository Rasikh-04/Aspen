package app.aspen.data.local

import app.aspen.data.onboarding.PersistentProfileStore
import app.aspen.domain.onboarding.model.OnboardingResult
import app.aspen.domain.onboarding.model.ProtectiveFlag
import app.aspen.domain.onboarding.model.RoutingHints
import app.aspen.domain.onboarding.model.SupportProfile
import app.aspen.domain.onboarding.model.SupportRoutingStrength
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * JVM integration tests for the file-backed store UNDER a real persistent store: what actually lands
 * on disk is ciphertext, corrupt files fail safe, and the profile survives "restart" (new store
 * instances over the same file — the Phase-3 cold-start leftout this closes, docs/STATUS.md).
 */
class DurableStorePersistenceTest {

    private fun uniqueName() = "test_profile_${Random.nextLong().toULong().toString(16)}"

    private fun sampleResult() = OnboardingResult(
        profiles = mapOf(SupportProfile.RESTRICTION_LEANING to 2, SupportProfile.BINGE_LEANING to 1),
        dominantProfile = SupportProfile.RESTRICTION_LEANING,
        protectiveFlags = setOf(ProtectiveFlag.SUPPRESS_FOOD_LOGGING),
        routingHints = RoutingHints(SupportRoutingStrength.ELEVATED, offerTrustedContactSetup = true, emphasiseTreatmentFinder = false),
    )

    @Test
    fun profileSurvivesAcrossStoreInstances() {
        // Arrange — one process-stable cipher (the JVM key is process-ephemeral) but fresh
        // store + blob instances, i.e. everything short of a process restart.
        val name = uniqueName()
        val cipher = platformLocalCipher()
        try {
            // Act
            PersistentProfileStore(cipher, FileEncryptedBlobStore(name)).save(sampleResult())
            val reloaded = PersistentProfileStore(cipher, FileEncryptedBlobStore(name)).current()

            // Assert
            assertEquals(sampleResult(), reloaded)
        } finally {
            FileEncryptedBlobStore(name).clear()
        }
    }

    @Test
    fun fileOnDiskIsCiphertextNotPlaintext() {
        // Arrange
        val name = uniqueName()
        try {
            PersistentProfileStore(platformLocalCipher(), FileEncryptedBlobStore(name)).save(sampleResult())

            // Act — read the raw file exactly as an attacker with the device storage would.
            val file = JvmBlobFileIo.defaultDirectory().resolve("$name.blob")
            assertTrue(file.exists(), "blob file must exist at ${file.path}")
            val raw = file.readBytes().decodeToString()

            // Assert — no domain plaintext in the stored bytes (CLAUDE.md: encrypted local store).
            assertFalse(raw.contains("RESTRICTION_LEANING"), "profile must be encrypted at rest")
        } finally {
            FileEncryptedBlobStore(name).clear()
        }
    }

    @Test
    fun corruptFileFailsSafeToNoProfile() {
        // Arrange — garbage bytes where ciphertext should be.
        val name = uniqueName()
        try {
            val file = JvmBlobFileIo.defaultDirectory().resolve("$name.blob")
            file.parentFile?.mkdirs()
            file.writeBytes("corrupted on disk".encodeToByteArray())

            // Act / Assert — fail SAFE to "no profile" (safest default), never throw.
            assertNull(PersistentProfileStore(platformLocalCipher(), FileEncryptedBlobStore(name)).current())
        } finally {
            FileEncryptedBlobStore(name).clear()
        }
    }

    @Test
    fun writeIsAtomicNoTempFileLeftBehind() {
        // Arrange
        val name = uniqueName()
        try {
            // Act
            FileEncryptedBlobStore(name).save("bytes".encodeToByteArray())

            // Assert — the temp-then-rename write leaves no partial artefacts to corrupt a later read.
            val dir = JvmBlobFileIo.defaultDirectory()
            val leftovers = dir.listFiles().orEmpty().filter { it.name.startsWith(name) && it.name != "$name.blob" }
            assertEquals(emptyList<File>(), leftovers)
        } finally {
            FileEncryptedBlobStore(name).clear()
        }
    }

    @Test
    fun blobNamesAreValidatedAgainstPathTraversal() {
        // Act / Assert — a name is a logical id, never a path (defence for the platform actuals).
        val outcome = runCatching { FileEncryptedBlobStore("../escape") }
        assertTrue(outcome.isFailure, "path-like blob names must be rejected")
    }
}
