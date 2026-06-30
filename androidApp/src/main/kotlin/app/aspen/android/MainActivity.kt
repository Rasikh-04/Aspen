package app.aspen.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.aspen.data.crisis.CrisisRegistryRepo
import app.aspen.data.local.InMemoryEncryptedBlobStore
import app.aspen.data.local.platformLocalCipher
import app.aspen.data.logging.PersistentLoggingStore
import app.aspen.data.onboarding.PersistentProfileStore
import app.aspen.domain.logging.LoggingService
import app.aspen.domain.onboarding.AppConfigProvider
import app.aspen.ui.AspenApp
import app.aspen.ui.AspenDeps
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Thin Android entry (docs/04 §4): no logic here — it hosts the shared Compose app and supplies the
 * domain use-cases (crisis resolver, profile store, logging service) so Flows 0/A/B/C are live.
 *
 * This mirrors `localStoreModule` in AspenModules.kt by hand because Koin-start at the platform entry
 * is still a tracked leftout (docs/STATUS.md). The blob stores are in-memory for now (durable on-disk
 * persistence is the same tracked leftout), so the inferred profile resets on a cold start.
 */
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalUuidApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val cipher = platformLocalCipher()
        val profileStore = PersistentProfileStore(cipher, InMemoryEncryptedBlobStore())
        val appConfig = AppConfigProvider(profileStore)
        val loggingStore = PersistentLoggingStore(cipher, InMemoryEncryptedBlobStore())
        val loggingService = LoggingService(
            store = loggingStore,
            appConfig = appConfig,
            newId = { Uuid.random().toString() },
        )

        val deps = AspenDeps(
            crisisResolver = CrisisRegistryRepo(),
            profileStore = profileStore,
            loggingService = loggingService,
        )

        setContent { AspenApp(deps = deps) }
    }
}
