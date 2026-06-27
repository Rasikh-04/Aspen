package app.aspen.ui.nav

/**
 * Flat, shallow navigation (docs/06 §4). Bottom presence: Home / Reflect / Calm / Settings.
 * Safety is deliberately NOT a tab — it is a persistent affordance reachable in <=2 taps and is
 * never buried (CLAUDE.md #6).
 */
object Routes {
    const val HOME = "home"
    const val REFLECT = "reflect"
    const val CALM = "calm"
    const val SETTINGS = "settings"
    const val SAFETY = "safety"
}
