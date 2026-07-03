# Aspen — Documentation Suite (Rebuild)

> **Status:** Research & Planning phase — *for approval before implementation.*
> **Pipeline:** Research → Plan → **Approve** → Implement → Debug/Refactor → Verify → Commit
> **This deliverable covers the first two stages only.** Nothing here is final until you sign off.

---

## What this suite is

The original Aspen documents (a web-first React + Supabase prototype produced on a Startup Weekend timeline) have been **set aside**. This suite starts from the problem, not from the old structure. The old docs were read only to recover intent; none of their architecture, stack, or screen decisions carry forward.

Everything here is grounded in cited sources where it makes a factual claim. The research file lists every reference. Where I have made an assumption to keep moving, it is marked **[ASSUMPTION]**. Where a decision genuinely needs you, it is marked **[DECISION NEEDED]** and collected at the end of this index.

---

## Reading order

| # | Document | What it answers |
|---|----------|-----------------|
| 01 | `01_RESEARCH_AND_CLINICAL_FOUNDATION.md` | What the problem actually is, what the evidence says, and the non-negotiable design rules that fall out of it. The case study + references live here. |
| 02 | `02_MARKET_ANALYSIS.md` | Who else is in this space, what they do, where they fail recovering users, and where Aspen sits. |
| 03 | `03_REQUIREMENTS_AND_FEASIBILITY.md` | Functional, non-functional, and safety requirements; technical/operational/financial feasibility of a free, donation-funded app. |
| 04 | `04_TECH_STACK_AND_ARCHITECTURE.md` | The Android-first stack decision (native Kotlin core, why not React Native), system architecture, on-device-first data model, and the performance/battery strategy. |
| 05 | `05_COMPANION_DESIGN.md` | The screen-overlay companion ("desktop pet for the phone") — interaction model, overlay architecture, and the safety guardrails that keep it from becoming harmful. |
| 06 | `06_UI_UX_AND_SAFETY_SPEC.md` | Design system, the three core flows, accessibility, and the crisis/safety architecture as a first-class subsystem. |
| 07 | `07_DEVELOPMENT_PLAN_AND_TIMELINE.md` | Phased plan, milestones, the Claude-Pro-assisted workflow, and a realistic timeline for a small team. |
| 08 | `08_IDENTITY_LINKAGE_AND_CONSENT.md` | Account/identity model, the encryption-key trade-off, and the consent-gated clinician-linkage roadmap (planned now so future features don't force breaking changes). |
| 09 | `09_PHASE2_SAFETY_AND_CONSENT_SPEC.md` | Implementation-ready Phase 2 spec: safety subsystem + consent primitive. |
| 10 | `10_CRISIS_REGISTRY_DRAFT.md` | **Draft** crisis/support resources per region (UK/DE/US/PK + worldwide fallback). Anchor orgs grounded in research; every number `⚠VERIFY` for advisors. |
| 11 | `11_ONBOARDING_QUESTIONNAIRE_DRAFT.md` | **Draft** disorder-adaptive onboarding questionnaire — numberless, personalises-not-diagnoses, with support-profile scoring. For clinical sign-off. |
| 12 | `12_LOCALIZATION.md` | First-class i18n requirement: 7 languages, RTL from day one, per-language review of sensitive surfaces. Wired so it can't drift. |
| 13 | `13_TEAM_ROLES_AND_RESPONSIBILITIES.md` | Two-developer ownership split (Phase 3 on), with the non-negotiable per-PR Definition of Done and a rigorous, symmetric review standard. |
| 14 | `14_GIT_WORKFLOW.md` | Branch → PR → merge-to-main flow, with Linux/Windows cross-platform handling (line endings, paths, gradlew). |

---

## The one-paragraph version

Aspen is a **free, text-first, Android-first (iOS to follow) between-session support app** for people living with eating disorders. It opens with a **gentle, adaptive questionnaire** that tailors the whole app to the person — because no two presentations are managed alike. It deliberately carries **no numbers** — no calories, weight, BMI, or macros — anywhere, because the evidence shows numbers are what the disorder recruits. Optional, **numberless** food/feeling logging exists as gentle self-knowledge (suppressible for presentations where it's risky), but the core is being a calm place to be a person between appointments: hold a thought, get a grounding exercise, reach a real human or a region-correct crisis line fast, and — optionally — have a small animated companion offer low-pressure presence. The AI is a supportive notebook and a curated, personalised companion voice, **not** the product and **not** a therapist. Specialist treatment is always the foreground; Aspen is the quiet room between sessions.

---

## Cross-cutting principles (apply to every document)

1. **Do less, not more.** For this population, "more features that engage with food/body" is a harm vector, not a value-add. The product earns trust by restraint.
2. **Safety is a subsystem, not a screen.** It is always-on, isolated, and cannot be broken by a feature change elsewhere.
3. **On-device first.** The most sensitive thing a user can do is write down a disordered thought. That should be able to live and stay on their phone, encrypted, with the cloud optional.
4. **No numbers, no bodies, no comparison, no streaks.** No calories, weight, BMI, macros, or portion counts *anywhere* — this is the actual harm vector and it is non-negotiable. (Numberless, optional, adaptive food/feeling logging *is* allowed; the line is at *numbers*, not at *logging* — see `01` §5.) No appearance comments in any direction. These are hard constraints, enforced in code and copy, not style preferences.
5. **The app is a bridge to humans, never a replacement for them.** Every flow has an exit toward real support.

---

## Decisions — resolved (this round)

1. **Regions:** Pakistan, Germany, UK at launch; USA when ready. Clinical advisors in US + Pakistan will review. (`06` §6.1 has the per-region anchor orgs, all advisor-verify-before-ship.)
2. **Team:** you (primary dev) + 1 dev partner (both Claude Pro) + 1 non-technical (owns trust-and-safety ops). (`07`)
3. **Stack:** **Kotlin Multiplatform + Compose Multiplatform** — native Android perf, ~90% shared logic+UI, committed iOS path. Android first, iOS target in CI from day one. (`04` ADR-001) — *recommended; confirm.*
4. **AI:** two-tier — on-device local (companion/notifications, curated+personalised) + explicit-consent cloud (Claude API) with brief warning. (`04` ADR-003)
5. **Identity:** anonymous by default; optional Aspen-native account; layerable auth; encrypted export. (`08`)
6. **New — adaptive onboarding questionnaire:** personalises (does not diagnose); drives the whole app. (`01` §5a, `06` Flow 0)
7. **New — numberless meal/feeling logging:** optional, non-enforced, adaptive, **no numbers**. (`01` §5, `03` FR-3b, `06` Flow B)
8. **New — clinician linkage (3 tiers), consent-gated:** directory → link-your-own → (separate future phase) affiliated telehealth. (`08` §4)

## Decisions — all confirmed ✅

1. **Stack:** Kotlin Multiplatform + Compose Multiplatform. Overlay companion is Android-only; **in-app companion is the cross-platform baseline** (works on iOS).
2. **Logging:** numberless, disorder-tailored, model-personalised in-app; other log types may be added later **only if doctor + patient reviews align**. Adaptive by design.
3. **Companion v1:** curated message library; the model **personalises** those approved messages (doesn't generate new content).
4. **Encryption:** **true E2E** for cloud (server stores ciphertext it can't decrypt); export/import gated by **auth-only verification**. *Open sub-decision: recovery mechanism (recovery code / device transfer / both) — `08` §2.*
5. **Telehealth (Tier 2):** explicitly-later, separately-governed phase, gated on USA healthcare-contact confirmation.
6. **Clinical sign-off:** everything manually checked and signed off by advisors before shipping; questionnaire + logging tailored as carefully as possible.
7. **Localization (NEW):** 7 languages from first release — **English (default/worldwide), Urdu, German, Mandarin, Hindi, Arabic, Spanish** — auto-selected by system default. First-class requirement, RTL from day one. (`docs/12`)
8. **Crisis registry & questionnaire drafts (NEW):** initial research-grounded drafts produced (`docs/10`, `docs/11`) — numbers `⚠VERIFY`, questionnaire pending clinical sign-off; both to be localised + culturally adapted per region.
9. **Phase order update (2026-07-03):** Phase 5.5 (companion refinement, incl. Phase-5 leftouts) **postponed until after Phase 6**; new **Phase 6.6 — UI design pass** (team's targeted fix list) added between Phase 6 and Phase 5.5/7. (`07` §2)

## One open sub-decision

- **E2E recovery mechanism** — recovery code vs. device-to-device key transfer vs. both (`08` §2). The only real UX consequence of choosing E2E; better decided before build.

## Next: Phase 2 spec is ready

`09_PHASE2_SAFETY_AND_CONSENT_SPEC.md` — the implementation-ready spec for the safety subsystem + consent primitive — is the bridge into development. Scaffold against it.

## Two things I pushed back on (so they're not buried)

- **Food logging:** I revised the docs to *include* it per your decision, but held a hard line: **numberless only** (no calories/weight/portions/macros — that's the actual harm vector in the evidence), optional, no streaks/shame, and **suppressible per disorder type** via the questionnaire. This needs clinical sign-off per presentation. (`01` §5)
- **iOS overlay companion is impossible** — not a stack choice; iOS forbids system-wide overlays in *any* framework. The companion's **in-app form is the cross-platform baseline**; the roam-the-screen overlay is an **Android-only enhancement**. (`04` ADR-001, `05`)
