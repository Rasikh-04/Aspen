package app.aspen.domain.onboarding.model

/**
 * Routing hints from Q8–Q10 (docs/11 §3, §4). These tune how strongly Aspen surfaces the human-exit
 * and treatment-finder — they are **not** a profile and **not** a risk assessment (docs/11 §3 Q10).
 */
data class RoutingHints(
    val supportRoutingStrength: SupportRoutingStrength,
    /** Offer trusted-contact / quick-reach setup (Q10 "set that up", or Q9 "not right now"). */
    val offerTrustedContactSetup: Boolean,
    /** Foreground the treatment-finder / Tier-1 directory (Q9 "not right now", high impact). */
    val emphasiseTreatmentFinder: Boolean,
)

/**
 * The outcome of scoring the questionnaire (`deriveProfile`, docs/11 §4 pseudocode contract).
 *
 * [profiles] are **soft weights**, internal only — never a user-visible label or score (CLAUDE.md
 * #9, docs/11 §0). [dominantProfile] is the highest-weighted profile (ties / low signal →
 * [SupportProfile.MIXED_OR_UNSURE]). [protectiveFlags] can override the dominant profile's defaults
 * toward the safer configuration (docs/11 §4 conservative bias).
 */
data class OnboardingResult(
    val profiles: Map<SupportProfile, Int>,
    val dominantProfile: SupportProfile,
    val protectiveFlags: Set<ProtectiveFlag>,
    val routingHints: RoutingHints,
)
