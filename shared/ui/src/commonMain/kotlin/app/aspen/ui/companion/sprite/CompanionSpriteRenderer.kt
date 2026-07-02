package app.aspen.ui.companion.sprite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.aspen.design.LocalReducedMotion
import app.aspen.domain.companion.CompanionState
import app.aspen.domain.companion.model.CompanionSpecies
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Default on-screen size of one sprite pixel (12×12 grid → 48dp companion). */
private val DEFAULT_PIXEL = 4.dp

/**
 * Draws the companion for [state] with a tiny frame ticker (docs/04 §6: frames advance only while
 * this is composed and visible; ambient is 2 fps, playful 6 fps, nothing runs when the state is
 * hidden/suspended because the caller doesn't compose this at all). Under reduced motion the sprite
 * is a single still frame — the coroutine is never launched (SR-6).
 */
@Composable
fun CompanionSprite(
    state: CompanionState,
    species: CompanionSpecies,
    modifier: Modifier = Modifier,
    pixel: Dp = DEFAULT_PIXEL,
) {
    val sheet = remember(species) { SpriteSheets.sheetFor(species) }
    val reducedMotion = LocalReducedMotion.current

    val animation = when (state) {
        is CompanionState.Ambient -> sheet.ambient
        is CompanionState.Playful -> sheet.playful
        is CompanionState.GentlePresence -> sheet.gentle
        else -> null // Hidden/Suspended hosts don't compose this; belt-and-braces: draw still.
    }

    var frameIndex by remember(animation) { mutableStateOf(0) }
    if (!reducedMotion && animation != null && animation.frames.size > 1) {
        LaunchedEffect(animation) {
            while (isActive) {
                delay(1000L / animation.fps)
                frameIndex += 1
            }
        }
    }

    val frame = if (reducedMotion || animation == null) {
        sheet.still
    } else {
        animation.frames[frameIndex % animation.frames.size]
    }

    Canvas(modifier.size(pixel * frame.width, pixel * frame.height)) {
        val cell = pixel.toPx()
        frame.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, char ->
                if (char != TRANSPARENT_PIXEL) {
                    val argb = sheet.palette[char] ?: return@forEachIndexed
                    drawRect(
                        color = Color(argb),
                        topLeft = Offset(x * cell, y * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
    }
}
