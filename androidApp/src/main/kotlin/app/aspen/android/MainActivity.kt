package app.aspen.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.aspen.ui.AspenApp

/** Thin Android entry (docs/04 §4): no logic here — it just hosts the shared Compose app. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AspenApp()
        }
    }
}
