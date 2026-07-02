package app.aspen.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.aspen.design.AspenTheme
import app.aspen.design.LocalReducedMotion

private val DOT_SIZE = 6.dp
private val CURRENT_DOT_SIZE = 8.dp

/**
 * Progress as *presence*: filled vs. soft dots with the current one breathing slightly larger —
 * never a number, fraction, or bar that can be "incomplete" (CLAUDE.md #3). Transitions are eased
 * fades; under reduced motion the dots snap.
 */
@Composable
fun AspenPresenceDots(total: Int, active: Int, modifier: Modifier = Modifier) {
    val motion = AspenTheme.motion
    val reducedMotion = LocalReducedMotion.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AspenTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (position in 1..total) {
            val color by animateColorAsState(
                targetValue = if (position <= active) AspenTheme.colors.primary else AspenTheme.colors.border,
                animationSpec = tween(motion.mediumMs),
            )
            val size by animateDpAsState(
                targetValue = if (position == active) CURRENT_DOT_SIZE else DOT_SIZE,
                animationSpec = if (reducedMotion) snap() else tween(motion.mediumMs),
            )
            Box(Modifier.size(size).clip(CircleShape).background(color))
        }
    }
}
