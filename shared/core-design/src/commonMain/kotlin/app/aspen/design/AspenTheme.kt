package app.aspen.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import app.aspen.core.i18n.SupportedLanguage

/**
 * The Aspen design-system theme: a thin custom token layer over a Material3 substrate (so we keep
 * Material's a11y/components while overriding palette, shape, and type). Provides all Local* tokens
 * and maps "error" onto soft amber — never red (CLAUDE.md #5).
 */
@Composable
fun AspenTheme(
    language: SupportedLanguage = SupportedLanguage.EN,
    reducedMotion: Boolean = false,
    colors: AspenColors = aspenLightColors(),
    content: @Composable () -> Unit,
) {
    val fonts = aspenFonts()
    val typography = remember(fonts, language) { aspenTypography(fonts, language) }

    val materialScheme = lightColorScheme(
        primary = colors.primary,
        onPrimary = colors.textInverse,
        secondary = colors.secondary,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        outline = colors.border,
        // Map Material's "error" role to soft amber so no component can render alarm red (docs/06 §2).
        error = colors.caution,
        onError = colors.textInverse,
        errorContainer = colors.cautionBg,
        onErrorContainer = colors.textPrimary,
    )

    CompositionLocalProvider(
        LocalAspenColors provides colors,
        LocalAspenTypography provides typography,
        LocalAspenSpacing provides AspenSpacing(),
        LocalAspenShapes provides AspenShapes(),
        LocalAspenMotion provides AspenMotion(reducedMotion = reducedMotion),
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = materialTypographyOf(typography),
            content = content,
        )
    }
}

/** Token accessors: `AspenTheme.colors`, `AspenTheme.spacing`, etc. (mirrors Material3's pattern). */
object AspenTheme {
    val colors: AspenColors
        @Composable get() = LocalAspenColors.current
    val typography: AspenTypography
        @Composable get() = LocalAspenTypography.current
    val spacing: AspenSpacing
        @Composable get() = LocalAspenSpacing.current
    val shapes: AspenShapes
        @Composable get() = LocalAspenShapes.current
    val motion: AspenMotion
        @Composable get() = LocalAspenMotion.current
}
