package app.aspen.core.color

import kotlin.test.Test
import kotlin.test.assertTrue

class ContrastTest {

    private val AA_NORMAL = 4.5
    private val AA_LARGE = 3.0

    private fun assertContrast(fg: Long, bg: Long, min: Double, name: String) {
        val ratio = Argb.contrastRatio(fg, bg)
        assertTrue(ratio >= min, "$name contrast $ratio is below $min")
    }

    @Test
    fun primaryTextMeetsAaOnSurfaces() {
        assertContrast(Palette.TextPrimary, Palette.WarmWhite, AA_NORMAL, "text/primary on warm-white")
        assertContrast(Palette.TextPrimary, Palette.WarmOffWhite, AA_NORMAL, "text/primary on warm-offwhite")
        assertContrast(Palette.TextSecondary, Palette.WarmWhite, AA_NORMAL, "text/secondary on warm-white")
    }

    @Test
    fun cautionAndCrisisSurfacesAreReadable() {
        assertContrast(Palette.TextPrimary, Palette.CautionBg, AA_NORMAL, "text on caution bg")
        assertContrast(Palette.TextPrimary, Palette.CrisisBg, AA_NORMAL, "text on crisis bg")
    }

    @Test
    fun inverseTextReadsOnPrimary() {
        assertContrast(Palette.TextInverse, Palette.Sage700, AA_NORMAL, "inverse text on sage/700")
    }

    @Test
    fun mutedTextMeetsAtLeastLargeText() {
        assertContrast(Palette.TextMuted, Palette.WarmWhite, AA_LARGE, "text/muted on warm-white")
    }

    @Test
    fun noTokenIsAlarmRed() {
        // CLAUDE.md #5 / docs/06 §2: no pure/alarm red anywhere — including caution and crisis.
        val tokens = listOf(
            Palette.Sage500, Palette.Sage700, Palette.Sand500,
            Palette.Caution, Palette.CautionBg, Palette.Crisis, Palette.CrisisBg,
        )
        for (t in tokens) {
            assertTrue(!Argb.isAlarmRed(t), "token ${t.toString(16)} reads as alarm red")
        }
    }
}
