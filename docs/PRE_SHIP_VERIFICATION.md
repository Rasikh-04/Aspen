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
- [ ] Durable on-disk `ConsentBlobStore` in place (currently in-memory only).
- [ ] Koin started at platform entries; localized `SafetyFallbackCopy` bound; trusted-contact capture + contact dialling wired.

## 4. Platform / build
- [ ] iOS targets compile + link on CI (`macos-14`); Xcode project embeds `Shared.framework`.
- [ ] Full app QA on Android (and iOS once wired): Flow C reachable ≤2 taps from every screen, calm (no alarm-red), reduced-motion respected.
- [ ] Re-run the full non-negotiables review (CLAUDE.md ⛔ list) against shipping copy + UI.

---

_When a locale/platform clears its section, flip 🟡 → ✅ here and in `docs/STATUS.md`._
