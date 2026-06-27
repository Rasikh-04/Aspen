package app.aspen.core.color

/**
 * Raw token colour values as 0xAARRGGBB longs (docs/06 §2). Soft, warm, calm — NO pure red
 * anywhere, including the crisis tone. The Compose layer ([:shared:core-design]) wraps these
 * into Color/Theme; the values live here so contrast and the no-alarm-red rule are unit-tested.
 */
object Palette {
    // Sage — primary, calm green family
    const val Sage50: Long = 0xFFF1F5EF
    const val Sage100: Long = 0xFFE2EADD
    const val Sage300: Long = 0xFFA7C09C
    const val Sage500: Long = 0xFF5F7E57
    const val Sage700: Long = 0xFF35502F

    // Sand — warm secondary
    const val Sand50: Long = 0xFFFBF7F0
    const val Sand300: Long = 0xFFE9D9BE
    const val Sand500: Long = 0xFFCBB58A

    // Surfaces / background
    const val WarmWhite: Long = 0xFFFCFAF6
    const val WarmOffWhite: Long = 0xFFF4F0E8
    const val Border: Long = 0xFFE2E0D6

    // Text
    const val TextPrimary: Long = 0xFF22251F
    const val TextSecondary: Long = 0xFF49503F
    const val TextMuted: Long = 0xFF6F7565
    const val TextInverse: Long = 0xFFFCFAF6

    // Caution — soft amber, used INSTEAD of error-red (docs/06 §2). Paired with text/icon, never colour-only.
    const val Caution: Long = 0xFFB9772A
    const val CautionBg: Long = 0xFFFBF1DF

    // Crisis — calm, serious, NON-alarming (a deep slate-teal), never bright red (docs/06 §6.1).
    const val Crisis: Long = 0xFF38525A
    const val CrisisBg: Long = 0xFFEDF1F1
}
