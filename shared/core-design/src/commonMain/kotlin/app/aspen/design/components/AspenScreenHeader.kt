package app.aspen.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.aspen.design.AspenTheme

/**
 * The standard screen opening: serif display title plus an optional loose-set subtitle. One shared
 * shape so every surface breathes the same way instead of each screen hand-rolling its heading.
 */
@Composable
fun AspenScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = title,
            style = AspenTheme.typography.display,
            color = AspenTheme.colors.textPrimary,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(AspenTheme.spacing.xs))
            Text(
                text = subtitle,
                style = AspenTheme.typography.bodyLoose,
                color = AspenTheme.colors.textSecondary,
            )
        }
    }
}
