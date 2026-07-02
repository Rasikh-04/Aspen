package app.aspen.companion.overlay

import app.aspen.domain.companion.CompanionBehaviour
import app.aspen.domain.companion.CompanionContext
import app.aspen.domain.companion.CompanionEvent
import app.aspen.domain.companion.CompanionState
import org.junit.Assert.assertEquals
import org.junit.Test

class FullscreenSignalsTest {

    private val on = CompanionContext(enabled = true, reducedMotion = false)

    @Test
    fun hiddenSystemBarsMeanSuspend() {
        assertEquals(CompanionEvent.FullscreenEntered, FullscreenSignals.eventFor(systemBarsVisible = false))
    }

    @Test
    fun visibleSystemBarsMeanResume() {
        assertEquals(CompanionEvent.FullscreenExited, FullscreenSignals.eventFor(systemBarsVisible = true))
    }

    @Test
    fun signalRoundTripSuspendsAndResumesTheMachine() {
        // Arrange — an ambient overlay companion.
        var state: CompanionState = CompanionState.Ambient

        // Act — a fullscreen app comes and goes.
        state = CompanionBehaviour.next(state, FullscreenSignals.eventFor(systemBarsVisible = false), on)
        val whileFullscreen = state
        state = CompanionBehaviour.next(state, FullscreenSignals.eventFor(systemBarsVisible = true), on)

        // Assert — fully suspended during, calm ambient after (never straight back into play).
        assertEquals(CompanionState.Suspended, whileFullscreen)
        assertEquals(CompanionState.Ambient, state)
    }

    @Test
    fun signalsNeverResurrectADismissedCompanion() {
        var state: CompanionState = CompanionState.Hidden
        state = CompanionBehaviour.next(state, FullscreenSignals.eventFor(systemBarsVisible = false), on)
        state = CompanionBehaviour.next(state, FullscreenSignals.eventFor(systemBarsVisible = true), on)
        assertEquals(CompanionState.Hidden, state)
    }
}
