package app.aspen.domain.onboarding

import app.aspen.domain.onboarding.model.OnboardingResult

/**
 * Persistence port for the inferred support profile (docs/11 §1.7 "stored locally, private"; docs/04
 * §5 `profile`). The domain depends only on this interface; the encrypted implementation lives in
 * `:shared:data`.
 *
 * **Fail-safe:** reads never throw. A missing or unreadable profile returns `null`, which callers
 * treat as "no profile yet" → the safest configuration (see [AppConfigProvider]). The profile is
 * re-runnable and editable (docs/11 §1.6): [save] overwrites; [clear] supports delete/reset.
 */
interface ProfileStore {
    /** Persist the latest scored result, replacing any previous profile. */
    fun save(result: OnboardingResult)

    /** The current profile, or null if none has been saved (or it can't be read). Never throws. */
    fun current(): OnboardingResult?

    /** Permanently remove the stored profile (reset / delete — FR-11). */
    fun clear()
}
