package app.aspen.domain.safety

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Table-driven tests for the non-negotiable predicates (docs/09 §2.3, §4). Covers en/de/ur samples.
 * The lexicon here is a small fixture; the real lexicon's parity with the canonical JSON is checked
 * by the JVM parity test (decision #3).
 */
class SafetyRulesTest {

    private val rules = SafetyRules(
        ForbiddenLexicon(
            byLanguage = mapOf(
                "en" to mapOf(
                    TokenCategory.NUMBERS_FOOD_BODY to listOf("calories", "bmi", "kg"),
                    TokenCategory.SHAME to listOf("failed", "you didn't"),
                    TokenCategory.APPEARANCE to listOf("you look", "skinny"),
                    TokenCategory.EATING_ADVICE to listOf("you should eat", "skip the meal"),
                ),
                "de" to mapOf(
                    TokenCategory.NUMBERS_FOOD_BODY to listOf("kalorien", "gewicht"),
                    TokenCategory.SHAME to listOf("versagt"),
                    TokenCategory.APPEARANCE to listOf("dünn"),
                    TokenCategory.EATING_ADVICE to emptyList(),
                ),
                "ur" to mapOf(
                    TokenCategory.NUMBERS_FOOD_BODY to listOf("وزن"),
                    TokenCategory.SHAME to emptyList(),
                    TokenCategory.APPEARANCE to emptyList(),
                    TokenCategory.EATING_ADVICE to emptyList(),
                ),
            ),
            universal = mapOf(
                TokenCategory.NUMBERS_FOOD_BODY to listOf("kcal"),
                TokenCategory.SHAME to emptyList(),
                TokenCategory.APPEARANCE to emptyList(),
                TokenCategory.EATING_ADVICE to emptyList(),
            ),
        ),
    )

    @Test
    fun detectsForbiddenNumbersAcrossLanguages() {
        listOf(
            "That was about 200 calories today",
            "your bmi looks",
            "ein paar Kalorien weniger",
            "آج آپ کا وزن",
            "roughly 3 kcal",
        ).forEach { assertTrue(rules.containsForbiddenNumbers(it), "should flag: $it") }
    }

    @Test
    fun allowsNumberlessSupportiveCopy() {
        listOf(
            "You showed up today, and that matters.",
            "Whatever today holds, you can be here.",
            "آپ یہاں محفوظ ہیں",
        ).forEach {
            assertFalse(rules.containsForbiddenNumbers(it), "should NOT flag: $it")
            assertFalse(rules.violatesAny(it), "should be clean: $it")
        }
    }

    @Test
    fun detectsShameAndAppearanceAndAdvice() {
        assertTrue(rules.containsShameLanguage("you failed again"))
        assertTrue(rules.containsShameLanguage("du hast versagt"))
        assertTrue(rules.containsAppearanceComment("you look tired"))
        assertTrue(rules.containsAppearanceComment("du bist so dünn"))
        assertTrue(rules.containsEatingAdvice("maybe you should eat less"))
    }

    @Test
    fun wholeWordMatchingAvoidsFalsePositives() {
        // Substrings inside larger words must not trip the whole-word matcher.
        assertFalse(rules.containsForbiddenNumbers("background"))
        assertFalse(rules.containsAppearanceComment("booklook")) // not the token "you look"
    }
}
