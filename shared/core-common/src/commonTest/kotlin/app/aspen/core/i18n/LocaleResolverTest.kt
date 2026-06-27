package app.aspen.core.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocaleResolverTest {

    @Test
    fun everySupportedLanguageResolvesFromItsOwnTag() {
        for (lang in SupportedLanguage.entries) {
            assertEquals(lang, LocaleResolver.resolve("${lang.code}-XX"))
            assertEquals(lang, LocaleResolver.resolve(lang.code))
        }
    }

    @Test
    fun unsupportedLanguageFallsBackToEnglish() {
        assertEquals(SupportedLanguage.EN, LocaleResolver.resolve("fr-FR"))
        assertEquals(SupportedLanguage.EN, LocaleResolver.resolve("ja"))
        assertEquals(SupportedLanguage.EN, LocaleResolver.resolve(null))
        assertEquals(SupportedLanguage.EN, LocaleResolver.resolve(""))
    }

    @Test
    fun userOverrideAlwaysWins() {
        assertEquals(SupportedLanguage.UR, LocaleResolver.resolve("en-US", override = SupportedLanguage.UR))
        assertEquals(SupportedLanguage.DE, LocaleResolver.resolve(null, override = SupportedLanguage.DE))
    }

    @Test
    fun underscoreSeparatedTagsResolve() {
        assertEquals(SupportedLanguage.DE, LocaleResolver.resolve("de_DE"))
    }

    @Test
    fun rtlLanguagesMapToRtlDirection() {
        assertEquals(LayoutDir.RTL, LocaleResolver.layoutDirFor(SupportedLanguage.UR))
        assertEquals(LayoutDir.RTL, LocaleResolver.layoutDirFor(SupportedLanguage.AR))
        assertEquals(LayoutDir.LTR, LocaleResolver.layoutDirFor(SupportedLanguage.EN))
        assertEquals(LayoutDir.LTR, LocaleResolver.layoutDirFor(SupportedLanguage.ZH))
    }

    @Test
    fun languageNeverImpliesRegion_arabicTagDoesNotPickACountry() {
        // docs/12 §6: language and region are independent. The resolver returns ONLY a language
        // and exposes no country, so an Arabic UI can never be assumed to mean an Arab region.
        val resolved = LocaleResolver.resolve("ar-DE") // Arabic UI, German region
        assertEquals(SupportedLanguage.AR, resolved)
        // The resolved type has no region/country member to leak — enforced structurally.
        assertTrue(resolved is SupportedLanguage)
    }
}
