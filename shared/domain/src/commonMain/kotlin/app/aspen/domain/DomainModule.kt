package app.aspen.domain

/**
 * Pure-Kotlin domain layer (docs/04 §4). Knows nothing about Android, the network, or the UI —
 * dependencies point inward. This is where the safety subsystem (SafetyEngine, CrisisResolver,
 * SafetyRules) and the consent primitive (ConsentManager) land in Phase 2 (docs/09 §1), isolated
 * from feature churn.
 *
 * Phase 1 keeps this module a real, compiling, tested KMP target so the boundary and CI coverage
 * exist before any safety code is written.
 */
object DomainModule {
    /** Current build phase — bumped as the spine is filled in. */
    const val PHASE: Int = 4
}
