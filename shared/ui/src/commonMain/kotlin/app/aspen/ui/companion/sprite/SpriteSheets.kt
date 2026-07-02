package app.aspen.ui.companion.sprite

import app.aspen.domain.companion.model.CompanionSpecies

/**
 * The v1 lineup (approved 2026-07-03): the Aspen sprite (default — an original warm-toned pixel
 * critter with a small floating heart and a leaf sprout; an homage to terminal-pet mascots, NOT
 * anyone's trade dress), a sage cat, and a sand bunny. All 12×12, soft non-human forms, calm
 * palette only (docs/05 §5). Colours are integrity-tested against [app.aspen.core.color.Argb.isAlarmRed].
 *
 * Palette chars: 'b' body · 'B' body shade · 'e' eye · 'x' accent (leaf / inner ear) ·
 * 'c' cheek · 'h' heart.
 */
object SpriteSheets {

    // Calm sprite tones (0xAARRGGBB). Warm apricot + existing Sage/Sand/Text tokens; the dusty
    // rose heart is deliberately far from alarm red (r≥200 ∧ g≤90 ∧ b≤90 fails it).
    private const val EYE = 0xFF22251F // TextPrimary
    private const val APRICOT = 0xFFDFA46C
    private const val AMBER_SHADE = 0xFFB9772A // Caution amber
    private const val LEAF = 0xFF5F7E57 // Sage500
    private const val HEART = 0xFFCF938F
    private const val SAGE_BODY = 0xFFA7C09C // Sage300
    private const val SAGE_SHADE = 0xFF5F7E57 // Sage500
    private const val SAND_BODY = 0xFFE9D9BE // Sand300
    private const val SAND_SHADE = 0xFFCBB58A // Sand500

    fun sheetFor(species: CompanionSpecies): SpriteSheet = when (species) {
        CompanionSpecies.ASPEN_SPRITE -> aspenSprite
        CompanionSpecies.CAT -> cat
        CompanionSpecies.BUNNY -> bunny
    }

    // ── The Aspen sprite ─────────────────────────────────────────────────────────────────────

    private val aspenRest = PixelFrame(
        listOf(
            "....h.......",
            "............",
            ".....x......",
            "....xx......",
            "...bbbbbb...",
            "..bbbbbbbb..",
            "..bebbbbeb..",
            "..bbbbbbbb..",
            "..bcbbbbcb..",
            "...bbbbbb...",
            "..bb....bb..",
            "............",
        ),
    )

    private val aspenBreathe = PixelFrame(
        listOf(
            "............",
            "....h.......",
            ".....x......",
            "....xx......",
            "...bbbbbb...",
            "..bbbbbbbb..",
            "..bebbbbeb..",
            "..bbbbbbbb..",
            "..bcbbbbcb..",
            "...bbbbbb...",
            "..bb....bb..",
            "............",
        ),
    )

    private val aspenBlink = PixelFrame(
        listOf(
            "....h.......",
            "............",
            ".....x......",
            "....xx......",
            "...bbbbbb...",
            "..bbbbbbbb..",
            "..bBbbbbBb..",
            "..bbbbbbbb..",
            "..bcbbbbcb..",
            "...bbbbbb...",
            "..bb....bb..",
            "............",
        ),
    )

    private val aspenLeanLeft = PixelFrame(
        listOf(
            "...h........",
            "............",
            "....x.......",
            "...xx.......",
            "..bbbbbb....",
            ".bbbbbbbb...",
            ".bebbbbeb...",
            ".bbbbbbbb...",
            ".bcbbbbcb...",
            "..bbbbbb....",
            ".bb....bb...",
            "............",
        ),
    )

    private val aspenHop = PixelFrame(
        listOf(
            "....h.......",
            ".....x......",
            "....xx......",
            "...bbbbbb...",
            "..bbbbbbbb..",
            "..bebbbbeb..",
            "..bbbbbbbb..",
            "..bcbbbbcb..",
            "...bbbbbb...",
            "..bb....bb..",
            "............",
            "............",
        ),
    )

    private val aspenLeanRight = PixelFrame(
        listOf(
            "........h...",
            "............",
            ".......x....",
            ".......xx...",
            "....bbbbbb..",
            "...bbbbbbbb.",
            "...bebbbbeb.",
            "...bbbbbbbb.",
            "...bcbbbbcb.",
            "....bbbbbb..",
            "...bb....bb.",
            "............",
        ),
    )

    private val aspenSprite = SpriteSheet(
        palette = mapOf('b' to APRICOT, 'B' to AMBER_SHADE, 'e' to EYE, 'x' to LEAF, 'c' to SAND_BODY, 'h' to HEART),
        ambient = SpriteAnimation(listOf(aspenRest, aspenBreathe, aspenRest, aspenBlink), AMBIENT_FPS),
        playful = SpriteAnimation(listOf(aspenLeanLeft, aspenHop, aspenLeanRight, aspenHop), PLAYFUL_FPS),
        gentle = SpriteAnimation(listOf(aspenRest, aspenBreathe), GENTLE_FPS),
    )

    // ── The cat ──────────────────────────────────────────────────────────────────────────────

    private val catRest = PixelFrame(
        listOf(
            "............",
            "..b......b..",
            "..bb....bb..",
            "..bxbbbbxb..",
            "..bbbbbbbb..",
            "..bebbbbeb..",
            "..bbbBBbbb..",
            "..bcbbbbcb..",
            "...bbbbbb...",
            "..bb....bbB.",
            "..........B.",
            "............",
        ),
    )

    private val catBlink = PixelFrame(
        listOf(
            "............",
            "..b......b..",
            "..bb....bb..",
            "..bxbbbbxb..",
            "..bbbbbbbb..",
            "..bBbbbbBb..",
            "..bbbBBbbb..",
            "..bcbbbbcb..",
            "...bbbbbb...",
            "..bb....bbB.",
            "...........B",
            "............",
        ),
    )

    private val catLeanLeft = PixelFrame(
        listOf(
            "............",
            ".b......b...",
            ".bb....bb...",
            ".bxbbbbxb...",
            ".bbbbbbbb...",
            ".bebbbbeb...",
            ".bbbBBbbb...",
            ".bcbbbbcb...",
            "..bbbbbb....",
            ".bb....bbB..",
            ".........B..",
            "............",
        ),
    )

    private val catPounce = PixelFrame(
        listOf(
            "..b......b..",
            "..bb....bb..",
            "..bxbbbbxb..",
            "..bbbbbbbb..",
            "..bebbbbeb..",
            "..bbbBBbbb..",
            "..bcbbbbcb..",
            "...bbbbbb...",
            "..bb....bbB.",
            "..........B.",
            "............",
            "............",
        ),
    )

    private val catLeanRight = PixelFrame(
        listOf(
            "............",
            "...b......b.",
            "...bb....bb.",
            "...bxbbbbxb.",
            "...bbbbbbbb.",
            "...bebbbbeb.",
            "...bbbBBbbb.",
            "...bcbbbbcb.",
            "....bbbbbb..",
            "...bb....bbB",
            "...........B",
            "............",
        ),
    )

    private val cat = SpriteSheet(
        palette = mapOf('b' to SAGE_BODY, 'B' to SAGE_SHADE, 'e' to EYE, 'x' to SAND_BODY, 'c' to SAND_SHADE),
        ambient = SpriteAnimation(listOf(catRest, catBlink), AMBIENT_FPS),
        playful = SpriteAnimation(listOf(catLeanLeft, catPounce, catLeanRight, catPounce), PLAYFUL_FPS),
        gentle = SpriteAnimation(listOf(catRest, catBlink), GENTLE_FPS),
    )

    // ── The bunny ────────────────────────────────────────────────────────────────────────────

    private val bunnyRest = PixelFrame(
        listOf(
            "...b....b...",
            "...bx..bx...",
            "...bx..bx...",
            "...bbbbbb...",
            "..bbbbbbbb..",
            "..bebbbbeb..",
            "..bbbBBbbb..",
            "..bcbbbbcb..",
            "...bbbbbb...",
            "..bb....bb..",
            "............",
            "............",
        ),
    )

    private val bunnyBlink = PixelFrame(
        listOf(
            "...b....b...",
            "...bx..bx...",
            "...bx..bx...",
            "...bbbbbb...",
            "..bbbbbbbb..",
            "..bBbbbbBb..",
            "..bbbBBbbb..",
            "..bcbbbbcb..",
            "...bbbbbb...",
            "..bb....bb..",
            "............",
            "............",
        ),
    )

    private val bunnyLeanLeft = PixelFrame(
        listOf(
            "..b....b....",
            "..bx..bx....",
            "..bx..bx....",
            "..bbbbbb....",
            ".bbbbbbbb...",
            ".bebbbbeb...",
            ".bbbBBbbb...",
            ".bcbbbbcb...",
            "..bbbbbb....",
            ".bb....bb...",
            "............",
            "............",
        ),
    )

    private val bunnyHop = PixelFrame(
        listOf(
            "...b....b...",
            "...bx..bx...",
            "...bx..bx...",
            "...bbbbbb...",
            "..bbbbbbbb..",
            "..bebbbbeb..",
            "..bbbBBbbb..",
            "..bcbbbbcb..",
            "...bbbbbb...",
            "............",
            "............",
            "............",
        ),
    )

    private val bunnyLeanRight = PixelFrame(
        listOf(
            "....b....b..",
            "....bx..bx..",
            "....bx..bx..",
            "....bbbbbb..",
            "...bbbbbbbb.",
            "...bebbbbeb.",
            "...bbbBBbbb.",
            "...bcbbbbcb.",
            "....bbbbbb..",
            "...bb....bb.",
            "............",
            "............",
        ),
    )

    private val bunny = SpriteSheet(
        palette = mapOf('b' to SAND_BODY, 'B' to SAND_SHADE, 'e' to EYE, 'x' to HEART, 'c' to HEART),
        ambient = SpriteAnimation(listOf(bunnyRest, bunnyBlink), AMBIENT_FPS),
        playful = SpriteAnimation(listOf(bunnyLeanLeft, bunnyHop, bunnyLeanRight, bunnyHop), PLAYFUL_FPS),
        gentle = SpriteAnimation(listOf(bunnyRest, bunnyBlink), GENTLE_FPS),
    )
}
