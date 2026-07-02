package app.aspen.domain.companion

import app.aspen.domain.ai.CompanionLine
import app.aspen.domain.ai.CompanionMoment
import app.aspen.domain.companion.model.CompanionPrefs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Decision rules for the companion's rare check-in notification (FR-8 off-by-default; SR-4 never
 * guilt/urgency; docs/05 §3.5: rare, gentle, never notification-spam, fully disable-able). Pure and
 * platform-free: the Android scheduler asks it whether NOW is an acceptable moment; the answer can
 * only ever be "no" more often than the caller would. There is deliberately no burst, retry, or
 * "missed you" path — one gentle hello, at most every [MIN_INTERVAL], daytime only.
 */
object NotificationPolicy {

    /** At most one check-in every 3 days — rare BY CONSTRUCTION, whatever the scheduler does. */
    val MIN_INTERVAL: Duration = 72.hours

    /** Local-time gentle window: nothing before 10:00, nothing at/after 21:00 (docs/05 §3.5). */
    const val WINDOW_START_HOUR: Int = 10
    const val WINDOW_END_HOUR_EXCLUSIVE: Int = 21

    /**
     * Whether a check-in may be delivered right now. [localHour] is the user's local hour-of-day
     * (0–23), supplied by the platform so this stays timezone-free.
     */
    fun shouldDeliver(prefs: CompanionPrefs, lastDeliveredAt: Instant?, now: Instant, localHour: Int): Boolean {
        if (!prefs.enabled || !prefs.notificationsEnabled) return false
        if (localHour < WINDOW_START_HOUR || localHour >= WINDOW_END_HOUR_EXCLUSIVE) return false
        if (lastDeliveredAt != null && now - lastDeliveredAt < MIN_INTERVAL) return false
        return true
    }

    /**
     * The ONLY way a notification gets its words: a curated line that carries the
     * [CompanionMoment.NOTIFICATION_PHRASING] moment. Anything else — however the caller obtained
     * it — is rejected, so a wiring mistake can never surface an out-of-context line in a
     * notification (docs/04 ADR-003).
     */
    fun phrasingKeyOrNull(line: CompanionLine): String? =
        if (line.moment == CompanionMoment.NOTIFICATION_PHRASING) line.key else null
}
