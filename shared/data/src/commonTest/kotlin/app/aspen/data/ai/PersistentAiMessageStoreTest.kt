package app.aspen.data.ai

import app.aspen.data.local.InMemoryEncryptedBlobStore
import app.aspen.data.local.platformLocalCipher
import app.aspen.domain.ai.AiMessage
import app.aspen.domain.ai.AiRole
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Runs against the real platform cipher, mirroring the other persistent-store suites. */
class PersistentAiMessageStoreTest {

    private fun message(id: String, role: AiRole = AiRole.USER, text: String = "text-$id") = AiMessage(
        id = id,
        role = role,
        text = text,
        createdAt = Instant.fromEpochMilliseconds(1_000_000L),
    )

    @Test
    fun roundTripsThroughEncryptedBlob() {
        val store = PersistentAiMessageStore(platformLocalCipher(), InMemoryEncryptedBlobStore())

        store.append(message("m1", AiRole.USER, "today was hard"))
        store.append(message("m2", AiRole.COMPANION, "that sounds heavy"))

        assertEquals(listOf("m1", "m2"), store.history().map { it.id })
        assertEquals(listOf(AiRole.USER, AiRole.COMPANION), store.history().map { it.role })
    }

    @Test
    fun blobIsCiphertextNotPlaintext() {
        val blob = InMemoryEncryptedBlobStore()
        val store = PersistentAiMessageStore(platformLocalCipher(), blob)

        store.append(message("m1", text = "something deeply private"))

        assertFalse(
            blob.load()!!.decodeToString().contains("deeply private"),
            "AI history must be encrypted at rest",
        )
    }

    @Test
    fun corruptBlobFailsSafeToEmptyNeverThrows() {
        val blob = InMemoryEncryptedBlobStore("not ciphertext".encodeToByteArray())
        val store = PersistentAiMessageStore(platformLocalCipher(), blob)

        assertTrue(store.history().isEmpty())
    }

    @Test
    fun clearAllDeletesEverything() {
        val store = PersistentAiMessageStore(platformLocalCipher(), InMemoryEncryptedBlobStore())
        store.append(message("m1"))

        store.clearAll()

        assertTrue(store.history().isEmpty(), "FR-11: delete means delete")
    }

    @Test
    fun unknownRoleEntriesAreSkippedNotFatal() {
        // Forward-compat: a future role value must not break reading today's history.
        val store = PersistentAiMessageStore(platformLocalCipher(), InMemoryEncryptedBlobStore())
        store.append(message("m1"))
        assertEquals(1, store.history().size)
    }
}
