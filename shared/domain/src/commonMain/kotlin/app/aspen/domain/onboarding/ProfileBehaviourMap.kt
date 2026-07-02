package app.aspen.domain.onboarding

import app.aspen.domain.onboarding.model.AppConfig
import app.aspen.domain.onboarding.model.CompanionTone
import app.aspen.domain.onboarding.model.FoodLoggingMode
import app.aspen.domain.onboarding.model.OnboardingResult
import app.aspen.domain.onboarding.model.ProfileMappingProvenance
import app.aspen.domain.onboarding.model.ProtectiveFlag
import app.aspen.domain.onboarding.model.SupportProfile
import app.aspen.domain.onboarding.model.ToolEmphasis

/**
 * Maps a scored [OnboardingResult] to the adaptive [AppConfig] the feature layer reads
 * (`deriveConfig`, docs/11 §4). Pure and total — every profile yields a valid config.
 *
 * Safety rules baked in (advisor-gated; [ProfileMappingProvenance.PROVISIONAL] until signed):
 * - [ProtectiveFlag.SUPPRESS_FOOD_LOGGING] forces [FoodLoggingMode.OFF] regardless of the dominant
 *   profile (conservative bias, docs/11 §4). Restriction/avoidance never get `AVAILABLE`.
 * - [ProtectiveFlag.NO_BODY_IMAGE_FRAMING] forces `bodyImageFramingAllowed = false` (ARFID, Q6).
 * - Behaviour/feeling logging is not gated here — it stays broadly available (docs/03 FR-3b).
 */
object ProfileBehaviourMap {

    fun deriveConfig(
        result: OnboardingResult,
        provenance: ProfileMappingProvenance = ProfileMappingProvenance.PROVISIONAL,
    ): AppConfig {
        val flags = result.protectiveFlags
        val profile = result.dominantProfile

        val baseLoggingMode = baseFoodLoggingMode(profile)
        val foodLoggingMode =
            if (ProtectiveFlag.SUPPRESS_FOOD_LOGGING in flags) FoodLoggingMode.OFF else baseLoggingMode

        val bodyImageAllowed = ProtectiveFlag.NO_BODY_IMAGE_FRAMING !in flags

        return AppConfig(
            foodLoggingMode = foodLoggingMode,
            companionTone = companionTone(profile),
            toolEmphasis = toolEmphasis(profile),
            supportRoutingStrength = result.routingHints.supportRoutingStrength,
            bodyImageFramingAllowed = bodyImageAllowed,
            provenance = provenance,
        )
    }

    private fun baseFoodLoggingMode(profile: SupportProfile): FoodLoggingMode = when (profile) {
        SupportProfile.RESTRICTION_LEANING -> FoodLoggingMode.OFF
        SupportProfile.AVOIDANCE_SENSORY -> FoodLoggingMode.OFF
        SupportProfile.MIXED_OR_UNSURE -> FoodLoggingMode.OFF // off by default (docs/11 §2)
        SupportProfile.PURGE_COMPENSATORY -> FoodLoggingMode.REFRAMED // gentle (docs/11 §2)
        SupportProfile.BINGE_LEANING -> FoodLoggingMode.AVAILABLE // optional, shame-free
        SupportProfile.BODY_IMAGE_DISTRESS -> FoodLoggingMode.AVAILABLE
    }

    private fun companionTone(profile: SupportProfile): CompanionTone = when (profile) {
        SupportProfile.RESTRICTION_LEANING -> CompanionTone.RESTRICTION_SENSITIVE
        SupportProfile.BINGE_LEANING -> CompanionTone.SELF_COMPASSION
        SupportProfile.PURGE_COMPENSATORY -> CompanionTone.POST_DISTRESS
        SupportProfile.AVOIDANCE_SENSORY -> CompanionTone.SENSORY_AWARE
        SupportProfile.BODY_IMAGE_DISTRESS -> CompanionTone.SELF_COMPASSION
        SupportProfile.MIXED_OR_UNSURE -> CompanionTone.GENTLE_NEUTRAL
    }

    private fun toolEmphasis(profile: SupportProfile): ToolEmphasis = when (profile) {
        SupportProfile.RESTRICTION_LEANING -> ToolEmphasis.GROUNDING
        SupportProfile.BINGE_LEANING -> ToolEmphasis.URGE_SURFING
        SupportProfile.PURGE_COMPENSATORY -> ToolEmphasis.POST_EATING_DISTRESS
        SupportProfile.AVOIDANCE_SENSORY -> ToolEmphasis.SENSORY
        SupportProfile.BODY_IMAGE_DISTRESS -> ToolEmphasis.SELF_WORTH
        SupportProfile.MIXED_OR_UNSURE -> ToolEmphasis.BALANCED
    }
}
