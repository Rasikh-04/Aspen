package app.aspen.ui.nav

/**
 * Flat, shallow navigation (docs/06 §4). Bottom presence: Home / Reflect / Calm / Settings.
 * Safety is deliberately NOT a tab — it is a persistent affordance reachable in <=2 taps and is
 * never buried (CLAUDE.md #6). Grounding tools are full-screen routes launched from the Calm chooser.
 */
object Routes {
    const val HOME = "home"
    const val REFLECT = "reflect"
    const val CALM = "calm"
    const val SETTINGS = "settings"
    const val SAFETY = "safety"

    // Full-screen Flow A tools (no bottom bar) — docs/06 §3.
    const val BREATHE = "breathe"
    const val GROUND_54321 = "ground_54321"
    const val RIDE_URGE = "ride_urge"

    /** Debug-only companion/guard preview (Phase 4). Reachable only when [app.aspen.ui.AspenDeps.isDebugBuild]. */
    const val DEBUG_COMPANION = "debug_companion"

    /** Routes that show the bottom navigation presence. Tools/safety are immersive (no bar). */
    val tabRoutes = setOf(HOME, REFLECT, CALM, SETTINGS)
}
