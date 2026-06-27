# Aspen

A free, Android-first (iOS to follow) **between-session support app for people living with eating
disorders**. A calm place to get through hard moments — grounding tools, private reflection, a fast
route to real human help, and an optional companion. **Not a therapist, not the product.** Success =
closing the app feeling steadier; never engagement metrics.

> Read **[CLAUDE.md](CLAUDE.md)** first — the non-negotiables there (no food/body numbers, no
> appearance comments, no streaks/scores, always a human exit, calm-never-alarm) are enforced in
> code, copy, and review. `docs/` is the source of truth for *what* we build.

## Stack
Kotlin Multiplatform + Compose Multiplatform. ~90% of the app lives in `:shared`. iOS compiles in CI
from day one. See `docs/04`.

| Module | Role |
|---|---|
| `:shared:core-common` | Pure logic: locale/RTL resolution, WCAG contrast + no-alarm-red, tokens, logging seam |
| `:shared:core-design` | `AspenTheme`, design tokens, bundled OFL fonts (Material3 substrate) |
| `:shared:domain` | Clean-architecture core (Phase 2: safety + consent) |
| `:shared:ui` | Navigation shell, Calm Home, locale provider, iOS entry; builds `Shared.framework` |
| `:androidApp` | Thin Android host |
| `:iosApp` | Thin SwiftUI host |
| `buildSrc` | **Copy-lint** safety gate (forbidden number/shame/appearance tokens, per language) |

## Build & verify
```bash
./gradlew copyLint                                   # forbidden-token gate (per language)
./gradlew -p buildSrc test                           # copy-lint logic tests
./gradlew :shared:core-common:jvmTest                # locale / contrast / no-alarm-red tests
./gradlew :androidApp:assembleDebug                  # Android app -> Calm Home
./gradlew :shared:ui:linkDebugFrameworkIosSimulatorArm64   # iOS framework (on macOS)
./gradlew check                                      # everything, incl. copyLint
```
CI runs all of the above (`.github/workflows/ci.yml`). See `docs/STATUS.md` to resume cold and
`docs/CI_RUNNER_SETUP.md` to register the macOS runner.

## Current phase
**Phase 1 — skeleton & design system.** Next: Phase 2 safety subsystem + consent primitive (`docs/09`).
