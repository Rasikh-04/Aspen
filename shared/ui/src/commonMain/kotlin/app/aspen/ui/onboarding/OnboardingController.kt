package app.aspen.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.aspen.domain.onboarding.OnboardingScoring
import app.aspen.domain.onboarding.model.OnboardingAnswers
import app.aspen.domain.onboarding.model.OnboardingResult

/**
 * Drives the onboarding questionnaire UI (Flow 0 — docs/06 §3, docs/11). A plain Compose state holder
 * (no ViewModel library in the catalog) that owns the in-progress [OnboardingAnswers] and a step
 * cursor. It defers all scoring to the domain [OnboardingScoring]; the UI never derives or shows a
 * profile/label/score itself (CLAUDE.md #9).
 *
 * Answers are replaced immutably on every edit (`copy()`), never mutated in place (coding-style).
 * Skipping (advancing without editing, or [skipAll]) leaves answers at their safe defaults, which the
 * domain scores to `MIXED_OR_UNSURE` — the safest configuration (docs/11 §3.3, §4).
 */
class OnboardingController {

    /** The in-progress answers. Starts fully "skipped"; each question edits one field. */
    var answers by mutableStateOf(OnboardingAnswers())
        private set

    /** Step cursor: [STEP_INTRO], then 1..[QUESTION_COUNT] questions, then [stepClosing]. */
    var step by mutableStateOf(STEP_INTRO)
        private set

    val isIntro: Boolean get() = step == STEP_INTRO
    val isClosing: Boolean get() = step == stepClosing
    val isQuestion: Boolean get() = step in 1..QUESTION_COUNT

    /** 1-based index of the current question (only meaningful while [isQuestion]). */
    val questionIndex: Int get() = step
    val questionCount: Int get() = QUESTION_COUNT

    /** Replace answers immutably. */
    fun edit(transform: (OnboardingAnswers) -> OnboardingAnswers) {
        answers = transform(answers)
    }

    /** Advance one step (intro → Q1 → … → Q10 → closing). Skipping a question = advancing unedited. */
    fun next() {
        if (step <= QUESTION_COUNT) step += 1
    }

    fun back() {
        if (step > STEP_INTRO) step -= 1
    }

    /** "Skip these for now": jump straight to the closing screen, leaving answers safe-default. */
    fun skipAll() {
        step = stepClosing
    }

    /** Score the current answers into the internal result (domain owns the heuristic). */
    fun result(): OnboardingResult = OnboardingScoring.deriveProfile(answers)

    private val stepClosing: Int get() = QUESTION_COUNT + 1

    companion object {
        const val STEP_INTRO = 0
        const val QUESTION_COUNT = 10
    }
}
