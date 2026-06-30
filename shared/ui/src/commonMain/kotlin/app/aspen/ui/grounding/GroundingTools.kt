package app.aspen.ui.grounding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import app.aspen.design.AspenTheme
import app.aspen.design.LocalReducedMotion
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.breathe_in
import app.aspen.ui.generated.resources.breathe_hold
import app.aspen.ui.generated.resources.breathe_out
import app.aspen.ui.generated.resources.breathe_reduced_note
import app.aspen.ui.generated.resources.breathe_title
import app.aspen.ui.generated.resources.grounding_close
import app.aspen.ui.generated.resources.grounding_done
import app.aspen.ui.generated.resources.grounding_gentle_close
import app.aspen.ui.generated.resources.ground_54321_hear
import app.aspen.ui.generated.resources.ground_54321_intro
import app.aspen.ui.generated.resources.ground_54321_next
import app.aspen.ui.generated.resources.ground_54321_see
import app.aspen.ui.generated.resources.ground_54321_smell
import app.aspen.ui.generated.resources.ground_54321_taste
import app.aspen.ui.generated.resources.ground_54321_title
import app.aspen.ui.generated.resources.ground_54321_touch
import app.aspen.ui.generated.resources.ride_urge_body
import app.aspen.ui.generated.resources.ride_urge_title

/**
 * Full-screen tool shell (docs/06 §3 Flow A): single-purpose, exit always visible (top Close + a
 * gentle "I'm okay for now"), calm close line — never "great job"/streaks/scores (CLAUDE.md #3).
 */
@Composable
private fun ToolScaffold(
    title: String,
    onExit: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AspenTheme.colors.background)
            .padding(horizontal = AspenTheme.spacing.l, vertical = AspenTheme.spacing.l),
    ) {
        TextButton(onClick = onExit) {
            Text(stringResource(Res.string.grounding_close), color = AspenTheme.colors.textSecondary)
        }
        Spacer(Modifier.height(AspenTheme.spacing.m))
        Text(title, style = AspenTheme.typography.title, color = AspenTheme.colors.textPrimary)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
        Text(
            text = stringResource(Res.string.grounding_gentle_close),
            style = AspenTheme.typography.body,
            color = AspenTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AspenTheme.spacing.s))
        Button(
            onClick = onExit,
            shape = AspenTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = AspenTheme.colors.primary,
                contentColor = AspenTheme.colors.textInverse,
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(Res.string.grounding_done), style = AspenTheme.typography.label)
        }
    }
}

/** Paced breathing. Honours reduced-motion (SR-6): no animation, static cue words instead. */
@Composable
fun BreatheScreen(onExit: () -> Unit) {
    val reduced = LocalReducedMotion.current
    ToolScaffold(title = stringResource(Res.string.breathe_title), onExit = onExit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (reduced) {
                Box(Modifier.size(160.dp).clip(CircleShape).background(AspenTheme.colors.primaryDark.copy(alpha = 0.18f)))
                Spacer(Modifier.height(AspenTheme.spacing.l))
                BreatheCue(Res.string.breathe_in)
                BreatheCue(Res.string.breathe_hold)
                BreatheCue(Res.string.breathe_out)
                Spacer(Modifier.height(AspenTheme.spacing.s))
                Text(
                    stringResource(Res.string.breathe_reduced_note),
                    style = AspenTheme.typography.caption,
                    color = AspenTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            } else {
                val transition = rememberInfiniteTransition()
                val t by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 9000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                )
                val scale = breatheScale(t)
                val cue = breatheCueFor(t)
                Box(
                    Modifier
                        .size(180.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .background(AspenTheme.colors.primaryDark.copy(alpha = 0.18f)),
                )
                Spacer(Modifier.height(AspenTheme.spacing.xl))
                BreatheCue(cue)
            }
        }
    }
}

@Composable
private fun BreatheCue(label: StringResource) {
    Text(
        text = stringResource(label),
        style = AspenTheme.typography.title,
        color = AspenTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = AspenTheme.spacing.xs),
    )
}

// 4s in (grow), 1.5s hold, 3.5s out (shrink) over a 9s cycle.
private fun breatheScale(t: Float): Float = when {
    t < 0.44f -> 0.55f + (t / 0.44f) * 0.45f
    t < 0.61f -> 1.0f
    else -> 1.0f - ((t - 0.61f) / 0.39f) * 0.45f
}

private fun breatheCueFor(t: Float): StringResource = when {
    t < 0.44f -> Res.string.breathe_in
    t < 0.61f -> Res.string.breathe_hold
    else -> Res.string.breathe_out
}

/** 5-4-3-2-1 sensory grounding. The counts here are sensory steps, not food/body numbers (CLAUDE.md #1). */
@Composable
fun Ground54321Screen(onExit: () -> Unit) {
    val steps = remember {
        listOf(
            Res.string.ground_54321_see,
            Res.string.ground_54321_hear,
            Res.string.ground_54321_touch,
            Res.string.ground_54321_smell,
            Res.string.ground_54321_taste,
        )
    }
    var index by remember { mutableStateOf(0) }
    ToolScaffold(title = stringResource(Res.string.ground_54321_title), onExit = onExit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(Res.string.ground_54321_intro),
                style = AspenTheme.typography.bodyLoose,
                color = AspenTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AspenTheme.spacing.xl))
            Text(
                stringResource(steps[index]),
                style = AspenTheme.typography.display,
                color = AspenTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AspenTheme.spacing.xl))
            if (index < steps.lastIndex) {
                Button(
                    onClick = { index += 1 },
                    shape = AspenTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AspenTheme.colors.surface,
                        contentColor = AspenTheme.colors.textPrimary,
                    ),
                ) {
                    Text(stringResource(Res.string.ground_54321_next), style = AspenTheme.typography.label)
                }
            }
        }
    }
}

/** Urge surfing — a calm "this will pass" wave framing (docs/05 tools). No timer, no countdown. */
@Composable
fun RideTheUrgeScreen(onExit: () -> Unit) {
    ToolScaffold(title = stringResource(Res.string.ride_urge_title), onExit = onExit) {
        Text(
            stringResource(Res.string.ride_urge_body),
            style = AspenTheme.typography.bodyLoose,
            color = AspenTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AspenTheme.spacing.s),
        )
    }
}
