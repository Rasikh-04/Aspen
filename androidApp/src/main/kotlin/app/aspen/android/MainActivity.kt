package app.aspen.android

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import app.aspen.data.ai.PersistentAiMessageStore
import app.aspen.data.ai.cloud.DisabledAiClient
import app.aspen.data.ai.local.LibraryCompanionVoice
import app.aspen.data.consent.DurableConsentBlobStore
import app.aspen.data.consent.PersistentConsentStore
import app.aspen.data.consent.platformConsentCipher
import app.aspen.data.crisis.CrisisRegistryRepo
import app.aspen.data.local.AspenLocalStorage
import app.aspen.data.local.FileEncryptedBlobStore
import app.aspen.data.local.platformLocalCipher
import app.aspen.data.logging.PersistentLoggingStore
import app.aspen.data.companion.PersistentCompanionPrefsStore
import app.aspen.data.onboarding.PersistentProfileStore
import app.aspen.domain.ai.ReflectionCompanion
import app.aspen.domain.consent.DefaultConsentManager
import app.aspen.domain.logging.LoggingService
import app.aspen.domain.onboarding.AppConfigProvider
import app.aspen.domain.safety.CrisisSignals
import app.aspen.domain.safety.DefaultCrisisSignalLexicon
import app.aspen.domain.safety.DefaultForbiddenLexicon
import app.aspen.domain.safety.DefaultSafetyEngine
import app.aspen.domain.safety.SafetyRules
import app.aspen.ui.AspenApp
import app.aspen.ui.AspenDeps
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.safety_ai_fallback
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.compose.resources.stringResource

/**
 * Thin Android entry (docs/04 §4): no logic here — it hosts the shared Compose app and supplies the
 * domain use-cases so Flows 0/A/B/C and the Phase-4 AI tiers are live.
 *
 * This mirrors the modules in AspenModules.kt by hand because Koin-start at the platform entry is
 * still a tracked leftout (docs/STATUS.md). Since Phase 4 the blob stores are durable on-disk
 * ([FileEncryptedBlobStore] under filesDir, Keystore-encrypted), so everything survives a cold start.
 * Deps are constructed inside composition because the safety guard's fallback line is LOCALIZED
 * user-facing copy (CLAUDE.md #11) — the UI layer owns it, so it is read via stringResource.
 */
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalUuidApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AspenLocalStorage.init(applicationContext)
        val isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

        setContent {
            val safeFallback = stringResource(Res.string.safety_ai_fallback)
            val deps = remember(safeFallback) { buildDeps(safeFallback, isDebug) }
            AspenApp(deps = deps)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun buildDeps(safeFallbackText: String, isDebug: Boolean): AspenDeps {
        val newId = { Uuid.random().toString() }
        val cipher = platformLocalCipher()

        val profileStore = PersistentProfileStore(cipher, FileEncryptedBlobStore("profile"))
        val appConfig = AppConfigProvider(profileStore)
        val loggingService = LoggingService(
            store = PersistentLoggingStore(cipher, FileEncryptedBlobStore("logs")),
            appConfig = appConfig,
            newId = newId,
        )

        val consentManager = DefaultConsentManager(
            store = PersistentConsentStore(platformConsentCipher(), DurableConsentBlobStore()),
            clock = Clock.System,
            newId = newId,
        )

        val crisisResolver = CrisisRegistryRepo()
        val safetyEngine = DefaultSafetyEngine(
            crisisResolver = crisisResolver,
            safetyRules = SafetyRules(DefaultForbiddenLexicon.lexicon),
            safeFallbackText = safeFallbackText,
        )
        val crisisSignals = CrisisSignals(DefaultCrisisSignalLexicon.lexicon)

        val reflectionCompanion = ReflectionCompanion(
            consent = consentManager,
            client = DisabledAiClient, // Cloud stays not-live-wired (docs/PRE_SHIP_VERIFICATION.md).
            safetyEngine = safetyEngine,
            crisisSignals = crisisSignals,
            store = PersistentAiMessageStore(cipher, FileEncryptedBlobStore("ai_messages")),
            newId = newId,
        )

        return AspenDeps(
            crisisResolver = crisisResolver,
            profileStore = profileStore,
            loggingService = loggingService,
            consentManager = consentManager,
            reflectionCompanion = reflectionCompanion,
            companionVoice = LibraryCompanionVoice(),
            appConfigProvider = appConfig,
            safetyEngine = safetyEngine,
            crisisSignals = crisisSignals,
            companionPrefsStore = PersistentCompanionPrefsStore(cipher, FileEncryptedBlobStore("companion_prefs")),
            isDebugBuild = isDebug,
        )
    }
}
