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

---

_When a locale/platform clears its section, flip 🟡 → ✅ here and in `docs/STATUS.md`._
