package app.aspen.design

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.aspen.core.i18n.SupportedLanguage

/**
 * Type tokens (docs/06 §2): warm serif display + humanist-sans body. Scale 12/14/16/18/22/26/32,
 * line-height favouring loose for body. All sizes are in sp so OS dynamic type is honoured (a11y).
 */
@Immutable
data class AspenTypography(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyLoose: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

internal fun aspenTypography(fonts: AspenFonts, language: SupportedLanguage): AspenTypography {
    // Urdu renders in Nastaliq; other languages use the humanist sans for body (docs/12 §3).
    val bodyFamily = if (language == SupportedLanguage.UR) fonts.nastaliq else fonts.body
    return AspenTypography(
        display = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 38.sp),
        title = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 32.sp),
        body = TextStyle(fontFamily = bodyFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyLoose = TextStyle(fontFamily = bodyFamily, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 32.sp),
        label = TextStyle(fontFamily = bodyFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        caption = TextStyle(fontFamily = bodyFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    )
}

/** Bridge our type tokens onto Material3 roles so default components inherit Aspen fonts. */
internal fun materialTypographyOf(t: AspenTypography): Typography = Typography(
    headlineLarge = t.display,
    headlineMedium = t.title,
    titleLarge = t.title,
    bodyLarge = t.bodyLoose,
    bodyMedium = t.body,
    labelLarge = t.label,
    bodySmall = t.caption,
)

val LocalAspenTypography = staticCompositionLocalOf<AspenTypography> {
    error("AspenTypography not provided — wrap content in AspenTheme { }")
}
