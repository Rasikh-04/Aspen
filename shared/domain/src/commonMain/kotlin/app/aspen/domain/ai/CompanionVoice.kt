package app.aspen.domain.ai

import app.aspen.domain.onboarding.model.AppConfig
import app.aspen.domain.onboarding.model.CompanionTone

/**
 * A moment the companion may speak into (docs/05 §4, docs/04 ADR-003). Moments are the ONLY
 * occasions for companion words — there is deliberately no moment for absence, streaks, progress,
 * food, or the body (SR-4; CLAUDE.md #1–#5). [NOTIFICATION_PHRASING] covers the *wording* of a
 * (rare, opt-in) check-in only; notification scheduling itself is deferred (docs/07 Phase 4 note).
 */
enum class CompanionMoment {
    GREETING,
    HARD_MOMENT_COMPANY,
    GROUNDING_INVITE,
    RETURN_TO_AMBIENT,
    NOTIFICATION_PHRASING,
}

/**
 * Per-language review state of a companion line (docs/12 §3): companion lines are a SENSITIVE
 * SURFACE — a language may not ship until its copy is native-speaker + ED-informed reviewed.
 */
enum class LineReviewStatus { PROVISIONAL, NATIVE_REVIEWED }

/**
 * One approved companion line. [key] is a string-resource key — the user-facing copy lives in the
 * UI layer's localized resources (CLAUDE.md #11), never here. [rankingHint] is a NON-user-facing
 * English descriptor used only as ranking features by the on-device selector; it is never shown.
 * [review] tracks per-language sign-off (docs/12 §3 "track per-key review status").
 */
data class CompanionLine(
    val key: String,
    val moment: CompanionMoment,
    val tones: Set<CompanionTone>,
    val rankingHint: String,
    val review: Map<String, LineReviewStatus>,
)

/**
 * The curated, clinically-reviewable companion-message library (docs/04 ADR-003 safety refinement):
 * the SOURCE OF TRUTH for everything the companion can ever say. Selection may reorder; nothing may
 * add. Content is pre-approved; the model personalises *delivery* only.
 */
data class CompanionLibrary(val lines: List<CompanionLine>) {

    init {
        require(lines.isNotEmpty()) { "companion library must never be empty" }
        require(lines.map { it.key }.toSet().size == lines.size) { "companion line keys must be unique" }
    }

    /**
     * Tone-suited lines for [moment]; falls back to every line of the moment so a narrow tone can
     * never leave the companion speechless. Library invariant tests assert this is non-empty for
     * every moment × tone.
     */
    fun candidates(moment: CompanionMoment, tone: CompanionTone): List<CompanionLine> {
        val forMoment = lines.filter { it.moment == moment }
        val forTone = forMoment.filter { tone in it.tones }
        return forTone.ifEmpty { forMoment }
    }
}

/**
 * On-device ranker seam (docs/04 ADR-003 Tier 1). May only choose among the [candidates] it is
 * given — by shape it cannot invent, merge, or edit a line. Implementations must be fast, offline
 * and failure-tolerant; callers treat null (and any thrown error) as "use the deterministic pick".
 */
interface LineRanker {
    fun pickBest(context: String, candidates: List<CompanionLine>): CompanionLine?
}

/**
 * The Tier-1 companion voice port (docs/04 ADR-003): given a moment and the person's [AppConfig]
 * (tone/emphasis from onboarding — the personalisation input), returns one approved line.
 * Total: implementations never return nothing and never throw — a companion that errors at a hard
 * moment is worse than one that says something simple. [variant] lets the UI rotate wording
 * (e.g. day-derived) without any stored usage history.
 */
interface CompanionVoice {
    fun line(moment: CompanionMoment, config: AppConfig, variant: Int = 0): CompanionLine
}
