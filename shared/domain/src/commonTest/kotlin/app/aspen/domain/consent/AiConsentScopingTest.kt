package app.aspen.domain.consent

import app.aspen.domain.consent.model.ConsentEvent
import app.aspen.domain.consent.model.ConsentGrant
import app.aspen.domain.consent.model.DataCategory
import app.aspen.domain.consent.model.Recipient
import app.aspen.domain.consent.model.RecipientType
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Phase-4 cloud-AI consent (docs/04 ADR-003) rides the existing primitive: an [RecipientType.AI_SERVICE]
 * grant over [DataCategory.AI_MESSAGES]. These tests pin the properties the reflection tier depends
 * on — default deny, scope precision, immediate revoke (CLAUDE.md #8/#10: cloud off by default).
 */
class AiConsentScopingTest {

    private class InMemoryConsentStore : ConsentStore {
        private val grants = mutableMapOf<String, ConsentGrant>()
        private val log = mutableListOf<ConsentEvent>()
        override fun allGrants() = grants.values.toList()
        override fun putGrant(grant: ConsentGrant) { grants[grant.id] = grant }
        override fun events() = log.toList()
        override fun appendEvent(event: ConsentEvent) { log += event }
    }

    private class FixedClock(private val at: Instant) : Clock {
        override fun now(): Instant = at
    }

    private var idSeq = 0
    private val manager = DefaultConsentManager(
        InMemoryConsentStore(),
        FixedClock(Instant.fromEpochMilliseconds(1_000_000L)),
    ) { "grant-${idSeq++}" }

    private val aiService = Recipient("ai-cloud", RecipientType.AI_SERVICE, "Cloud reflection")

    @Test
    fun cloudAiIsDeniedByDefault() {
        assertFalse(manager.canAccess("ai-cloud", DataCategory.AI_MESSAGES))
    }

    @Test
    fun aiGrantCoversOnlyAiMessages() {
        manager.grant(aiService, setOf(DataCategory.AI_MESSAGES), purpose = "cloud reflection")

        assertTrue(manager.canAccess("ai-cloud", DataCategory.AI_MESSAGES))
        assertFalse(manager.canAccess("ai-cloud", DataCategory.REFLECTIONS), "AI grant must not open the notebook")
        assertFalse(manager.canAccess("ai-cloud", DataCategory.PROFILE), "AI grant must not open the profile")
    }

    @Test
    fun revokingAiConsentDeniesImmediately() {
        val grant = manager.grant(aiService, setOf(DataCategory.AI_MESSAGES), purpose = "cloud reflection")
        assertTrue(manager.canAccess("ai-cloud", DataCategory.AI_MESSAGES))

        manager.revoke(grant.id)

        assertFalse(manager.canAccess("ai-cloud", DataCategory.AI_MESSAGES))
    }
}
