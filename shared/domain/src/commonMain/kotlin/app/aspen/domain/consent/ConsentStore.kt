package app.aspen.domain.consent

import app.aspen.domain.consent.model.ConsentEvent
import app.aspen.domain.consent.model.ConsentGrant

/**
 * Persistence port for consent state (docs/09 §3.2). The concrete implementation in :shared:data
 * persists this **encrypted** with a device key. This interface is the boundary the domain depends
 * on, so the manager stays pure and testable with an in-memory fake.
 *
 * **Fail-safe contract:** every read MUST be total — never throw. If the backing store is missing,
 * unreadable, or corrupted, reads return empty. Combined with the manager's default-deny, a corrupt
 * store therefore denies all access rather than failing open.
 */
interface ConsentStore {
    /** All grants ever issued (active, revoked, expired). Empty on a missing/corrupt store. */
    fun allGrants(): List<ConsentGrant>

    /** Insert or replace a grant by id. */
    fun putGrant(grant: ConsentGrant)

    /** The append-only audit log, oldest first. Empty on a missing/corrupt store. */
    fun events(): List<ConsentEvent>

    /** Append one audit event. */
    fun appendEvent(event: ConsentEvent)
}
