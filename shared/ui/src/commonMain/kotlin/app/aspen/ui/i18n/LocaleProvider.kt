package app.aspen.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.aspen.core.i18n.LocaleResolver
import app.aspen.core.i18n.SupportedLanguage
import app.aspen.ui.platform.systemLanguageTag

/** The active UI language for the composition. */
val LocalAppLanguage = staticCompositionLocalOf { SupportedLanguage.DEFAULT }

/**
 * Resolves the UI language (system default -> supported? -> English) and drives [LocalLayoutDirection]
 * from it, so RTL (Urdu, Arabic) is correct from day one (docs/12 §2, §4). [override] is the user's
 * Settings choice (Phase 3); null follows the system.
 */
@Composable
fun LocaleProvider(
    override: SupportedLanguage? = null,
    content: @Composable () -> Unit,
) {
    val language = remember(override) { LocaleResolver.resolve(systemLanguageTag(), override) }
    val direction = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalLayoutDirection provides direction,
        content = content,
    )
}
