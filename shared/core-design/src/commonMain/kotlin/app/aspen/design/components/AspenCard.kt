package app.aspen.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.aspen.design.AspenTheme

private const val BORDER_ALPHA = 0.7f

/**
 * The Aspen surface for grouped content: warm white, softly rounded, a whisper of an edge and
 * shadow — depth without hard dividers (docs/06 §1 "softness over sharpness"). Pass [onClick] to
 * make the whole card the touch target (with the shared gentle press settle).
 */
@Composable
fun AspenCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val padding = contentPadding ?: PaddingValues(AspenTheme.spacing.m)
    val border = BorderStroke(1.dp, AspenTheme.colors.border.copy(alpha = BORDER_ALPHA))
    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            shape = AspenTheme.shapes.medium,
            color = AspenTheme.colors.surface,
            border = border,
            shadowElevation = 1.dp,
            interactionSource = interactionSource,
            modifier = modifier.pressableScale(interactionSource),
        ) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Surface(
            shape = AspenTheme.shapes.medium,
            color = AspenTheme.colors.surface,
            border = border,
            shadowElevation = 1.dp,
            modifier = modifier,
        ) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}
