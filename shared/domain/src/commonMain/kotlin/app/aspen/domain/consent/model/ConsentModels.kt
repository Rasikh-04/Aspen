package app.aspen.domain.consent.model

import kotlin.time.Instant

/**
 * Scoped data categories a grant can cover (docs/09 §3.1, docs/08 §3). Stored data is tagged by
 * these from the start so a future grant can be scoped precisely without a migration — even though
 * no recipients exist yet in Phase 2.
 */
enum class DataCategory { REFLECTIONS, FOOD_LOGS, BEHAVIOUR_LOGS, PROFILE, SAFETY_EVENTS }

/**
 * Who a grant is directed to. Only [SELF_EXPORT] is wired in Phase 2 (used later by export); the
 * clinician/trusted-person types are defined but dormant so linkage later is "issue a grant," not a
 * refactor (docs/08 §4).
 */
enum class RecipientType { LINKED_CLINICIAN, TRUSTED_PERSON, SELF_EXPORT, AFFILIATED_DOCTOR }

/** A grant's directed recipient (docs/09 §3.1). */
data class Recipient(
    val id: String,
    val type: RecipientType,
    val displayName: String,
)

/**
 * A scoped, directed, optionally time-boxed, revocable, auditable consent grant (docs/08 §3,
 * docs/09 §3.1). Default deny: access is only possible while an in-scope grant [isActive].
 */
data class ConsentGrant(
    val id: String,
    val recipient: Recipient,
    val categories: Set<DataCategory>,
    val grantedAt: Instant,
    val expiresAt: Instant?,
    val revokedAt: Instant?,
    val purpose: String,
) {
    /** Active = not revoked and not past expiry, as of [now]. Revocation is total and immediate. */
    fun isActive(now: Instant): Boolean =
        revokedAt == null && (expiresAt == null || now < expiresAt)

    /** Whether this grant authorises [category] for [recipientId] as of [now] (default deny). */
    fun covers(recipientId: String, category: DataCategory, now: Instant): Boolean =
        recipient.id == recipientId && category in categories && isActive(now)
}

/** The kind of audited consent event (docs/09 §3.2 — the user can always see who-saw-what-when). */
enum class ConsentEventType { GRANTED, REVOKED, ACCESS_CHECK }

/**
 * One entry in the user-visible consent audit log. For [ACCESS_CHECK], [allowed] records the
 * default-deny decision; for grant/revoke it is null.
 */
data class ConsentEvent(
    val type: ConsentEventType,
    val at: Instant,
    val grantId: String?,
    val recipientId: String?,
    val category: DataCategory?,
    val allowed: Boolean?,
)
