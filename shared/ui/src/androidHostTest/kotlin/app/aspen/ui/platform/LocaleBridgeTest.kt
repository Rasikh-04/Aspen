package app.aspen.ui.platform

import app.aspen.core.i18n.SupportedLanguage
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The override redirects string resolution (the process default locale) while
 * [systemLanguageTag] keeps reporting the REAL device language — that is what lets
 * "match my device" restore the truth (docs/12 §4).
 */
class LocaleBridgeTest {

    @AfterTest
    fun restoreDeviceLocale() {
        applyLanguageOverride(null)
    }

    @Test
    fun overrideChangesTheProcessDefaultLocale() {
        // Arrange
        val deviceTag = systemLanguageTag()

        // Act
        applyLanguageOverride(SupportedLanguage.UR)

        // Assert
        assertEquals("ur", Locale.getDefault().language)
        assertEquals(deviceTag, systemLanguageTag(), "the device tag must not follow the override")
    }

    @Test
    fun nullRestoresTheDeviceLocale() {
        // Arrange
        val deviceTag = systemLanguageTag()
        applyLanguageOverride(SupportedLanguage.UR)

        // Act
        applyLanguageOverride(null)

        // Assert
        assertEquals(deviceTag, Locale.getDefault().toLanguageTag())
    }

    @Test
    fun reapplyingTheSameOverrideIsIdempotent() {
        applyLanguageOverride(SupportedLanguage.UR)
        applyLanguageOverride(SupportedLanguage.UR)
        assertEquals("ur", Locale.getDefault().language)
    }
}
