package app.aspen.data.ai.local

import app.aspen.domain.ai.CompanionLine
import app.aspen.domain.ai.CompanionLibrary
import app.aspen.domain.ai.CompanionMoment
import app.aspen.domain.ai.LineRanker
import app.aspen.domain.ai.LineReviewStatus
import app.aspen.domain.onboarding.model.AppConfig
import app.aspen.domain.onboarding.model.CompanionTone
import app.aspen.domain.onboarding.model.FoodLoggingMode
import app.aspen.domain.onboarding.model.ProfileMappingProvenance
import app.aspen.domain.onboarding.model.SupportRoutingStrength
import app.aspen.domain.onboarding.model.ToolEmphasis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryCompanionVoiceTest {

    private fun config(tone: CompanionTone = CompanionTone.GENTLE_NEUTRAL) = AppConfig(
        foodLoggingMode = FoodLoggingMode.OFF,
        companionTone = tone,
        toolEmphasis = ToolEmphasis.BALANCED,
        supportRoutingStrength = SupportRoutingStrength.STANDARD,
        bodyImageFramingAllowed = false,
        provenance = ProfileMappingProvenance.PROVISIONAL,
    )

    private fun line(key: String, moment: CompanionMoment, vararg tones: CompanionTone) = CompanionLine(
        key = key,
        moment = moment,
        tones = tones.toSet(),
        rankingHint = "hint $key",
        review = mapOf("en" to LineReviewStatus.PROVISIONAL),
    )

    private val library = CompanionLibrary(
        listOf(
            line("g1", CompanionMoment.GREETING, CompanionTone.GENTLE_NEUTRAL),
            line("g2", CompanionMoment.GREETING, CompanionTone.GENTLE_NEUTRAL),
            line("h1", CompanionMoment.HARD_MOMENT_COMPANY, CompanionTone.GENTLE_NEUTRAL),
        ),
    )

    // ---- totality over the REAL library: every moment × tone yields a line, never a throw ----

    @Test
    fun defaultLibraryIsTotalOverEveryMomentAndTone() {
        val voice = LibraryCompanionVoice(ranker = null)
        CompanionMoment.entries.forEach { moment ->
            CompanionTone.entries.forEach { tone ->
                val chosen = voice.line(moment, config(tone))
                assertEquals(moment, chosen.moment, "line for $moment/$tone must belong to the moment")
            }
        }
    }

    @Test
    fun defaultLibraryLinesAreAllProvisionalUntilAdvisorReview() {
        // docs/07 Phase 4 [APPROVE]: nothing in the shipped library may claim review it hasn't had.
        DefaultCompanionLibrary.library.lines.forEach { line ->
            assertTrue(
                line.review.values.all { it == LineReviewStatus.PROVISIONAL },
                "${line.key} must stay PROVISIONAL until advisors sign off",
            )
        }
    }

    @Test
    fun defaultLibraryHintsAreNonEmptyAndKeysWellFormed() {
        DefaultCompanionLibrary.library.lines.forEach { line ->
            assertTrue(line.rankingHint.isNotBlank(), "${line.key}: blank ranking hint")
            assertTrue(line.key.matches(Regex("[a-z0-9_]+")), "${line.key}: keys are string-resource ids")
        }
    }

    // ---- ranker containment: it may reorder, never invent ----

    @Test
    fun rankerChoiceOutsideCandidatesIsDiscarded() {
        val foreign = line("smuggled", CompanionMoment.GREETING, CompanionTone.GENTLE_NEUTRAL)
        val voice = LibraryCompanionVoice(
            library = library,
            ranker = object : LineRanker {
                override fun pickBest(context: String, candidates: List<CompanionLine>) = foreign
            },
        )

        val chosen = voice.line(CompanionMoment.GREETING, config(), variant = 0)

        assertEquals("g1", chosen.key, "out-of-candidates ranker result must be ignored")
    }

    @Test
    fun rankerPickWithinCandidatesIsUsed() {
        val voice = LibraryCompanionVoice(
            library = library,
            ranker = object : LineRanker {
                override fun pickBest(context: String, candidates: List<CompanionLine>) =
                    candidates.last()
            },
        )

        assertEquals("g2", voice.line(CompanionMoment.GREETING, config()).key)
    }

    @Test
    fun throwingRankerFallsBackToDeterministicPick() {
        val voice = LibraryCompanionVoice(
            library = library,
            ranker = object : LineRanker {
                override fun pickBest(context: String, candidates: List<CompanionLine>): CompanionLine =
                    error("model blew up")
            },
        )

        // Never errors at a hard moment: the deterministic pick answers.
        assertEquals("g1", voice.line(CompanionMoment.GREETING, config(), variant = 0).key)
    }

    @Test
    fun nullRankerRotatesDeterministicallyByVariant() {
        val voice = LibraryCompanionVoice(library = library, ranker = null)

        assertEquals("g1", voice.line(CompanionMoment.GREETING, config(), variant = 0).key)
        assertEquals("g2", voice.line(CompanionMoment.GREETING, config(), variant = 1).key)
        assertEquals("g1", voice.line(CompanionMoment.GREETING, config(), variant = 2).key)
        // Negative variants must not crash (Int.mod keeps the index in range).
        assertEquals("g2", voice.line(CompanionMoment.GREETING, config(), variant = -1).key)
    }
}
