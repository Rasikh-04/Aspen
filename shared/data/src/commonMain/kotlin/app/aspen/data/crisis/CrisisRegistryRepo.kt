package app.aspen.data.crisis

import app.aspen.domain.safety.CrisisResolver
import app.aspen.domain.safety.model.CrisisResource
import app.aspen.domain.safety.model.CrisisResourceSet
import app.aspen.domain.safety.model.LocaleKey
import app.aspen.domain.safety.model.Purpose

/**
 * The offline [CrisisResolver] (docs/09 §2.2). Resolves a region to a usable [CrisisResourceSet]
 * from the in-code [CrisisRegistry] — no network, no file IO, no platform code on the crisis path.
 *
 * Contract, enforced by [app.aspen.data.crisis.CrisisRegistryRepoTest]:
 * - **Never empty, never throws.** Every path ends at a non-empty set; the worst case is the INTL
 *   fallback, which the registry always carries.
 * - A region is served directly only if it is in [CrisisRegistry.enabledLocales]; otherwise (unknown,
 *   disabled like US, or a region with no data) it falls back to INTL with [CrisisResourceSet.isFallback] = true.
 * - Resolving INTL itself returns the INTL set with `isFallback = false` (it is the requested region,
 *   not a fallback from somewhere else).
 */
class CrisisRegistryRepo(
    private val byLocale: Map<LocaleKey, List<CrisisResource>> = CrisisRegistry.byLocale,
    private val enabledLocales: Set<LocaleKey> = CrisisRegistry.enabledLocales,
) : CrisisResolver {

    override fun resolve(locale: LocaleKey): CrisisResourceSet {
        val enabled = locale in enabledLocales
        val direct = if (enabled) byLocale[locale].orEmpty() else emptyList()

        if (direct.isNotEmpty()) {
            return groupInto(locale, direct, isFallback = false)
        }
        // Fallback path: anything not directly served lands on INTL. Resolving INTL directly is not
        // a "fallback" — it is the requested region — so only mark isFallback when we came from elsewhere.
        val intl = byLocale[LocaleKey.INTL].orEmpty()
        return groupInto(LocaleKey.INTL, intl, isFallback = locale != LocaleKey.INTL)
    }

    private fun groupInto(
        locale: LocaleKey,
        resources: List<CrisisResource>,
        isFallback: Boolean,
    ): CrisisResourceSet = CrisisResourceSet(
        locale = locale,
        edSupport = resources.filter { it.purpose == Purpose.ED_SUPPORT },
        acuteCrisis = resources.filter { it.purpose == Purpose.ACUTE_CRISIS },
        treatmentFinder = resources.filter { it.purpose == Purpose.TREATMENT_FINDER },
        isFallback = isFallback,
    )
}
