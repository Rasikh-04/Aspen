package app.aspen.ui

import androidx.compose.runtime.Composable
import app.aspen.design.AspenTheme
import app.aspen.ui.i18n.LocalAppLanguage
import app.aspen.ui.i18n.LocaleProvider
import app.aspen.ui.nav.AppScaffold

/**
 * Cross-platform application root (shared on Android + iOS, docs/04 §4). Resolves locale, applies
 * the Aspen theme for that language, then shows the navigation shell with the empty Calm Home.
 */
@Composable
fun AspenApp() {
    LocaleProvider {
        val language = LocalAppLanguage.current
        AspenTheme(language = language) {
            AppScaffold()
        }
    }
}
