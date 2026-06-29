# Aspen — STATUS

_Resume-cold notes. Update at the end of every working session (CLAUDE.md)._

**Phase:** 3 — Onboarding + Grounding + Reflection/Logging (`docs/07`, `docs/13`). Two-dev split active: **Dev A (lead)** owns `:domain` profile/logic + logging-suppression rules + encrypted-store integration; **Dev B** owns the feature UI. Phase 2 spine remains built; **crisis-freshness gate still RED by design** until advisors verify content.

---

## Done (Phase 3 — Dev A: onboarding profile domain) — branch `feat/onboarding-profile-domain`

The pure-`:domain` contract the questionnaire UI (Dev B) binds to. No UI, no persistence yet (kept small per `docs/13` §5); encrypted profile store is the next Dev-A branch.

### Onboarding subsystem (`:shared:domain/onboarding`, pure Kotlin)
- **Models** (`onboarding/model/`): `SupportProfile` (6 internal profiles, never user-visible — CLAUDE.md #9) +
  `ProtectiveFlag` (`SUPPRESS_FOOD_LOGGING`, `NO_BODY_IMAGE_FRAMING`); `Questionnaire.kt` (typed, **numberless**,
  **no user-facing strings** — IDs/option tokens only; copy stays in `:ui`); `OnboardingResult`/`RoutingHints`;
  `AppConfig` (`FoodLoggingMode` OFF/REFRAMED/AVAILABLE, `CompanionTone`, `ToolEmphasis`, `SupportRoutingStrength`,
  `bodyImageFramingAllowed`) + `ProfileMappingProvenance`.
- **`OnboardingScoring.deriveProfile()`** — heuristic tally (docs/11 §4): conservative bias (any restriction/avoidance
  signal raises `SUPPRESS_FOOD_LOGGING`), Q6 `YES` raises `NO_BODY_IMAGE_FRAMING` + zeroes body-image weight,
  ties/low-signal/skip-all → `MIXED_OR_UNSURE`. Never throws, never returns an empty profile map.
- **`ProfileBehaviourMap.deriveConfig()`** — full nuanced mapping (per decision: **full mapping now, advisor flag is
  metadata**). `SUPPRESS_FOOD_LOGGING` forces `FoodLoggingMode.OFF` regardless of dominant profile; restriction/
  avoidance never get `AVAILABLE`. Total over all profiles.
- `DomainModule.PHASE = 3`.

### ⚠ Profile→behaviour mapping is PROVISIONAL (advisor gate open — `docs/07` Phase 3 `[APPROVE]`)
- The mapping (esp. **logging-suppression-per-disorder**) needs ED-informed advisor sign-off before *enabling*
  (`docs/01` §5a, `docs/11` §6). Carried as `ProfileMappingProvenance.PROVISIONAL` (`advisorVerified = false`,
  `revision = "draft-2026-06-29"`) so a Phase-7 release gate can refuse an unverified mapping — same "build the
  mechanism, mark it provisional" pattern as the crisis registry. Question set + mapping are a clinical-review item.

### Verified locally (Dev A / Linux — no Xcode)
- ✅ `:shared:domain:jvmTest` — 17 onboarding tests (scoring conservative bias, ARFID down-weight, ties→mixed,
  skip-all→mixed, routing; mapping suppression invariants, totality, provisional provenance) + existing suites green.
- ✅ `copyLint` — passes (domain adds no user-facing strings).
- ✅ `:shared:domain:compileCommonMainKotlinMetadata` — common compiles for all targets; iOS target configures
  (Kotlin/Native iOS link stays a `macos-14` CI concern, per Phase 1/2 notes).

### Open / next (Dev A, Phase 3 — after the local-store branch below)
- [ ] Advisor sign-off on the question set + profile→behaviour mapping → flip `advisorVerified`.
- [ ] Review Dev B's questionnaire/grounding/reflection/logging UI PRs (wire to `deriveProfile`/`deriveConfig`/`LoggingService`).
- [ ] Durable on-disk `EncryptedBlobStore` (platform path/Context); iOS Keychain+CryptoKit `LocalCipher` actual.
- [ ] PR + CI when git workflow §6 is enabled (repo still private; no PR yet per current instruction).

---

## Done (Phase 3 — Dev A: encrypted local store) — branch `feat/phase3-encrypted-local-store` (stacked on onboarding domain)

The on-device encrypted store under the profile + logging features, plus **data-layer enforcement** of the
food-logging suppression rules. Independent of Dev B (B's screens consume these ports/services).

### Unified local crypto (`:shared:data/local`)
- **`LocalCipher` seam** (`expect fun platformLocalCipher()`) — the single key-backed AES/GCM crypto for **all**
  on-device data. Actuals: JVM AES-256/GCM (process-ephemeral, test grade), Android AES/GCM via AndroidKeyStore
  (`aspen.local.v1`), iOS passthrough **placeholder**. **Consent now delegates to this** (its `ConsentCipher`
  actuals are thin adapters) — removed the duplicated per-store cryptography; one audited implementation.
- **`EncryptedBlobStore`** (generic) + `InMemoryEncryptedBlobStore` default, with `clear()` for hard-delete.

### Profile persistence (`:shared:data/onboarding`, `:shared:domain/onboarding`)
- **`ProfileStore`** port (domain) + **`PersistentProfileStore`** (data) — JSON DTO → encrypted blob; **fail-safe**
  (missing/corrupt/undecryptable → `null` → safest default). Re-runnable/editable (`save` overwrites; `clear` resets).
- **`AppConfigProvider`** — single read-path for adaptivity; **safe by default** (no profile → `MIXED_OR_UNSURE` →
  food logging OFF). Adaptivity only opens up from the safest baseline once a profile exists.

### Numberless logging store + suppression enforcement (`:shared:domain/logging`, `:shared:data/logging`)
- **Entities** `Reflection`/`FoodLog`/`BehaviourLog` + `FeelingTag` — **structurally numberless** (no numeric
  fields exist; SR-1) and string-free (UI localizes tags).
- **`LoggingStore`** port + **`PersistentLoggingStore`** — encrypted, fail-safe (corrupt → empty), **hard deletes**
  + `clearAll()` (FR-11).
- **`LoggingService`** — the **single enforcement point**: `logFood` is refused (`LogOutcome.SuppressedFoodLogging`,
  not an error/shame state) when the active `AppConfig.foodLoggingMode == OFF`; reflections + behaviour logs are
  always available (docs/03 FR-3b). Features depend on the service, never the store, so the rule can't be bypassed.
- **DI** (`localStoreModule` in `AspenModules.kt`): shared `LocalCipher`, per-store blobs, `AppConfigProvider`,
  `LoggingService`; added to `aspenSharedModules`.

### Verified locally (Dev A / Linux — no Xcode)
- ✅ `:shared:domain:jvmTest` + `:shared:data:jvmTest` — logging suppression (restriction/no-profile → suppressed,
  binge → saved, reflections/behaviour always on, delete-everything), profile round-trip/encrypted-at-rest/
  corrupt→null/clear, logging round-trip/hard-delete/clearAll/corrupt→empty, DI graph resolves (food logging
  suppressed by default). Existing safety/consent/crisis suites still green.
- ✅ `copyLint` — passes (no user-facing strings added).
- ✅ `:shared:data:compileAndroidMain` (consent-cipher delegation compiles) + `compileCommonMainKotlinMetadata`
  (iOS target configures).

---

## Done (Phase 2 — safety + consent)

### Domain spine (`:shared:domain`, pure)
- **Safety models** (`safety/model/CrisisModels.kt`): `LocaleKey` (PK/DE/UK/US/INTL, `fromCountryCode`,
  region ≠ UI language), `CrisisResource`/`CrisisResourceSet`/`Contact`/`Purpose`/`ContactMethod`.
- **`SafetyRules`** + `ForbiddenLexicon` — number/appearance/eating-advice/shame predicates, whole-word
  Unicode matching, en/de/ur.
- **`SafetyEngine`/`DefaultSafetyEngine`** — `crisis()` delegates to the resolver; `guardOutput()` is the
  documented Phase-2 stub (rule check → withhold + safe fallback, never echoes unsafe text; real wiring Phase 4).
- **Consent primitive** (`consent/`): `ConsentGrant`/`Recipient`/`DataCategory`/`ConsentEvent`, `ConsentStore`
  port (fail-safe: reads never throw), `ConsentManager`/`DefaultConsentManager` (default-deny, immediate
  revoke, expiry, full audit log; `Clock` + `newId` injected for determinism).
- `DomainModule.PHASE = 2`.

### Crisis registry + resolver (`:shared:data`)
- **Canonical JSON** `config/safety/crisis/{pk,de,uk,us,intl}.json` — anchor org NAMES from `docs/10`; **every
  contact value + `verifiedOn`/`verifiedBy` is `TODO-VERIFY`**; never NEDA.
- **In-code `CrisisRegistry`** (mirror of the JSON) + **`CrisisRegistryRepo : CrisisResolver`** — offline,
  never-empty, never-throws; PK/DE/UK enabled; **US present-but-disabled → INTL fallback**; unknown → INTL.
- Tests: 10 resolver/registry tests (every-locale non-empty, region-correct, INTL fallback, NEDA-absent) +
  JVM parity test pinning in-code registry to the JSON.

### Release gates (`buildSrc`, wired into every module's `check`)
- **`CrisisRegistryLint` + `CrisisGateTask`** (`crisisGate`): **NEDA-deny** (any locale) + **SR-2 freshness**
  (launch locales PK/DE/UK only; INTL fallback not gated; US gated only once enabled). 9 unit tests prove
  both fire. **Two gates now:** dev **`crisisGate`** (wired into `check`, also CI) ACCEPTS provisional
  launch content so dev isn't halted; release **`crisisGateStrict`** REJECTS provisional content and is
  the ship gate (`docs/PRE_SHIP_VERIFICATION.md`). `TODO-VERIFY` (no marker) still fails both.

### ⚠ Crisis content is PROVISIONAL, not advisor-verified (deliberate dev-unblock)
- Launch locales **PK/DE/UK** are marked `verifiedBy = "PROVISIONAL-UNVERIFIED"`, `verifiedOn = 2026-06-28`
  in both the JSON and `CrisisRegistry` (parity-pinned). **All contact VALUES remain `TODO-VERIFY`** — so the
  UI renders them **non-actionable** ("Details are being verified"); **no unverified number can be dialled**, and
  no fake crisis numbers were invented. US/INTL stay `TODO-VERIFY`.
- This makes `./gradlew check` / dev `crisisGate` GREEN while keeping the real verification mandatory:
  **`./gradlew crisisGateStrict` is RED** until advisors verify (real `verifiedBy`/dates + real contacts).
- **Every item to clear before ship is tracked in `docs/PRE_SHIP_VERIFICATION.md`** (the "several verifications
  before shipment" gate). CI `crisisGate` stays informational (`continue-on-error`).

### Consent persistence (`:shared:data`)
- **Crypto seam** `expect fun platformConsentCipher()` → actuals: **JVM** AES-256/GCM (ephemeral key, test-grade),
  **Android** AES/GCM via **AndroidKeyStore**, **iOS** passthrough **placeholder** (see leftouts).
- **`PersistentConsentStore`** — JSON DTOs → encrypted blob via `ConsentBlobStore`; **fail-safe**: missing/
  corrupt/undecryptable → empty → default-deny. `InMemoryConsentBlobStore` is the Phase-2 default + test double.
- 6 persistence tests (encrypted-not-plaintext, round-trip, corrupted→deny, expiry-across-reload, manager-over-real-store).

### DI (`:shared:data`, Koin 4.1)
- `safetyModule` + `consentModule` + `aspenSharedModules`; **manual platform-init guide is in
  `AspenModules.kt`** (Android `Application.startKoin`, iOS `startKoin` from `MainViewController`, app supplies
  localized `SafetyFallbackCopy`). 3 graph-resolution tests.

### Flow C UI (`:shared:ui`)
- **`SafetyScreen`** — calm (soft amber `crisis`/`crisisBg`, never red), grouped (acute / ED-support / finder),
  explicit **region picker** (PK/DE/UK/INTL, independent of UI language), always-present **trusted-person row**
  (≤2 taps, CLAUDE.md #6), **unverified contacts shown but NON-actionable** (can't dial `TODO-VERIFY`).
- Wired via `AspenApp(crisisResolver)` → `AppScaffold`; **live on Android** (`MainActivity` passes
  `CrisisRegistryRepo()`); iOS keeps the placeholder until its entry is wired (leftout).
- New localized strings in `values/` + `values-ur/` (ur = English placeholders, flagged for ED-informed review).

## Verified locally (JVM + Android compile; this Linux host has no Xcode)
- ✅ `:shared:domain:jvmTest`, `:shared:data:jvmTest` — all safety/consent/crisis/DI tests green.
- ✅ `:buildSrc:test` — copy-lint + crisis-gate logic tests green.
- ✅ `crisisGate` — **FAILS with 42 freshness findings, 0 NEDA** (intended; the backstop works).
- ✅ `copyLint` — passes incl. new safety strings.
- ✅ `:shared:data:compileAndroidMain` (Keystore actual), `:shared:ui:compileAndroidMain`,
  `:androidApp:compileDebugKotlin` — Android compiles end-to-end.

## ⚠ Deviations & leftouts (Phase 2 — explicit, per CLAUDE.md)
- **DEVIATION (registry mechanism):** `docs/09`/decision-#4 specified "bundled JSON + `expect/actual` resource
  reader". Implemented instead as **in-code Kotlin registry mirrored from canonical JSON + parity test** (same
  single-source pattern approved for the token lexicon). Reason: makes never-empty/never-throws/offline a
  compile-time guarantee on every platform; advisors still edit JSON; the gate still reads JSON. Switchable if undesired.
- **ALL crisis content is `TODO-VERIFY`** (numbers, hours, `verifiedOn/By`). Advisor + ED-informed native-language
  verification is required before any launch locale ships; the `crisisGate` enforces this (red until done).
- **iOS consent cipher is a passthrough placeholder** (compiles, NOT secure, NOT device-verified). Must be replaced
  with a Keychain + CryptoKit AES-GCM actual before any iOS release. Aspen is Android-first; no consent data persists on iOS in Phase 2.
- **Android Keystore + JVM ciphers are not device/hardware-verified** from this host — JVM logic is unit-tested; on-device verification is a CI/device task.
- **`ConsentBlobStore` is in-memory only** (not durable on disk) in Phase 2 — durable platform file/Keychain-backed persistence is a later task.
- **Koin is not yet started at the platform entries**; Flow C on Android currently constructs `CrisisRegistryRepo()` directly in `MainActivity`. Wiring Koin (and `SafetyFallbackCopy`, trusted-contact capture, contact dialing via `UriHandler`) is next.
- **iOS not compiled here** (no Xcode on Linux) — iOS targets are the authoritative CI (`macos-14`) check.
- **Urdu safety strings are English placeholders** — ED-informed native review required before ship (`docs/12` §3).

## Open / next
- [ ] Advisors/T&S: verify crisis content per launch locale (turn `crisisGate` green); ED-informed Urdu review of safety copy.
- [ ] Start Koin at platform entries; bind localized `SafetyFallbackCopy`; wire trusted-contact capture + contact dialing.
- [ ] iOS Keychain/CryptoKit `ConsentCipher` actual; durable `ConsentBlobStore`.
- [ ] Push & confirm CI green on both jobs (incl. iOS link on `macos-14`).
- [ ] Compose UI smoke/RTL test for `SafetyScreen` (Robolectric/host).

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
  JVM tests + `assembleDebug` + android lint); **GitHub-hosted** macOS job (`runs-on: macos-14`,
  `linkDebugFrameworkIosSimulatorArm64`). No self-hosted runner to register.

### Fixes applied this session (first real local build on the dev's machine)
- **AGP 9 built-in Kotlin:** removed `org.jetbrains.kotlin.android` from `:androidApp` (and the root
  `apply false`). AGP 9.1.1 hard-errors if that plugin is applied; `com.android.application` now
  provides Kotlin. The `kotlin { compilerOptions { jvmTarget } }` block still applies.
- **Dropped `iosX64`** from all four shared modules. Compose MP 1.11.0 publishes no `iosX64` (Intel
  simulator) artifacts → `KMP Dependencies Resolution Failure`. Targets are now `iosArm64` (devices)
  + `iosSimulatorArm64` (simulators/CI), which is the modern CMP standard.
- **CI runner:** switched the iOS job from self-hosted to GitHub-hosted `macos-14`. `docs/CI_RUNNER_SETUP.md`
  rewritten accordingly (the self-hosted attempt failed with `Exec format error` — a macOS runner
  binary can't run on the dev's Linux box).

## Verified locally (dev's machine, clean network)
- ✅ `./gradlew -p buildSrc test` — copy-lint logic tests pass.
- ✅ `./gradlew copyLint` — "no forbidden tokens across 3 string files".
- ✅ `./gradlew :shared:core-common:jvmTest` — locale/RTL + contrast/no-alarm-red.
- ✅ `./gradlew :androidApp:assembleDebug` — **APK builds → Calm Home.**
- Expected non-error warnings on Linux: `iosSimulatorArm64Test`/`iosArm64` tests "disabled — simulator
  tests require macOS". Correct; that's what the `macos-14` CI job covers.
- Fonts are real binaries with OFL licenses bundled.

## Open / next
- [ ] **Push and confirm first CI run green on both jobs** (the authoritative iOS DoD check — the
      Kotlin/Native iOS link can only run on the `macos-14` job, not on Linux). Nothing to register.
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
