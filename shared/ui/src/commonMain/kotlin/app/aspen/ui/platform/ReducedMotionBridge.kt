package app.aspen.ui.platform

/**
 * Whether the OS asks for reduced motion (SR-6; docs/03 NFR-a11y). Read at composition start and
 * fed to [app.aspen.design.AspenTheme]; every animated surface (companion included) already honours
 * `LocalReducedMotion`. Fails toward `false` (= respect the platform default) only where the OS
 * offers no signal.
 */
expect fun systemReducedMotion(): Boolean
