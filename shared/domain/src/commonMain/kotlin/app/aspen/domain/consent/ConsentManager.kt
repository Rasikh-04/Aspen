package app.aspen.domain.consent

import app.aspen.domain.consent.model.ConsentEvent
import app.aspen.domain.consent.model.ConsentEventType
import app.aspen.domain.consent.model.ConsentGrant
import app.aspen.domain.consent.model.DataCategory
import app.aspen.domain.consent.model.Recipient
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The consent primitive (docs/08 §3, docs/09 §3.2) — the spine future sharing hangs off. Built now
 * with no real recipients so clinician linkage/export later become "issue a grant," not a refactor.
 *
 * Invariants (the reasons this is safe to build on):
 * - **Default deny:** [canAccess] is false unless a non-expired, non-revoked grant covers that exact
 *   category for that recipient.
 * - **Revoke is immediate and total** for future access.
 * - **Auditable:** grant / revoke / access-check are all logged for the user's eyes.
 */
interface ConsentManager {
    fun grant(
        recipient: Recipient,
        categories: Set<DataCategory>,
        purpose: String,
        expiresAt: Instant? = null,
    ): ConsentGrant

    /** Immediate, total revoke of future access; reflected in the audit log. */
    fun revoke(grantId: String)

    fun activeGrants(): List<ConsentGrant>

    /** Default-DENY access check. Every call is recorded in the audit log. */
    fun canAccess(recipientId: String, category: DataCategory): Boolean

    fun auditLog(): List<ConsentEvent>
}

/**
 * Default, pure implementation over a [ConsentStore]. [clock] and [newId] are injected so the
 * manager is fully deterministic in tests. No platform, network, or UI knowledge.
 */
class DefaultConsentManager(
    private val store: ConsentStore,
    private val clock: Clock,
    private val newId: () -> String,
) : ConsentManager {

    override fun grant(
        recipient: Recipient,
        categories: Set<DataCategory>,
        purpose: String,
        expiresAt: Instant?,
    ): ConsentGrant {
        val now = clock.now()
        val grant = ConsentGrant(
            id = newId(),
            recipient = recipient,
            categories = categories,
            grantedAt = now,
            expiresAt = expiresAt,
            revokedAt = null,
            purpose = purpose,
        )
        store.putGrant(grant)
        store.appendEvent(
            ConsentEvent(
                type = ConsentEventType.GRANTED,
                at = now,
                grantId = grant.id,
                recipientId = recipient.id,
                category = null,
                allowed = null,
            ),
        )
        return grant
    }

    override fun revoke(grantId: String) {
        val existing = store.allGrants().firstOrNull { it.id == grantId } ?: return
        if (existing.revokedAt != null) return
        val now = clock.now()
        store.putGrant(existing.copy(revokedAt = now))
        store.appendEvent(
            ConsentEvent(
                type = ConsentEventType.REVOKED,
                at = now,
                grantId = grantId,
                recipientId = existing.recipient.id,
                category = null,
                allowed = null,
            ),
        )
    }

    override fun activeGrants(): List<ConsentGrant> {
        val now = clock.now()
        return store.allGrants().filter { it.isActive(now) }
    }

    override fun canAccess(recipientId: String, category: DataCategory): Boolean {
        val now = clock.now()
        val allowed = store.allGrants().any { it.covers(recipientId, category, now) }
        store.appendEvent(
            ConsentEvent(
                type = ConsentEventType.ACCESS_CHECK,
                at = now,
                grantId = null,
                recipientId = recipientId,
                category = category,
                allowed = allowed,
            ),
        )
        return allowed
    }

    override fun auditLog(): List<ConsentEvent> = store.events()
}
