# 07 — Development Plan & Timeline

> A phased plan from research to a verified v1, the Claude-Pro-assisted workflow, and a realistic timeline. This follows your pipeline — **Research → Plan → Approve → Implement → Debug/Refactor → Verify → Commit** — applied per feature, not just once.

---

## 1. Working model

**Assumptions** (now confirmed):
- Team: **you (primary dev) + one dev partner** (both Claude Pro), **+ one non-technical person**. The non-technical teammate owns the **trust-and-safety operational layer**: crisis-registry curation/verification per region, copy review against `01` §6, clinical-advisor coordination, directory curation (Tier 1), beta/community ops. This is real, important work — not filler.
- **Stack:** **KMP + Compose Multiplatform** (`04` ADR-001). **Build Android first**, but in CMP from day one with the iOS target compiling in CI, so iOS is port-and-polish, not rewrite.
- **AI:** two-tier — local (companion/notifications, curated+personalised) + explicit-consent cloud (`04` ADR-003).
- **Regions:** PK/DE/UK at launch, US later; US + Pakistan clinical advisors review.
- No hard deadline, so the timeline is **gate-based**: phases advance on *done-and-verified*.

**The per-feature loop** (applies inside every phase):
```
Research → Plan → [APPROVE] → Implement → Debug/Refactor → Verify → Commit
```
The two **[APPROVE]** gates that need a human (you) are: (1) this whole documentation set, and (2) the design/spec for each major feature before its implementation. Everything else the loop handles continuously.

---

## 2. Phases (dependency-ordered; each has a Definition of Done)

### Phase 0 — Approve foundation *(this deliverable)*
- **Do:** review docs 00–12; settle the `00` [DECISION NEEDED] items (launch region, team, AI posture, identity).
- **DoD:** docs signed off; decisions recorded; backlog seeded.
- **Gate:** ← you are here.

### Phase 1 — Skeleton & design system (KMP + CMP)
- **Do:** KMP/CMP module skeleton (`04` §4) with **iOS target compiling in CI from day one**; `:core-design` tokens + CMP theme; shared navigation shell; empty Calm Home; CI (build for both targets, lint, test) **including the forbidden-token copy lint (`06` §6.4)**.
- **DoD:** Android app launches to Calm Home; iOS target builds (even if unpolished); tokens resolve; CI green; copy-lint active.

### Phase 2 — Safety subsystem **+ consent primitive** *(both early, on purpose)*
- **Do:** `:domain` safety engine + crisis resolver; `:data:crisis` region registry (**PK/DE/UK**, US-ready) + offline cache; Flow C UI; trusted-contact; **the scoped/revocable consent primitive (`08` §3)** even with no recipients yet; release-gating checks.
- **DoD:** crisis routing works **offline**, region-correct (advisor-verified per region), ≤2 taps from anywhere; consent primitive in place and tested; highest test coverage in the app.
- **Why both early:** safety is highest-stakes; the consent primitive built now means clinician-linkage later is "issue a grant," not a refactor.

### Phase 3 — Onboarding questionnaire + Grounding + Reflection/Logging
- **Do:** Flow 0 adaptive questionnaire → support profile (`01` §5a); Flow A grounding tools (offline, 60fps); Flow B reflection + **numberless adaptive logging** (profile-driven suppression rules); Room/SQLDelight + encryption, Keystore/Keychain (`expect/actual`), PIN/biometric, export/delete.
- **DoD:** questionnaire infers profile and drives tool/logging visibility; tools run offline at 60fps; logging is numberless and adaptive; reflections encrypted; delete permanent; a11y passes (incl. **iOS a11y check**).
- **[APPROVE]** questionnaire question-set + profile→behaviour mapping + logging-per-disorder rules **reviewed by clinical advisors** before enabling (`01` §5/§5a).

### Phase 4 — AI tiers (local companion/notifications + cloud reflection)
- **Do:** `:data:ai-local` (curated companion-message library + local personalisation/notifications — `04` ADR-003); `:data:ai-cloud` Claude client + output guard (SR-3) + system prompt + explicit-consent/brief-warning UX; **red-team suite**; graceful offline.
- **DoD:** red-team suite passes (no eating advice / numbers / appearance / crisis-mishandling escapes either tier); cloud off by default; companion words come from approved library; offline degradation clean.
- **[APPROVE]** AI behaviour/prompt/guard + companion library reviewed (with clinical advisor).
- **Scope note (approved 2026-07-02):** notification *phrasing* selection ships in Phase 4
  (`NOTIFICATION_PHRASING` moment in the library); notification *scheduling/plumbing* is deferred —
  notifications are off by default (FR-8) and the delivery mechanics land with the companion work in
  Phase 5. The cloud client is compiled + tested but **not live-wired** (no endpoint/key anywhere);
  the endpoint/proxy decision is deferred to save cost — tracked in `PRE_SHIP_VERIFICATION.md`.

### Phase 5 — Companion (in-app shared + Android overlay)
- **Do:** **in-app companion in shared `:ui`** (works Android + iOS) as the baseline; **`:companion-overlay-android`** system overlay (`SYSTEM_ALERT_WINDOW`, **no AccessibilityService**, Android-only) as enhancement; ambient/playful/suspend states; drag/tap/dismiss; fullscreen-suspend; permission-explainer; battery/frame budgets (`04` §6).
- **DoD:** in-app companion works on both platforms; Android overlay holds 60fps active / **<~1%/hr idle**, suspends on fullscreen, one-tap dismiss, works-without-permission path verified, tested on aggressive-battery OEMs; **iOS gets in-app companion only (documented platform limit).**
- **[APPROVE]** companion spec before build; **user-validation gate** (`05` §8) before wide release.

### Phase 6 — Accounts, optional cloud sync, hardening, a11y, privacy
- **Do:** app-native accounts + layerable auth (`08` §1); optional cloud sync with chosen key model (`08` §2); full a11y audit (**Android + iOS**); privacy review; performance gating; copy review.
- **DoD:** anonymous still fully works; account/auth flows pass; key-model disclosed plainly; WCAG 2.2 AA-equivalent on both platforms; privacy review signed off.

### Phase 7 — Clinical review, closed beta, Android launch
- **Do:** clinical/advisor review (US + PK) of all safety flows, questionnaire, logging rules, copy; closed beta **with people in recovery** (companion + questionnaire + logging validation); fix; Play Store listing (honest positioning); donation surface.
- **DoD:** advisor sign-off; beta feedback addressed; companion/logging validated or shipped conservative; public Android v1.

### Phase 8 — iOS polish & launch
- **Do:** iOS-specific UI/native-feel polish, **iOS accessibility pass** (CMP's weaker spot), App Store positioning; in-app companion (no overlay).
- **DoD:** iOS app meets the same a11y/safety bars; App Store v1.

### Later (separate approvals)
- **Tier 1 directory** → **Tier 3 link-your-own-clinician** (consent-gated, `08` §4).
- **Tier 2 affiliated telehealth** — *its own phase with legal + clinical-governance design* (`08` §4); not bolted onto the app.
- Text-only community **only when moderation is funded/staffed** (`03` B.2).

---

## 3. Timeline (relative, gate-based)

Since there's no fixed deadline, this is expressed as **relative effort bands** for the two-part-time-engineer + Claude-Pro assumption. Treat as planning aid, not commitment; solo ≈ ×1.6; full-time ≈ ×0.5.

| Phase | Relative effort | Notes |
|---|---|---|
| 0 — Approve foundation | ~days | review + decisions |
| 1 — Skeleton & design system (KMP+CMP) | ~2–3 weeks | dual-target scaffolding, CI, tokens |
| 2 — Safety + consent primitive | ~3–4 weeks | highest-rigour; per-region verification; don't rush |
| 3 — Onboarding + Grounding + Reflection/Logging | ~4–6 weeks | questionnaire + adaptive logging needs advisor gate |
| 4 — AI tiers | ~2–3 weeks | mostly guard + library + red-team |
| 5 — Companion (in-app + Android overlay) | ~3–5 weeks | deepest; OEM testing; in-app is shared |
| 6 — Accounts/sync + hardening/a11y/privacy | ~3–4 weeks | both-platform a11y; key-model UX |
| 7 — Clinical review + beta + Android launch | ~3–4 weeks + advisor/beta calendar | gated by external people |
| 8 — iOS polish & launch | ~3–5 weeks | CMP iOS native-feel + a11y pass |

**Rough total:** ~**6–9 months** part-time to a verified Android v1 (Phases 0–7), with iOS following (Phase 8). The growth vs. the earlier estimate reflects the added scope you confirmed (questionnaire, adaptive logging, accounts/consent, committed iOS). The calendar tail is still set mostly by **clinical review and user validation** — correctly, for this product. Solo ≈ ×1.6; the non-technical teammate offloads trust-and-safety ops, which materially helps.

---

## 4. The Claude-Pro-assisted workflow (how to actually run this)

You're using Claude (Pro, 1–2 accounts) as the primary implementation partner. A way to run it that matches your pipeline:

- **One feature in flight at a time**, run through the loop. Keep the relevant spec doc (02–06) as the source of truth in context.
- **Research/Plan** with Claude → you **[APPROVE]** → **Implement** in focused sessions → **Debug/Refactor** → **Verify** against the Definition of Done → **Commit**.
- **Keep the non-negotiables (`01` §6) and the relevant SR-* requirements pinned** in any implementation session so generated code/copy can't drift into a forbidden pattern. The CI copy-lint is the backstop.
- **The safety engine, crisis registry, and AI output guard get extra review** — these are the release-gating pieces; treat AI-generated code there as a draft to scrutinise, not to trust.
- Two accounts → parallelise *independent* modules (e.g. one on grounding tools, one on the crisis registry) since the module boundaries (`04` §4) are clean.
- **Verification is human-owned.** Claude can write tests; *you* decide a gate is met — especially for anything safety-bearing.

---

## 5. Definition of "v1 done"

- Calm Home, Grounding tools, Reflection notebook — all offline, encrypted, a11y-pass, 60fps.
- Safety subsystem — region-correct, offline, ≤2 taps, release-gate verified, **never NEDA**.
- AI companion — optional, local-first, output-guarded, red-team-passed, off by default.
- Overlay companion — optional, off by default, battery/frame budgets met, overlay-only permissions, validated with users (or shipped conservative).
- No food/weight/calorie surface anywhere (structurally absent).
- Honest store positioning (support, not treatment); donation surface non-intrusive; privacy review + clinical sign-off complete.

---

## 6. Risk register (top items)

| Risk | Severity | Mitigation |
|---|---|---|
| Crisis flow wrong/stale resource | **Critical** | region registry + per-release verification checklist; release-blocking; advisor-curated |
| AI emits eating advice / numbers / appearance | **Critical** | output guard + system prompt + red-team suite; release-blocking |
| Companion becomes intrusive/dependency | High | `05` guardrails; off-by-default; user validation gate |
| OEM kills overlay / battery complaints | Medium | graceful degradation; OEM testing; budgets |
| "No logging" → low conventional engagement | Medium (expected) | engagement isn't the success metric; donation/grant funding model (`02` §6, `03` B.3) |
| Solo-dev capacity / scope creep | Medium | gate-based phases; community/iOS explicitly deferred |
| Positioning drifts toward "treatment" claims | High (legal/ethical) | disciplined copy; advisor review; no efficacy claims |
