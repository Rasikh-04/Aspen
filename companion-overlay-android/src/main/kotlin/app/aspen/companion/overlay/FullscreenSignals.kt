package app.aspen.companion.overlay

import app.aspen.domain.companion.CompanionEvent

/**
 * Maps the overlay window's inset signals to companion events (docs/05 §6 "fullscreen detection &
 * suspend"). NON-INVASIVE by design: we look only at whether the system bars are visible on OUR
 * window — when a fullscreen/immersive app (video, game, camera) is foreground the bars hide, and
 * the companion fully suspends (docs/04 §6, the single biggest battery win). We never read other
 * apps' content or identity.
 *
 * Pure and unit-tested; the service feeds it from an insets listener.
 */
object FullscreenSignals {
    fun eventFor(systemBarsVisible: Boolean): CompanionEvent =
        if (systemBarsVisible) CompanionEvent.FullscreenExited else CompanionEvent.FullscreenEntered
}
