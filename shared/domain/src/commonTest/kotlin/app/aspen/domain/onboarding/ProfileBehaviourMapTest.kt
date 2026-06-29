package app.aspen.domain.onboarding

import app.aspen.domain.onboarding.model.FoodLoggingMode
import app.aspen.domain.onboarding.model.OnboardingResult
import app.aspen.domain.onboarding.model.ProtectiveFlag
import app.aspen.domain.onboarding.model.RoutingHints
import app.aspen.domain.onboarding.model.SupportProfile
import app.aspen.domain.onboarding.model.SupportRoutingStrength
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The mapping decides whether contraindicated food logging is shown, so its safety invariants get
 * no-compromise coverage (docs/09 §4, docs/11 §4). Until advisor sign-off the mapping is provisional
 * (docs/07 Phase 3 `[APPROVE]`) — that provenance must be carried, not silently dropped.
 */
class ProfileBehaviourMapTest {

    private fun resultFor(
        profile: SupportProfile,
        flags: Set<ProtectiveFlag> = emptySet(),
        strength: SupportRoutingStrength = SupportRoutingStrength.STANDARD,
    ) = OnboardingResult(
        profiles = mapOf(profile to 1),
        dominantProfile = profile,
        protectiveFlags = flags,
        routingHints = RoutingHints(strength, offerTrustedContactSetup = false, emphasiseTreatmentFinder = false),
    )

    @Test
    fun `suppress-food-logging flag forces logging OFF for every profile`() {
        // Arrange / Act / Assert — even a profile whose base mode is AVAILABLE goes OFF under the flag.
        SupportProfile.entries.forEach { profile ->
            val config = ProfileBehaviourMap.deriveConfig(
                resultFor(profile, flags = setOf(ProtectiveFlag.SUPPRESS_FOOD_LOGGING)),
            )
            assertEquals(FoodLoggingMode.OFF, config.foodLoggingMode, "profile=$profile must suppress")
        }
    }

    @Test
    fun `restriction and avoidance never produce AVAILABLE food logging`() {
        // Arrange — the protective presentations, even with no explicit flag set on the result.
        listOf(SupportProfile.RESTRICTION_LEANING, SupportProfile.AVOIDANCE_SENSORY).forEach { profile ->
            // Act
            val config = ProfileBehaviourMap.deriveConfig(resultFor(profile))

            // Assert
            assertNotEquals(FoodLoggingMode.AVAILABLE, config.foodLoggingMode, "profile=$profile")
        }
    }

    @Test
    fun `no-body-image-framing flag disables body-image framing`() {
        // Act
        val config = ProfileBehaviourMap.deriveConfig(
            resultFor(SupportProfile.AVOIDANCE_SENSORY, flags = setOf(ProtectiveFlag.NO_BODY_IMAGE_FRAMING)),
        )

        // Assert
        assertFalse(config.bodyImageFramingAllowed)
    }

    @Test
    fun `mixed-or-unsure defaults to food logging off`() {
        // Act
        val config = ProfileBehaviourMap.deriveConfig(resultFor(SupportProfile.MIXED_OR_UNSURE))

        // Assert
        assertEquals(FoodLoggingMode.OFF, config.foodLoggingMode)
    }

    @Test
    fun `every profile yields a valid config without throwing`() {
        // Arrange / Act / Assert — totality of the mapping.
        SupportProfile.entries.forEach { profile ->
            val config = ProfileBehaviourMap.deriveConfig(resultFor(profile))
            assertTrue(config.foodLoggingMode in FoodLoggingMode.entries)
        }
    }

    @Test
    fun `routing strength flows through from the result`() {
        // Act
        val config = ProfileBehaviourMap.deriveConfig(
            resultFor(SupportProfile.MIXED_OR_UNSURE, strength = SupportRoutingStrength.FOREGROUND),
        )

        // Assert
        assertEquals(SupportRoutingStrength.FOREGROUND, config.supportRoutingStrength)
    }

    @Test
    fun `mapping is provisional - not advisor verified - until sign-off`() {
        // Act
        val config = ProfileBehaviourMap.deriveConfig(resultFor(SupportProfile.BINGE_LEANING))

        // Assert — the clinical-review gate (docs/07 Phase 3) is still open.
        assertFalse(config.provenance.advisorVerified)
    }

    @Test
    fun `binge profile offers shame-free logging when no protective flag`() {
        // Act
        val config = ProfileBehaviourMap.deriveConfig(resultFor(SupportProfile.BINGE_LEANING))

        // Assert
        assertEquals(FoodLoggingMode.AVAILABLE, config.foodLoggingMode)
    }
}
