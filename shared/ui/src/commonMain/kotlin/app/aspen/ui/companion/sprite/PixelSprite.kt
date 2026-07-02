package app.aspen.ui.companion.sprite

/**
 * The procedural pixel-sprite model (Phase-5 approved design decision 2026-07-03, deviation from
 * docs/05 §5 "soft round creature" reconciled as: pixel FORM, calm palette, slow gentle motion).
 * Characters are authored as rows of characters — no binary art assets in the repo, trivially
 * reviewable in a diff, recolourable in one place, and identical in-app and in the Android overlay.
 *
 * This file is pure Kotlin (no Compose) so sprite integrity is host-testable like domain code.
 */

/** A pixel not drawn at all. */
const val TRANSPARENT_PIXEL: Char = '.'

/**
 * Frame-rate ceiling (docs/04 §6 battery budget; docs/05 §5 "slow, breathing, gentle" — nothing
 * twitchy). The integrity test rejects any animation above it.
 */
const val MAX_SPRITE_FPS: Int = 8

/** One drawn frame: equal-length rows of palette characters ([TRANSPARENT_PIXEL] = skip). */
data class PixelFrame(val rows: List<String>) {
    val height: Int get() = rows.size
    val width: Int get() = rows.firstOrNull()?.length ?: 0
}

/** A looping animation. [fps] is deliberately tiny — ambient is 2, playful peaks at 6. */
data class SpriteAnimation(val frames: List<PixelFrame>, val fps: Int)

/**
 * Everything one character can do. There is intentionally NO animation for absence, sadness-at-
 * being-left, or attention-seeking — an expression that doesn't exist can't guilt anyone (SR-4,
 * docs/05 §5 "'don't leave me' faces are banned").
 */
data class SpriteSheet(
    /** Palette char → 0xAARRGGBB. Colours are integrity-tested: calm, never alarm-red. */
    val palette: Map<Char, Long>,
    /** Resting at an edge: breathing, the odd blink. Near-zero cost. */
    val ambient: SpriteAnimation,
    /** After a tap: a gentle lean-and-hop. Time-boxed by [app.aspen.domain.companion.CompanionBehaviour]. */
    val playful: SpriteAnimation,
    /** Calm company in the hard-moment flow: the ambient frames, even slower. */
    val gentle: SpriteAnimation,
) {
    /** The reduced-motion face: the first ambient frame, drawn once, never animated (SR-6). */
    val still: PixelFrame get() = ambient.frames.first()

    val animations: List<SpriteAnimation> get() = listOf(ambient, playful, gentle)
}

internal const val AMBIENT_FPS = 2
internal const val PLAYFUL_FPS = 6
internal const val GENTLE_FPS = 1
