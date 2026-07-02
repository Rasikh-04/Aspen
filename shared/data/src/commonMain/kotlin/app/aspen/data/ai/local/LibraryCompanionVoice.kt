package app.aspen.data.ai.local

import app.aspen.domain.ai.CompanionLibrary
import app.aspen.domain.ai.CompanionLine
import app.aspen.domain.ai.CompanionMoment
import app.aspen.domain.ai.CompanionVoice
import app.aspen.domain.ai.LineRanker
import app.aspen.domain.onboarding.model.AppConfig

/**
 * Platform-provided on-device line ranker (docs/04 ADR-003 Tier 1), or null where none is
 * available. Android: LiteRT/MediaPipe text-embedder over the bundled model asset (returns null if
 * the model file is absent — the app must work fully without it). JVM + iOS: null (deterministic
 * selection; the iOS ranker is a tracked later task, like the iOS cipher).
 */
expect fun platformLineRanker(): LineRanker?

/**
 * The Tier-1 [CompanionVoice] (docs/04 ADR-003): selection over the curated [library], optionally
 * sharpened by an on-device [ranker]. Safety by construction, in order of importance:
 *
 * 1. Every word comes from the approved library — the ranker can only REORDER candidates; any
 *    out-of-candidates result is discarded (it cannot smuggle in a line from another moment/tone).
 * 2. Total and failure-proof: a null, throwing, or misbehaving ranker degrades silently to the
 *    deterministic pick — the companion never errors at a hard moment.
 * 3. Personalisation input is only the onboarding [AppConfig] (tone) + [variant]; no usage history,
 *    no engagement signal (success is the user feeling steadier, not the companion being used).
 */
class LibraryCompanionVoice(
    private val library: CompanionLibrary = DefaultCompanionLibrary.library,
    private val ranker: LineRanker? = platformLineRanker(),
) : CompanionVoice {

    override fun line(moment: CompanionMoment, config: AppConfig, variant: Int): CompanionLine {
        val candidates = library.candidates(moment, config.companionTone)
        check(candidates.isNotEmpty()) { "companion library has no lines for $moment (library invariant broken)" }

        val ranked = ranker?.let { r ->
            runCatching { r.pickBest(rankingContext(moment, config), candidates) }.getOrNull()
        }
        return ranked?.takeIf { it in candidates } ?: candidates[variant.mod(candidates.size)]
    }

    /** Non-user-facing English feature text for the ranker — mirrors the lines' rankingHint style. */
    private fun rankingContext(moment: CompanionMoment, config: AppConfig): String =
        "moment: ${moment.name.lowercase().replace('_', ' ')}; " +
            "tone: ${config.companionTone.name.lowercase().replace('_', ' ')}; " +
            "emphasis: ${config.toolEmphasis.name.lowercase().replace('_', ' ')}"
}
