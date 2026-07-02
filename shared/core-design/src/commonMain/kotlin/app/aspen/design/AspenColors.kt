package app.aspen.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.aspen.core.color.Palette

/** Semantic colour tokens (docs/06 §2). Soft, warm, calm — no pure red, including the crisis tone. */
@Immutable
data class AspenColors(
    val primary: Color,
    val primaryDark: Color,
    val primarySoft: Color,
    val primaryFaint: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textInverse: Color,
    val caution: Color,
    val cautionBg: Color,
    val crisis: Color,
    val crisisBg: Color,
)

internal fun aspenLightColors(): AspenColors = AspenColors(
    primary = Color(Palette.Sage500),
    primaryDark = Color(Palette.Sage700),
    primarySoft = Color(Palette.Sage100),
    primaryFaint = Color(Palette.Sage50),
    secondary = Color(Palette.Sand500),
    background = Color(Palette.WarmOffWhite),
    surface = Color(Palette.WarmWhite),
    border = Color(Palette.Border),
    textPrimary = Color(Palette.TextPrimary),
    textSecondary = Color(Palette.TextSecondary),
    textMuted = Color(Palette.TextMuted),
    textInverse = Color(Palette.TextInverse),
    caution = Color(Palette.Caution),
    cautionBg = Color(Palette.CautionBg),
    crisis = Color(Palette.Crisis),
    crisisBg = Color(Palette.CrisisBg),
)

val LocalAspenColors = staticCompositionLocalOf { aspenLightColors() }
