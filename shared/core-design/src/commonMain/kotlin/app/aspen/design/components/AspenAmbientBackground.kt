package app.aspen.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.aspen.design.AspenTheme

private const val WASH_END_FRACTION = 0.45f
private const val HALO_ALPHA = 0.5f
private const val HALO_RADIUS_FRACTION = 0.55f
private const val HALO_CENTER_X_FRACTION = 0.82f
private const val HALO_CENTER_Y_FRACTION = -0.25f

/**
 * The quiet light behind every screen: warm background with a faint sage wash settling from the
 * top and one soft halo — a room with daylight in it, not a flat text panel. Drawn statically in
 * [drawBehind] (no animation, no recomposition) so ambience never costs battery (docs/04 NFR-battery).
 */
@Composable
fun AspenAmbientBackground(
    modifier: Modifier = Modifier,
    tint: Color = AspenTheme.colors.primaryFaint,
    halo: Color = AspenTheme.colors.primarySoft,
    content: @Composable BoxScope.() -> Unit,
) {
    val background = AspenTheme.colors.background
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(background)
                drawRect(
                    Brush.verticalGradient(
                        0f to tint,
                        WASH_END_FRACTION to background,
                        endY = size.height,
                    ),
                )
                val radius = size.width * HALO_RADIUS_FRACTION
                val center = Offset(
                    x = size.width * HALO_CENTER_X_FRACTION,
                    y = radius * HALO_CENTER_Y_FRACTION,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(halo.copy(alpha = HALO_ALPHA), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            },
        content = content,
    )
}
