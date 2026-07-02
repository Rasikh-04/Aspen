package app.aspen.domain.consent

import app.aspen.domain.consent.model.ConsentEvent
import app.aspen.domain.consent.model.ConsentEventType
import app.aspen.domain.consent.model.ConsentGrant
import app.aspen.domain.consent.model.DataCategory
import app.aspen.domain.consent.model.Recipient
import app.aspen.domain.consent.model.RecipientType
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** In-memory fake of the persistence port — the manager logic is what's under test here. */
private class InMemoryConsentStore : ConsentStore {
    private val grants = mutableMapOf<String, ConsentGrant>()
    private val log = mutableListOf<ConsentEvent>()
    override fun allGrants() = grants.values.toList()
    override fun putGrant(grant: ConsentGrant) { grants[grant.id] = grant }
    override fun events() = log.toList()
    override fun appendEvent(event: ConsentEvent) { log += event }
}

/** A clock the test advances by hand, for deterministic expiry checks. */
private class MutableClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

class ConsentManagerTest {

    private val t0 = Instant.fromEpochMilliseconds(1_000_000L)
    private val clock = MutableClock(t0)
    private var idSeq = 0
    private val store = InMemoryConsentStore()
    private val manager = DefaultConsentManager(store, clock) { "grant-${idSeq++}" }

    private val clinician = Recipient("rec-1", RecipientType.LINKED_CLINICIAN, "Dr. Ahmed")

    @Test
    fun defaultDenyWithNoGrant() {
        assertFalse(manager.canAccess("rec-1", DataCategory.REFLECTIONS))
    }

    @Test
    fun grantAllowsOnlyTheScopedCategoryAndRecipient() {
        manager.grant(clinician, setOf(DataCategory.REFLECTIONS), purpose = "care")
        assertTrue(manager.canAccess("rec-1", DataCategory.REFLECTIONS))
        assertFalse(manager.canAccess("rec-1", DataCategory.FOOD_LOGS)) // out of scope
        assertFalse(manager.canAccess("rec-2", DataCategory.REFLECTIONS)) // wrong recipient
    }

    @Test
    fun revokeImmediatelyDenies() {
        val grant = manager.grant(clinician, setOf(DataCategory.REFLECTIONS), purpose = "care")
        assertTrue(manager.canAccess("rec-1", DataCategory.REFLECTIONS))
        manager.revoke(grant.id)
        assertFalse(manager.canAccess("rec-1", DataCategory.REFLECTIONS))
        assertTrue(manager.activeGrants().isEmpty())
    }

    @Test
    fun expiryDenies() {
        val expires = t0.plusMillis(1000)
        manager.grant(clinician, setOf(DataCategory.REFLECTIONS), purpose = "care", expiresAt = expires)
        assertTrue(manager.canAccess("rec-1", DataCategory.REFLECTIONS))
        clock.now = expires // at the boundary it is no longer active (now < expiresAt is false)
        assertFalse(manager.canAccess("rec-1", DataCategory.REFLECTIONS))
    }

    @Test
    fun auditLogRecordsGrantRevokeAndAccessChecks() {
        val grant = manager.grant(clinician, setOf(DataCategory.REFLECTIONS), purpose = "care")
        manager.canAccess("rec-1", DataCategory.REFLECTIONS)
        manager.revoke(grant.id)
        val types = manager.auditLog().map { it.type }
        assertEquals(
            listOf(ConsentEventType.GRANTED, ConsentEventType.ACCESS_CHECK, ConsentEventType.REVOKED),
            types,
        )
        val accessEvent = manager.auditLog().first { it.type == ConsentEventType.ACCESS_CHECK }
        assertEquals(true, accessEvent.allowed)
    }

    private fun Instant.plusMillis(ms: Long) = Instant.fromEpochMilliseconds(toEpochMilliseconds() + ms)
}
