package app.aspen.ui.companion

/**
 * Platform hook for the rare companion check-in notification (FR-8; docs/05 §3.5). The shared UI
 * owns the opt-in toggle and its wording; the platform owns scheduling and the OS notification
 * permission. Null (e.g. iOS until Phase 8) → the row never appears. All DECISION logic lives in
 * the domain [app.aspen.domain.companion.NotificationPolicy] — a scheduler can only ask, never
 * bypass.
 */
interface CompanionNotificationsControl {
    /** Start/stop the periodic scheduler; requests the OS notification permission when needed. */
    fun setScheduled(active: Boolean)
}
