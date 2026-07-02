package app.aspen.domain.safety

import app.aspen.domain.safety.model.CrisisResourceSet
import app.aspen.domain.safety.model.LocaleKey

/**
 * Resolves a region to a usable set of crisis/support resources (docs/09 §2.2).
 *
 * Hard contracts (the reason safety is built first and tested hardest):
 * - **Never empty, never throws** — always at least the [LocaleKey.INTL] fallback.
 * - **Fully offline** — resolves from bundled/in-app data, no network on the crisis path.
 * - **Pure/deterministic** — trivially unit-testable for every locale.
 */
interface CrisisResolver {
    fun resolve(locale: LocaleKey): CrisisResourceSet
}

/** Where an AI candidate string is headed — lets a rewrite hand off to the right region (Phase 4). */
data class OutputContext(val locale: LocaleKey = LocaleKey.INTL)

/** The result of vetting AI output (docs/09 §2.4). */
sealed interface SafetyVerdict {
    /** The candidate is safe to show as-is. */
    data class Pass(val text: String) : SafetyVerdict

    /** The candidate tripped a non-negotiable; show [safeText] (validation + Flow C handoff) instead. */
    data class Rewrite(val safeText: String, val reason: String) : SafetyVerdict
}

/**
 * The safety subsystem façade (docs/09 §2.4). Lives in :shared:domain, isolated from feature churn
 * (CLAUDE.md). Features depend on this, never on the registry or rules directly.
 */
interface SafetyEngine {
    /** Region-correct, offline, never-empty crisis resources for [locale]. */
    fun crisis(locale: LocaleKey): CrisisResourceSet

    /**
     * Vet an AI candidate string. Since Phase 4 this is LIVE on every cloud reply — the mandatory
     * last step of [app.aspen.domain.ai.ReflectionCompanion]'s pipeline (docs/03 SR-3); the
     * red-team suite (config/safety/redteam) release-gates it.
     */
    fun guardOutput(candidate: String, ctx: OutputContext = OutputContext()): SafetyVerdict
}

/**
 * Default façade. [crisisResolver] does the resolving; [safetyRules] backs [guardOutput].
 *
 * [guardOutput] applies the heuristic rules and, on any trip, replaces the candidate with a neutral
 * validating line that points the user at the human exit (Flow C). It does NOT attempt to "fix"
 * unsafe model text — on a violation it withholds and hands off (CLAUDE.md #8). [safeFallbackText]
 * is injected so it stays localized + reviewable. Heuristics stay the BACKSTOP; the front line is
 * the system prompt + curation (docs/09 §2.3).
 */
class DefaultSafetyEngine(
    private val crisisResolver: CrisisResolver,
    private val safetyRules: SafetyRules,
    private val safeFallbackText: String,
) : SafetyEngine {

    override fun crisis(locale: LocaleKey): CrisisResourceSet = crisisResolver.resolve(locale)

    override fun guardOutput(candidate: String, ctx: OutputContext): SafetyVerdict =
        if (safetyRules.violatesAny(candidate)) {
            SafetyVerdict.Rewrite(
                safeText = safeFallbackText,
                reason = "candidate tripped a non-negotiable safety rule (Phase 2 stub)",
            )
        } else {
            SafetyVerdict.Pass(candidate)
        }
}
