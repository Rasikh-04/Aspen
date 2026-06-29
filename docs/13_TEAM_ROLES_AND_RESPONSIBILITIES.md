# 13 — Two-Developer Roles & Responsibilities

> Phase 2 (safety subsystem + consent primitive) was completed **solo** by Dev A. From **Phase 3 onward** the work is split between two developers. This document divides ownership in detail and defines the **non-negotiable completion checks** that every piece of work — from either developer — must pass. Time is **not** a constraint here; quality is. No allowance is made for poor work from either side, including the lead's.

> Reads alongside `docs/07` (phased plan) and runs every feature through the pipeline: **Research → Plan → Approve → Implement → Debug/Refactor → Verify → Commit.**

---

## 1. People & machines

| | **Dev A** | **Dev B** |
|---|---|---|
| Role | Lead / architecture authority | Feature developer |
| OS | Linux (Fedora) | Windows |
| Builds | Android + shared (JVM/Android) targets | Android + shared (JVM/Android) targets |
| iOS targets | Declare-only (needs macOS/CI to compile) | Declare-only (needs macOS/CI to compile) |
| Claude Pro | Yes | Yes |

> **iOS reality (`docs/04` ADR-001):** neither machine can compile `iosArm64`/`iosSimulatorArm64`. iOS assembly is a **macOS-CI concern** (Phase 8). Both devs keep the iOS targets in the build so code stays cross-platform-honest; neither "owns" iOS polish until a Mac/CI runner exists. A non-technical third teammate owns trust-and-safety **content** ops (crisis registry verification, copy/translation review coordination) — not code.

---

## 2. Ownership model — own modules, share contracts

The architecture (`docs/04` §4) has clean module boundaries. We exploit them: **each dev owns whole modules; nobody edits the other's module without a PR to that owner.** What is *shared* are the **contracts** (interfaces, design tokens, navigation, data models) — and contracts are owned by the lead, changed only via an approved PR, so the two work streams never collide on a moving definition.

### Dev A (lead) owns
- **`:shared:domain`** — use cases, the **safety engine**, **consent primitive**, entities, the **profile→behaviour mapping** (the rules that drive adaptivity). These are the contracts everything depends on.
- **`:shared:data`** — `:local` (encrypted store, `expect/actual` keys), `:ai-local` + `:ai-cloud` (the **AI output guard** is high-risk), `:crisis` registry mechanism.
- **`:companion-overlay-android`** — Android-only overlay service, OEM/battery testing (`docs/05`).
- **`:androidApp`** wiring, DI, navigation host.
- **CI, release gates, version catalog** (copy-lint per language, crisis-freshness, NEDA-deny — `docs/09` §2.5, `docs/12`).
- **Merge authority to `main`** (but A's own PRs still require B's review — §5).
- Architecture decisions; contract changes; final say on safety-bearing code.

### Dev B (feature dev) owns
- **`:shared:ui` feature screens** (Compose Multiplatform): onboarding questionnaire UI, grounding tools, reflection + logging screens, settings, the **in-app companion** UI.
- **Localization plumbing & wiring** (`docs/12`): string externalisation, RTL layout correctness, locale provider consumption, per-screen pseudo-localization/overflow checks.
- **Test authoring for B-owned modules** (UI/interaction/snapshot/RTL).
- **Design-system consumption** from `:core-design` (B uses tokens; A owns the tokens).

### Shared, lead-governed (change = lead-approved PR)
`:core-design` tokens · navigation contract · `:shared:domain` interfaces · data models · the non-negotiables in `CLAUDE.md`. If B needs a new token, interface, or nav route, B opens a small PR against it; A reviews fast. **Nobody silently edits a shared contract.**

> **Collision rule:** at most **one feature per dev in flight**, in **separate modules**. If two pieces of work would touch the same file, they are sequenced, not parallelised.

---

## 3. Phase-by-phase split (Phases 3–8, from `docs/07`)

| Phase | Dev A (lead) | Dev B |
|---|---|---|
| **3 — Onboarding + Grounding + Reflection/Logging** | `:domain` profile/logic + logging suppression rules; encrypted store integration; review B's UI PRs | Questionnaire UI, grounding tools UI, reflection + numberless logging screens; wire to A's use cases; tests |
| **4 — AI tiers** | `:ai-cloud` client + **output guard** + system prompt; `:ai-local` curated library plumbing; red-team suite | AI/reflection **UI**, consent/warning screens, settings toggles; offline-degradation UX |
| **5 — Companion** | **Android overlay** service, states, OEM/battery budgets | **In-app companion** (shared, both platforms): art integration, drag/tap/dismiss, ambient states |
| **6 — Accounts/sync + hardening** | App-native accounts, layerable auth, **E2E** sync + recovery flow (`docs/08`), CI gating | a11y audit (both platforms), localization completion + RTL screenshot tests, privacy-UX review |
| **7 — Clinical review + closed beta + Android launch** | Build/release pipeline, gate enforcement, fixes on safety/AI/overlay | Beta build packaging, copy/translation integration, UI fixes from beta feedback |
| **8 — iOS polish & launch** | (with Mac/CI) iOS-specific platform glue, App Store build | (with Mac/CI) iOS UI/native-feel + iOS a11y pass |

Rule of thumb: **A owns logic, safety, platform-native, and infra; B owns shared UI, localization, and feature presentation.** Tests are owned by whoever owns the module under test; cross-review is mandatory regardless.

---

## 4. Definition of Done (every PR, both devs — non-negotiable)

A change is **not done** until *all* of the following are true. The author asserts them in the PR; the reviewer independently verifies them. Missing any one = the PR does not merge.

**Build & test**
- [ ] Builds **Android + shared** targets locally; iOS targets at least *configure* without error.
- [ ] All tests pass. New logic has tests. **Safety/consent code: no-compromise coverage** (untested branches are blocking — `docs/09` §4).
- [ ] No new warnings introduced (or each is justified in the PR).

**Non-negotiables (`CLAUDE.md`) — in code *and* copy**
- [ ] No numbers about food/body anywhere (incl. logging, onboarding, AI output, notifications).
- [ ] No appearance comments; no streaks/scores/pass-fail; no comparison; no shame/failure language; no alarm-red.
- [ ] Every flow keeps a human exit ≤2 taps; crisis routing intact, region-correct, never NEDA.
- [ ] AI stays a notebook (no eating advice/diagnosis); questionnaire personalises, never diagnoses.

**Gates (CI — `docs/09` §2.5, `docs/12`)**
- [ ] Copy-lint passes **per language** (forbidden number/shame/appearance tokens in every shipped language).
- [ ] Crisis-freshness gate green; NEDA-deny green.
- [ ] No hardcoded user-facing strings; **RTL not regressed** (Urdu/Arabic).
- [ ] a11y not regressed (dynamic type, screen reader, reduced-motion).

**Hygiene**
- [ ] Conventional-commit messages; PR description states what/why + which `docs/` spec it implements.
- [ ] `docs/STATUS.md` updated.
- [ ] No secrets, keys, or real crisis numbers committed (registry stays `TODO-VERIFY` until advisor-signed).

---

## 5. Review standard — rigorous, symmetric, no edge given

> The point the brief insists on: **neither developer gets a pass on quality — including the lead.** The reviewer is not a rubber stamp.

- **Every change goes through PR review by the other developer.** No direct pushes to `main`. **No self-merge** — even A's PRs require B's approval before A merges them.
- **The reviewer must actually run it**, not just read the diff: pull the branch, build, run the tests, exercise the changed flow, check the gates. Approval means "I verified this," not "looks fine."
- **The reviewer rejects** (requests changes) for, at minimum: a failing/absent test on new logic; any non-negotiable violation in code or copy; a hardcoded string; a contract changed without lead approval; missing DoD items; unclear/oversized PR; copy that's clinically off-tone.
- **Safety/AI/consent/crisis code gets a second, slower pass** — treat generated code there as a draft to scrutinise, never to trust (`docs/09` §0, `CLAUDE.md`).
- **Disagreements** on contracts/architecture: lead decides, but must record the rationale (an ADR in `docs/04` or a PR note). Disagreements on *quality*: the stricter view wins.
- **Small PRs.** One feature, reviewable in one sitting. Big-bang PRs are themselves a reject reason — they defeat real review.
- **No "I'll fix it later" merges.** It's green and complete, or it doesn't go in. Time is not the constraint; correctness is.

---

## 6. Cadence & coordination (no fixed timeline)

- **Sync at phase boundaries** (and on any contract change): agree the module split for the phase, confirm interfaces, then split.
- **`docs/STATUS.md`** is the async source of truth (boot-resume); update at end of each session so the other dev resumes cold. Pair with `ecc:ck` for memory (`docs/DEV_SETUP.md`).
- **Each dev: one feature in flight**, run fully through the pipeline before taking the next.
- **Gates are the timekeeper.** A phase advances when its Definition of Done is met across all its PRs — not on a date.

---

## 7. Escalation / blockers

- **Blocked on a contract** → ping the lead; lead turns the contract PR around quickly (it unblocks B).
- **Blocked on content** (crisis numbers, translations, advisor sign-off) → that's the non-technical teammate + advisors; build the *mechanism* with `TODO-VERIFY` placeholders and proceed (`docs/10`, `docs/11`).
- **Blocked on iOS** → declare-only locally; defer assembly to Phase 8 / macOS CI.
- **Quality dispute** → stricter standard wins; if it's architectural, lead decides on record.
