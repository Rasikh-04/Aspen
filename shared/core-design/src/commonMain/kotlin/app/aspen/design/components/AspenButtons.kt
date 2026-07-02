package app.aspen.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.aspen.design.AspenTheme

/** Minimum control height: comfortable to hit in a hard moment (docs/06 §7, ≥48dp + calm air). */
private val MIN_CONTROL_HEIGHT = 56.dp

/**
 * The single forward action of a screen: soft pill, sage fill, gentle press settle. There is at
 * most one of these per surface — a screen that "needs" two primary actions is too busy (docs/06 §1).
 * Disabled is a quieter tint, never a greyed "wrong" state (CLAUDE.md #3/#5).
 */
@Composable
fun AspenPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = AspenTheme.shapes.large,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = AspenTheme.colors.primary,
            contentColor = AspenTheme.colors.textInverse,
            disabledContainerColor = AspenTheme.colors.primarySoft,
            disabledContentColor = AspenTheme.colors.textMuted,
        ),
        modifier = modifier.heightIn(min = MIN_CONTROL_HEIGHT).pressableScale(interactionSource),
    ) {
        Text(label, style = AspenTheme.typography.label)
    }
}

/**
 * A secondary route: faint sage fill with a soft edge — present without competing with the
 * primary action. Replaces raw [androidx.compose.material3.OutlinedButton] everywhere.
 */
@Composable
fun AspenQuietButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = AspenTheme.shapes.large,
        interactionSource = interactionSource,
        border = BorderStroke(1.dp, AspenTheme.colors.primarySoft),
        colors = ButtonDefaults.buttonColors(
            containerColor = AspenTheme.colors.primaryFaint,
            contentColor = AspenTheme.colors.primaryDark,
            disabledContainerColor = AspenTheme.colors.surface,
            disabledContentColor = AspenTheme.colors.textMuted,
        ),
        modifier = modifier.heightIn(min = MIN_CONTROL_HEIGHT).pressableScale(interactionSource),
    ) {
        Text(label, style = AspenTheme.typography.label)
    }
}

/** A low-key inline action (back / skip / close / delete): quiet text, no chrome. */
@Composable
fun AspenTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AspenTheme.colors.textSecondary,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(label, style = AspenTheme.typography.label, color = color)
    }
}
