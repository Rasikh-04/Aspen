package app.aspen.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.calm_placeholder
import app.aspen.ui.generated.resources.reflect_placeholder
import app.aspen.ui.generated.resources.settings_placeholder

@Composable
private fun CalmPlaceholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(AspenTheme.spacing.l),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AspenTheme.typography.bodyLoose,
            color = AspenTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ReflectScreen() = CalmPlaceholder(stringResource(Res.string.reflect_placeholder))

@Composable
fun CalmScreen() = CalmPlaceholder(stringResource(Res.string.calm_placeholder))

@Composable
fun SettingsScreen() = CalmPlaceholder(stringResource(Res.string.settings_placeholder))
