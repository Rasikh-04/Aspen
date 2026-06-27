package app.aspen.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4/8/12/16/20/24/32/40/48 dp scale (docs/06 §2). Default to the generous end. */
@Immutable
data class AspenSpacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val m: Dp = 16.dp,
    val ml: Dp = 20.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 40.dp,
    val xxxl: Dp = 48.dp,
)

val LocalAspenSpacing = staticCompositionLocalOf { AspenSpacing() }
