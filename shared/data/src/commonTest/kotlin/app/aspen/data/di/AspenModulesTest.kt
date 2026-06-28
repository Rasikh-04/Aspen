package app.aspen.data.di

import app.aspen.domain.consent.ConsentManager
import app.aspen.domain.consent.model.DataCategory
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
}
