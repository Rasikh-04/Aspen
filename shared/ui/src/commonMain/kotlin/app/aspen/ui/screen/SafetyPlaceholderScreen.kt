package app.aspen.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
import app.aspen.design.components.AspenTextAction
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.back
import app.aspen.ui.generated.resources.safety_body
import app.aspen.ui.generated.resources.safety_title

/**
 * Phase 1 placeholder for Flow C (docs/06 §6). The destination and its <=2-tap route exist now so
 * the human exit is structurally present (CLAUDE.md #6). It carries NO crisis numbers — crisis
 * content is advisor-verified and lands in Phase 2 (docs/09). Calm, serious tone — never alarm-red.
 */
@Composable
fun SafetyPlaceholderScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AspenTheme.colors.crisisBg)
            .padding(AspenTheme.spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(AspenTheme.spacing.xxl))
        Text(
            text = stringResource(Res.string.safety_title),
            style = AspenTheme.typography.title,
            color = AspenTheme.colors.crisis,
        )
        Spacer(Modifier.height(AspenTheme.spacing.m))
        Text(
            text = stringResource(Res.string.safety_body),
            style = AspenTheme.typography.bodyLoose,
            color = AspenTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(AspenTheme.spacing.xl))
        AspenTextAction(label = stringResource(Res.string.back), onClick = onBack)
    }
}
