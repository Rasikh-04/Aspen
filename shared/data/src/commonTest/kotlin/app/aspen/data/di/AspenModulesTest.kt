package app.aspen.data.di

import app.aspen.data.ai.cloud.DisabledAiClient
import app.aspen.domain.ai.AiClient
import app.aspen.domain.ai.CompanionMoment
import app.aspen.domain.ai.CompanionVoice
import app.aspen.domain.ai.ReflectionCompanion
import app.aspen.domain.consent.ConsentManager
import app.aspen.domain.consent.model.DataCategory
import app.aspen.domain.logging.LoggingService
import app.aspen.domain.onboarding.model.AppConfig
import app.aspen.domain.onboarding.model.CompanionTone
import app.aspen.domain.onboarding.model.FoodLoggingMode
import app.aspen.domain.onboarding.model.ProfileMappingProvenance
import app.aspen.domain.onboarding.model.SupportRoutingStrength
import app.aspen.domain.onboarding.model.ToolEmphasis
import app.aspen.domain.safety.CrisisResolver
import app.aspen.domain.safety.SafetyEngine
import app.aspen.domain.safety.model.LocaleKey
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * Proves the Koin graph resolves end-to-end (docs/09). Binds a test [SafetyFallbackCopy] exactly as
 * the app would, then resolves and smoke-checks each public dependency.
 */
class AspenModulesTest {

    private fun koin() = koinApplication {
        modules(aspenSharedModules + module { single { SafetyFallbackCopy("test fallback") } })
    }.koin

    @Test
    fun safetyEngineResolvesAndReturnsOfflineCrisisResources() {
        val engine = koin().get<SafetyEngine>()
        val set = engine.crisis(LocaleKey.UK)
        assertNotNull(set)
        assertFalse(set.isEmpty(), "wired SafetyEngine must return non-empty crisis resources")
    }

    @Test
    fun crisisResolverIsBoundToTheOfflineRegistry() {
        val resolver = koin().get<CrisisResolver>()
        // unknown/disabled region must fall back to INTL, never empty
        assertTrue(resolver.resolve(LocaleKey.US).isFallback)
        assertFalse(resolver.resolve(LocaleKey.US).isEmpty())
    }

    @Test
    fun consentManagerResolvesAndDefaultsDeny() {
        val manager = koin().get<ConsentManager>()
        assertFalse(manager.canAccess("anyone", DataCategory.REFLECTIONS), "default-deny out of the box")
    }

    @Test
    fun loggingServiceResolvesAndSuppressesFoodLoggingByDefault() {
        // No profile stored → safest config → food logging off out of the box (Phase 3 wiring).
        val logging = koin().get<LoggingService>()
        assertFalse(logging.isFoodLoggingOffered(), "food logging suppressed until a profile permits it")
    }

    @Test
    fun cloudAiIsProvablyOffByDefault() {
        // Phase 4 (docs/04 ADR-003): the DI default is the DisabledAiClient — no endpoint, no key,
        // and the reflection surface reports itself disabled until an explicit consent grant.
        assertTrue(koin().get<AiClient>() === DisabledAiClient, "cloud client must default to Disabled")
        assertFalse(koin().get<ReflectionCompanion>().isEnabled(), "cloud reflection is off by default")
    }

    @Test
    fun companionVoiceResolvesAndSpeaksFromTheLibrary() {
        val voice = koin().get<CompanionVoice>()
        val config = AppConfig(
            foodLoggingMode = FoodLoggingMode.OFF,
            companionTone = CompanionTone.GENTLE_NEUTRAL,
            toolEmphasis = ToolEmphasis.BALANCED,
            supportRoutingStrength = SupportRoutingStrength.STANDARD,
            bodyImageFramingAllowed = false,
            provenance = ProfileMappingProvenance.PROVISIONAL,
        )
        val line = voice.line(CompanionMoment.GREETING, config)
        assertTrue(line.key.startsWith("companion_"), "voice must return curated library keys")
    }
}
