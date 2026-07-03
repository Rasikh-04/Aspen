package app.aspen.ui.platform

import app.aspen.core.i18n.SupportedLanguage

/** Platform's current UI language tag (e.g. "ur-PK"), used only to resolve UI LANGUAGE (docs/12). */
expect fun systemLanguageTag(): String?

/**
 * Points the platform's string resolution at [language]; null restores the device language.
 * Compose resources resolve strings from the platform locale (their environment is not
 * app-overridable in CMP 1.11), so the Settings choice (docs/12 §4) is applied here.
 * [systemLanguageTag] must keep reporting the REAL device language regardless of this override.
 */
expect fun applyLanguageOverride(language: SupportedLanguage?)
