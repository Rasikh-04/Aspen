package app.aspen.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.aspen.data.crisis.CrisisRegistryRepo
import app.aspen.ui.AspenApp

/**
 * Thin Android entry (docs/04 §4): no logic here — it hosts the shared Compose app and supplies the
 * offline crisis resolver so Flow C is live. (A later pass can source this from Koin instead; see the
 * platform-init guide in AspenModules.kt.)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AspenApp(crisisResolver = CrisisRegistryRepo())
        }
    }
}
