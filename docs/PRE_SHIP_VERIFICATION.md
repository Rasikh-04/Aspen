# Pre-Ship Verification Checklist

> **Why this exists.** To keep development moving, some safety-bearing content is filled in
> **PROVISIONALLY** (clearly marked, not advisor-verified) so the dev gates pass. None of it may ship
> in that state. This file is the gate between "builds locally" and "shippable". Every box must be
> ticked — with evidence — before a public release of the relevant locale/platform.

Status legend: ⬜ not done · 🟡 provisional (dev-only) · ✅ verified for ship.

---

## 1. Crisis registry content (CLAUDE.md #7, docs/09 §2.5, docs/10)

Current state: 🟡 **PROVISIONAL** for launch locales (PK/DE/UK). `verifiedBy = "PROVISIONAL-UNVERIFIED"`,
`verifiedOn = 2026-06-28`, and **all contact values remain `TODO-VERIFY`** (rendered non-actionable in
the UI — no unverified number can be dialled). US/INTL remain `TODO-VERIFY`.

Before ship, for **each launch locale**:
- [ ] Trust-and-safety advisor confirms each organisation is operating and ED-relevant (or correctly
      categorised as general MH / emergency).
- [ ] Each contact value confirmed against the organisation's **official source**, dated; replace every
      `TODO-VERIFY` contact value with the real, confirmed value.
- [ ] `verifiedBy` set to the real advisor (not `PROVISIONAL-*`); `verifiedOn` set to the confirmation date.
- [ ] Crisis copy reviewed in the target language by an **ED-informed native speaker** (Urdu for PK, German for DE).
- [ ] **NEDA appears nowhere** (gate enforces; re-confirm).
- [ ] An acute-crisis option is present and clearly distinguished from ED support.
- [ ] Tested offline (cached) and reachable in ≤2 taps.
- [ ] **`./gradlew crisisGateStrict` is GREEN** (this is the machine check — it rejects provisional content).

> The dev gate `crisisGate` (wired into `check`, also CI-informational) accepts provisional content.
> The release gate **`crisisGateStrict`** does **not** — run it as the ship gate.

## 2. Localized copy (CLAUDE.md #11, docs/12 §3)
- [ ] Urdu safety/crisis strings replaced with ED-informed native review (currently English placeholders).
- [ ] de/ur forbidden-token lists reviewed (currently starter sets).
- [ ] RTL verified on the safety surfaces.

## 3. Consent persistence / crypto (docs/08 §2)
- [ ] iOS `ConsentCipher` replaced: Keychain-held key + CryptoKit AES-GCM (currently a passthrough placeholder).
- [ ] Android Keystore cipher verified on a real device/emulator.
- [x] Durable on-disk `ConsentBlobStore` in place (Phase 4: `DurableConsentBlobStore` over `FileEncryptedBlobStore`).
- [ ] Koin started at platform entries; localized `SafetyFallbackCopy` bound; trusted-contact capture + contact dialling wired.
- [ ] iOS `LocalCipher` replaced (Keychain + CryptoKit); THEN flip `IOS_CIPHER_IS_REAL` in
      `BlobFileIo.ios.kt` so iOS storage becomes durable (deliberately in-memory until the cipher is real —
      durable files would persist plaintext).

## 4. AI tiers (Phase 4 — docs/04 ADR-003, docs/07 Phase 4 [APPROVE])
- [ ] **Companion library reviewed by clinically-informed advisors** — every line in
      `config/companion/library.json` is `PROVISIONAL` (drafts by dev); flip per-language review status
      only on real sign-off. A language may not ship companion lines unreviewed (docs/12 §3).
- [ ] **Tier-2 system prompt reviewed by advisors** (`ReflectionSystemPrompt`, revision `draft-2026-07-02`).
- [ ] **Live cloud endpoint decision** (deferred to save cost): direct-with-proxy vs no cloud at v1. The
      client is compiled/tested with injectable endpoint+auth; **no key may ever land in the repo/app** —
      a proxy service holds credentials and the hard monthly budget (docs/03 B.3). Until decided,
      `DisabledAiClient` stays the binding.
- [ ] Red-team corpus extended per shipping language (en/de/ur starter today; hi/zh/ar/es pending lexicons).
- [ ] Crisis-signal phrase lists advisor-reviewed per language (`config/safety/crisis_signals.json`, starter).
- [ ] Companion ranker model ship decision: bundle vs on-demand download (asset is optional + git-ignored
      in dev, docs/DEV_SETUP §7); iOS ranker actual (deterministic selector until then).
- [x] Notification scheduling (deferred from Phase 4): **landed with Phase 5** — opt-in twice over,
      off by default, ≥72h cadence + daytime window by construction (`NotificationPolicy`), phrasing
      only from the reviewed library's NOTIFICATION_PHRASING moment (SR-4). Copy review still gated
      by §4's library sign-off above.

## 4b. Companion (Phase 5, docs/05)
- [ ] **User-validation gate (docs/05 §8, release-blocking for wide release):** test with people in
      recovery via an ED charity/advisory relationship — kind company vs surveillance/pressure. If a
      meaningful subset finds it obstructive it ships MORE conservative (summon-only) or not at all.
- [ ] **Real-device QA (cannot be verified on the Linux dev host):** overlay 60fps active,
      **<~1%/hr idle battery**, suspend on fullscreen/immersive apps (insets signal), drag across
      OEM launchers; aggressive-battery OEMs (Xiaomi/MIUI, Samsung, Oppo) — degrade gracefully
      when the service is killed (docs/04 §6).
- [ ] Play policy review of the `specialUse` foreground-service subtype declaration + overlay
      permission explainer copy.
- [ ] Overlay/notification strings native-reviewed per language before that language ships
      (same sensitive-surface bar as companion lines, docs/12 §3).

## 5. Platform / build
- [ ] iOS targets compile + link on CI (`macos-14`); Xcode project embeds `Shared.framework`.
- [ ] Full app QA on Android (and iOS once wired): Flow C reachable ≤2 taps from every screen, calm (no alarm-red), reduced-motion respected.
- [ ] Re-run the full non-negotiables review (CLAUDE.md ⛔ list) against shipping copy + UI.

## 6. Server, account & sync privacy review (Phase 6 — docs/08 §1–2, docs/00 decisions #10/#11)

### 6a. Server data inventory (re-verify against code before every server release)

What the server **holds** — the complete list; anything appearing beyond it is a defect:

| Data | Where | Notes |
|---|---|---|
| Account record | accounts store (file/dev) | `accountId`, **optional** email, PBKDF2-SHA256 password hash (600k, per-salt), createdAt |
| Session tokens | **memory only** | opaque 32-byte random; revocable; never written to disk |
| Recovery tokens | **memory only** | single-use, 30-min TTL; email path restores **login only, never the data key** |
| Sync blob | blob store, 1 per account | **ciphertext only** — sealed on-device under a key the server never sees |

What the server **can never see** (structural, each covered by a test):
- Plaintext journal/log/AI content — no content repository exists in the codebase; the AI relay is
  stateless by construction (`server statelessness` test proves marker text never lands in the data dir).
- The backup data key, passphrase, or recovery code — only wrapped/derived material crosses the wire
  (`ServerBackupManagerTest` proves the upload contains no readable content).
- Vendor AI keys on the device / user identity at the vendor — the relay strips auth before forwarding;
  the vendor sees Aspen's server, not the person.

### 6b. Before any hosted deployment (Phase 6.9 — none exists today; release builds ship with **no server URL**)
- [ ] Hosting decision recorded (region/jurisdiction, provider) + TLS termination; the debug-only
      cleartext manifest **must not** exist in release (it lives in `androidApp/src/debug/` — re-verify).
- [ ] Production store: file stores replaced behind the existing repository ports; verify sessions/recovery
      tokens **remain memory-only** and blobs remain ciphertext-only after the swap.
- [ ] Real mail delivery for email recovery (undifferentiated "if that address is known to us" copy —
      no account enumeration); mail provider sees email + timestamp only, nothing else.
- [ ] Rate limits re-tuned for production; verify denials stay calm/undifferentiated.
- [ ] Server logs reviewed: no request bodies, no tokens, no emails at INFO; retention set.
- [ ] Delete-means-delete verified end-to-end on the production store (FR-11): account row, blob, and
      sessions all gone; a re-registered identical email starts empty.
- [ ] AI provider env (`ASPEN_AI_PROVIDER/BASE_URL/MODEL/KEY`) set via secret manager — key appears in
      no repo, image, or log; vendor DPA/zero-retention terms reviewed for the chosen provider.
- [ ] Advisor review of the *account/backup* user copy (key model honesty: "we can't read or reset this").

### 6c. Device QA for account + backup surfaces (manual, against `./gradlew :server:run`)
- [ ] TalkBack pass: create/sign-in errors and backup outcomes are **announced** (live regions);
      password/email fields present correct IME; recovery-code dialog readable + copyable.
- [ ] Recovery-code dialog: shown exactly once; dismiss = acknowledged (no trap); code restores on a
      second device/emulator, including sloppily-typed (case/dashes/spaces).
- [ ] Passphrase restore on a fresh install; wrong secret gives the calm error, no lockout spiral.
- [ ] Sign-out/offline: cloud reflection degrades to Unavailable quietly; nothing gates on the account.
- [ ] Turn-off deletes the server copy (verify blob gone from server data dir) and leaves on-device
      writing untouched.
- [ ] Touch targets ≥48dp on the new rows (AspenCard defaults — re-measure once on-device).

---

_When a locale/platform clears its section, flip 🟡 → ✅ here and in `docs/STATUS.md`._
