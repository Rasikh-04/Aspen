# 04 — Tech Stack & System Architecture

> The Android-first stack decision (written as an architecture decision record), the system architecture, the on-device-first data model, and the performance/battery strategy that the non-functional requirements demand. This replaces the old React-Native-+-Supabase plan entirely.

---

## 1. ADR-001 — **Kotlin Multiplatform + Compose Multiplatform** (revised recommendation)

**Status:** Recommended (pending your confirmation — you asked for suggestions before deciding).
**Context (updated by your decisions):** iOS is now a **committed multi-year goal**, not a maybe. You want **shared logic AND shared UI**, with **native-like Android performance**, and an easy path to build iOS later from the same code. That requirement changes the earlier draft's recommendation (which shared only logic, with native UI per platform). Here is the honest re-evaluation.

### Your direct question: is KMP suitable for Android (native-like perf) and easy to extend to iOS with shared code, logic, and UI?

**Short answer: yes — with one honest caveat about iOS UI maturity and one hard platform limit on the overlay companion.**

- **On Android, KMP *is* native.** Shared Kotlin compiles to ordinary JVM/Android bytecode; if you use **Compose Multiplatform (CMP)** for UI, the Android UI is literally **Jetpack Compose** — the same thing a pure-native Android app uses. So on Android there is **no performance penalty**; it's native, not "native-like." This is the key fact for your question.
- **Shared logic + shared UI is real with CMP:** teams report **~90–95% code sharing** with KMP+CMP (vs ~70–85% for React Native, which doesn't help you here anyway) [KMP docs; Sthenos 2025]. You write the screens once in Compose and they run on both platforms.
- **iOS is a genuine, stable path now.** Kotlin Multiplatform is production-stable (since Nov 2023; Google officially backs it for shared logic since I/O 2024), and **Compose Multiplatform for iOS reached *stable* in 2025**. Companies like McDonald's, Netflix, and Cash App ship KMP in production [KMP docs; kmpship 2025].

### The two honest caveats (why I'm recommending *with eyes open*, not selling it)

1. **CMP renders iOS UI via its own engine (Skia), not native UIKit widgets.** This is closer to Flutter's model than to true-native SwiftUI. Consequence: ~90% UI sharing, but **iOS gets less "automatic" native feel and native accessibility than SwiftUI would**, and the iOS side is *younger* than the rock-solid Android side. For Aspen this is **acceptable and probably ideal**, because: the app is **visually simple and calm** (text, soft cards, few screens) — exactly the kind of UI CMP shares cleanly — and the hard, deeply-native parts are isolated as platform modules anyway (next point). **But:** since accessibility (WCAG 2.2 AA) is a hard requirement and iOS CMP a11y is the historically weaker spot, **budget explicit iOS-accessibility testing on real devices**; don't assume parity for free.
2. **The overlay companion is Android-only — a platform limit, not a stack choice.** iOS does **not permit system-wide "draw over other apps" overlays** (there is no `SYSTEM_ALERT_WINDOW` equivalent; Apple forbids it). So the companion that *roams across the whole screen* **cannot exist on iOS in any framework** — Flutter, RN, KMP, or native. This is a fixed platform reality you should know now. The design response (see `05`): the companion's **in-app form is the cross-platform baseline** (shared Compose, works on both), and the **system-wide overlay is an Android-only enhancement** delivered as a platform-specific module. The questionnaire, grounding, reflection, logging, safety, and in-app companion all share; only the Android overlay layer is Android-only.

### Decision

Adopt **Kotlin Multiplatform with Compose Multiplatform** as the foundation:
- **Shared:** `:domain` (pure Kotlin), `:data` (with `expect/actual` for platform bits), and **`:ui` (Compose Multiplatform)** — questionnaire, grounding, reflection, logging, safety, settings, in-app companion.
- **Platform-specific:** `:androidApp`, `:iosApp`, and **`:companion-overlay-android`** (the system overlay — no iOS twin), plus `expect/actual` for storage keys (Android Keystore / iOS Keychain) and any deep-native integration.
- **Build Android first** (your near-term focus) but in CMP from day one, so iOS is a *port-and-polish* effort, not a rewrite. Keep the iOS target compiling in CI early even before you invest in iOS polish, so you never drift away from cross-platform-compatible code.

### Why not the alternatives (updated)

- **Native Kotlin + native SwiftUI (the earlier draft):** shares logic only, **not UI** — directly contradicts your shared-UI requirement, and means building/maintaining the iOS UI twice. Rejected *given the new requirement* (it was the right call only under "Android-only-ish v1").
- **React Native:** doesn't meet "native-like Android perf + deep overlay/encryption integration," shares less, and you're Kotlin-strong. Rejected (as before).
- **Flutter:** would also share UI and has strong perf, but means **Dart** (a second ecosystem for a Kotlin-strong team), and its overlay-on-Android story is third-party-plugin territory just like everyone else's — no advantage over KMP for your case, and it throws away your Kotlin investment. Rejected.

### Consequences

You need a **Mac for iOS builds**; the iOS UI needs explicit accessibility + native-feel testing; the talent pool is smaller than RN's (fine — small, Kotlin-strong team). In exchange you get **true-native Android performance, ~90% shared logic+UI, a stable committed iOS path, and clean isolation of the unavoidably-Android-only overlay.** This is the best fit for the requirement you actually stated.

---

## 2. ADR-002 — "Mix technologies" pragmatically, only where it pays

You asked about mixing technologies / going low-level for optimisation. The disciplined version:

| Layer | Technology | Why |
|---|---|---|
| UI | **Kotlin + Jetpack Compose** | Modern declarative UI; good animation story; native perf. |
| Domain / business logic | **Kotlin, KMP-ready module** | Pure, testable, portable to iOS later. |
| Overlay companion rendering | **Compose + Canvas/`SurfaceView`**; drop to **native Canvas / OpenGL ES** *only if* sprite animation can't hold 60fps within battery budget | Start high-level; go lower **only** if profiling demands it. Don't pre-optimise. |
| Local storage | **Room + SQLCipher** | Encrypted relational store; mature. |
| Key management | **Android Keystore** | Hardware-backed keys; biometric gate. |
| Async / concurrency | **Kotlin Coroutines + Flow** | Structured concurrency; no busy loops (battery). |
| DI | **Hilt** | Standard, testable. |
| AI client | **Kotlin client → Claude API** (cloud) and/or **on-device GGUF/LiteRT small model** (fallback) | Local-first, cloud-optional (ADR-003). |
| Native interop | **NDK / C++ via JNI** *only* for a proven hotspot | Reserved; not used unless a profiler says so. |

**Principle:** *low-level is a tool you reach for when a profiler points at a wall, not a default.* The architecture makes it *possible* to drop to Canvas/OpenGL/NDK for the companion or animation hotspots, but the plan is to ship in Compose/Kotlin and descend only on evidence. Premature native optimisation would cost time and bugs for a product whose hard part is *safety and restraint*, not raw compute.

---

## 3. ADR-003 — Two-tier AI: on-device local model + explicit-consent cloud

**Decision (refined with your input):** Aspen runs **two clearly-separated AI tiers**.

**Tier 1 — On-device local model (default, private, always available).** Handles the *small, bounded, low-risk* jobs: **companion (mascot) lines, personalised notification phrasing, and light on-device reasoning/agentic micro-tasks** (e.g. "which gentle check-in fits this moment," "phrase this reminder warmly"). Runs locally via a small model (LiteRT / llama.cpp-class, ~1–3B) — **no sensitive text leaves the device** for any of this.

> **Safety refinement (important):** for the companion's actual *words*, do **not** rely on open free-generation even from a local model — the whole point is "minimal error risk," and an unconstrained generator can always drift. Instead use a **curated, clinically-reviewed message library** as the source of truth, and let the local model **select and lightly personalise** (tone, timing, which of the approved lines fits the moment) rather than invent. This guarantees companion output stays inside the non-negotiables (`01` §6) by construction. The local model personalises *delivery*; the *content* is pre-approved. This is safer **and** lighter than on-device generation, and sidesteps the cost/complexity of fine-tuning for v1. (You can introduce a fine-tuned local model later if the library proves too rigid — but start safe.)

**Tier 2 — Cloud LLM (opt-in, explicit consent, for depth).** For richer reflection/conversation where quality matters, route to a cloud model (e.g. Claude API), **only after** the user enables it and sees a **brief, calm one-time warning** ("Turning this on means your messages are sent securely to an AI service to give better responses. You can turn it off anytime."). Constrained by the system prompt + output guard + red-team suite (`06` SR-3). On-demand only; never background; never for crisis handling (warm hand-off only).

**Rules across both tiers:**
- Grounding tools, notebook, logging, and safety **work fully without any AI** (offline, no model needed).
- The **safety engine / output guard (`04` §4) applies to both tiers** — including the local one.
- **Explicit, revocable consent** gates the cloud tier; the local tier is private by nature but still disclosed in settings.
- No AI ever outputs numbers-about-food/body, eating advice, appearance comments, or diagnosis (`01` §6).

**Why this shape:** it gives you private-by-default personality and notifications (the local tier, which is most of the everyday AI surface), reserves cloud cost+exposure for the moments that genuinely benefit, keeps the highest-risk surface (companion words) provably safe via curation, and keeps infra cost controllable (`03` B.3).

**[CONFIRM]** This matches what you described. One thing to confirm: are you OK starting the companion on a **curated+personalised library** rather than a trained local generator for v1? (Strongly recommended for safety; the trained model can come later.)

> **Phase-4 implementation note (approved 2026-07-02):** the v1 "local model" is a small
> LiteRT/MediaPipe **text-embedder ranker** (~4 MB), not a 1–3B generative model — it re-ranks the
> approved library lines by moment/tone context and can only return keys from the candidate set
> (safety by construction; null/failure degrades to deterministic selection). A generative local
> model remains a later drop-in behind the same `CompanionVoice`/`LineRanker` ports. iOS runs the
> deterministic selector until an iOS ranker actual lands (tracked). Tier 2 is implemented behind
> `AiClient` with injectable endpoint/auth and ships bound to `DisabledAiClient` — compiled, tested,
> **not live-wired**; no credential exists in the repo.

---

## 4. System architecture (clean, layered, modular)

A clean-architecture-style separation. Dependencies point inward; the domain knows nothing about Android, the network, or the UI.

```
┌──────────────────────────────────────────────────────────────┐
│  PRESENTATION (Android, Kotlin + Jetpack Compose)             │
│  • Calm Home   • Grounding   • Reflection   • Safety   • Set.  │
│  • Companion overlay UI (separate window surface)             │
│  state via ViewModels (MVVM) + unidirectional data flow       │
└───────────────▲───────────────────────────────────────────────┘
                │ (UI state / events only)
┌───────────────┴───────────────────────────────────────────────┐
│  DOMAIN  (pure Kotlin, KMP-ready, no Android imports)         │
│  • Use cases: StartGrounding, SaveReflection, ResolveCrisis…  │
│  • Entities, policies, the SAFETY RULES engine                │
│  • This layer encodes the non-negotiables as code            │
└───────────────▲───────────────────────────────────────────────┘
                │ (interfaces / ports)
┌───────────────┴───────────────────────────────────────────────┐
│  DATA / SERVICES  (adapters implementing domain ports)        │
│  • LocalStore (Room+SQLCipher)  • KeyVault (Keystore)         │
│  • AIClient (cloud Claude API | on-device model)             │
│  • CrisisRegistry (region-aware, cached, offline)            │
│  • CompanionEngine (overlay service + sprite/anim)           │
│  • ContentRepo (grounding exercises, all bundled/offline)    │
└────────────────────────────────────────────────────────────────┘
```

### Module layout (KMP + Compose Multiplatform)

```
:shared
  :domain              ← pure Kotlin, shared (use cases, SAFETY engine, entities, profile→behaviour rules)
  :data                ← shared, with expect/actual for platform bits
     :local            ← encrypted store (SQLDelight/Room-KMP + SQLCipher-equiv); expect/actual keys
     :ai-local         ← on-device model + curated companion-message library (Tier 1)
     :ai-cloud         ← cloud client + output guard (Tier 2)
     :crisis           ← region resource registry (PK/DE/UK/US…), offline cache
  :ui                  ← Compose Multiplatform (SHARED screens):
                          onboarding-questionnaire, home, grounding, reflection,
                          logging, safety, settings, in-app companion
  :core-design         ← design tokens, CMP theme, shared UI atoms
  :core-common         ← coroutines, result types, privacy-safe logging

:androidApp            ← Android entry, DI wiring, Android nav host
  :companion-overlay-android   ← SYSTEM_ALERT_WINDOW overlay service (ANDROID-ONLY; no iOS twin)
:iosApp                ← iOS entry (built later; compiles in CI from day one)
```

**Key points:**
- **~90% of the product lives in `:shared` (logic + Compose UI)** and runs on both platforms.
- **Safety subsystem is in shared `:domain`** — one implementation, both platforms, isolated from feature churn.
- **The system overlay is the *only* major Android-only module** (`05`; iOS can't do system overlays). The companion's *in-app* form lives in shared `:ui`, so iOS still gets a companion — just not a roam-the-whole-screen one.
- `expect/actual` bridges the few genuinely platform things: secure key storage (Android Keystore / iOS Keychain), overlay (Android only), biometrics.

### The safety rules engine (in `:domain`)

A first-class domain component, not scattered UI checks:
- **Output guard** for AI: rejects/rewrites any response containing numbers-about-food/body, eating advice, appearance comments, or crisis-handling-beyond-handoff (`06` SR-3).
- **Copy guard** (build-time): a CI lint over string resources flags forbidden tokens (calorie, BMI, kcal, weight, "failed," etc.) for human review (`06` SR-1).
- **Crisis resolver:** given locale → returns the correct, current resources; always has an international fallback; works from cache offline.

---

## 5. Data architecture (on-device-first)

```
On-device (encrypted, default — device/user-held keys, NOT server-readable):
  profile            (support_profile enum, onboarding answers, created/updated)   ← from questionnaire, drives adaptivity
  reflections        (id, text [encrypted], created_at, deleted_at)
  food_logs          (id, note [encrypted, QUALITATIVE text only], felt_how [enum/tags], created_at)
                     ↑ NO numeric columns exist: no calories/portions/weight/macros. Numberless by schema.
  behaviour_logs     (id, note [encrypted], feeling [tags], created_at)
  ai_messages        (id, tier, role, text [encrypted], created_at)   — only if cloud AI enabled
  companion_state    (mood, settings)                                  — non-sensitive
  settings           (companion scope, AI consent, motion, locale, trusted_contact_ref, lock)
  crisis_cache       (locale → resources snapshot)                     — offline FR-4

Cloud (ONLY if user opts into an account / sync — see 08_IDENTITY_LINKAGE_AND_CONSENT):
  account            (app-native id; NOT federated-identity-dependent; email optional for recovery)
  synced_data        (encrypted; server-held decrypt key model — see note)
  consent_grants     (scoped, time-boxed, revocable; powers clinician linkage — doc 08)
```

- **There are no numeric food/weight/calorie columns anywhere in the schema** — the absence is structural, so numbers literally cannot be logged even by a future careless feature (`03` SR-1). Food logs are **qualitative text + feeling tags only**.
- **Two key zones (see `08`):** *local* data uses **device/user-held keys** (true on-device privacy). *Cloud sync/export*, **if** the user opts in, is **true end-to-end** — the key is derived on-device and **never sent to the server**, so the cloud stores only ciphertext it cannot decrypt. Export/import is gated by **auth-only verification** (the server checks identity/authorization, never plaintext).
- **Delete means delete** (FR-11): hard-delete locally; tombstone+purge in cloud.

---

## 6. Performance & battery strategy (how we hit NFR-perf / NFR-batt)

This is where the native choice earns its keep. Concrete tactics:

**Cold start < 1s**
- Native Kotlin app, no JS engine to spin up; Compose with baseline profiles; lazy-init everything non-essential; safety + home on the critical path, AI/companion initialised after first frame.

**Companion overlay smoothness (60fps) without draining battery**
- **Render only when visible.** When the companion is idle/static, drop to near-zero frames; animate only on interaction or scheduled micro-actions.
- **Suspend on fullscreen.** Detect when a fullscreen/immersive app is foregrounded and *fully pause* the overlay (sprite hidden, no frames) — matches Shimeji "hide when fullscreen app in use" behaviour and is the single biggest battery win.
- **No busy loops / no polling.** Animation driven by the choreographer/frame callbacks only while active; coroutines for scheduling, cancelled when hidden.
- **Frame-throttle** static/ambient states (e.g. idle breathing at low fps).
- **Sprite packs downloaded on demand** and cached (keeps base APK lean; mirrors Shimeji's model-download approach).
- **No background network.** AI calls strictly foreground/on-demand.

**Memory**
- Recycle sprite bitmaps; cap concurrent companion instances (one by default — not Shimeji's many); release overlay resources immediately on dismiss.

**Measurement (so optimisation is evidence-led, per ADR-002)**
- Profile with Android Studio Profiler + Macrobenchmark + Battery Historian. Define budgets (cold-start ms, overlay idle mAh/hr, frame-miss %) and gate releases on them. **Only** descend to Canvas/OpenGL/NDK if a budget can't be met in Compose/Kotlin.

**OEM reality**
- Test on aggressive-battery OEMs (Xiaomi/MIUI, Samsung, Oppo) early; document the per-OEM "allow overlay / disable battery optimisation" steps for users; design the app to **degrade gracefully** (companion just doesn't appear) rather than break if the OS kills the overlay.

---

## 7. What carries over from the old stack: essentially nothing

For the record, so there's no ambiguity: the old React Native + Expo + Supabase + pgvector RAG architecture is **not** carried forward. The *intent* (calm, text-first, safety-aware, AI-supported) carries; the *technology and structure* are replaced for the reasons in ADR-001/002/003.

---

## 8. Sources

- Kotlin Multiplatform vs React Native — JetBrains official comparison (KMP stable Nov 2023; Google I/O 2024 support; sharing strategies). https://kotlinlang.org/docs/multiplatform/kotlin-multiplatform-react-native.html
- MetaDesign Solutions, *React Native vs Kotlin Multiplatform 2025* (cold start ~30%, memory 15–20%, battery 10–15%, RN battery-drain notes). https://metadesignsolutions.com/blog/react-native-vs-kotlin-multiplatform-in-2025-the-crossplatform-showdown-performance-devex-hiring-trends
- MVP App Forge, *KMP vs Flutter vs React Native 2025* (benchmarks; RN trails under heavy UI; Flutter cold-start/frames). https://www.mvpappforge.com/blog/kotlin-multiplatform-vs-flutter-vs-react-native
- Sthenos / Dotcode / Luciq 2025–2026 comparisons (code-sharing %, native-integration edge for device-heavy/background work).
- Shimeji Android apps (overlay via `SYSTEM_ALERT_WINDOW`, drag/tap, pass-through, hide-on-fullscreen, on-demand model download, lightweight footprint) — Google Play & Softonic listings (see `05`).
- `flutter_floatwing` plugin docs (illustrates overlay = `SYSTEM_ALERT_WINDOW` + window-manager service pattern).
