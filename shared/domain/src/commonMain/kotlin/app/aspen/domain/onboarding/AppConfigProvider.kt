package app.aspen.domain.onboarding

import app.aspen.domain.onboarding.model.AppConfig
import app.aspen.domain.onboarding.model.OnboardingAnswers
import app.aspen.domain.onboarding.model.ProfileMappingProvenance

/**
 * Resolves the live [AppConfig] the feature layer reads, from whatever profile is currently stored
 * (docs/11 §4). This is the single read-path for adaptivity: tools, companion tone, and — critically —
 * whether food logging is offered ([LoggingService] consults it before any food-log write).
 *
 * **Safe by default:** when no profile is stored (first run, reset, or an unreadable store), it falls
 * back to the [SAFEST] configuration — the `MIXED_OR_UNSURE` result, which has food logging OFF
 * (docs/11 §2, §4). Adaptivity can only ever *open up* from the safest baseline once a profile exists.
 */
class AppConfigProvider(
    private val profileStore: ProfileStore,
    private val provenance: ProfileMappingProvenance = ProfileMappingProvenance.PROVISIONAL,
) {
    fun current(): AppConfig {
        val result = profileStore.current() ?: SAFEST
        return ProfileBehaviourMap.deriveConfig(result, provenance)
    }

    private companion object {
        /** No profile yet → score an empty questionnaire → MIXED_OR_UNSURE, the safest config. */
        val SAFEST = OnboardingScoring.deriveProfile(OnboardingAnswers())
    }
}
