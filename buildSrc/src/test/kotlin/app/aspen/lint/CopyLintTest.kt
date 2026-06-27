package app.aspen.lint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyLintTest {

    private val tokens = ForbiddenTokens.defaults()
    private val universal = ForbiddenTokens.universal()

    private fun scan(language: String, value: String, allow: Set<String> = emptySet()) =
        CopyLint.scanStrings(language, listOf("test" to value), tokens, universal, allow)

    @Test
    fun flagsFoodBodyNumbers() {
        assertTrue(scan("en", "This has 200 calories").any { it.token == "calories" })
        assertTrue(scan("en", "Track your kcal here").isNotEmpty())
        assertTrue(scan("en", "Your BMI is shown").isNotEmpty())
        assertTrue(scan("en", "Log your weight today").any { it.category == TokenCategory.NUMBERS_FOOD_BODY })
        assertTrue(scan("en", "Set a goal weight").any { it.token == "goal weight" })
    }

    @Test
    fun flagsShameLanguage() {
        assertTrue(scan("en", "You failed today").any { it.category == TokenCategory.SHAME })
        assertTrue(scan("en", "You missed your entry").any { it.token == "missed" })
        assertTrue(scan("en", "This day is incomplete").isNotEmpty())
    }

    @Test
    fun flagsAppearanceComments() {
        assertTrue(scan("en", "You look great").any { it.category == TokenCategory.APPEARANCE })
        assertTrue(scan("en", "You seem thin").any { it.token == "thin" })
    }

    @Test
    fun passesCleanCalmCopy() {
        assertTrue(scan("en", "A quiet place to be.").isEmpty())
        assertTrue(scan("en", "I am having a hard moment").isEmpty())
        // Regression: nav label "Calm" / words like calendar must NOT trip the "cal" family.
        assertTrue(scan("en", "Calm").isEmpty())
        assertTrue(scan("en", "Open your calendar").isEmpty())
        // Whole-word: "lightweight" must not match the token "weight".
        assertTrue(scan("en", "a lightweight design").isEmpty())
    }

    @Test
    fun allowListSuppressesAToken() {
        assertTrue(scan("en", "the portion of the screen").isNotEmpty())
        assertTrue(scan("en", "the portion of the screen", allow = setOf("portion")).isEmpty())
    }

    @Test
    fun perLanguageTokensApply() {
        // German calorie/weight words flagged only under the German list.
        assertTrue(scan("de", "Zähle deine Kalorien").isNotEmpty())
        assertTrue(scan("de", "Dein Gewicht heute").isNotEmpty())
        // A German-only token does not fire for an English string of the same letters absence.
        assertTrue(scan("en", "Zähle deine Kalorien").isEmpty())
        // Urdu weight token (starter list) flagged under ur.
        assertTrue(scan("ur", "آپ کا وزن").isNotEmpty())
    }

    @Test
    fun universalTokensApplyToEveryLanguage() {
        assertTrue(scan("es", "tu kcal diaria").isNotEmpty())
        assertTrue(scan("hi", "your bmi value").isNotEmpty())
    }

    @Test
    fun parserExtractsStringsAndLocale() {
        val xml = """
            <resources>
              <string name="a">hello</string>
              <string name="b">two words</string>
            </resources>
        """.trimIndent()
        val parsed = StringResourceParser.parse(xml)
        assertEquals(2, parsed.size)
        assertEquals("hello", parsed.first { it.first == "a" }.second)
    }

    @Test
    fun caseInsensitiveMatching() {
        assertFalse(scan("en", "CALORIE").isEmpty())
        assertFalse(scan("en", "Bmi").isEmpty())
    }
}
