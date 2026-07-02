package app.aspen.domain.companion

import app.aspen.domain.companion.model.CompanionPrefs

/**
 * Persistence port for the companion preferences. The domain depends only on this interface; the
 * encrypted implementation lives in `:shared:data` (same pattern as [app.aspen.domain.onboarding.ProfileStore]).
 *
 * **Fail-safe:** reads never throw. Missing or unreadable prefs return `null`, which callers treat
 * as [CompanionPrefs] defaults — i.e. everything OFF (docs/05 §3.1). A storage failure can only
 * ever make the companion *quieter*, never louder.
 */
interface CompanionPrefsStore {
    /** Persist the latest choices, replacing any previous ones. */
    fun save(prefs: CompanionPrefs)

    /** The current prefs, or null if none saved (or unreadable). Never throws. */
    fun current(): CompanionPrefs?

    /** Permanently remove stored prefs (reset — FR-11 "delete everything"). */
    fun clear()
}
