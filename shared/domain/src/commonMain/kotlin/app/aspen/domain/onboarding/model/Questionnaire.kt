package app.aspen.domain.onboarding.model

/**
 * Structural model of the onboarding questionnaire answers (docs/11 §3).
 *
 * This is the **domain contract** the questionnaire UI (`:shared:ui`, Dev B) binds to. It holds
 * answer *structure* only — option tokens and a soft-Likert scale — and deliberately contains **no
 * user-facing copy** (CLAUDE.md #11; localized strings live in `:ui`). It is **numberless** by
 * construction (docs/11 §1.1): every signal is a categorical choice, never a count or amount.
 *
 * Every question is skippable. A skipped / "prefer not to say" answer is represented by `null`
 * (single-select), an empty set (multi-select), or [PREFER_NOT_TO_SAY], and contributes no signal —
 * skipping everything resolves to the safest profile (docs/11 §3.3, §4).
 */

/** Soft-Likert scale for the relationship/behaviour questions (docs/11 §3). Numberless. */
enum class Likert { NOT_REALLY, SOMETIMES, OFTEN, PREFER_NOT_TO_SAY }

/** Q1 — opening orientation (multi-select). Tunes first-run emphasis; not scored to a disorder. */
enum class HelpWanted {
    CALMER_MOMENT,
    PRIVATE_SPACE,
    LOW_DEMAND_COMPANY,
    REACH_REAL_HELP,
    NOT_SURE,
}

/** Q2 — relationship-with-eating descriptors (multi-select); each maps to a profile (docs/11 R1). */
enum class EatingRelationship {
    TENSE_RULES, // → RESTRICTION
    OUT_OF_CONTROL, // → BINGE
    UNDO_AFTER, // → PURGE
    SENSORY_HARD, // → AVOIDANCE_SENSORY
    BODY_FOCUSED, // → BODY_IMAGE
    VARIES, // low signal
    PREFER_NOT_TO_SAY,
}

/** Q6 — ARFID/sensory driver (docs/11 §3 Q6). A `YES` down-weights body-image framing entirely. */
enum class SensoryDriver { YES, SOMEWHAT, NO, PREFER_NOT_TO_SAY }

/** Q7 — body-image salience, asked gently and numberless (docs/11 §3 Q7). */
enum class BodyImageSalience { NOT_MUCH, SOMETIMES, A_LOT, PREFER_NOT_TO_SAY }

/** Q8 — everyday-life impact (docs/11 §3 Q8). Tunes routing strength, not a profile. */
enum class LifeImpact { A_LITTLE, SOME, A_LOT, PREFER_NOT_TO_SAY }

/** Q9 — support context (docs/11 §3 Q9). Carer/professional involvement tunes routing. */
enum class SupportContext { HAS_PROFESSIONAL, HAS_TRUSTED_PERSON, NONE_RIGHT_NOW, PREFER_NOT_TO_SAY }

/** Q10 — quick-reach preference (docs/11 §3 Q10). NOT a risk screen; just eases the human-exit. */
enum class QuickReachPreference { SET_UP_NOW, MAYBE_LATER, NO }

/**
 * A complete (or partial) set of questionnaire answers. All fields default to "skipped" so an empty
 * instance models "skip all" → the safest profile. The map nature is intentionally avoided in favour
 * of a typed record so the contract is explicit and the scoring is exhaustive over known questions.
 */
data class OnboardingAnswers(
    val helpWanted: Set<HelpWanted> = emptySet(), // Q1
    val eatingRelationship: Set<EatingRelationship> = emptySet(), // Q2
    val holdingBack: Likert? = null, // Q3 — restriction
    val lossOfControl: Likert? = null, // Q4 — binge
    val urgeToCompensate: Likert? = null, // Q5 — purge/compensatory
    val sensoryDriver: SensoryDriver? = null, // Q6 — ARFID
    val bodyImageSalience: BodyImageSalience? = null, // Q7 — body-image
    val lifeImpact: LifeImpact? = null, // Q8 — routing
    val supportContext: SupportContext? = null, // Q9 — routing
    val quickReach: QuickReachPreference? = null, // Q10 — routing
) {
    /** True when nothing meaningful was answered (all skipped) → resolves to MIXED_OR_UNSURE. */
    val isEmpty: Boolean
        get() = helpWanted.isEmpty() &&
            eatingRelationship.all { it == EatingRelationship.PREFER_NOT_TO_SAY } &&
            holdingBack.isNoSignal() &&
            lossOfControl.isNoSignal() &&
            urgeToCompensate.isNoSignal() &&
            (sensoryDriver == null || sensoryDriver == SensoryDriver.PREFER_NOT_TO_SAY) &&
            (bodyImageSalience == null || bodyImageSalience == BodyImageSalience.PREFER_NOT_TO_SAY) &&
            (lifeImpact == null || lifeImpact == LifeImpact.PREFER_NOT_TO_SAY) &&
            (supportContext == null || supportContext == SupportContext.PREFER_NOT_TO_SAY) &&
            quickReach == null
}

private fun Likert?.isNoSignal(): Boolean =
    this == null || this == Likert.PREFER_NOT_TO_SAY || this == Likert.NOT_REALLY
