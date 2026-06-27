# Aspen — STATUS

_Resume-cold notes. Update at the end of every working session (CLAUDE.md)._

**Phase:** 1 — Skeleton & design system (KMP + CMP). Phase 2 (safety + consent, `docs/09`) is next.

---

## Done (Phase 1 skeleton)

- **Build system / toolchain** (versions as specified):
  Kotlin 2.4.0, AGP 9.1.1 (incl. `com.android.kotlin.multiplatform.library`), Compose MP 1.11.0,
  Gradle wrapper **9.4.1**, compileSdk/targetSdk 36, minSdk 24, JVM target 17.
  Version catalog in `gradle/libs.versions.toml`.
- **Modules** (`settings.gradle.kts`):
  - `:shared:core-common` — pure Kotlin: `LocaleResolver` + `SupportedLanguage` (7 langs, RTL flag,
    language≠region), `Argb` WCAG contrast + `isAlarmRed`, `Palette` raw tokens, `AspenLog` seam.
    JVM-tested.
  - `:shared:core-design` — `AspenTheme` (Material3 substrate + custom tokens), colours/spacing/
    shapes/motion, `LocalReducedMotion`, bundled OFL fonts (Fraunces / Plus Jakarta Sans / Noto
    Nastaliq Urdu). "error" role mapped to soft amber — never red.
  - `:shared:domain` — clean-architecture placeholder (depends only on core-common). Phase 2 fills
    `safety/` + `consent/` here.
  - `:shared:ui` — nav shell (Home/Reflect/Calm/Settings + non-tab Safety), **Calm Home** (empty,
    no metrics, hard-moment entry + persistent ≤2-tap route to a person), `LocaleProvider`
    (drives RTL from locale), platform `expect/actual` locale bridge, iOS `MainViewController`,
    strings `en` + `ur` (RTL stub), produces `Shared.framework`.
  - `:androidApp` — thin host Activity → `AspenApp()`, RTL-enabled manifest.
  - `:iosApp` — SwiftUI host wrapping the shared Compose UI (xcodegen `project.yml`; Xcode wiring
    deferred to Mac).
- **Copy-lint safety gate** — `buildSrc` plugin (`CopyLint`, `CopyLintTask`), per-language forbidden
  number/shame/appearance tokens, whole-word Unicode matching, allow-list at
  `config/copylint/allowlist.txt`. Wired so every module's `check` depends on `copyLint`. Unit-tested.
- **CI** — `.github/workflows/ci.yml`: Linux job (copy-lint unit tests + `copyLint` gate + shared
  JVM tests + `assembleDebug` + android lint); self-hosted macOS job (`linkDebugFrameworkIosSimulatorArm64`).

## Verified locally
- All declared versions resolve (Kotlin/AGP/KMP-plugin/CMP/nav — confirmed 200 on repos).
- Fonts are real binaries with OFL licenses bundled.
- _Build green:_ see "Open / next" — local Gradle in the dev sandbox runs very slowly and could not
  be relied on for a full green; **CI is the gate**. Pure-logic (`core-common`, `domain`, copy-lint)
  validation was run locally.

## Open / next
- [ ] Confirm first **CI run green** on both jobs (the authoritative Phase-1 DoD check). Register the
      self-hosted macOS runner first — see `docs/CI_RUNNER_SETUP.md`.
- [ ] iOS: generate the Xcode project from `iosApp/project.yml` on a Mac and embed `Shared.framework`
      (port-and-polish; the Kotlin iOS target already compiles+links in CI).
- [ ] Urdu strings are placeholders (one real word) — native, ED-informed translation pending
      (`docs/12 §3`). Same for de/ur copy-lint token lists (starter only).
- [ ] Re-enable `org.gradle.configuration-cache` after a CC-compat pass on `CopyLintTask`.
- [ ] Optional: wire ktlint (declared in catalog, not yet applied) and add a Compose UI smoke/RTL
      test (Robolectric host test) — RTL *logic* is already covered in `core-common`.

## Not started (correctly — later phases)
- Crisis registry **content** (advisor-verified; NEDA-deny + crisis-freshness gate) — Phase 2.
- Safety engine, AI output guard, consent primitive — Phase 2 (`docs/09`).
