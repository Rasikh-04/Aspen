package app.aspen.domain.companion.model

/**
 * The v1 companion lineup (docs/05 §5, Phase-5 approved design decision 2026-07-03): three
 * procedurally-drawn pixel characters. `ASPEN_SPRITE` is the default — an Aspen-original,
 * warm-toned pixel critter with a small floating heart (an homage to terminal-pet mascots,
 * deliberately NOT anyone's trade dress). All species are soft, non-human, non-body forms
 * (docs/05 §5 "no body-image projection"). Dog/owl/panda are a fast-follow in the same format.
 */
enum class CompanionSpecies { ASPEN_SPRITE, CAT, BUNNY }

/**
 * Where the companion is allowed to appear. `IN_APP` is the cross-platform baseline (docs/04
 * ADR-001); `OVERLAY` is the Android-only system-overlay enhancement (docs/05 §6) and is ignored
 * on platforms without overlays. "Home-screen-only" is deliberately NOT a scope: Android offers no
 * privacy-clean way to detect the launcher (approved deferral 2026-07-03, recorded in STATUS).
 */
enum class CompanionScope { IN_APP, OVERLAY }

/**
 * User choices for the companion — EVERYTHING defaults to off/quiet (docs/05 §3.1 "off by default,
 * fully optional"; CLAUDE.md #10). Enabling is always an explicit act in Settings; nothing here is
 * ever flipped on by the app itself.
 */
data class CompanionPrefs(
    /** Master switch for the in-app companion. */
    val enabled: Boolean = false,
    val species: CompanionSpecies = CompanionSpecies.ASPEN_SPRITE,
    /** Android overlay ("appear across apps"); meaningless without [enabled] and the OS permission. */
    val overlayEnabled: Boolean = false,
    /** Rare, gentle check-in notifications (SR-4); scheduling lands in Phase 5, off by default (FR-8). */
    val notificationsEnabled: Boolean = false,
)
