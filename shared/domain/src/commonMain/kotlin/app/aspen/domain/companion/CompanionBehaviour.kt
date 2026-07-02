package app.aspen.domain.companion

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * The companion's presence states (docs/05 §4). There is deliberately no state for absence-guilt,
 * progress, or attention-seeking — a state that isn't representable can't be shipped (SR-4).
 */
sealed interface CompanionState {
    /** Not on screen. Only [CompanionEvent.Summon] leaves this state — it never self-returns. */
    data object Hidden : CompanionState

    /** Resting at a screen edge; tiny, slow micro-animation (near-zero frames, docs/04 §6). */
    data object Ambient : CompanionState

    /** After a tap: wanders/peeks/can be dragged. Time-boxed; settles back to [Ambient] alone. */
    data class Playful(val startedAt: Instant) : CompanionState

    /** Calm company alongside the hard-moment flow. Never blocks the safety exit (CLAUDE.md #6). */
    data object GentlePresence : CompanionState

    /** A fullscreen/immersive app is foreground: fully paused and hidden (docs/04 §6). */
    data object Suspended : CompanionState
}

/** Everything that can happen to the companion. Position/drag is host-side and stateless here. */
sealed interface CompanionEvent {
    data object Summon : CompanionEvent
    data class Tap(val at: Instant) : CompanionEvent
    data object FlingDismiss : CompanionEvent
    data class Tick(val now: Instant) : CompanionEvent
    data object FullscreenEntered : CompanionEvent
    data object FullscreenExited : CompanionEvent
    data object HardMomentOpened : CompanionEvent
    data object HardMomentClosed : CompanionEvent
}

/** The two live inputs every transition honours: the master switch and reduced-motion (SR-6). */
data class CompanionContext(val enabled: Boolean, val reducedMotion: Boolean)

/**
 * Pure, total transition function for the companion (docs/05 §3/§4). The guardrails live HERE, as
 * structure, so no host (in-app or overlay) can misbehave: a disabled companion is [CompanionState.Hidden]
 * whatever happens; a dismissed one stays hidden until summoned; reduced motion refuses play;
 * a hard moment gets gentle presence, never games. Total and non-throwing — a companion that
 * crashes at a hard moment is worse than one that sits still.
 */
object CompanionBehaviour {

    /** How long the playful state may last before settling back to ambient (docs/05 §4). */
    val PLAYFUL_TIMEBOX: Duration = 60.seconds

    fun initial(context: CompanionContext): CompanionState =
        if (context.enabled) CompanionState.Ambient else CompanionState.Hidden

    fun next(state: CompanionState, event: CompanionEvent, context: CompanionContext): CompanionState {
        if (!context.enabled) return CompanionState.Hidden

        return when (event) {
            is CompanionEvent.Summon -> when (state) {
                CompanionState.Suspended -> CompanionState.Suspended
                else -> CompanionState.Ambient
            }

            is CompanionEvent.Tap -> when (state) {
                CompanionState.Ambient, is CompanionState.Playful ->
                    if (context.reducedMotion) CompanionState.Ambient else CompanionState.Playful(event.at)
                else -> state
            }

            is CompanionEvent.FlingDismiss -> CompanionState.Hidden

            is CompanionEvent.Tick -> when (state) {
                is CompanionState.Playful ->
                    if (context.reducedMotion || event.now - state.startedAt >= PLAYFUL_TIMEBOX) {
                        CompanionState.Ambient
                    } else {
                        state
                    }
                else -> state
            }

            is CompanionEvent.FullscreenEntered -> when (state) {
                CompanionState.Hidden -> CompanionState.Hidden
                else -> CompanionState.Suspended
            }

            is CompanionEvent.FullscreenExited -> when (state) {
                // Resume to calm ambient only — never straight back into play.
                CompanionState.Suspended -> CompanionState.Ambient
                else -> state
            }

            is CompanionEvent.HardMomentOpened -> when (state) {
                CompanionState.Hidden -> CompanionState.Hidden
                CompanionState.Suspended -> CompanionState.Suspended
                else -> CompanionState.GentlePresence
            }

            is CompanionEvent.HardMomentClosed -> when (state) {
                CompanionState.GentlePresence -> CompanionState.Ambient
                else -> state
            }
        }
    }
}
