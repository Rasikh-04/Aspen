package app.aspen.ui.platform

import app.aspen.core.i18n.SupportedLanguage
import java.util.Locale

/**
 * The device's own locale, captured before any override can run ([systemLanguageTag] is always
 * called first, from LocaleProvider's resolve step). Keeping it lets "match my device" restore
 * the truth after an override has replaced the process default.
 */
private val deviceDefaultLocale: Locale = Locale.getDefault()

actual fun systemLanguageTag(): String? = deviceDefaultLocale.toLanguageTag()

actual fun applyLanguageOverride(language: SupportedLanguage?) {
    val target = language?.let { Locale.forLanguageTag(it.code) } ?: deviceDefaultLocale
    if (Locale.getDefault() != target) Locale.setDefault(target)
}
