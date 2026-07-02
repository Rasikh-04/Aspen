package app.aspen.data.logging

import app.aspen.data.local.InMemoryEncryptedBlobStore
import app.aspen.data.local.platformLocalCipher
import app.aspen.domain.logging.model.BehaviourLog
import app.aspen.domain.logging.model.FeelingTag
import app.aspen.domain.logging.model.FoodLog
import app.aspen.domain.logging.model.Reflection
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Runs on JVM against the real AES/GCM [platformLocalCipher]. */
class PersistentLoggingStoreTest {

    private val t = Instant.fromEpochSeconds(1_700_000_000)

    private fun store(blob: InMemoryEncryptedBlobStore = InMemoryEncryptedBlobStore()) =
        PersistentLoggingStore(platformLocalCipher(), blob)

    @Test
    fun roundTripsAllLogKinds() {
        // Arrange
        val s = store()

        // Act
        s.addReflection(Reflection("r1", "wrote this", t))
        s.addFoodLog(FoodLog("f1", "lunch", setOf(FeelingTag.ANXIOUS), t))
        s.addBehaviourLog(BehaviourLog("b1", "walk", setOf(FeelingTag.CALM), t))

        // Assert
        assertEquals(listOf("r1"), s.reflections().map { it.id })
        assertEquals(setOf(FeelingTag.ANXIOUS), s.foodLogs().single().feelings)
        assertEquals("walk", s.behaviourLogs().single().note)
    }

    @Test
    fun blobIsCiphertextNotPlaintext() {
        // Arrange
        val blob = InMemoryEncryptedBlobStore()
        val s = store(blob)

        // Act
        s.addReflection(Reflection("r1", "a very private thought", t))
        val onDisk = blob.load()!!.decodeToString()

        // Assert
        assertFalse(onDisk.contains("a very private thought"), "reflection must be encrypted at rest")
    }

    @Test
    fun deletesAreHardDeletes() {
        // Arrange
        val s = store()
        s.addFoodLog(FoodLog("f1", "a", emptySet(), t))
        s.addFoodLog(FoodLog("f2", "b", emptySet(), t))

        // Act
        s.deleteFoodLog("f1")

        // Assert
        assertEquals(listOf("f2"), s.foodLogs().map { it.id })
    }

    @Test
    fun clearAllWipesEverything() {
        // Arrange
        val s = store()
        s.addReflection(Reflection("r1", "x", t))
        s.addFoodLog(FoodLog("f1", "y", emptySet(), t))
        s.addBehaviourLog(BehaviourLog("b1", "z", emptySet(), t))

        // Act
        s.clearAll()

        // Assert
        assertTrue(s.reflections().isEmpty() && s.foodLogs().isEmpty() && s.behaviourLogs().isEmpty())
    }

    @Test
    fun corruptBlobReadsAsEmptyNeverThrows() {
        // Arrange
        val blob = InMemoryEncryptedBlobStore("garbage".encodeToByteArray())
        val s = store(blob)

        // Act / Assert — fail SAFE to empty.
        assertTrue(s.reflections().isEmpty())
        assertTrue(s.foodLogs().isEmpty())
    }
}
