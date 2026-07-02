package app.aspen.domain.companion

import app.aspen.domain.ai.CompanionLine
import app.aspen.domain.ai.CompanionMoment
import app.aspen.domain.companion.model.CompanionPrefs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * The check-in notification rules (FR-8; SR-4; docs/05 §3.5) as tested invariants: off by default,
 * opt-in twice over, RARE by construction, quiet at night, and only ever worded by the reviewed
 * library's NOTIFICATION_PHRASING moment.
 */
class NotificationPolicyTest {

    private val now = Instant.fromEpochMilliseconds(1_000_000_000_000)
    private val allOn = CompanionPrefs(enabled = true, notificationsEnabled = true)
    private val midday = 13

    @Test
    fun defaultPrefsNeverDeliver() {
        assertFalse(NotificationPolicy.shouldDeliver(CompanionPrefs(), lastDeliveredAt = null, now = now, localHour = midday))
    }

    @Test
    fun companionOnButNotificationsOffNeverDelivers() {
        val prefs = CompanionPrefs(enabled = true, notificationsEnabled = false)
        assertFalse(NotificationPolicy.shouldDeliver(prefs, lastDeliveredAt = null, now = now, localHour = midday))
    }

    @Test
    fun notificationsOnButCompanionOffNeverDelivers() {
        // The check-in belongs to the companion; no companion, no voice to say hello with.
        val prefs = CompanionPrefs(enabled = false, notificationsEnabled = true)
        assertFalse(NotificationPolicy.shouldDeliver(prefs, lastDeliveredAt = null, now = now, localHour = midday))
    }

    @Test
    fun bothOptInsAndNoHistoryDeliversAtMidday() {
        assertTrue(NotificationPolicy.shouldDeliver(allOn, lastDeliveredAt = null, now = now, localHour = midday))
    }

    @Test
    fun cadenceCapIsAtLeastSeventyTwoHours() {
        val justUnder = now - (NotificationPolicy.MIN_INTERVAL - 1.hours)
        val exactlyAt = now - NotificationPolicy.MIN_INTERVAL

        assertFalse(NotificationPolicy.shouldDeliver(allOn, lastDeliveredAt = justUnder, now = now, localHour = midday))
        assertTrue(NotificationPolicy.shouldDeliver(allOn, lastDeliveredAt = exactlyAt, now = now, localHour = midday))
        assertTrue(NotificationPolicy.MIN_INTERVAL >= 72.hours, "check-ins must stay rare by construction")
    }

    @Test
    fun quietHoursAreSilent() {
        for (hour in listOf(21, 22, 23, 0, 3, 6, 9)) {
            assertFalse(
                NotificationPolicy.shouldDeliver(allOn, lastDeliveredAt = null, now = now, localHour = hour),
                "hour $hour must be quiet",
            )
        }
        for (hour in listOf(10, 12, 16, 20)) {
            assertTrue(
                NotificationPolicy.shouldDeliver(allOn, lastDeliveredAt = null, now = now, localHour = hour),
                "hour $hour is within the gentle window",
            )
        }
    }

    @Test
    fun phrasingOnlyAcceptsTheNotificationMoment() {
        val notifyLine = CompanionLine(
            key = "companion_notify_gentle_checkin",
            moment = CompanionMoment.NOTIFICATION_PHRASING,
            tones = emptySet(),
            rankingHint = "",
            review = emptyMap(),
        )
        val wrongMoment = notifyLine.copy(key = "companion_greeting_here", moment = CompanionMoment.GREETING)

        assertEquals("companion_notify_gentle_checkin", NotificationPolicy.phrasingKeyOrNull(notifyLine))
        assertNull(NotificationPolicy.phrasingKeyOrNull(wrongMoment), "non-notification lines must be rejected")
    }
}
