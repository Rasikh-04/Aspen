package app.aspen.companion.overlay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.aspen.design.AspenTheme
import app.aspen.design.components.AspenTextAction
import app.aspen.domain.companion.CompanionState
import app.aspen.ui.companion.CompanionController
import app.aspen.ui.companion.sprite.CompanionSprite
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.companion_a11y_label
import app.aspen.ui.generated.resources.companion_rest_action
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.stringResource

private const val PLAYFUL_TICK_MS = 1000L

/**
 * What actually lives inside the overlay window: the same sprite + rules as the in-app layer.
 * Suspended (fullscreen app underneath) renders NOTHING — no composition subtree, no frames, no
 * battery (docs/04 §6). Dismiss ("rest") stops the whole service via [onDismissed].
 */
@Composable
internal fun OverlayCompanionContent(
    controller: CompanionController,
    onMoveBy: (dx: Float, dy: Float) -> Unit,
    onDismissed: () -> Unit,
) {
    val state = controller.state

    // Hidden means the user dismissed it: the service has no reason to exist until re-summoned.
    LaunchedEffect(state) {
        if (state is CompanionState.Hidden) onDismissed()
    }
    if (state !is CompanionState.Ambient && state !is CompanionState.Playful) return

    if (state is CompanionState.Playful) {
        LaunchedEffect(Unit) {
            while (isActive) {
                delay(PLAYFUL_TICK_MS)
                controller.tick()
            }
        }
    }

    AspenTheme(reducedMotion = controller.reducedMotion) {
        val a11yLabel = stringResource(Res.string.companion_a11y_label)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(AspenTheme.spacing.s)) {
            CompanionSprite(
                state = state,
                species = controller.prefs.species,
                modifier = Modifier
                    .semantics { contentDescription = a11yLabel }
                    .pointerInput(Unit) { detectTapGestures(onTap = { controller.tap() }) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, amount ->
                            change.consume()
                            onMoveBy(amount.x, amount.y)
                        }
                    },
            )
            if (state is CompanionState.Playful) {
                Spacer(Modifier.height(AspenTheme.spacing.xs))
                // The guaranteed one-tap dismiss (docs/05 §3.1) — stops the service entirely.
                AspenTextAction(
                    label = stringResource(Res.string.companion_rest_action),
                    onClick = { controller.rest() },
                )
            }
        }
    }
}
