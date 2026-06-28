package app.aspen.domain.safety

import app.aspen.domain.safety.model.CrisisResourceSet
import app.aspen.domain.safety.model.LocaleKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SafetyEngineTest {

    private val emptyButPresentResolver = object : CrisisResolver {
        override fun resolve(locale: LocaleKey) = CrisisResourceSet(
            locale = LocaleKey.INTL,
            edSupport = emptyList(),
            acuteCrisis = emptyList(),
            treatmentFinder = emptyList(),
            isFallback = true,
        )
    }

    private val rules = SafetyRules(
        ForbiddenLexicon(
            byLanguage = mapOf(
                "en" to mapOf(TokenCategory.NUMBERS_FOOD_BODY to listOf("calories")),
            ),
            universal = emptyMap(),
        ),
    )

    private val engine = DefaultSafetyEngine(
        crisisResolver = emptyButPresentResolver,
        safetyRules = rules,
        safeFallbackText = "I can't speak to that, but you don't have to be alone with it.",
    )

    @Test
    fun guardOutputPassesCleanText() {
        val verdict = engine.guardOutput("You showed up today.")
        assertTrue(verdict is SafetyVerdict.Pass)
        assertEquals("You showed up today.", (verdict as SafetyVerdict.Pass).text)
    }

    @Test
    fun guardOutputRewritesViolatingTextWithoutEchoingIt() {
        val verdict = engine.guardOutput("you ate 500 calories")
        assertTrue(verdict is SafetyVerdict.Rewrite)
        val safe = (verdict as SafetyVerdict.Rewrite).safeText
        // The unsafe candidate is withheld, never echoed back.
        assertTrue(!safe.contains("calories"))
    }

    @Test
    fun crisisDelegatesToResolver() {
        val set = engine.crisis(LocaleKey.PK)
        assertEquals(LocaleKey.INTL, set.locale) // resolver fell back
        assertTrue(set.isFallback)
    }
}
