package app.aspen.ui.companion

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.aspen.design.AspenTheme
import app.aspen.design.LocalReducedMotion
import app.aspen.design.components.AspenCard
import app.aspen.design.components.AspenTextAction
import app.aspen.domain.ai.CompanionMoment
import app.aspen.domain.ai.CompanionVoice
import app.aspen.domain.companion.CompanionState
import app.aspen.domain.onboarding.AppConfigProvider
import app.aspen.ui.companion.sprite.CompanionSprite
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.companion_a11y_label
import app.aspen.ui.generated.resources.companion_rest_action
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** How often the playful time-box is re-checked (the box itself lives in the domain machine). */
private const val PLAYFUL_TICK_MS = 1000L

/**
 * The in-app companion (docs/05 §4) — the cross-platform baseline that works on Android AND iOS.
 * Floats above the tab content: ambient at the bottom edge, draggable, tap for a short playful
 * spell, and during play a quiet one-tap "rest" action dismisses it (docs/05 §3.1) until the user
 * summons it again from Settings. In the hard-moment flow it becomes gentle presence with one
 * curated line — never blocking anything, never handling the moment itself (docs/05 §3.4).
 *
 * The caller must NOT compose this on the crisis screen — the safety surface stays absolutely
 * clear (CLAUDE.md #6).
 */
@Composable
fun BoxScope.InAppCompanionLayer(
    controller: CompanionController,
    voice: CompanionVoice?,
    appConfigProvider: AppConfigProvider?,
) {
    controller.reducedMotion = LocalReducedMotion.current
    val state = controller.state
    if (state !is CompanionState.Ambient && state !is CompanionState.Playful && state !is CompanionState.GentlePresence) return

    // The playful time-box heartbeat: only runs while playful, cancelled the moment it isn't.
    if (state is CompanionState.Playful) {
        LaunchedEffect(Unit) {
            while (isActive) {
                delay(PLAYFUL_TICK_MS)
                controller.tick()
            }
        }
    }

    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    val a11yLabel = stringResource(Res.string.companion_a11y_label)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = AspenTheme.spacing.l, bottom = AspenTheme.spacing.xl)
            .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) },
    ) {
        if (state is CompanionState.GentlePresence && voice != null && appConfigProvider != null) {
            // One curated line of calm company (Phase-4 library; key → localized copy).
            val lineKey = remember { voice.line(CompanionMoment.HARD_MOMENT_COMPANY, appConfigProvider.current()).key }
            AspenCard(modifier = Modifier.widthIn(max = 220.dp)) {
                Text(
                    companionLineText(lineKey),
                    style = AspenTheme.typography.caption,
                    color = AspenTheme.colors.textSecondary,
                )
            }
            Spacer(Modifier.height(AspenTheme.spacing.s))
        }

        CompanionSprite(
            state = state,
            species = controller.prefs.species,
            modifier = Modifier
                .semantics { contentDescription = a11yLabel }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { controller.tap() })
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, amount ->
                        change.consume()
                        // Long-drag is "pick up and move"; the sprite settles where dropped.
                        dragX = (dragX + amount.x).coerceAtMost(0f)
                        dragY = (dragY + amount.y).coerceAtMost(0f)
                    }
                },
        )

        if (state is CompanionState.Playful) {
            Spacer(Modifier.height(AspenTheme.spacing.xs))
            // The guaranteed one-tap dismiss (docs/05 §3.1): calm wording, no guilt.
            AspenTextAction(
                label = stringResource(Res.string.companion_rest_action),
                onClick = {
                    dragX = 0f
                    dragY = 0f
                    controller.rest()
                },
            )
        }
    }
}
