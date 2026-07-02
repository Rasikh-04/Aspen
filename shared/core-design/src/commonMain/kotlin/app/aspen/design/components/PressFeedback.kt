package app.aspen.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import app.aspen.design.LocalAspenMotion
import app.aspen.design.LocalReducedMotion

private const val PRESSED_SCALE = 0.98f

/**
 * A gentle press acknowledgement shared by every tappable Aspen component: a slight settle on
 * press, eased with the motion tokens. Disabled entirely under reduced motion (docs/06 §7, SR-6).
 * The scale is read inside [graphicsLayer] so pressing never recomposes the subtree.
 */
@Composable
internal fun Modifier.pressableScale(interactionSource: InteractionSource): Modifier {
    val reducedMotion = LocalReducedMotion.current
    val motion = LocalAspenMotion.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) PRESSED_SCALE else 1f,
        animationSpec = tween(motion.shortMs),
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
