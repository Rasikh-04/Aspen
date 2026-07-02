package app.aspen.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.aspen.design.AspenTheme
import app.aspen.ui.i18n.LocalAppLanguage
import app.aspen.ui.i18n.LocaleProvider
import app.aspen.ui.nav.AppScaffold
import app.aspen.ui.onboarding.OnboardingFlow

/**
 * Cross-platform application root (shared on Android + iOS, docs/04 §4). Resolves locale, applies the
 * Aspen theme, then either runs Flow 0 onboarding (first run) or the navigation shell.
 *
 * [deps] are supplied by the platform entry from the data layer. When a field is null (e.g. iOS before
 * its DI lands) the corresponding surface degrades to a calm placeholder rather than breaking
 * (tracked in docs/STATUS.md).
 */
@Composable
fun AspenApp(deps: AspenDeps = AspenDeps()) {
    LocaleProvider {
        val language = LocalAppLanguage.current
        AspenTheme(language = language) {
            AppRoot(deps)
        }
    }
}

@Composable
private fun AppRoot(deps: AspenDeps) {
    val profileStore = deps.profileStore

    // First run = a profile store exists but nothing has been saved yet. With no store wired
    // (unconfigured platform), skip straight to the app so nothing breaks.
    var showOnboarding by remember { mutableStateOf(profileStore != null && profileStore.current() == null) }
    var startAtSafety by remember { mutableStateOf(false) }

    if (showOnboarding && profileStore != null) {
        OnboardingFlow(onFinish = { result, goToSafety ->
            profileStore.save(result)
            startAtSafety = goToSafety
            showOnboarding = false
        })
    } else {
        AppScaffold(
            deps = deps,
            startAtSafety = startAtSafety,
            onConsumedStart = { startAtSafety = false },
            onRevisitQuestions = { if (profileStore != null) showOnboarding = true },
        )
    }
}
