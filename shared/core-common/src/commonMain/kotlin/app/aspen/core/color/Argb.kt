package app.aspen.core.color

import kotlin.math.pow

/**
 * Pure ARGB colour math (no platform/Compose dependency) so accessibility contrast can be
 * unit-tested without a device (docs/06 §7 "verify each token pair").
 * Colours are encoded as 0xAARRGGBB in a [Long].
 */
object Argb {

    fun red(argb: Long): Int = ((argb shr 16) and 0xFF).toInt()
    fun green(argb: Long): Int = ((argb shr 8) and 0xFF).toInt()
    fun blue(argb: Long): Int = (argb and 0xFF).toInt()

    /** WCAG relative luminance of a colour (alpha ignored). */
    fun relativeLuminance(argb: Long): Double {
        fun linear(channel8: Int): Double {
            val s = channel8 / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = linear(red(argb))
        val g = linear(green(argb))
        val b = linear(blue(argb))
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** WCAG contrast ratio between two colours; always >= 1.0. */
    fun contrastRatio(a: Long, b: Long): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * An "alarm red" is banned anywhere in Aspen, including the crisis surface (CLAUDE.md #5,
     * docs/06 §2). Heuristic: a strongly-dominant red channel with suppressed green/blue.
     */
    fun isAlarmRed(argb: Long): Boolean {
        val r = red(argb); val g = green(argb); val b = blue(argb)
        return r >= 200 && g <= 90 && b <= 90
    }
}
