package app.aspen.ui

import androidx.compose.runtime.Composable
import app.aspen.design.AspenTheme
import app.aspen.domain.safety.CrisisResolver
import app.aspen.ui.i18n.LocalAppLanguage
import app.aspen.ui.i18n.LocaleProvider
import app.aspen.ui.nav.AppScaffold

/**
 * Cross-platform application root (shared on Android + iOS, docs/04 §4). Resolves locale, applies
 * the Aspen theme for that language, then shows the navigation shell with the empty Calm Home.
 *
 * [crisisResolver] is supplied by the platform entry (from DI). When null, Flow C shows the Phase-1
 * placeholder — so iOS, whose entry can't yet reach :shared:data, stays non-broken until wired
 * (tracked in docs/STATUS.md). Android passes the real offline resolver, making Flow C live.
 */
@Composable
fun AspenApp(crisisResolver: CrisisResolver? = null) {
    LocaleProvider {
        val language = LocalAppLanguage.current
        AspenTheme(language = language) {
            AppScaffold(crisisResolver = crisisResolver)
        }
    }
}
