package app.aspen.ui

import app.aspen.domain.ai.CompanionVoice
import app.aspen.domain.ai.ReflectionCompanion
import app.aspen.domain.companion.CompanionPrefsStore
import app.aspen.domain.consent.ConsentManager
import app.aspen.domain.i18n.LanguagePrefStore
import app.aspen.domain.logging.LoggingService
import app.aspen.domain.onboarding.AppConfigProvider
import app.aspen.domain.onboarding.ProfileStore
import app.aspen.domain.safety.CrisisResolver
import app.aspen.domain.safety.CrisisSignals
import app.aspen.domain.safety.SafetyEngine

/**
 * The domain use-cases the shared UI needs, supplied by the platform entry (Android `MainActivity`,
 * iOS `MainViewController`) — the same explicit-injection pattern already used for [crisisResolver]
 * (Koin-start at platform entries is a tracked leftout, docs/STATUS.md). Every field is nullable so a
 * not-yet-wired platform (e.g. iOS) renders calm placeholders instead of crashing.
 *
 * Only domain types appear here, so `:shared:ui` stays dependent on `:shared:domain` alone — the
 * concrete encrypted implementations are constructed in the platform module that owns `:shared:data`.
 *
 * Phase 4 additions: [consentManager] + [reflectionCompanion] (Tier-2 cloud reflection — off until
 * an explicit grant), [companionVoice] + [appConfigProvider] (Tier-1 curated voice), and
 * [safetyEngine] + [crisisSignals] for the debug-only guard playground. [isDebugBuild] gates the
 * debug companion-preview surface — it must never be true in a release build.
 */
data class AspenDeps(
    val crisisResolver: CrisisResolver? = null,
    val profileStore: ProfileStore? = null,
    val loggingService: LoggingService? = null,
    val consentManager: ConsentManager? = null,
    val reflectionCompanion: ReflectionCompanion? = null,
    val companionVoice: CompanionVoice? = null,
    val appConfigProvider: AppConfigProvider? = null,
    val safetyEngine: SafetyEngine? = null,
    val crisisSignals: CrisisSignals? = null,
    /** Phase 5: companion presence prefs (docs/05). Null → companion features absent, all off. */
    val companionPrefsStore: CompanionPrefsStore? = null,
    /** Phase 5: Android overlay hook (docs/05 §6); null on platforms without system overlays. */
    val overlayControl: app.aspen.ui.companion.CompanionOverlayControl? = null,
    /** Phase 5: check-in scheduling hook (FR-8, off by default); null → row absent. */
    val notificationsControl: app.aspen.ui.companion.CompanionNotificationsControl? = null,
    val isDebugBuild: Boolean = false,
    /** UI-language override store (docs/12 §4). Null → the Settings language row is absent. */
    val languagePrefStore: LanguagePrefStore? = null,
    /** Phase 6: the optional Aspen-native account (docs/08 §1). Null → the Settings row is absent. */
    val accountManager: app.aspen.domain.account.AccountManager? = null,
    /** Phase 6: E2E backup (docs/08 §2). Null → backup UI absent (e.g. iOS, or signed out). */
    val backupManager: app.aspen.domain.sync.BackupManager? = null,
)
