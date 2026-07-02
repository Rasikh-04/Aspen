package app.aspen.ui.onboarding

import app.aspen.domain.onboarding.model.EatingRelationship
import app.aspen.domain.onboarding.model.HelpWanted
import app.aspen.domain.onboarding.model.ProtectiveFlag
import app.aspen.domain.onboarding.model.SupportProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the onboarding state holder (Flow 0 UI logic). Scoring itself is the domain's job and
 * is tested there (OnboardingScoringTest); here we verify the cursor, immutable edits, and that the
 * controller defers to the domain — never inventing a profile/label of its own (CLAUDE.md #9).
 */
class OnboardingControllerTest {

    @Test
    fun starts_at_intro_with_empty_answers() {
        val c = OnboardingController()
        assertTrue(c.isIntro)
        assertFalse(c.isQuestion)
        assertTrue(c.answers.isEmpty)
    }

    @Test
    fun next_walks_intro_through_questions_to_closing() {
        val c = OnboardingController()
        c.next() // → Q1
        assertTrue(c.isQuestion)
        assertEquals(1, c.questionIndex)
        repeat(OnboardingController.QUESTION_COUNT) { c.next() } // → closing
        assertTrue(c.isClosing)
    }

    @Test
    fun back_returns_toward_intro() {
        val c = OnboardingController()
        c.next(); c.next() // Q2
        c.back()
        assertEquals(1, c.questionIndex)
    }

    @Test
    fun skipAll_jumps_to_closing_leaving_answers_empty() {
        val c = OnboardingController()
        c.next() // into questions
        c.skipAll()
        assertTrue(c.isClosing)
        assertTrue(c.answers.isEmpty)
    }

    @Test
    fun edit_replaces_answers_immutably() {
        val c = OnboardingController()
        val before = c.answers
        c.edit { it.copy(helpWanted = it.helpWanted + HelpWanted.CALMER_MOMENT) }
        assertTrue(before.helpWanted.isEmpty()) // original untouched (immutability)
        assertTrue(c.answers.helpWanted.contains(HelpWanted.CALMER_MOMENT))
    }

    @Test
    fun empty_answers_score_to_safest_mixed_profile() {
        val result = OnboardingController().result()
        assertEquals(SupportProfile.MIXED_OR_UNSURE, result.dominantProfile)
    }

    @Test
    fun restriction_answer_raises_suppress_food_logging_via_domain() {
        val c = OnboardingController()
        c.edit { it.copy(eatingRelationship = setOf(EatingRelationship.TENSE_RULES)) }
        val result = c.result()
        assertTrue(ProtectiveFlag.SUPPRESS_FOOD_LOGGING in result.protectiveFlags)
    }
}
