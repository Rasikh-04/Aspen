package app.aspen.ui.companion

import app.aspen.core.color.Argb
import app.aspen.domain.companion.model.CompanionSpecies
import app.aspen.ui.companion.sprite.MAX_SPRITE_FPS
import app.aspen.ui.companion.sprite.SpriteSheets
import app.aspen.ui.companion.sprite.TRANSPARENT_PIXEL
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The sprites are data, so their guardrails are testable: uniform grids (stable layout), every
 * pixel resolvable, frame rates inside the calm budget (docs/04 §6), and no colour anywhere near
 * alarm red (CLAUDE.md #5) for every species and state.
 */
class SpriteSheetIntegrityTest {

    @Test
    fun everySpeciesHasUniformFrameGrids() {
        for (species in CompanionSpecies.entries) {
            val sheet = SpriteSheets.sheetFor(species)
            val width = sheet.still.width
            val height = sheet.still.height
            assertTrue(width > 0 && height > 0, "$species must have a drawable still frame")
            for (animation in sheet.animations) {
                assertTrue(animation.frames.isNotEmpty(), "$species has an empty animation")
                for (frame in animation.frames) {
                    assertTrue(frame.rows.all { it.length == width }, "$species has ragged rows")
                    assertTrue(frame.height == height && frame.width == width, "$species frames differ in size")
                }
            }
        }
    }

    @Test
    fun everyDrawnPixelResolvesInThePalette() {
        for (species in CompanionSpecies.entries) {
            val sheet = SpriteSheets.sheetFor(species)
            for (animation in sheet.animations) for (frame in animation.frames) for (row in frame.rows) {
                for (char in row) {
                    if (char != TRANSPARENT_PIXEL) {
                        assertTrue(char in sheet.palette, "$species uses unknown palette char '$char'")
                    }
                }
            }
        }
    }

    @Test
    fun frameRatesStayInsideTheCalmBudget() {
        for (species in CompanionSpecies.entries) {
            for (animation in SpriteSheets.sheetFor(species).animations) {
                assertTrue(animation.fps in 1..MAX_SPRITE_FPS, "$species animates too fast (${animation.fps} fps)")
            }
        }
    }

    @Test
    fun noSpriteColourIsAlarmRed() {
        for (species in CompanionSpecies.entries) {
            for ((char, argb) in SpriteSheets.sheetFor(species).palette) {
                assertFalse(Argb.isAlarmRed(argb), "$species palette '$char' is alarm red (CLAUDE.md #5)")
            }
        }
    }

    @Test
    fun everySpeciesActuallyLooksDifferent() {
        val stills = CompanionSpecies.entries.map { SpriteSheets.sheetFor(it).still.rows }
        assertTrue(stills.toSet().size == stills.size, "species must be visually distinct")
    }
}
