package app.aspen.ui.platform

import android.animation.ValueAnimator
import android.os.Build

/**
 * Android: animator scale 0 ("remove animations" a11y setting / developer setting) = reduced
 * motion. [ValueAnimator.areAnimatorsEnabled] needs API 26; on 24–25 there is no static signal
 * without a Context, so those two levels keep motion on (the in-app default, docs/STATUS.md).
 */
actual fun systemReducedMotion(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()
