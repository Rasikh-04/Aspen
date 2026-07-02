package app.aspen.domain.onboarding.model

/**
 * How food logging is offered for a given presentation (docs/03 FR-3b, docs/01 §5).
 *
 * Note this governs *food* logging only — behaviour/feeling logging is lower-risk and broadly
 * available regardless (docs/03 FR-3b). In every mode logging stays **numberless** (text + feeling
 * tags); there is no numeric food/body data anywhere (CLAUDE.md #1).
 */
enum class FoodLoggingMode {
    /** Not offered at all — contraindicated (e.g. restriction, avoidance-sensory). */
    OFF,

    /** Offered but softened/reframed away from meal-checking toward feelings. */
    REFRAMED,

    /** Offered plainly (still numberless, shame-free, silent on empty days). */
    AVAILABLE,
}

/** How the companion speaks for this presentation (docs/05; copy itself lives in `:ui`). */
enum class CompanionTone {
    GENTLE_NEUTRAL,
    RESTRICTION_SENSITIVE,
    SELF_COMPASSION,
    POST_DISTRESS,
    SENSORY_AWARE,
}

/** Which tools to surface first (docs/11 §2 "key adaptivity"). */
enum class ToolEmphasis {
    BALANCED,
    GROUNDING,
    URGE_SURFING,
    POST_EATING_DISTRESS,
    SENSORY,
    SELF_WORTH,
}

/** How strongly to foreground the human-exit / treatment routing (docs/11 §3 Q8–Q10). */
enum class SupportRoutingStrength { STANDARD, ELEVATED, FOREGROUND }

/**
 * Provenance of the profile→behaviour mapping (docs/07 Phase 3 `[APPROVE]`, docs/11 §6).
 *
 * The mapping rules — **especially the logging-suppression rules** — are a clinical-review gate
 * (docs/01 §5a, §5). Until ED-informed advisors sign each presentation off, [advisorVerified] stays
 * `false`. The mechanism runs so Dev B can wire and exercise the real adaptivity, but the flag lets a
 * release gate (Phase 7) refuse to enable an unverified mapping — mirroring the crisis-registry
 * "build the mechanism, mark it provisional" pattern (docs/STATUS.md).
 */
data class ProfileMappingProvenance(
    val advisorVerified: Boolean,
    val revision: String,
) {
    companion object {
        /** Current state: full mapping implemented, NOT yet advisor-signed (docs/11 §6). */
        val PROVISIONAL = ProfileMappingProvenance(advisorVerified = false, revision = "draft-2026-06-29")
    }
}

/**
 * The resolved, adaptive app configuration derived from an [OnboardingResult]
 * (`deriveConfig`, docs/11 §4). This is what the feature layer reads to decide what to show.
 *
 * [bodyImageFramingAllowed] is `false` for ARFID/avoidance presentations so no body/shape framing
 * appears at all (docs/11 Q6, §2). [provenance] records whether the underlying mapping has clinical
 * sign-off — see [ProfileMappingProvenance].
 */
data class AppConfig(
    val foodLoggingMode: FoodLoggingMode,
    val companionTone: CompanionTone,
    val toolEmphasis: ToolEmphasis,
    val supportRoutingStrength: SupportRoutingStrength,
    val bodyImageFramingAllowed: Boolean,
    val provenance: ProfileMappingProvenance,
)
