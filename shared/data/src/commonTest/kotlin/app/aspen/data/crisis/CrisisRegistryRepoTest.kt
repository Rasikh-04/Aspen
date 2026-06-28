package app.aspen.data.crisis

import app.aspen.domain.safety.model.LocaleKey
import app.aspen.domain.safety.model.Purpose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The crisis-path contract tests — the highest-stakes resolver in the app, so they are exhaustive
 * over every [LocaleKey]. They prove the docs/09 §2.2 guarantees: never empty, never throws, fully
 * offline (these run with no network/device), region-correct, INTL fallback for anything unserved.
 */
class CrisisRegistryRepoTest {

    private val repo = CrisisRegistryRepo()

    @Test
    fun resolveIsNeverEmptyForEveryLocale() {
        for (locale in LocaleKey.entries) {
            val set = repo.resolve(locale)
            assertFalse(set.isEmpty(), "resolve($locale) returned an empty set — crisis path must never be empty")
        }
    }

    @Test
    fun enabledLaunchLocalesResolveDirectlyAndAreNotFallback() {
        for (locale in CrisisRegistry.enabledLocales) {
            val set = repo.resolve(locale)
            assertEquals(locale, set.locale, "enabled locale $locale should resolve to itself")
            assertFalse(set.isFallback, "enabled locale $locale should not be a fallback")
        }
    }

    @Test
    fun pkDeUkAreTheEnabledLaunchLocales() {
        assertEquals(setOf(LocaleKey.PK, LocaleKey.DE, LocaleKey.UK), CrisisRegistry.enabledLocales)
    }

    @Test
    fun disabledUsFallsBackToIntl() {
        val set = repo.resolve(LocaleKey.US)
        assertEquals(LocaleKey.INTL, set.locale, "US is present-but-disabled and must fall back to INTL")
        assertTrue(set.isFallback, "US resolution is a fallback")
    }

    @Test
    fun intlResolvesToItselfWithoutBeingMarkedFallback() {
        val set = repo.resolve(LocaleKey.INTL)
        assertEquals(LocaleKey.INTL, set.locale)
        assertFalse(set.isFallback, "INTL requested directly is not a fallback from elsewhere")
        assertFalse(set.isEmpty())
    }

    @Test
    fun everyResolvedSetHasAtLeastOneReachablePathToAHuman() {
        // ≤2 taps to a person (CLAUDE.md #6): every region must surface at least one acute-crisis
        // OR ed-support resource, never only a treatment-finder link.
        for (locale in LocaleKey.entries) {
            val set = repo.resolve(locale)
            val hasHumanRoute = set.acuteCrisis.isNotEmpty() || set.edSupport.isNotEmpty()
            assertTrue(hasHumanRoute, "resolve($locale) must offer a support/crisis human route, not only a finder")
        }
    }

    @Test
    fun resourcesAreGroupedByTheirOwnPurpose() {
        val set = repo.resolve(LocaleKey.UK)
        assertTrue(set.edSupport.all { it.purpose == Purpose.ED_SUPPORT })
        assertTrue(set.acuteCrisis.all { it.purpose == Purpose.ACUTE_CRISIS })
        assertTrue(set.treatmentFinder.all { it.purpose == Purpose.TREATMENT_FINDER })
    }

    @Test
    fun nedaAppearsNowhereInTheRegistry() {
        // CLAUDE.md #7: NEDA is disconnected and must never appear in crisis data, any field.
        for ((_, resources) in CrisisRegistry.byLocale) {
            for (r in resources) {
                val haystack = (r.name + " " + (r.notes ?: "") + " " + r.contacts.joinToString { it.label + " " + it.value }).uppercase()
                assertFalse(haystack.contains("NEDA"), "NEDA found in resource ${r.id}")
            }
        }
    }
}
