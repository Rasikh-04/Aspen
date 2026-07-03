# Aspen — STATUS

_Resume-cold notes. Update at the end of every working session (CLAUDE.md)._

**Phase:** 6 **IN PROGRESS** (slice ① server done 2026-07-03, below). Phase order (docs/00
decision #9, docs/07 §2): 6 → 6.6 (UI design pass) → 5.5 (companion refinement) → 7; new
**Phase 6.9 — account layering & cloud maturity** added 2026-07-03 (docs/00 decision #11) for
Google/Apple sign-in, device-to-device key transfer, prod DB/hosting/mail.
**`crisisGateStrict` still RED by design** until advisors verify content.

---

## Done (Phase 6 — slice ①: the Aspen server) — `feat/phase6-server` (2026-07-03)

New **`:server`** (Ktor JVM, runnable JAR) + **`:shared:server-api`** (KMP wire-DTO module both
sides compile against — no app/server drift). Decisions folded in: recovery = **recovery code +
email-attached account recovery (login only, never the data key)** (docs/00 #10, docs/08 §2 ✅);
**AI routes through the server; provider-agnostic** (docs/00 #11).

- **Auth (docs/08 §1):** register (email optional) / login (id or email) / logout / delete /
  email recovery. PBKDF2-HMAC-SHA256 600k iters, per-password salt, constant-time verify,
  self-describing hash format; opaque 32-byte session tokens, server-side + revocable; one
  undifferentiated denial (no account enumeration); recovery tokens single-use + 30-min TTL,
  outwardly always 202. Delete = purge (account, sessions, recovery tokens, blob — FR-11).
- **E2E sync (docs/08 §2):** PUT/GET/DELETE `/v1/sync/blob` — opaque ciphertext verbatim, per
  account, cross-account access structurally impossible (no parameter names another account),
  5 MB cap. Client-side encryption lands in slice ②.
- **AI relay (stateless BY CONSTRUCTION):** POST `/v1/ai/reflect` — no content store exists in
  the server, proven by test (distinctive text never lands in the data dir). **`ModelProvider`
  port + two adapters**: Anthropic Messages AND OpenAI-compatible Chat Completions (OpenAI,
  Gemini, Mistral, Groq, Ollama/vLLM, …) — **any model behind either shape, env-selected**
  (`ASPEN_AI_PROVIDER/BASE_URL/MODEL/KEY`); missing/partial config → **FakeModelProvider**
  (deterministic, offline — the whole stack runs + tests with NO live API and no key anywhere).
  `ReflectionSystemPrompt` relocated server-side (same `draft-2026-07-02` revision; app copy
  retired in slice ②). On-device consent/crisis/guard pipeline unchanged (CLAUDE.md #8).
- **Storage:** repository ports; in-memory (zero-config default) + file-backed (atomic writes,
  fail-safe corrupt→empty, path-traversal-proof blob names). Sessions/recovery deliberately
  never on disk. Rate limiting: hand-rolled sliding window (login per-identifier, relay
  per-account). Malformed bodies → 400, never 500.
- **Verified:** `:server:test` **54 green** (hasher totality, service, file stores, config
  selection, both adapters over MockEngine, route round-trips incl. brute-force 429 +
  statelessness proof) · copyLint · crisisGate · `:androidApp:assembleDebug` ·
  domain/data jvmTest regression · `:shared:server-api` compiles JVM + iosArm64 · secret grep
  clean. Gradle gotcha: `kotlin-jvm` alias needs root `apply false`; KMP jvm() must pin
  `jvmTarget 17` or the server toolchain can't load its classes.
- ~~Left for slice ②~~ **done below.** Slice ③: a11y/privacy audit pass. Dev-only mailer logs
  recovery tokens to console (real mail = Phase 6.9); server deployment/hosting deferred (6.9).

## Done (Phase 6 — slice ②: app clients + optional account UI) — `feat/phase6-app-clients` (2026-07-03)

The app-side half: login/register is now REAL (Settings-only, per FR-9/#10 — deliberately NOT
offered after onboarding; team may add a soft mention via the 6.6 fix list if wanted).

- **`AccountManager`** (domain/account, port): current/register/signIn/signOut/deleteAccount;
  total + calm (`AccountResult` incl. `Unavailable` for offline). **`ServerAccountManager`**
  (data/account) over the slice-① endpoints; errors map by machine code only (copy stays in UI
  resources, #11); password exists only in transit; **`PersistentSessionStore`** (encrypted
  `account_session` blob, fail-safe: unreadable → signed OUT, never in). Delete-account keeps the
  local session unless the server purge SUCCEEDED (user can retry; no orphaned ciphertext).
- **`AspenServerAiClient`** replaces the retired `ClaudeAiClient` (+ app-side
  `ReflectionSystemPrompt` deleted — server holds it now): speaks Aspen's own wire shape
  (`:shared:server-api`), bearer = session token, **no vendor key/shape on the device**; no URL or
  no session ⇒ `Disabled` with zero network (tested); 401/error/offline ⇒ calm `Unavailable`.
  `DisabledAiClient` remains the DI default — Koin graph unchanged, cloud still off by default.
  ReflectionCompanion pipeline untouched (consent → crisis → client → guard, #8).
- **Settings → "An account (optional)"** (`AccountSection`): quiet card → Create (email optional)
  / Sign in (email or account id) with warm single-line fields; signed-in shows the **account id**
  (the way back in when no email is attached); Sign out; Delete account (confirm dialog, amber,
  copy states on-device writing is untouched). Calm error lines; no feature gates on any of it.
- **Wiring:** `MainActivity` — debug builds point at `http://10.0.2.2:8080` (emulator loopback;
  debug-manifest `usesCleartextTraffic`), **release has NO URL ⇒ account row absent + AI Disabled
  (release behaviour = exactly Phase 4)**. `INTERNET` permission added (manifest comment: no
  analytics/background egress; nothing leaves without account/AI opt-in). One shared ktor CIO
  client at the entry; `:shared:data` stays engine-less.
- **Verified:** `:shared:data:jvmTest` (10 new: account manager register/persist-encrypted/
  error-codes/offline/sign-out-clears/delete-only-on-success + AI client disabled-no-network/
  wire-shape/degradations) · domain/ui suites · `assembleDebug` · data+ui `compileKotlinIosArm64` ·
  `copyLint` (24 new strings) · `crisisGate` green.
- **Leftouts (explicit):** "forgot password" UI (server endpoint exists, no app surface yet);
  iOS entry deps still null ⇒ row absent on iOS; ur strings = English fallback (pattern
  unchanged); no Compose screen tests (tracked since P3).

## Done (Phase 6 — slice ②b: E2E backup/restore) — `feat/phase6-app-clients` (2026-07-03)

True-E2E backup per docs/08 §2, with both decided recovery paths (docs/00 #10).

- **`BackupManager`** (domain/sync port): isConfigured / enable / backUpNow / restore / disable —
  total, calm outcomes. **`ServerBackupManager`** (data/sync): random data key **K** seals the
  payload; K travels only **wrapped twice** — under the passphrase-derived key AND under the
  once-shown recovery-code-derived key (both PBKDF2-HMAC-SHA256 600k + AES-256-GCM) — so either
  secret restores, neither is derivable from the blob, and the server holds ONLY the
  `SyncEnvelope` ciphertext. K persists locally sealed by the DEVICE cipher ("back up now" needs
  no retyping); local key is forgotten only after the server copy is confirmed gone (disable
  mirrors delete-account semantics). Recovery code: ~100-bit Crockford-style
  XXXXX-XXXXX-XXXXX-XXXXX, typo-forgiving normalization.
- **`SyncCrypto` expect/actual** (NEW seam, distinct from LocalCipher because backup must open on
  a DIFFERENT device): jvm+android = javax.crypto; **iOS actual = null ⇒ feature absent** (a
  passthrough would upload plaintext — same placeholder policy as the cipher, PRE_SHIP §3).
- **What syncs:** content only — `profile`, `logs`, `ai_messages` blobs via `LocalStoreBundle`
  (decrypt with device key → bundle → seal with K; restore re-encrypts under the new device's
  key). Consent grants, sessions, companion/language prefs stay device-scoped on purpose.
- **UI (`BackupSection`, signed-in only):** enable with passphrase (key-model note: "locked before
  it leaves your phone… we can never unlock it for you") → **recovery code shown ONCE** with
  honest copy (email reset restores sign-in, never the backup); manual "Back up now" (no
  background sync, docs/04 §6); restore field takes passphrase or code; turn-off deletes the
  server copy (amber confirm; device writing untouched). 27 new en strings; ur falls back.
- **Verified:** `:shared:data:jvmTest` (8 new: seal/open totality, code format+normalization,
  ciphertext-only upload proof, cross-"device" restore via passphrase AND via sloppily-typed
  code, wrong-secret/no-backup/offline, weak-passphrase, disable purge) · ui host tests ·
  `assembleDebug` · data+ui iosArm64 compile · `copyLint` · `crisisGate`.
- **Leftouts:** "forgot password" app surface; **iOS SyncCrypto + LocalCipher actuals** (one
  CryptoKit task, PRE_SHIP §3); passphrase change = disable+re-enable for now (documented UX gap,
  fine for v1); restore does not merge (it overwrites the three content blobs — acceptable v1,
  revisit with multi-device in 6.9).

---

## Done (Phase 6 — slice ③: a11y + privacy audit pass) — `feat/phase6-app-clients` (2026-07-03)

Audit of the Phase-6 surfaces (AccountSection/BackupSection) + the privacy-review checklist. No new
feature surfaces — code deltas are a11y-only.

- **A11y fixes:** error/status texts on both surfaces are now polite **live regions** (they were
  visually quiet by design, which left screen-reader users in silence — WCAG 4.1.3); fields declare
  **keyboard types** (Password/Email — right IME, and keyboards don't learn/suggest secrets); the
  once-shown **recovery code is selectable** (copyable — hand-transcribing 24 chars was a
  motor/vision burden; copying stays user-initiated).
- **Audited, no change needed:** reduced-motion parity (only animation is the shared
  `pressableScale`, already disabled under reduced motion); touch targets (AspenCard full-width
  rows, padding `m`); contrast tokens (same textSecondary/caution pairs as the rest of Settings);
  no alarm red anywhere (delete/turn-off confirms use `caution` amber); dialogs dismissible every
  way (no trap).
- **Privacy review:** new **PRE_SHIP §6** — server data inventory (holds: account record w/ optional
  email + PBKDF2 hash, memory-only sessions/recovery tokens, one ciphertext blob; can never see:
  content, keys, recovery codes — each structural claim tied to its proving test), §6b hosted-
  deployment checklist (Phase 6.9 gate), §6c manual device-QA list (TalkBack pass, cross-device
  restore, delete/turn-off verification).
- **Verified:** ui host tests · `assembleDebug` · ui iosArm64 compile · `copyLint` · `crisisGate`.
- **Leftouts (human):** everything in PRE_SHIP §6b/§6c is manual by nature; expanded/collapsed
  state announcements on tappable disclosure cards (app-wide pattern, not Phase-6-specific —
  fold into the Phase 6.6 UI design pass).

---

## Done (interim, 2026-07-03 — i18n language setting, docs/12 §4) — `feat/i18n-language-setting`

Between-phase task: the i18n plumbing existed (resolver, RTL, `values-ur` stub) but nothing fed
`LocaleProvider(override=…)` — no Settings row, no persistence, and compose-resources strings
followed only the device locale. Now:

- **`LanguagePrefStore`** (domain/i18n port) + encrypted fail-safe **`PersistentLanguagePrefStore`**
  (`language_pref` blob; unreadable/unknown code → null → follow the device — a storage fault can
  never pin a wrong language). DI binding in `localStoreModule`; wired in `MainActivity`.
- **Settings → "Language"** chips: **Match my device / English / اردو** (row absent when the store
  isn't wired, e.g. iOS entry). Only languages that HAVE string resources are offered; the other
  five join as their native-reviewed files land (CLAUDE.md #11).
- **`applyLanguageOverride` expect/actual** (LocaleBridge): compose-resources' environment is not
  app-overridable in CMP 1.11 (`ResourceEnvironment` ctor is internal — a
  `LocalComposeEnvironment` attempt was abandoned), so the override sets the **process default
  locale on Android/JVM**; the REAL device tag is captured first so "match my device" restores
  truth. **iOS actual = documented no-op** (store not wired on the iOS entry yet; LocalAppLanguage
  + RTL still follow the choice there).
- **Strings:** `settings_language_label` has real Urdu (**زبان** — needs no clinical review);
  language names are endonyms (identical across locale files). All other ur strings remain English
  placeholders pending native ED-informed review (unchanged leftout).
- **Verified:** domain/data/core-common jvmTest + ui host tests (6 new store tests, 3 locale-bridge
  tests) · `copyLint` · `crisisGate` · `:androidApp:assembleDebug` · iosArm64 compiles (new actual).
- **Leftout:** live in-app switch re-renders shared-UI strings; the Android overlay/notification
  module's own `res/` strings still follow the OS locale (fine — they're OS-surface copy).

---

## Done (Phase 5 — companion, docs/05 / docs/07 Phase-5) — three stacked branches

### ① In-app companion (`feat/phase5-companion-core`) — the cross-platform baseline
- **`CompanionBehaviour`** (domain/companion, pure): total, non-throwing state machine
  (Hidden/Ambient/Playful/GentlePresence/Suspended) with the guardrails AS STRUCTURE — disabled ⇒
  Hidden whatever happens; **dismissed never self-returns** (only Summon; SR-4 no-nag); reduced
  motion refuses/settles play (SR-6); hard-moment ⇒ gentle presence (no play); fullscreen ⇒ suspend,
  resume to calm ambient only. Playful is time-boxed (60s). 21 tests incl. totality sweep.
- **`CompanionPrefs`** (ALL defaults off) + encrypted fail-safe **`PersistentCompanionPrefsStore`**
  (unreadable/unknown-species → null → all off — storage faults can only make it quieter);
  `companionModule` DI + off-by-default graph test.
- **Procedural pixel sprites** (ui/companion/sprite; **approved deviation 2026-07-03** from docs/05
  §5 "soft round creature" — reconciliation: pixel FORM, calm palette, 2–6 fps, nothing twitchy):
  in-code 12×12 pixel grids → Compose Canvas, **zero binary assets**, reviewable in a diff. Lineup:
  **Aspen sprite** (default; original warm-apricot critter, leaf sprout + floating dusty-rose heart —
  a terminal-pet homage, NOT Anthropic trade dress), **sage cat**, **sand bunny** (dog/owl/panda =
  fast-follow, same format). Integrity tests: uniform grids, palette-resolvable, fps ≤ 8, **no
  alarm-red**, species distinct. Renderer ticker runs only while composed/animated; reduced motion ⇒
  single still frame, no coroutine.
- **`CompanionController`** (plain state holder) + **`InAppCompanionLayer`** in `AppScaffold`:
  ambient at the bottom edge, drag to move, tap → playful (domain time-box via 1s ticks), during
  play a **one-tap calm "Rest now"** dismisses until re-summoned (docs/05 §3.1). Hard-moment routes
  (Calm + tools) ⇒ gentle presence with ONE curated Phase-4 library line; **never composed on the
  safety route** (crisis surface stays clear, CLAUDE.md #6). Settings: "A small companion" toggle
  (off by default) + species chips. 12 host tests.

### ② Android overlay (`feat/phase5-overlay-android`) — platform enhancement
- New **`:companion-overlay-android`** module: foreground service (**`specialUse`** FGS subtype,
  quiet IMPORTANCE_MIN honest notification) hosting one ComposeView on the WindowManager
  (`TYPE_APPLICATION_OVERLAY` + `FLAG_NOT_FOCUSABLE` — can never intercept typing).
  **`SYSTEM_ALERT_WINDOW` only, never AccessibilityService** (docs/05 §6 guardrail).
- **Same shared controller/machine/sprites as in-app** — one rulebook. `FullscreenSignals`
  (insets-based, non-invasive): system bars hidden ⇒ suspend (renders NOTHING — no frames);
  4 unit tests incl. never-resurrect-dismissed. `START_NOT_STICKY`: OEM kills it ⇒ companion just
  disappears, nothing else breaks. Service refuses to run unless enabled+overlayEnabled+permission.
- **Permissions UX** (docs/05 §6): Settings "Across your screen" row → plain-language
  explain-BEFORE-request dialog (incl. what Aspen CANNOT do) → OS grant screen; `onResume` re-sync;
  disable = instant. Master toggle takes the overlay down too. In-app layer yields while the overlay
  carries presence (single instance). `CompanionOverlayControl` seam is null on iOS ⇒ row absent
  (documented platform limit, docs/04 ADR-001).
- **Approved deferral 2026-07-03:** "home-screen-only" scope — no privacy-clean launcher detection
  exists (would need `PACKAGE_USAGE_STATS`); v1 = in-app / overlay-everywhere-with-auto-suspend.

### ③ Check-ins + reduced motion (`feat/phase5-notifications-reduced-motion`) — Phase-4 leftouts land
- **`NotificationPolicy`** (domain, pure, 7 tests): opt-in **twice over** (companion AND
  notifications), **≥72h cadence by construction**, 10:00–21:00 window, wording accepted ONLY from
  the library's `NOTIFICATION_PHRASING` moment (out-of-moment lines rejected at the seam). No
  burst/retry/"missed you" path exists. Android: daily WorkManager probe that may only ASK the
  policy; encrypted last-delivery timestamp; silent channel; `POST_NOTIFICATIONS` requested only
  inside the opt-in act; revoked ⇒ silent, never re-asks. Settings row "A rare hello".
- **OS-sourced reduced motion** (closes the Phase-3 leftout): `systemReducedMotion()` expect/actual
  (Android animator-scale; iOS Reduce Motion) → `AspenTheme` at the shared root + overlay service
  controller (SR-6).

### Verified locally (Linux — no Xcode; iOS link = CI `macos-14`)
- ✅ `:shared:domain:jvmTest` (28 new companion tests) · `:shared:data:jvmTest` (7 new) ·
  `:shared:ui:testAndroidHostTest` (12 new) · `:companion-overlay-android:testDebugUnitTest` (4)
- ✅ `copyLint` green (**4 string files** — overlay module res now scanned too) · `crisisGate` green
  (`crisisGateStrict` red by design) · `:androidApp:assembleDebug` · iOS metadata compiles ·
  `:shared:ui:compileKotlinIosArm64` (new iosMain actual compiles)
- Deps added: `androidx.work:work-runtime-ktx 2.10.1`, `lifecycle-runtime-ktx 2.9.1`,
  `savedstate-ktx 1.3.0`, `core-ktx 1.16.0`; new plugin alias `com.android.library` (root
  `apply false` required — AGP already on classpath).

### Phase 5.5 reserved — companion refinement (docs/07)
- Placeholder phase added 2026-07-03; **scope TBD by the team** before it starts. The Phase-5
  deferrals below are listed there as candidates.
- **Postponed 2026-07-03:** runs after Phase 6 and the new Phase 6.6 UI design pass (docs/07 §2,
  docs/00 decision #9).

### ⚠ Deviations & leftouts (Phase 5 — explicit, per CLAUDE.md)
- **DEVIATION (docs/05 §5 art):** procedural pixel-art instead of soft-round vector — approved
  2026-07-03 (calm palette + slow motion reconciliation, recorded above). Sprite-pack
  download/cache mechanism (docs/05 §5) not built — pixel maps make it unnecessary for v1.
- **DEVIATION (docs/05 §4 gestures):** dismiss is the one-tap "Rest now" action during play (+
  Settings toggle) rather than fling-to-edge; fling/double-tap-configurable deferred. A11y-friendlier
  and satisfies §3.1 one-tap dismiss; revisit after user validation.
- **Dismissal is session-scoped:** "Rest now" hides until re-summon OR next cold start/app resume
  (overlay: next `onResume` restarts it). Acceptable v1 reading of "stays gone until summoned";
  flag for the docs/05 §8 user-validation gate.
- **Overlay behaviours kept minimal on purpose (docs/05 §3 "do less"):** no floating summon button,
  no hide-behind-icons illusions, no proactive check-in speech — presence only. Candidates for
  post-validation iterations.
- **Real-device verification pending** (Linux host): 60fps / <~1%/hr idle battery / OEM
  suspend-and-kill behaviour — now a PRE_SHIP §4b checklist item.
- **API 24–25 have no static reduced-motion signal** without a Context — those two levels keep
  motion on (in-app default).
- **iOS:** in-app companion compiles for iOS by construction (shared code, iosArm64 klib green) but
  is **not device-verified**; overlay + check-ins are Android-only (platform limit / Phase 8).
- **No new companion lines added** — zero new advisor surface; overlay/notification UI strings are
  en drafts, ur falls back to en (docs/12 §3 review still owed for all Phase 3–5 strings).
- **No Compose screen-level UI tests yet** (state logic unit-tested) — same tracked follow-up as
  Phases 3–4; CI still doesn't run `:shared:ui:testAndroidHostTest` (lead infra task).

---

## Done (Phase 4 — AI tiers, docs/07 / docs/04 ADR-003 / docs/03 SR-3) — three stacked branches

### ① Durable on-disk encrypted store (`feat/phase4-durable-store`) — closes the Phase-2/3 leftout
- **`FileEncryptedBlobStore`** (common, fail-safe: load/clear never throw; names are logical ids) over a
  tiny `BlobFileIo` expect/actual seam. JVM = tmp-dir (dev grade); **Android = `filesDir/aspen_blobs`,
  atomic temp-then-rename writes, Keystore-encrypted bytes** — profile/logs/consent now survive cold
  start. `AspenLocalStorage.init(context)` anchors Context at app start (fails fast if missed).
- **iOS deliberately NOT durable yet:** real file IO is implemented but gated behind
  `IOS_CIPHER_IS_REAL = false` — the iOS cipher is still a passthrough, and durable files would persist
  PLAINTEXT. A process-wide in-memory stand-in keeps the no-plaintext-at-rest guarantee (PRE_SHIP §3).
- DI + `MainActivity` rebound to durable blobs; `DurableConsentBlobStore` adapter for consent.

### ② Guard + companion voice + red-team gate (`feat/phase4-guard-companion`)
- **`CrisisSignals`** (domain/safety): conservative, heuristic crisis-sign INPUT check — explicit
  phrases only (en/de/ur starter), boolean hand-off signal, never a label (CLAUDE.md #8/#9);
  deliberately does not over-trigger on ordinary distress (docs/06 §6.3). Canonical
  `config/safety/crisis_signals.json` + runtime mirror + parity test.
- **Consent:** `RecipientType.AI_SERVICE` + `DataCategory.AI_MESSAGES` — cloud AI rides the existing
  default-deny/revocable/audited primitive ("issue a grant, not a refactor" pays off as designed).
- **Companion Tier 1** (ADR-003 safety refinement): `CompanionMoment`/`CompanionLine`/`CompanionLibrary`
  + `CompanionVoice`/`LineRanker` ports (domain); curated **14-line PROVISIONAL library**
  (`config/companion/library.json` + mirror + parity; copy = UI string keys per docs/12 §3).
  `LibraryCompanionVoice`: ranker can only REORDER candidates (out-of-set picks discarded, throwing/null
  → deterministic, total over every moment × tone — all tested).
- **Android ranker actual:** MediaPipe text-EMBEDDER (~4 MB, NOT generative) over optional
  `companion_ranker.tflite` asset (fetch guide: docs/DEV_SETUP §7; git-ignored); absent →
  deterministic. JVM/iOS: null ranker (iOS actual tracked, like the cipher).
- **Red-team gate:** `config/safety/redteam/corpus.json` (21 adversarial + 5 crisis + 5 benign
  anti-over-blocking entries) + `RedTeamSuiteTest` in `check`; corpus-shrink protection.
  `DomainModule.PHASE = 4`.

### ③ Cloud reflection, consent UX, debug preview (`feat/phase4-cloud-reflection`)
- **`ReflectionCompanion`** (domain/ai) — THE single pipeline (features never touch `AiClient`):
  consent (default-deny) → crisis-sign check BEFORE anything leaves the device → client →
  `guardOutput` (withhold + replace; unsafe text never echoed/persisted) → encrypted history.
  Tested against the REAL consent manager/guard/signals (fakes only at client/store edges).
- **`ClaudeAiClient`** (Ktor, Messages-API shape) with **injectable `AiEndpointConfig`** — **no key,
  no endpoint in the repo**; refusal/errors/offline → calm `Unavailable`. **Shipped binding is
  `DisabledAiClient`** (DI test proves cloud is off by default). Live endpoint = deferred decision
  (PRE_SHIP §4). `ReflectionSystemPrompt` (revision `draft-2026-07-02`) = advisor-review surface.
- **`PersistentAiMessageStore`** — encrypted `ai_messages`, fail-safe, FR-11 hard delete (Settings
  "delete everything" now clears AI history too).
- **UI:** Settings "Deeper reflection" row issues/revokes the consent grant with the calm ADR-003
  one-time warning (revoke = instant, no friction); Reflect shows the companion card ONLY while the
  grant is active (hand-off outcome → validating line + "Reach someone now" → Flow C); **debug-only
  `CompanionPreviewScreen`** (Settings, debug builds): cycle moments/tones, see chosen line + key,
  guard/crisis playground. `AspenDeps` extended (domain types only); `MainActivity` builds deps in
  composition so the guard fallback line stays localized (CLAUDE.md #11).
- **Strings:** 14 companion lines (en drafts, PROVISIONAL — sensitive surface), AI consent/settings,
  reflection surface, `safety_ai_fallback`, debug strings. `copyLint` green. ur = English fallback
  (unchanged pattern).

### Verified locally (Linux — no Xcode; iOS link = CI `macos-14`)
- ✅ `:shared:domain:jvmTest` (incl. ReflectionCompanion pipeline, CrisisSignals over/under-trigger,
  red-team suite, parity tests) · `:shared:data:jvmTest` (durable store, AI store, mock-engine client,
  DI cloud-off-by-default) · `:shared:ui:testAndroidHostTest` · `:buildSrc:test`
- ✅ `copyLint` (all new strings) · `crisisGate` green / `crisisGateStrict` red by design · no
  secret/key anywhere (grep) · `:androidApp:assembleDebug` · metadata compiles (iOS configures)
- Deps: ktor 3.5.1 (3.2.0 has a known Android D8/dexing bug), mediapipe tasks-text 0.10.35,
  kotlinx-coroutines-test.

### ⚠ Deviations & leftouts (Phase 4 — explicit, per CLAUDE.md)
- **DEVIATION (ADR-003 "1–3B local model"):** v1 ranker is a ~4 MB text-embedder, NOT generative —
  approved 2026-07-02; noted in docs/04. Generative local model = later drop-in behind the same ports.
- **Cloud tier not live-wired** (approved): `DisabledAiClient` binding; endpoint/proxy decision +
  budget cap deferred (PRE_SHIP §4). No `ai_messages` can leave a device today.
- **Notification scheduling deferred to Phase 5** (approved; docs/07 Phase-4 scope note) — phrasing
  moment exists in the library only.
- **All companion lines + system prompt + crisis-signal phrases are PROVISIONAL drafts** — Phase-4
  [APPROVE] advisor gate is OPEN (PRE_SHIP §4).
- **iOS:** ranker + durable storage + cipher all pending (deterministic voice + in-memory store on iOS).
- **No Compose UI tests for the new surfaces yet** (state logic unit-tested; screen-level tests remain
  the tracked follow-up from Phase 3).

---

## Done (2026-07-02 — UI maturation: the component layer) — on `feat/phase3-feature-ui`, pre-merge

Phase 3 shipped with minimal text-based screens. Decision (now in `docs/06` §2.1): **calm is a design,
not an absence of one** — the UI stays quiet/simple *by intent*, but must never look like a placeholder
text-holder with default buttons. Fixed before Phase 4 so no feature builds on raw-Material UI and needs
a rewrite later.

- **New component layer `app.aspen.design.components`** (`:shared:core-design`): `AspenPrimaryButton` /
  `AspenQuietButton` / `AspenTextAction`, `AspenCard`, `AspenChoiceChip` / `AspenTagPill`,
  `AspenScreenHeader`, `AspenAmbientBackground` (static `drawBehind` — zero battery cost),
  `AspenPresenceDots`, shared press-settle feedback. All honour the motion tokens + `LocalReducedMotion`.
  New colour tokens: `primarySoft` (Sage100), `primaryFaint` (Sage50).
- **Every Phase 3 screen refit onto the layer** (visuals only — no logic changes, no new user-facing
  strings, so no new localization review load): nav shell (custom quiet tab bar with animated presence
  dot, route cross-fades, ambient background), home, onboarding (animated option selection, presence-dot
  progress), grounding chooser (whole-card targets), grounding tools (54321 cross-fade + presence dots),
  reflect (cards, sage tag pills, warm text fields — feeling-chip selection moved off `cautionBg` amber
  onto calm sage), settings, safety screens (Flow C re-toned slate; `TODO-VERIFY` non-actionable logic
  untouched).
- **Rule from Phase 4 on** (`docs/06` §2.1): feature UI composes these primitives; raw Material widgets
  on shipped surfaces are a review-blocking defect; extend the layer rather than hand-rolling.
- **Post-merge CI fix:** the iOS framework link (macOS runner only) failed with "IrTypeAliasSymbolImpl
  is already bound" — navigation/lifecycle 2.9 pulls `kotlinx-datetime` 0.7.x while we pinned 0.6.1.
  Resolved by migrating all `Clock`/`Instant` usage to stdlib `kotlin.time` and **dropping the
  `kotlinx-datetime` dependency entirely**. Reminder: only CI exercises the iOS *link* step — Linux
  `check` compiles iOS klibs but can't catch link-time klib conflicts.

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

## Done (Phase 3 — Dev B: feature UI) — branch `feat/phase3-feature-ui` (stacked on encrypted-local-store)

The shared `:shared:ui` Compose screens for Flows 0/A/B + Settings, wired to Dev A's domain use-cases
(`OnboardingScoring`/`ProfileStore`, `LoggingService`/`AppConfigProvider`). All copy externalised,
numberless, no streaks/scores/alarm-red; every flow keeps the ≤2-tap human exit (CLAUDE.md #3/#5/#6).

### Flow 0 — onboarding questionnaire (`ui/onboarding`)
- **`OnboardingController`** (plain Compose state holder; no ViewModel lib): owns in-progress
  `OnboardingAnswers` + a step cursor; edits are immutable `copy()`; scores **only** via the domain
  `OnboardingScoring.deriveProfile()` — the UI never derives/shows a profile, label, or score (#9).
- **One numberless question per screen** (Q1–Q10, docs/11 §3), intro + closing; every item skippable
  ("skip this one" = prefer-not-to-say → no signal), "skip these for now" → safe `MIXED_OR_UNSURE`.
  Progress shown as **soft dots, never "3 of 10"** (#3). Closing routes toward real help first.
- First-run gating in `AppRoot`: no stored profile → onboarding; on finish `ProfileStore.save()`; the
  questionnaire is **re-runnable from Settings**. (Treating null-profile as "not onboarded" — small
  routing convention on the existing contract; no contract change.)

### Flow A — grounding tools (`ui/grounding`)
- **Chooser** (Calm tab): Breathe · Ground (5-4-3-2-1) · Ride the urge · Write it down · **Reach
  someone** (always-present human exit). Full-screen tools as routes (bottom bar hidden); calm,
  non-evaluative close ("Glad you took a moment") — never "great job"/streaks.
- **BreatheScreen** paced-breathing animation that **honours reduced-motion** (`LocalReducedMotion`)
  → static cue words, no animation (SR-6). `Ground54321Screen` (sensory counts, not food/body numbers),
  `RideTheUrgeScreen` (wave framing, no timer).

### Flow B — reflection + numberless logging (`ui/reflect`)
- **`ReflectScreen`** wired to `LoggingService` (the single enforcement point): reflections + feeling
  logs always available; **food logging entry only appears when `isFoodLoggingOffered()`** for the
  active profile. Feeling tags are emotions only (no intensity scale/count — SR-1). One-tap delete per
  entry; **empty days are silent** (no "you missed"). Null service → calm placeholder (iOS-safe).

### Settings (`ui/settings`)
- **Revisit the questions** (re-run Flow 0) + **delete everything I've written** (confirmed dialog →
  `LoggingService.deleteEverything()`, FR-11). Calm copy, amber (never red) on the destructive action.

### Wiring
- **`AspenDeps`** (domain types only — keeps `:shared:ui` on `:shared:domain` alone) threaded through
  `AspenApp`/`AppScaffold`, mirroring the existing `crisisResolver` injection. **Android `MainActivity`**
  constructs the encrypted store stack by hand (Koin-start at platform entries is still the tracked
  leftout). **iOS `MainViewController` unchanged** (`AspenApp()` → null deps → calm placeholders).

### Verified locally (Dev B-role / Linux — no Xcode)
- ✅ `:androidApp:assembleDebug` — full chain (`:shared:ui` → `:shared:data` → `:shared:domain` →
  app) compiles; APK builds.
- ✅ `:shared:ui:testAndroidHostTest` — 8 UI tests (controller cursor/immutability, empty→MIXED,
  restriction→SUPPRESS_FOOD_LOGGING via domain, feeling-tag label coverage). Enabled host tests on
  `:shared:ui` via `withHostTestBuilder {}` so commonTest runs JVM-hosted on Linux.
- ✅ `copyLint` passes incl. all new Phase-3 strings; ✅ `:shared:ui:compileCommonMainKotlinMetadata`
  (iOS target configures); ✅ existing `:shared:domain:jvmTest` + `:shared:data:jvmTest` still green.

### ⚠ Deviations & leftouts (Phase 3 — Dev B, explicit per CLAUDE.md)
- **DEVIATION (single branch):** docs/13 §5 / docs/14 mandate small one-feature-per-PR. By explicit
  request this ships all three Flow-0/A/B features + Settings on **one branch** (`feat/phase3-feature-ui`)
  / one PR. Noted so the reviewer expects a larger-than-usual (but still cohesive) diff.
- **In-memory blob store → profile resets on cold start** (same durable-on-disk leftout as Phase 2),
  so onboarding re-shows each fresh launch. Fine for dev; durable persistence is the tracked next task.
- **Urdu (and other locales) Phase-3 strings fall back to English** at runtime (values-ur not mirrored);
  questionnaire/companion copy needs ED-informed native review before ship (docs/11 §5, docs/12 §3).
- **Reduced-motion is honoured in the UI** (`LocalReducedMotion`) but **not yet sourced from the OS
  setting** — the OS→theme plumbing is a small follow-up (currently defaults to motion on).
- **No Compose UI/interaction/RTL snapshot tests yet** — state logic is unit-tested; screen-level
  Robolectric/snapshot + RTL screenshot tests are a follow-up (docs/13 §4 a11y/RTL).
- **CI does not yet run `:shared:ui:testAndroidHostTest`** — add it to `.github/workflows/ci.yml`
  alongside the existing jvmTest gates (Dev A / lead infra task).

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
