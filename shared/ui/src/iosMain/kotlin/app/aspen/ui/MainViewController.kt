package app.aspen.ui

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** iOS entry point: hosts the shared Compose UI in a UIViewController for the SwiftUI app. */
fun MainViewController(): UIViewController = ComposeUIViewController { AspenApp() }
