package app.aspen.domain.ai

import app.aspen.domain.onboarding.model.CompanionTone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompanionLibraryTest {

    private fun line(key: String, moment: CompanionMoment, vararg tones: CompanionTone) = CompanionLine(
        key = key,
        moment = moment,
        tones = tones.toSet(),
        rankingHint = "hint for $key",
        review = mapOf("en" to LineReviewStatus.PROVISIONAL),
    )

    @Test
    fun candidatesPreferToneSuitedLines() {
        val library = CompanionLibrary(
            listOf(
                line("a", CompanionMoment.GREETING, CompanionTone.GENTLE_NEUTRAL),
                line("b", CompanionMoment.GREETING, CompanionTone.RESTRICTION_SENSITIVE),
            ),
        )

        assertEquals(
            listOf("b"),
            library.candidates(CompanionMoment.GREETING, CompanionTone.RESTRICTION_SENSITIVE).map { it.key },
        )
    }

    @Test
    fun candidatesFallBackToMomentWhenNoToneMatch() {
        // A narrow tone must never leave the companion speechless.
        val library = CompanionLibrary(
            listOf(line("a", CompanionMoment.GREETING, CompanionTone.GENTLE_NEUTRAL)),
        )

        val candidates = library.candidates(CompanionMoment.GREETING, CompanionTone.SENSORY_AWARE)

        assertEquals(listOf("a"), candidates.map { it.key })
    }

    @Test
    fun emptyLibraryIsRejectedAtConstruction() {
        assertFailsWith<IllegalArgumentException> { CompanionLibrary(emptyList()) }
    }

    @Test
    fun duplicateKeysAreRejectedAtConstruction() {
        assertFailsWith<IllegalArgumentException> {
            CompanionLibrary(
                listOf(
                    line("same", CompanionMoment.GREETING, CompanionTone.GENTLE_NEUTRAL),
                    line("same", CompanionMoment.GREETING, CompanionTone.SELF_COMPASSION),
                ),
            )
        }
    }

    @Test
    fun candidatesForUncoveredMomentAreEmptyNotThrowing() {
        // The DEFAULT library's invariant test asserts full coverage; the type itself stays total.
        val library = CompanionLibrary(
            listOf(line("a", CompanionMoment.GREETING, CompanionTone.GENTLE_NEUTRAL)),
        )
        assertTrue(library.candidates(CompanionMoment.GROUNDING_INVITE, CompanionTone.GENTLE_NEUTRAL).isEmpty())
    }
}
