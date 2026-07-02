package app.aspen.data.ai.local

import app.aspen.domain.ai.CompanionLibrary
import app.aspen.domain.ai.CompanionLine
import app.aspen.domain.ai.CompanionMoment
import app.aspen.domain.ai.LineReviewStatus
import app.aspen.domain.onboarding.model.CompanionTone

/**
 * The curated companion-message library (docs/04 ADR-003) — runtime mirror of the canonical
 * `config/companion/library.json`, pinned by `CompanionLibraryParityTest` (JVM). Hand-edit BOTH
 * together; the parity test is the guard.
 *
 * ⚠ ALL lines are `PROVISIONAL` drafts (docs/07 Phase 4 [APPROVE]): clinically-informed advisor
 * review of the library is required before the companion is enabled outside debug builds — the same
 * "build the mechanism, mark it provisional" pattern as the crisis registry. Only `en` copy exists
 * so far; a language may not ship companion lines until reviewed (docs/12 §3 sensitive surface).
 *
 * The user-facing text for each key lives in the UI layer's localized string resources
 * (CLAUDE.md #11) and is covered by the per-language copy-lint. `rankingHint` is never shown.
 */
object DefaultCompanionLibrary {

    private val ALL_TONES = CompanionTone.entries.toSet()
    private val EN_PROVISIONAL = mapOf("en" to LineReviewStatus.PROVISIONAL)

    private fun line(key: String, moment: CompanionMoment, hint: String, tones: Set<CompanionTone> = ALL_TONES) =
        CompanionLine(key = key, moment = moment, tones = tones, rankingHint = hint, review = EN_PROVISIONAL)

    val library: CompanionLibrary = CompanionLibrary(
        listOf(
            // ---- GREETING ----
            line("companion_greeting_here", CompanionMoment.GREETING, "quiet hello, simply present, no demand"),
            line("companion_greeting_company", CompanionMoment.GREETING, "gentle offer of company if wanted"),
            line(
                "companion_greeting_soft", CompanionMoment.GREETING,
                "soft warm acknowledgement of showing up today",
                setOf(CompanionTone.SELF_COMPASSION, CompanionTone.POST_DISTRESS, CompanionTone.GENTLE_NEUTRAL),
            ),

            // ---- HARD_MOMENT_COMPANY ----
            line("companion_hard_sit_together", CompanionMoment.HARD_MOMENT_COMPANY, "offer to sit together through a hard moment"),
            line(
                "companion_hard_no_fixing", CompanionMoment.HARD_MOMENT_COMPANY,
                "quiet validating presence without fixing or questions",
                setOf(CompanionTone.GENTLE_NEUTRAL, CompanionTone.SELF_COMPASSION, CompanionTone.RESTRICTION_SENSITIVE),
            ),
            line("companion_hard_reach_someone", CompanionMoment.HARD_MOMENT_COMPANY, "offer the route to a real person right now"),
            line(
                "companion_hard_passing", CompanionMoment.HARD_MOMENT_COMPANY,
                "hard moments pass like weather, staying nearby meanwhile",
                setOf(CompanionTone.POST_DISTRESS, CompanionTone.SELF_COMPASSION),
            ),

            // ---- GROUNDING_INVITE ----
            line("companion_ground_breathe", CompanionMoment.GROUNDING_INVITE, "invite to one slow minute of breathing together"),
            line(
                "companion_ground_senses", CompanionMoment.GROUNDING_INVITE,
                "invite to gently notice the room and senses",
                setOf(CompanionTone.SENSORY_AWARE, CompanionTone.GENTLE_NEUTRAL),
            ),
            line(
                "companion_ground_write", CompanionMoment.GROUNDING_INVITE,
                "invite to put the feeling into words in the notebook",
                setOf(CompanionTone.GENTLE_NEUTRAL, CompanionTone.SELF_COMPASSION),
            ),

            // ---- RETURN_TO_AMBIENT ----
            line("companion_ambient_here_if_needed", CompanionMoment.RETURN_TO_AMBIENT, "returning to quiet, available anytime, no follow-up"),
            line("companion_ambient_no_followup", CompanionMoment.RETURN_TO_AMBIENT, "settling nearby without asking anything"),

            // ---- NOTIFICATION_PHRASING (wording only; scheduling deferred, docs/07) ----
            line("companion_notify_gentle_checkin", CompanionMoment.NOTIFICATION_PHRASING, "rare gentle optional check-in with zero pressure"),
            line("companion_notify_tools_nearby", CompanionMoment.NOTIFICATION_PHRASING, "calm reminder that the quiet tools exist, no urgency"),
        ),
    )
}
