package app.aspen.domain.companion

import app.aspen.domain.companion.model.CompanionPrefs
import app.aspen.domain.companion.model.CompanionSpecies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * The companion's presence rules (docs/05 §3/§4) as tested invariants. The safety-bearing ones:
 * off-by-default, dismissed-stays-dismissed (no self-return = no nagging, SR-4), reduced-motion
 * stills it (SR-6), and the hard-moment flow gets gentle presence that never plays.
 */
class CompanionBehaviourTest {

    private val t0 = Instant.fromEpochMilliseconds(0)
    private fun t(seconds: Int) = Instant.fromEpochMilliseconds(seconds * 1000L)

    private val on = CompanionContext(enabled = true, reducedMotion = false)
    private val off = CompanionContext(enabled = false, reducedMotion = false)
    private val onStill = CompanionContext(enabled = true, reducedMotion = true)

    private val allStates = listOf(
        CompanionState.Hidden,
        CompanionState.Ambient,
        CompanionState.Playful(startedAt = t0),
        CompanionState.GentlePresence,
        CompanionState.Suspended,
    )

    private val allEvents = listOf(
        CompanionEvent.Summon,
        CompanionEvent.Tap(at = t0),
        CompanionEvent.FlingDismiss,
        CompanionEvent.Tick(now = t(3600)),
        CompanionEvent.FullscreenEntered,
        CompanionEvent.FullscreenExited,
        CompanionEvent.HardMomentOpened,
        CompanionEvent.HardMomentClosed,
    )

    // --- off by default (CLAUDE.md #10; docs/05 §3.1) ---

    @Test
    fun prefsDefaultToEverythingOff() {
        val prefs = CompanionPrefs()
        assertEquals(false, prefs.enabled)
        assertEquals(false, prefs.overlayEnabled)
        assertEquals(false, prefs.notificationsEnabled)
        assertEquals(CompanionSpecies.ASPEN_SPRITE, prefs.species)
    }

    @Test
    fun disabledContextIsHiddenFromEveryStateOnEveryEvent() {
        assertEquals(CompanionState.Hidden, CompanionBehaviour.initial(off))
        for (state in allStates) for (event in allEvents) {
            assertEquals(
                CompanionState.Hidden,
                CompanionBehaviour.next(state, event, off),
                "disabled companion must stay hidden ($state, $event)",
            )
        }
    }

    @Test
    fun enabledInitialStateIsAmbient() {
        assertEquals(CompanionState.Ambient, CompanionBehaviour.initial(on))
    }

    // --- playful state, time-boxed (docs/05 §4 "returns to ambient on its own") ---

    @Test
    fun tapInAmbientStartsPlayful() {
        val next = CompanionBehaviour.next(CompanionState.Ambient, CompanionEvent.Tap(at = t0), on)
        assertIs<CompanionState.Playful>(next)
        assertEquals(t0, next.startedAt)
    }

    @Test
    fun playfulStaysPlayfulBeforeTimeboxExpires() {
        val playful = CompanionState.Playful(startedAt = t0)
        val justBefore = CompanionEvent.Tick(now = t0 + CompanionBehaviour.PLAYFUL_TIMEBOX / 2)
        assertIs<CompanionState.Playful>(CompanionBehaviour.next(playful, justBefore, on))
    }

    @Test
    fun playfulReturnsToAmbientWhenTimeboxExpires() {
        val playful = CompanionState.Playful(startedAt = t0)
        val expired = CompanionEvent.Tick(now = t0 + CompanionBehaviour.PLAYFUL_TIMEBOX)
        assertEquals(CompanionState.Ambient, CompanionBehaviour.next(playful, expired, on))
    }

    @Test
    fun tapDuringPlayfulRestartsTheTimebox() {
        val playful = CompanionState.Playful(startedAt = t0)
        val next = CompanionBehaviour.next(playful, CompanionEvent.Tap(at = t(30)), on)
        assertIs<CompanionState.Playful>(next)
        assertEquals(t(30), next.startedAt)
    }

    // --- dismissed stays dismissed: the no-nag invariant (SR-4; docs/05 §3.1/§3.2) ---

    @Test
    fun flingDismissHidesFromEveryVisibleState() {
        for (state in allStates) {
            assertEquals(
                CompanionState.Hidden,
                CompanionBehaviour.next(state, CompanionEvent.FlingDismiss, on),
                "fling must always dismiss ($state)",
            )
        }
    }

    @Test
    fun hiddenNeverReturnsOnItsOwn() {
        val nonSummonEvents = allEvents.filter { it != CompanionEvent.Summon }
        for (event in nonSummonEvents) {
            assertEquals(
                CompanionState.Hidden,
                CompanionBehaviour.next(CompanionState.Hidden, event, on),
                "hidden companion must not self-return on $event",
            )
        }
    }

    @Test
    fun onlySummonBringsAHiddenCompanionBack() {
        assertEquals(
            CompanionState.Ambient,
            CompanionBehaviour.next(CompanionState.Hidden, CompanionEvent.Summon, on),
        )
    }

    // --- fullscreen suspend (docs/05 §4; docs/04 §6 battery) ---

    @Test
    fun fullscreenSuspendsEveryVisibleState() {
        for (state in listOf(CompanionState.Ambient, CompanionState.Playful(t0), CompanionState.GentlePresence)) {
            assertEquals(
                CompanionState.Suspended,
                CompanionBehaviour.next(state, CompanionEvent.FullscreenEntered, on),
                "fullscreen must suspend ($state)",
            )
        }
    }

    @Test
    fun fullscreenDoesNotResurrectAHiddenCompanion() {
        assertEquals(
            CompanionState.Hidden,
            CompanionBehaviour.next(CompanionState.Hidden, CompanionEvent.FullscreenEntered, on),
        )
        assertEquals(
            CompanionState.Hidden,
            CompanionBehaviour.next(CompanionState.Hidden, CompanionEvent.FullscreenExited, on),
        )
    }

    @Test
    fun fullscreenExitResumesToCalmAmbientOnly() {
        assertEquals(
            CompanionState.Ambient,
            CompanionBehaviour.next(CompanionState.Suspended, CompanionEvent.FullscreenExited, on),
        )
    }

    @Test
    fun summonWhileSuspendedStaysSuspended() {
        assertEquals(
            CompanionState.Suspended,
            CompanionBehaviour.next(CompanionState.Suspended, CompanionEvent.Summon, on),
        )
    }

    // --- reduced motion stills the companion (SR-6; docs/05 §3.6) ---

    @Test
    fun reducedMotionRefusesPlayful() {
        assertEquals(
            CompanionState.Ambient,
            CompanionBehaviour.next(CompanionState.Ambient, CompanionEvent.Tap(at = t0), onStill),
        )
    }

    @Test
    fun reducedMotionSettlesAnActivePlayfulBackToAmbient() {
        assertEquals(
            CompanionState.Ambient,
            CompanionBehaviour.next(CompanionState.Playful(t0), CompanionEvent.Tick(now = t(1)), onStill),
        )
    }

    // --- gentle presence in the hard-moment flow (docs/05 §4; CLAUDE.md #6/#8) ---

    @Test
    fun hardMomentTurnsAmbientIntoGentlePresence() {
        assertEquals(
            CompanionState.GentlePresence,
            CompanionBehaviour.next(CompanionState.Ambient, CompanionEvent.HardMomentOpened, on),
        )
        assertEquals(
            CompanionState.GentlePresence,
            CompanionBehaviour.next(CompanionState.Playful(t0), CompanionEvent.HardMomentOpened, on),
        )
    }

    @Test
    fun hardMomentNeverResurrectsADismissedCompanion() {
        assertEquals(
            CompanionState.Hidden,
            CompanionBehaviour.next(CompanionState.Hidden, CompanionEvent.HardMomentOpened, on),
        )
    }

    @Test
    fun noPlayDuringAHardMoment() {
        assertEquals(
            CompanionState.GentlePresence,
            CompanionBehaviour.next(CompanionState.GentlePresence, CompanionEvent.Tap(at = t0), on),
        )
    }

    @Test
    fun hardMomentClosedReturnsToAmbient() {
        assertEquals(
            CompanionState.Ambient,
            CompanionBehaviour.next(CompanionState.GentlePresence, CompanionEvent.HardMomentClosed, on),
        )
    }

    // --- totality: the machine never throws, whatever happens (fail-safe like the rest of :domain) ---

    @Test
    fun transitionIsTotalOverEveryStateEventContextCombination() {
        val contexts = listOf(on, off, onStill, CompanionContext(enabled = false, reducedMotion = true))
        for (state in allStates) for (event in allEvents) for (context in contexts) {
            val next = CompanionBehaviour.next(state, event, context)
            assertTrue(next in allStates || next is CompanionState.Playful, "unexpected state $next")
        }
    }
}
