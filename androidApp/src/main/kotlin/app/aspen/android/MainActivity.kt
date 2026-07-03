package app.aspen.android

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import app.aspen.data.account.PersistentSessionStore
import app.aspen.data.account.ServerAccountManager
import app.aspen.data.ai.PersistentAiMessageStore
import app.aspen.data.ai.cloud.AspenServerAiClient
import app.aspen.data.ai.cloud.DisabledAiClient
import app.aspen.data.ai.local.LibraryCompanionVoice
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import app.aspen.data.consent.DurableConsentBlobStore
import app.aspen.data.consent.PersistentConsentStore
import app.aspen.data.consent.platformConsentCipher
import app.aspen.data.crisis.CrisisRegistryRepo
import app.aspen.data.local.AspenLocalStorage
import app.aspen.data.local.FileEncryptedBlobStore
import app.aspen.data.local.platformLocalCipher
import app.aspen.data.i18n.PersistentLanguagePrefStore
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
import app.aspen.companion.overlay.CompanionOverlayService
import app.aspen.ui.AspenApp
import app.aspen.ui.AspenDeps
import app.aspen.ui.companion.CompanionNotificationsControl
import app.aspen.ui.companion.CompanionOverlayControl
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

    // One shared instance so Settings, the in-app layer, the overlay service and onResume all read
    // the same prefs. Constructed lazily — AspenLocalStorage.init runs first in onCreate.
    private val companionPrefs by lazy {
        PersistentCompanionPrefsStore(platformLocalCipher(), FileEncryptedBlobStore("companion_prefs"))
    }

    private val overlayControl = object : CompanionOverlayControl {
        override fun isPermissionGranted() = CompanionOverlayService.isPermissionGranted(this@MainActivity)

        override fun requestPermission() {
            // The OS-owned grant screen (docs/05 §6): we can only take the user there, never
            // show our own dialog — the shared UI has already explained it in plain language.
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
            )
        }

        override fun setOverlayActive(active: Boolean) {
            if (active) CompanionOverlayService.start(this@MainActivity) else CompanionOverlayService.stop(this@MainActivity)
        }
    }

    // Result ignored on purpose: if the user declines, the worker stays scheduled but the policy
    // path goes quiet (worker checks the permission) — we never re-ask outside the opt-in act.
    private val notificationsPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private val notificationsControl = object : CompanionNotificationsControl {
        override fun setScheduled(active: Boolean) {
            if (active) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationsPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                CompanionCheckinScheduler.schedule(this@MainActivity)
            } else {
                CompanionCheckinScheduler.cancel(this@MainActivity)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-sync after the user returns from the OS permission screen. Start only when every
        // opt-in is present (docs/05 §3.1); if the permission was revoked the service refuses
        // itself, so no stop-side handling is needed here.
        val prefs = companionPrefs.current()
        if (prefs != null && prefs.enabled && prefs.overlayEnabled && overlayControl.isPermissionGranted()) {
            CompanionOverlayService.start(this)
        }
    }

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

        // Phase 6: the OPTIONAL account + the server-routed AI tier (docs/00 decisions #10/#11).
        // The server URL is dev-only loopback in debug builds (10.0.2.2 = emulator host) and ABSENT
        // in release — no production endpoint exists yet (deployment = Phase 6.9). Absent URL ⇒
        // account row hidden and AI client Disabled: release behaviour is exactly Phase 4's.
        val serverBaseUrl = if (isDebug) "http://10.0.2.2:8080" else null
        val sessionStore = PersistentSessionStore(cipher, FileEncryptedBlobStore("account_session"))
        val http = serverBaseUrl?.let { HttpClient(CIO) }
        val accountManager = serverBaseUrl?.let { ServerAccountManager(it, sessionStore, http!!) }
        val aiClient = if (accountManager != null) {
            AspenServerAiClient(serverBaseUrl, { accountManager.sessionToken() }, http!!)
        } else {
            DisabledAiClient // No server configured: cloud provably never touches the network.
        }

        val reflectionCompanion = ReflectionCompanion(
            consent = consentManager,
            client = aiClient,
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
            companionPrefsStore = companionPrefs,
            overlayControl = overlayControl,
            notificationsControl = notificationsControl,
            isDebugBuild = isDebug,
            languagePrefStore = PersistentLanguagePrefStore(cipher, FileEncryptedBlobStore("language_pref")),
            accountManager = accountManager,
        )
    }
}
