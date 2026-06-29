package app.aspen.domain.onboarding

import app.aspen.domain.onboarding.model.BodyImageSalience
import app.aspen.domain.onboarding.model.EatingRelationship
import app.aspen.domain.onboarding.model.LifeImpact
import app.aspen.domain.onboarding.model.Likert
import app.aspen.domain.onboarding.model.OnboardingAnswers
import app.aspen.domain.onboarding.model.OnboardingResult
import app.aspen.domain.onboarding.model.ProtectiveFlag
import app.aspen.domain.onboarding.model.RoutingHints
import app.aspen.domain.onboarding.model.SensoryDriver
import app.aspen.domain.onboarding.model.SupportContext
import app.aspen.domain.onboarding.model.SupportProfile
import app.aspen.domain.onboarding.model.SupportRoutingStrength
import app.aspen.domain.onboarding.model.QuickReachPreference

/**
 * Scores the onboarding questionnaire into an internal [OnboardingResult] (docs/11 §4).
 *
 * This is a **tailoring heuristic, not a diagnostic test** (docs/11 §0): the validated screeners owe
 * their accuracy to the numeric/body-shame items Aspen forbids, so once softened and numberless they
 * are no longer validated instruments. The output is never shown as a label or score (CLAUDE.md #9).
 *
 * Properties this guarantees (all covered by tests — safety-bearing logic, docs/09 §4):
 * - Never throws; always returns a non-empty profile map.
 * - **Conservative bias** (docs/11 §4): any meaningful restriction or avoidance-sensory signal
 *   raises [ProtectiveFlag.SUPPRESS_FOOD_LOGGING] even when another profile scores higher.
 * - A clear ARFID/sensory signal (Q6 `YES`) raises [ProtectiveFlag.NO_BODY_IMAGE_FRAMING] and
 *   down-weights body-image scoring entirely (docs/11 Q6).
 * - Ties, low signal, or "skip all" resolve to [SupportProfile.MIXED_OR_UNSURE] (safest config).
 */
object OnboardingScoring {

    // Soft weights — internal scoring only, never surfaced; not food/body numbers (CLAUDE.md #1).
    private const val LIKERT_SOMETIMES = 1
    private const val LIKERT_OFTEN = 2
    private const val RELATIONSHIP_SIGNAL = 2
    private const val SENSORY_SOMEWHAT = 2
    private const val SENSORY_YES = 3
    private const val BODY_SALIENCE_SOMETIMES = 1
    private const val BODY_SALIENCE_A_LOT = 2

    fun deriveProfile(answers: OnboardingAnswers): OnboardingResult {
        val weights = scoreProfiles(answers)
        val sensoryStrong = answers.sensoryDriver == SensoryDriver.YES

        // Q6 YES down-weights body-image framing entirely (docs/11 Q6).
        val adjusted = if (sensoryStrong) {
            weights - SupportProfile.BODY_IMAGE_DISTRESS
        } else {
            weights
        }

        val dominant = dominantProfile(adjusted)
        val flags = protectiveFlags(adjusted, sensoryStrong)

        // Result always carries a non-empty map; MIXED_OR_UNSURE is the floor.
        val profiles = adjusted.ifEmpty { mapOf(SupportProfile.MIXED_OR_UNSURE to 0) }

        return OnboardingResult(
            profiles = profiles,
            dominantProfile = dominant,
            protectiveFlags = flags,
            routingHints = routingHints(answers),
        )
    }

    private fun scoreProfiles(answers: OnboardingAnswers): Map<SupportProfile, Int> {
        val tally = mutableMapOf<SupportProfile, Int>()
        fun add(profile: SupportProfile, weight: Int) {
            if (weight > 0) tally[profile] = (tally[profile] ?: 0) + weight
        }

        // Q2 — relationship descriptors (multi-select).
        answers.eatingRelationship.forEach { rel ->
            when (rel) {
                EatingRelationship.TENSE_RULES -> add(SupportProfile.RESTRICTION_LEANING, RELATIONSHIP_SIGNAL)
                EatingRelationship.OUT_OF_CONTROL -> add(SupportProfile.BINGE_LEANING, RELATIONSHIP_SIGNAL)
                EatingRelationship.UNDO_AFTER -> add(SupportProfile.PURGE_COMPENSATORY, RELATIONSHIP_SIGNAL)
                EatingRelationship.SENSORY_HARD -> add(SupportProfile.AVOIDANCE_SENSORY, RELATIONSHIP_SIGNAL)
                EatingRelationship.BODY_FOCUSED -> add(SupportProfile.BODY_IMAGE_DISTRESS, RELATIONSHIP_SIGNAL)
                EatingRelationship.VARIES, EatingRelationship.PREFER_NOT_TO_SAY -> Unit
            }
        }

        // Q3–Q5 — Likert behaviour signals.
        add(SupportProfile.RESTRICTION_LEANING, likertWeight(answers.holdingBack))
        add(SupportProfile.BINGE_LEANING, likertWeight(answers.lossOfControl))
        add(SupportProfile.PURGE_COMPENSATORY, likertWeight(answers.urgeToCompensate))

        // Q6 — ARFID/sensory.
        add(SupportProfile.AVOIDANCE_SENSORY, sensoryWeight(answers.sensoryDriver))

        // Q7 — body-image salience.
        add(SupportProfile.BODY_IMAGE_DISTRESS, bodySalienceWeight(answers.bodyImageSalience))

        return tally.toMap()
    }

    private fun likertWeight(value: Likert?): Int = when (value) {
        Likert.SOMETIMES -> LIKERT_SOMETIMES
        Likert.OFTEN -> LIKERT_OFTEN
        Likert.NOT_REALLY, Likert.PREFER_NOT_TO_SAY, null -> 0
    }

    private fun sensoryWeight(value: SensoryDriver?): Int = when (value) {
        SensoryDriver.YES -> SENSORY_YES
        SensoryDriver.SOMEWHAT -> SENSORY_SOMEWHAT
        SensoryDriver.NO, SensoryDriver.PREFER_NOT_TO_SAY, null -> 0
    }

    private fun bodySalienceWeight(value: BodyImageSalience?): Int = when (value) {
        BodyImageSalience.SOMETIMES -> BODY_SALIENCE_SOMETIMES
        BodyImageSalience.A_LOT -> BODY_SALIENCE_A_LOT
        BodyImageSalience.NOT_MUCH, BodyImageSalience.PREFER_NOT_TO_SAY, null -> 0
    }

    /**
     * Highest-weighted profile. A tie at the top, or no signal at all, resolves to
     * [SupportProfile.MIXED_OR_UNSURE] — the safest configuration (docs/11 §4).
     */
    private fun dominantProfile(weights: Map<SupportProfile, Int>): SupportProfile {
        val max = weights.values.maxOrNull() ?: return SupportProfile.MIXED_OR_UNSURE
        if (max <= 0) return SupportProfile.MIXED_OR_UNSURE
        val leaders = weights.filterValues { it == max }.keys
        return if (leaders.size == 1) leaders.first() else SupportProfile.MIXED_OR_UNSURE
    }

    /**
     * Conservative bias (docs/11 §4): any meaningful restriction or avoidance-sensory signal forces
     * the protective configuration regardless of which profile dominates. ARFID strong signal also
     * removes body-image framing.
     */
    private fun protectiveFlags(
        weights: Map<SupportProfile, Int>,
        sensoryStrong: Boolean,
    ): Set<ProtectiveFlag> {
        val flags = mutableSetOf<ProtectiveFlag>()
        val restriction = (weights[SupportProfile.RESTRICTION_LEANING] ?: 0) > 0
        val avoidance = (weights[SupportProfile.AVOIDANCE_SENSORY] ?: 0) > 0
        if (restriction || avoidance) flags += ProtectiveFlag.SUPPRESS_FOOD_LOGGING
        if (avoidance || sensoryStrong) flags += ProtectiveFlag.NO_BODY_IMAGE_FRAMING
        return flags
    }

    /** Q8–Q10 → routing strength + setup offers (docs/11 §3, §4). Not a profile, not a risk screen. */
    private fun routingHints(answers: OnboardingAnswers): RoutingHints {
        val strength = when (answers.lifeImpact) {
            LifeImpact.A_LOT -> SupportRoutingStrength.FOREGROUND
            LifeImpact.SOME -> SupportRoutingStrength.ELEVATED
            LifeImpact.A_LITTLE, LifeImpact.PREFER_NOT_TO_SAY, null -> SupportRoutingStrength.STANDARD
        }
        val noSupport = answers.supportContext == SupportContext.NONE_RIGHT_NOW
        val offerTrustedContact =
            answers.quickReach == QuickReachPreference.SET_UP_NOW || noSupport
        val emphasiseFinder = noSupport || answers.lifeImpact == LifeImpact.A_LOT
        return RoutingHints(
            supportRoutingStrength = strength,
            offerTrustedContactSetup = offerTrustedContact,
            emphasiseTreatmentFinder = emphasiseFinder,
        )
    }
}
