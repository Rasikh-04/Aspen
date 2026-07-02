package app.aspen.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
import app.aspen.design.components.AspenPrimaryButton
import app.aspen.design.components.AspenTextAction
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.home_greeting
import app.aspen.ui.generated.resources.home_hard_moment
import app.aspen.ui.generated.resources.home_reach_person
import app.aspen.ui.generated.resources.home_subtitle

/**
 * Calm Home (docs/06 Flow A.1): near-empty, generous whitespace, no metrics and no food/number
 * prompts. One soft entry to the hard-moment flow, plus a persistent, low-key route to a person
 * (never red, never pushy — CLAUDE.md #6). Progress is never shown as a number or streak.
 */
@Composable
fun CalmHomeScreen(
    onHardMoment: () -> Unit,
    onReachPerson: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AspenTheme.spacing.l, vertical = AspenTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.home_greeting),
            style = AspenTheme.typography.display,
            color = AspenTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AspenTheme.spacing.m))
        Text(
            text = stringResource(Res.string.home_subtitle),
            style = AspenTheme.typography.bodyLoose,
            color = AspenTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AspenTheme.spacing.xxl))
        AspenPrimaryButton(
            label = stringResource(Res.string.home_hard_moment),
            onClick = onHardMoment,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        )
        Spacer(Modifier.height(AspenTheme.spacing.xxxl))
        AspenTextAction(
            label = stringResource(Res.string.home_reach_person),
            onClick = onReachPerson,
        )
    }
}
