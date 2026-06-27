package app.aspen.ui.platform

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun systemLanguageTag(): String? =
    (NSLocale.preferredLanguages.firstOrNull() as? String)
