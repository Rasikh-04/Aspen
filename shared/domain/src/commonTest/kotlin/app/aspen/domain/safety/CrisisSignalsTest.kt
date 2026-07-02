package app.aspen.domain.safety

/**
 * The crisis-sign input check (docs/06 §5, CLAUDE.md #8): CONSERVATIVE and HEURISTIC, never
 * diagnostic. It exists to trigger a warm hand-off BEFORE any AI call — and, just as deliberately,
 * to NOT treat ordinary distress as an emergency (docs/06 §6.3: most hard moments are distress, not
 * a 911 event; the human exit is always visible anyway, so under-triggering is the accepted trade).
 */
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrisisSignalsTest {

    private val signals = CrisisSignals(DefaultCrisisSignalLexicon.lexicon)

    // ---- explicit crisis phrases MUST trip (per starter language) ----

    @Test
    fun explicitEnglishCrisisPhrasesTrip() {
        listOf(
            "I want to kill myself",
            "i've been thinking about suicide",
            "I just want to end my life",
            "sometimes I want to hurt myself",
            "I don't want to be alive anymore",
        ).forEach { text ->
            assertTrue(signals.suggestsCrisis(text), "should hand off: $text")
        }
    }

    @Test
    fun explicitGermanCrisisPhrasesTrip() {
        listOf(
            "ich will mich umbringen",
            "ich denke an selbstmord",
            "ich will nicht mehr leben",
        ).forEach { text ->
            assertTrue(signals.suggestsCrisis(text), "should hand off: $text")
        }
    }

    @Test
    fun explicitUrduCrisisPhrasesTrip() {
        listOf(
            "میں خودکشی کرنا چاہتی ہوں",
            "میں مرنا چاہتا ہوں",
        ).forEach { text ->
            assertTrue(signals.suggestsCrisis(text), "should hand off: $text")
        }
    }

    @Test
    fun matchingIsCaseInsensitive() {
        assertTrue(signals.suggestsCrisis("I WANT TO KILL MYSELF"))
    }

    // ---- ordinary distress MUST NOT trip (docs/06 §6.3 — don't over-trigger) ----

    @Test
    fun ordinaryDistressDoesNotTrip() {
        listOf(
            "today was really hard",
            "I feel awful about dinner",
            "I'm exhausted and sad and I don't know why",
            "I hate this feeling",
            "everything feels too much right now",
            "ich hatte einen schweren tag",
        ).forEach { text ->
            assertFalse(signals.suggestsCrisis(text), "must not over-trigger on: $text")
        }
    }

    @Test
    fun embeddedWordsDoNotFalsePositive() {
        // Word-boundary matching: tokens inside longer words are not signals.
        assertFalse(signals.suggestsCrisis("the suicidepreventionresource page"), "no whole-word match")
    }

    @Test
    fun emptyAndBlankTextDoesNotTrip() {
        assertFalse(signals.suggestsCrisis(""))
        assertFalse(signals.suggestsCrisis("   "))
    }

    // ---- the check is a signal, never a label (CLAUDE.md #9) ----

    @Test
    fun apiExposesOnlyABooleanHandOffSignal() {
        // Compile-time by shape: suggestsCrisis returns Boolean. This test documents the intent —
        // no severity score, no category, no clinical label ever leaves this class.
        val result: Boolean = signals.suggestsCrisis("I want to end my life")
        assertTrue(result)
    }
}
