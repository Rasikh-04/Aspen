package app.aspen.ui.platform

import java.util.Locale

actual fun systemLanguageTag(): String? = Locale.getDefault().toLanguageTag()
