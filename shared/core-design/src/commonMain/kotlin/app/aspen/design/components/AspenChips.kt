package app.aspen.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.aspen.design.AspenTheme

private const val SELECTED_BORDER_ALPHA = 0.4f

/**
 * A pill toggle whose selection is a calm tint shift (an eased fade — reduced-motion-safe by
 * definition, docs/06 §2 Motion). Never a hard check mark or a green/red state (CLAUDE.md #3).
 * [selectedContainer]/[selectedLabel] exist so serious surfaces (Flow C) can re-tone it.
 */
@Composable
fun AspenChoiceChip(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Checkbox,
    selectedContainer: Color = AspenTheme.colors.primarySoft,
    selectedLabel: Color = AspenTheme.colors.primaryDark,
) {
    val motion = AspenTheme.motion
    val container by animateColorAsState(
        targetValue = if (selected) selectedContainer else AspenTheme.colors.surface,
        animationSpec = tween(motion.shortMs),
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) selectedLabel else AspenTheme.colors.textSecondary,
        animationSpec = tween(motion.shortMs),
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) selectedLabel.copy(alpha = SELECTED_BORDER_ALPHA) else AspenTheme.colors.border,
        animationSpec = tween(motion.shortMs),
    )
    Surface(
        shape = AspenTheme.shapes.pill,
        color = container,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.toggleable(value = selected, role = role, onValueChange = { onToggle() }),
    ) {
        Text(
            text = label,
            style = AspenTheme.typography.label,
            color = labelColor,
            modifier = Modifier.padding(horizontal = AspenTheme.spacing.m, vertical = AspenTheme.spacing.sm),
        )
    }
}

/** A small read-only pill for showing an already-chosen tag (e.g. feelings on a saved entry). */
@Composable
fun AspenTagPill(label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = AspenTheme.shapes.pill,
        color = AspenTheme.colors.primaryFaint,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = AspenTheme.typography.caption,
            color = AspenTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = AspenTheme.spacing.sm, vertical = AspenTheme.spacing.xs),
        )
    }
}
