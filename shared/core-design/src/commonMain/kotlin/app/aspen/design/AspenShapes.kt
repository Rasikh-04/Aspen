package app.aspen.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Radius tokens (docs/06 §2): small 8 / medium 16 / large 24; pills for chips. Softness over sharpness. */
@Immutable
data class AspenShapes(
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(16.dp),
    val large: RoundedCornerShape = RoundedCornerShape(24.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
)

val LocalAspenShapes = staticCompositionLocalOf { AspenShapes() }
