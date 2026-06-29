package app.aspen.domain.onboarding.model

/**
 * Internal support profiles inferred by the onboarding questionnaire (docs/11 §2, docs/01 §5a).
 *
 * These are **never shown to the user** as labels and never presented as a diagnosis or confidence
 * score (CLAUDE.md #9, docs/11 §0). They drive adaptivity only — which tools surface, how the
 * companion speaks, and crucially whether food logging is offered or suppressed (docs/03 FR-3b).
 *
 * A person can carry more than one profile (common); scoring stores all as soft weights and acts on
 * the dominant one plus any protective flag (docs/11 §4).
 */
enum class SupportProfile {
    /** AN-spectrum restriction. Food logging suppressed/reframed; grounding emphasised. */
    RESTRICTION_LEANING,

    /** BED-spectrum. Logging optional + shame-free; urge-surfing/self-compassion tools. */
    BINGE_LEANING,

    /** BN-spectrum. Logging gentle; post-eating distress tools; strong human-exit. */
    PURGE_COMPENSATORY,

    /** ARFID. No body-image framing at all; sensory-aware, non-weight language. */
    AVOIDANCE_SENSORY,

    /** Cross-cutting body-image distress. No-comparison emphasis; appearance-talk banned hard. */
    BODY_IMAGE_DISTRESS,

    /** Default / skip / low-signal. Safest, most neutral configuration; food logging off. */
    MIXED_OR_UNSURE,
}

/**
 * Protective flags raised independently of the dominant profile (docs/11 §4 "conservative bias").
 *
 * Any meaningful restriction or avoidance-sensory signal raises [SUPPRESS_FOOD_LOGGING] even when a
 * different profile scores higher — "when in doubt, the more protective configuration." Flags are
 * additive and always win over the dominant-profile default.
 */
enum class ProtectiveFlag {
    /** Food logging must be off/reframed — contraindicated for this presentation (docs/01 §5). */
    SUPPRESS_FOOD_LOGGING,

    /** Body/shape/appearance framing must be absent entirely (ARFID — docs/11 Q6). */
    NO_BODY_IMAGE_FRAMING,
}
