package app.aspen.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion tokens (docs/06 §2): slow, eased, calming (200-400ms). [reducedMotion] replaces
 * transitions with fades and disables companion/breathing animation (docs/06 §7, SR-6).
 */
@Immutable
data class AspenMotion(
    val reducedMotion: Boolean = false,
    val shortMs: Int = 200,
    val mediumMs: Int = 300,
    val longMs: Int = 400,
)

val LocalAspenMotion = staticCompositionLocalOf { AspenMotion() }

/** Convenience seam for animations to check the reduced-motion preference. */
val LocalReducedMotion = staticCompositionLocalOf { false }
