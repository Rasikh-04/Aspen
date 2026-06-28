package app.aspen.data.di

import app.aspen.data.consent.ConsentBlobStore
import app.aspen.data.consent.InMemoryConsentBlobStore
import app.aspen.data.consent.PersistentConsentStore
import app.aspen.data.consent.platformConsentCipher
import app.aspen.data.crisis.CrisisRegistryRepo
import app.aspen.domain.consent.ConsentManager
import app.aspen.domain.consent.ConsentStore
import app.aspen.domain.consent.DefaultConsentManager
import app.aspen.domain.safety.CrisisResolver
import app.aspen.domain.safety.DefaultForbiddenLexicon
import app.aspen.domain.safety.DefaultSafetyEngine
import app.aspen.domain.safety.SafetyEngine
import app.aspen.domain.safety.SafetyRules
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The localized fallback line `SafetyEngine.guardOutput` substitutes when AI output trips a rule
 * (Phase-4). It is USER-FACING, so it must never be hardcoded in shared code (CLAUDE.md #11): the UI
 * layer, which owns localized resources, binds this. Defined here only as the injection type.
 */
data class SafetyFallbackCopy(val text: String)

/**
 * Safety subsystem wiring (docs/09 §2). Features depend only on [SafetyEngine]. The crisis resolver
 * is fully offline ([CrisisRegistryRepo]); the rules use the canonical lexicon mirror.
 *
 * The app MUST also provide a [SafetyFallbackCopy] binding (from a localized string) for
 * [SafetyEngine] to resolve — see the platform-init guide in this file's footer.
 */
val safetyModule: Module = module {
    single<CrisisResolver> { CrisisRegistryRepo() }
    single { SafetyRules(DefaultForbiddenLexicon.lexicon) }
    single<SafetyEngine> { DefaultSafetyEngine(get(), get(), get<SafetyFallbackCopy>().text) }
}

/**
 * Consent primitive wiring (docs/08 §3, docs/09 §3). [ConsentStore] is the encrypted, fail-safe
 * [PersistentConsentStore]; the cipher is the platform-keyed [platformConsentCipher].
 *
 * Phase-2 leftout: [ConsentBlobStore] is in-memory (not yet durable on-disk) — see docs/STATUS.md.
 */
@OptIn(ExperimentalUuidApi::class)
val consentModule: Module = module {
    single { platformConsentCipher() }
    single<ConsentBlobStore> { InMemoryConsentBlobStore() }
    single<ConsentStore> { PersistentConsentStore(get(), get()) }
    single<ConsentManager> {
        DefaultConsentManager(store = get(), clock = Clock.System, newId = { Uuid.random().toString() })
    }
}

/** Everything :shared:data contributes. Pass these to `startKoin { modules(aspenSharedModules) }`. */
val aspenSharedModules: List<Module> = listOf(safetyModule, consentModule)

/*
 * ───────────────────────── MANUAL PLATFORM-INIT GUIDE (Koin 4.1) ─────────────────────────
 *
 * Koin needs to be started ONCE per platform at app launch, and the app must supply the localized
 * SafetyFallbackCopy binding (CLAUDE.md #11 — the string lives in the UI string resources).
 *
 * 1) ANDROID — in your Application subclass (register it in AndroidManifest `android:name`):
 *
 *      class AspenApp : Application() {
 *          override fun onCreate() {
 *              super.onCreate()
 *              startKoin {
 *                  androidContext(this@AspenApp)
 *                  modules(
 *                      aspenSharedModules + module {
 *                          single { SafetyFallbackCopy(getString(R.string.safety_ai_fallback)) }
 *                      },
 *                  )
 *              }
 *          }
 *      }
 *   (androidContext requires koin-android; the core wiring above needs only koin-core.)
 *
 * 2) iOS — in MainViewController (or app entry) before the first Compose screen:
 *
 *      fun initKoin() = startKoin {
 *          modules(
 *              aspenSharedModules + module {
 *                  single { SafetyFallbackCopy(/* localized iOS string */) }
 *              },
 *          )
 *      }
 *   Call initKoin() from Swift (KoinKt.doInitKoin()) in the iOS app start.
 *
 * 3) VERIFY after wiring: `./gradlew :shared:data:jvmTest` (AspenModulesTest resolves the graph with a
 *    test SafetyFallbackCopy), then run the Android app and open Flow C (Settings → Safety).
 *
 * Leftouts tracked in docs/STATUS.md: durable on-disk ConsentBlobStore; device-verified Keystore/
 * Keychain ciphers; iOS Keychain cipher actual (currently a passthrough placeholder).
 */
