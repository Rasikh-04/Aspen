package app.aspen.ui.platform

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

/** iOS: the system-wide Reduce Motion accessibility setting. */
actual fun systemReducedMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
