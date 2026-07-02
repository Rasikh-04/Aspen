package app.aspen.ui.companion

/**
 * Platform hook for the system-overlay companion ("across your screen", docs/05 §6). Android
 * supplies an implementation backed by the overlay service; platforms without system overlays
 * (iOS — a platform limit, docs/04 ADR-001) leave it null and the Settings row simply never
 * appears. The shared UI owns the explain-BEFORE-request wording (docs/05 §6 permissions UX);
 * the platform owns the actual permission screen and service lifecycle.
 */
interface CompanionOverlayControl {
    /** Whether the OS "display over other apps" permission is currently granted. */
    fun isPermissionGranted(): Boolean

    /** Open the OS screen where the user can grant it (never a dialog we can spring). */
    fun requestPermission()

    /** Start/stop the overlay presence. Must be a no-op when the permission is missing. */
    fun setOverlayActive(active: Boolean)
}
