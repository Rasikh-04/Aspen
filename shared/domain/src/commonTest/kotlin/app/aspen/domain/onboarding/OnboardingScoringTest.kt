package app.aspen.domain.onboarding

import app.aspen.domain.onboarding.model.BodyImageSalience
import app.aspen.domain.onboarding.model.EatingRelationship
import app.aspen.domain.onboarding.model.LifeImpact
import app.aspen.domain.onboarding.model.Likert
import app.aspen.domain.onboarding.model.OnboardingAnswers
import app.aspen.domain.onboarding.model.ProtectiveFlag
import app.aspen.domain.onboarding.model.QuickReachPreference
import app.aspen.domain.onboarding.model.SensoryDriver
import app.aspen.domain.onboarding.model.SupportContext
import app.aspen.domain.onboarding.model.SupportProfile
import app.aspen.domain.onboarding.model.SupportRoutingStrength
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Scoring is safety-bearing (it decides whether contraindicated logging is suppressed), so coverage
 * here is no-compromise (docs/09 §4): conservative bias, ARFID handling, ties, and skip-all.
 */
class OnboardingScoringTest {

    @Test
    fun `skip all resolves to mixed-or-unsure with no profile signal`() {
        // Arrange
        val answers = OnboardingAnswers()

        // Act
        val result = OnboardingScoring.deriveProfile(answers)

        // Assert
        assertEquals(SupportProfile.MIXED_OR_UNSURE, result.dominantProfile)
        assertTrue(answers.isEmpty)
        assertTrue(result.profiles.isNotEmpty(), "profile map is never empty")
    }

    @Test
    fun `dominant profile is the highest-weighted single signal`() {
        // Arrange — strong binge signal only.
        val answers = OnboardingAnswers(
            lossOfControl = Likert.OFTEN,
            eatingRelationship = setOf(EatingRelationship.OUT_OF_CONTROL),
        )

        // Act
        val result = OnboardingScoring.deriveProfile(answers)

        // Assert
        assertEquals(SupportProfile.BINGE_LEANING, result.dominantProfile)
    }

    @Test
    fun `restriction signal raises suppress-food-logging even when another profile scores higher`() {
        // Arrange — strong binge dominance, but a real restriction signal present.
        val answers = OnboardingAnswers(
            lossOfControl = Likert.OFTEN,
            eatingRelationship = setOf(EatingRelationship.OUT_OF_CONTROL),
            holdingBack = Likert.SOMETIMES, // meaningful restriction signal
        )

        // Act
        val result = OnboardingScoring.deriveProfile(answers)

        // Assert — binge still dominates, but the protective flag is raised regardless.
        assertEquals(SupportProfile.BINGE_LEANING, result.dominantProfile)
        assertContains(result.protectiveFlags, ProtectiveFlag.SUPPRESS_FOOD_LOGGING)
    }

    @Test
    fun `avoidance-sensory yes raises both protective flags and down-weights body image`() {
        // Arrange — clear ARFID signal alongside a body-image signal.
        val answers = OnboardingAnswers(
            sensoryDriver = SensoryDriver.YES,
            bodyImageSalience = BodyImageSalience.A_LOT,
        )

        // Act
        val result = OnboardingScoring.deriveProfile(answers)

        // Assert — body-image weight removed; ARFID dominates; both flags raised.
        assertEquals(SupportProfile.AVOIDANCE_SENSORY, result.dominantProfile)
        assertFalse(result.profiles.containsKey(SupportProfile.BODY_IMAGE_DISTRESS))
        assertContains(result.protectiveFlags, ProtectiveFlag.SUPPRESS_FOOD_LOGGING)
        assertContains(result.protectiveFlags, ProtectiveFlag.NO_BODY_IMAGE_FRAMING)
    }

    @Test
    fun `equal top weights resolve to mixed-or-unsure`() {
        // Arrange — restriction and binge tied at the top.
        val answers = OnboardingAnswers(
            holdingBack = Likert.OFTEN,
            lossOfControl = Likert.OFTEN,
        )

        // Act
        val result = OnboardingScoring.deriveProfile(answers)

        // Assert — a top-tie is ambiguous → safest config.
        assertEquals(SupportProfile.MIXED_OR_UNSURE, result.dominantProfile)
    }

    @Test
    fun `prefer-not-to-say and not-really contribute no signal`() {
        // Arrange
        val answers = OnboardingAnswers(
            holdingBack = Likert.NOT_REALLY,
            lossOfControl = Likert.PREFER_NOT_TO_SAY,
            sensoryDriver = SensoryDriver.PREFER_NOT_TO_SAY,
            eatingRelationship = setOf(EatingRelationship.PREFER_NOT_TO_SAY, EatingRelationship.VARIES),
        )

        // Act
        val result = OnboardingScoring.deriveProfile(answers)

        // Assert
        assertEquals(SupportProfile.MIXED_OR_UNSURE, result.dominantProfile)
        assertTrue(result.protectiveFlags.isEmpty())
    }

    @Test
    fun `body-image salience without sensory signal yields body-image profile`() {
        // Arrange
        val answers = OnboardingAnswers(bodyImageSalience = BodyImageSalience.A_LOT)

        // Act
        val result = OnboardingScoring.deriveProfile(answers)

        // Assert
        assertEquals(SupportProfile.BODY_IMAGE_DISTRESS, result.dominantProfile)
        assertFalse(result.protectiveFlags.contains(ProtectiveFlag.NO_BODY_IMAGE_FRAMING))
    }

    @Test
    fun `high life impact and no support foreground routing and finder`() {
        // Arrange
        val answers = OnboardingAnswers(
            lifeImpact = LifeImpact.A_LOT,
            supportContext = SupportContext.NONE_RIGHT_NOW,
            quickReach = QuickReachPreference.SET_UP_NOW,
        )

        // Act
        val hints = OnboardingScoring.deriveProfile(answers).routingHints

        // Assert
        assertEquals(SupportRoutingStrength.FOREGROUND, hints.supportRoutingStrength)
        assertTrue(hints.offerTrustedContactSetup)
        assertTrue(hints.emphasiseTreatmentFinder)
    }

    @Test
    fun `default routing is standard with no setup offers`() {
        // Act
        val hints = OnboardingScoring.deriveProfile(OnboardingAnswers()).routingHints

        // Assert
        assertEquals(SupportRoutingStrength.STANDARD, hints.supportRoutingStrength)
        assertFalse(hints.offerTrustedContactSetup)
        assertFalse(hints.emphasiseTreatmentFinder)
    }
}
