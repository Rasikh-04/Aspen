package app.aspen.data.onboarding

import app.aspen.domain.onboarding.model.OnboardingResult
import app.aspen.domain.onboarding.model.ProtectiveFlag
import app.aspen.domain.onboarding.model.RoutingHints
import app.aspen.domain.onboarding.model.SupportProfile
import app.aspen.domain.onboarding.model.SupportRoutingStrength
import kotlinx.serialization.Serializable

/**
 * Serialization DTO for the stored support profile. The domain model stays pure (no `@Serializable`);
 * this mirror carries the wire shape. Enums are stored as strings so an unrecognised value fails to
 * map (→ treated as "no profile" → safest default) rather than silently coercing — see [toDomainOrNull].
 */
@Serializable
data class ProfileStateDto(
    val profiles: Map<String, Int> = emptyMap(),
    val dominantProfile: String? = null,
    val protectiveFlags: List<String> = emptyList(),
    val routing: RoutingDto? = null,
)

@Serializable
data class RoutingDto(
    val strength: String,
    val offerTrustedContactSetup: Boolean,
    val emphasiseTreatmentFinder: Boolean,
)

fun OnboardingResult.toDto(): ProfileStateDto = ProfileStateDto(
    profiles = profiles.entries.associate { (k, v) -> k.name to v },
    dominantProfile = dominantProfile.name,
    protectiveFlags = protectiveFlags.map { it.name },
    routing = RoutingDto(
        strength = routingHints.supportRoutingStrength.name,
        offerTrustedContactSetup = routingHints.offerTrustedContactSetup,
        emphasiseTreatmentFinder = routingHints.emphasiseTreatmentFinder,
    ),
)

/**
 * Map back to the domain result, or null if the core fields can't be read (corrupt → "no profile" →
 * the [app.aspen.domain.onboarding.AppConfigProvider] safest default). Unknown profile keys are
 * skipped leniently so one bad entry can't lose the whole profile.
 */
fun ProfileStateDto.toDomainOrNull(): OnboardingResult? = runCatching {
    val dominant = SupportProfile.valueOf(dominantProfile ?: return null)
    val routingDto = routing ?: return null
    OnboardingResult(
        profiles = profiles.mapNotNull { (k, v) ->
            runCatching { SupportProfile.valueOf(k) to v }.getOrNull()
        }.toMap(),
        dominantProfile = dominant,
        protectiveFlags = protectiveFlags.mapNotNull {
            runCatching { ProtectiveFlag.valueOf(it) }.getOrNull()
        }.toSet(),
        routingHints = RoutingHints(
            supportRoutingStrength = SupportRoutingStrength.valueOf(routingDto.strength),
            offerTrustedContactSetup = routingDto.offerTrustedContactSetup,
            emphasiseTreatmentFinder = routingDto.emphasiseTreatmentFinder,
        ),
    )
}.getOrNull()
