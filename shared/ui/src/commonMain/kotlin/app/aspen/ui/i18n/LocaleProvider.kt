package app.aspen.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.aspen.core.i18n.LocaleResolver
import app.aspen.core.i18n.SupportedLanguage
import app.aspen.ui.platform.applyLanguageOverride
import app.aspen.ui.platform.systemLanguageTag

/** The active UI language for the composition. */
val LocalAppLanguage = staticCompositionLocalOf { SupportedLanguage.DEFAULT }

/**
 * Resolves the UI language (system default -> supported? -> English) and drives [LocalLayoutDirection]
 * from it, so RTL (Urdu, Arabic) is correct from day one (docs/12 §2, §4). [override] is the user's
 * Settings choice; null follows the system.
 *
 * The override is also applied to the platform's string resolution ([applyLanguageOverride]) INSIDE
 * the same remember step, so it takes effect before any `stringResource` in [content] resolves —
 * without it the Settings choice would change layout direction but not the words. Resolve reads the
 * device tag first, so "follow the system" always restores the real device language.
 */
@Composable
fun LocaleProvider(
    override: SupportedLanguage? = null,
    content: @Composable () -> Unit,
) {
    val language = remember(override) {
        LocaleResolver.resolve(systemLanguageTag(), override).also { applyLanguageOverride(override) }
    }
    val direction = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalLayoutDirection provides direction,
        content = content,
    )
}
