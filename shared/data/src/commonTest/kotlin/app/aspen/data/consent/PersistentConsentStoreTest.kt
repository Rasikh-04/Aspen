package app.aspen.data.consent

import app.aspen.domain.consent.DefaultConsentManager
import app.aspen.domain.consent.model.DataCategory
import app.aspen.domain.consent.model.Recipient
import app.aspen.domain.consent.model.RecipientType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Persistence + fail-safe contract for [PersistentConsentStore] and its integration with the domain
 * [DefaultConsentManager]. Runs on the JVM target against the real AES/GCM [platformConsentCipher].
 */
class PersistentConsentStoreTest {

    private class FixedClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private val t0 = Instant.parse("2026-06-28T10:00:00Z")
    private val self = Recipient("self", RecipientType.SELF_EXPORT, "You")

    private fun newStore(blob: ConsentBlobStore, cipher: ConsentCipher) =
        PersistentConsentStore(cipher, blob)

    @Test
    fun grantRoundTripsThroughEncryptedBlob() {
        val cipher = platformConsentCipher()
        val blob = InMemoryConsentBlobStore()
        val clock = FixedClock(t0)
        val manager = DefaultConsentManager(newStore(blob, cipher), clock) { "grant-1" }

        manager.grant(self, setOf(DataCategory.REFLECTIONS), purpose = "export")

        // A brand-new store over the SAME blob + cipher must decrypt and see the grant.
        val reopened = newStore(blob, cipher)
        val grants = reopened.allGrants()
        assertEquals(1, grants.size)
        assertEquals("grant-1", grants.single().id)
        assertEquals(setOf(DataCategory.REFLECTIONS), grants.single().categories)
    }

    @Test
    fun blobIsActuallyEncryptedNotPlaintext() {
        val cipher = platformConsentCipher()
        val blob = InMemoryConsentBlobStore()
        DefaultConsentManager(newStore(blob, cipher), FixedClock(t0)) { "g" }
            .grant(self, setOf(DataCategory.PROFILE), purpose = "secret-purpose-token")

        val raw = blob.load()!!.decodeToString()
        assertFalse(raw.contains("secret-purpose-token"), "consent blob must be ciphertext, not plaintext")
        assertFalse(raw.contains("PROFILE"))
    }

    @Test
    fun corruptedBlobReadsAsEmptyAndDeniesAccess() {
        val cipher = platformConsentCipher()
        val blob = InMemoryConsentBlobStore("this is not valid ciphertext".encodeToByteArray())
        val store = newStore(blob, cipher)

        // Fail safe: unreadable store → empty → default-deny everything.
        assertTrue(store.allGrants().isEmpty())
        assertTrue(store.events().isEmpty())
        val manager = DefaultConsentManager(store, FixedClock(t0)) { "x" }
        assertFalse(manager.canAccess("self", DataCategory.REFLECTIONS))
    }

    @Test
    fun missingBlobReadsAsEmpty() {
        val store = newStore(InMemoryConsentBlobStore(initial = null), platformConsentCipher())
        assertTrue(store.allGrants().isEmpty())
        assertTrue(store.events().isEmpty())
    }

    @Test
    fun managerOverRealStoreEnforcesScopeRevokeAndAudit() {
        val cipher = platformConsentCipher()
        val blob = InMemoryConsentBlobStore()
        val clock = FixedClock(t0)
        var n = 0
        val manager = DefaultConsentManager(newStore(blob, cipher), clock) { "g-${n++}" }

        // default-deny before any grant
        assertFalse(manager.canAccess("self", DataCategory.REFLECTIONS))

        val grant = manager.grant(self, setOf(DataCategory.REFLECTIONS), purpose = "export")
        assertTrue(manager.canAccess("self", DataCategory.REFLECTIONS))
        // out-of-scope category stays denied
        assertFalse(manager.canAccess("self", DataCategory.FOOD_LOGS))

        manager.revoke(grant.id)
        assertFalse(manager.canAccess("self", DataCategory.REFLECTIONS), "revoke is immediate and total")
        assertTrue(manager.activeGrants().isEmpty())

        // audit log persisted and ordered: GRANTED, ACCESS_CHECKs, REVOKED, ACCESS_CHECK…
        val log = manager.auditLog()
        assertTrue(log.size >= 5)
        assertEquals("GRANTED", log.first { it.grantId == grant.id }.type.name)
    }

    @Test
    fun expiryIsRespectedAcrossReload() {
        val cipher = platformConsentCipher()
        val blob = InMemoryConsentBlobStore()
        val clock = FixedClock(t0)
        val manager = DefaultConsentManager(newStore(blob, cipher), clock) { "g" }
        val expires = Instant.parse("2026-06-28T11:00:00Z")
        manager.grant(self, setOf(DataCategory.SAFETY_EVENTS), purpose = "temp", expiresAt = expires)

        assertTrue(manager.canAccess("self", DataCategory.SAFETY_EVENTS))
        // advance past expiry; a reloaded manager over the same blob must deny
        clock.instant = Instant.parse("2026-06-28T12:00:00Z")
        val reloaded = DefaultConsentManager(newStore(blob, cipher), clock) { "g2" }
        assertFalse(reloaded.canAccess("self", DataCategory.SAFETY_EVENTS), "expired grant must not authorise")
    }
}
