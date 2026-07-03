package app.aspen.ui.platform

import app.aspen.core.i18n.SupportedLanguage
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun systemLanguageTag(): String? =
    (NSLocale.preferredLanguages.firstOrNull() as? String)

actual fun applyLanguageOverride(language: SupportedLanguage?) {
    // No-op for now: iOS resolves compose resources from the locale captured at launch, and the
    // language store is not wired on the iOS entry yet (docs/STATUS.md). When it is, write
    // AppleLanguages to NSUserDefaults so the choice applies on the next launch. LocalAppLanguage
    // and RTL direction still follow the in-app choice on iOS.
}
