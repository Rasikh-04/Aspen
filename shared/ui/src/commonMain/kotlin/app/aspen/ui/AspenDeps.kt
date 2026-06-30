package app.aspen.ui

import app.aspen.domain.logging.LoggingService
import app.aspen.domain.onboarding.ProfileStore
import app.aspen.domain.safety.CrisisResolver

/**
 * The domain use-cases the shared UI needs, supplied by the platform entry (Android `MainActivity`,
 * iOS `MainViewController`) — the same explicit-injection pattern already used for [crisisResolver]
 * (Koin-start at platform entries is a tracked leftout, docs/STATUS.md). Every field is nullable so a
 * not-yet-wired platform (e.g. iOS) renders calm placeholders instead of crashing.
 *
 * Only domain types appear here, so `:shared:ui` stays dependent on `:shared:domain` alone — the
 * concrete encrypted implementations are constructed in the platform module that owns `:shared:data`.
 */
data class AspenDeps(
    val crisisResolver: CrisisResolver? = null,
    val profileStore: ProfileStore? = null,
    val loggingService: LoggingService? = null,
)
