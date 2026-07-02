package app.aspen.ui.reflect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.aspen.design.AspenTheme
import app.aspen.design.components.AspenCard
import app.aspen.design.components.AspenPrimaryButton
import app.aspen.design.components.AspenQuietButton
import app.aspen.domain.ai.ReflectionCompanion
import app.aspen.domain.ai.ReflectionOutcome
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.reflect_companion_handoff
import app.aspen.ui.generated.resources.reflect_companion_handoff_button
import app.aspen.ui.generated.resources.reflect_companion_hint
import app.aspen.ui.generated.resources.reflect_companion_send
import app.aspen.ui.generated.resources.reflect_companion_thinking
import app.aspen.ui.generated.resources.reflect_companion_title
import app.aspen.ui.generated.resources.reflect_companion_unavailable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** What the companion surface is currently showing. */
private sealed interface CompanionUiState {
    data object Idle : CompanionUiState
    data object Waiting : CompanionUiState
    data class Replied(val text: String) : CompanionUiState
    data object HandOff : CompanionUiState
    data object Unavailable : CompanionUiState
}

/**
 * The Tier-2 cloud reflection surface (docs/03 FR-5), rendered ONLY while the user's explicit
 * consent grant is active — visibility itself is the consent state. One exchange at a time: send →
 * guarded reply. On a crisis hand-off the surface shows a validating line and the route to a real
 * person (CLAUDE.md #8: warm hand-off, never management); offline/errors degrade to one calm line
 * (never an error state). All logic lives in [ReflectionCompanion]; this card only renders outcomes.
 */
@Composable
internal fun ReflectionCompanionCard(
    companion: ReflectionCompanion,
    onReachSomeone: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<CompanionUiState>(CompanionUiState.Idle) }
    val scope = rememberCoroutineScope()

    AspenCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.reflect_companion_title),
            style = AspenTheme.typography.label,
            color = AspenTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(AspenTheme.spacing.s))

        when (val current = state) {
            is CompanionUiState.Replied -> CompanionLineText(current.text)
            CompanionUiState.HandOff -> {
                CompanionLineText(stringResource(Res.string.reflect_companion_handoff))
                Spacer(Modifier.height(AspenTheme.spacing.s))
                AspenPrimaryButton(
                    label = stringResource(Res.string.reflect_companion_handoff_button),
                    onClick = onReachSomeone,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CompanionUiState.Unavailable -> CompanionLineText(stringResource(Res.string.reflect_companion_unavailable))
            CompanionUiState.Waiting -> CompanionLineText(stringResource(Res.string.reflect_companion_thinking))
            CompanionUiState.Idle -> Unit
        }

        Spacer(Modifier.height(AspenTheme.spacing.s))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = {
                Text(
                    stringResource(Res.string.reflect_companion_hint),
                    style = AspenTheme.typography.body,
                    color = AspenTheme.colors.textMuted,
                )
            },
            textStyle = AspenTheme.typography.body,
            shape = AspenTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AspenTheme.colors.primary,
                unfocusedBorderColor = AspenTheme.colors.border,
                focusedContainerColor = AspenTheme.colors.surface,
                unfocusedContainerColor = AspenTheme.colors.surface,
                cursorColor = AspenTheme.colors.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AspenTheme.spacing.s))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AspenQuietButton(
                label = stringResource(Res.string.reflect_companion_send),
                onClick = {
                    val text = input
                    input = ""
                    state = CompanionUiState.Waiting
                    scope.launch {
                        state = when (val outcome = companion.reflect(text)) {
                            is ReflectionOutcome.Reply -> CompanionUiState.Replied(outcome.text)
                            ReflectionOutcome.CrisisHandOff -> CompanionUiState.HandOff
                            ReflectionOutcome.Disabled, ReflectionOutcome.Unavailable -> CompanionUiState.Unavailable
                        }
                    }
                },
                enabled = input.isNotBlank() && state != CompanionUiState.Waiting,
            )
        }
    }
}

@Composable
private fun CompanionLineText(text: String) {
    Text(text, style = AspenTheme.typography.bodyLoose, color = AspenTheme.colors.textPrimary)
}
