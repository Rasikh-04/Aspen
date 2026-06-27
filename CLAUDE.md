# CLAUDE.md — Aspen

> Read this first, every session. It is the source of truth for *how we build Aspen*. The `docs/` folder is the source of truth for *what* we build. When in doubt, the non-negotiables below win over everything — speed, cleverness, my own suggestions, all of it.

---

## What Aspen is (one paragraph)

Aspen is a **free, Android-first (iOS to follow) between-session support app for people living with eating disorders**. It opens with a gentle, adaptive questionnaire that tailors the app to the person, then is a calm place to get through hard moments: grounding tools, private reflection, optional numberless logging, a fast route to real human help, and an optional animated companion. The AI is a supportive notebook + a curated companion voice — **not a therapist, not the product**. Specialist treatment is always the foreground. Success = the user closing the app feeling steadier, **never** engagement/session-length/DAU.

---

## ⛔ NON-NEGOTIABLES (hard constraints — enforced in code, copy, and review)

These are not style preferences. A violation is a release-blocking defect. If a task seems to require breaking one, **stop and flag it** — do not "reframe" it to make it fit.

1. **No numbers about food/body — ever.** No calories, macros, BMI, weight, portion counts, "goal weight," numeric meal frequencies. *Anywhere* — UI, logging, onboarding, AI output, notifications. Food/feeling logging is **qualitative text + feeling tags only**. There are **no numeric food/body columns in the schema** — keep it that way.
2. **No appearance comments, any direction.** Not from the companion, the AI, copy, or notifications. "You look healthy" can land as "you look fat." Just don't.
3. **No streaks, scores, ranks, or pass/fail states.** A broken streak is a relapse trigger. Progress is shown as *presence*, never as a metric that can be "broken." No green/red right/wrong.
4. **No comparison or social-feed mechanics.** No leaderboards, no "others like you," no follower counts, no visual food/body feeds.
5. **No shame/failure language in user copy.** Banned tokens: fail, failed, missed, incomplete, "you didn't." Empty days are silent. Caution states use soft amber — **never alarm red**, including on the crisis screen.
6. **Every flow has a human exit.** A route to a real person (trusted contact / region crisis line / treatment finder) is always ≤2 taps away and never trapped behind anything.
7. **Crisis resources are region-correct, current, offline-capable, and NEVER NEDA** (it's disconnected). US uses the **National Alliance for Eating Disorders** (clinician-staffed) + 988. No crisis content ships unverified by our advisors.
8. **The AI is a notebook, not an authority.** It validates and reflects. It never advises on eating/diet/exercise, never diagnoses, never claims to treat/cure, and on any crisis sign it does a **warm hand-off** to the safety flow + a human — it does not try to manage risk.
9. **Personalise, never diagnose.** The onboarding questionnaire infers an internal *support profile*; it never shows the user a clinical label and always routes toward real assessment.
10. **Do less, not more.** For this population, "more features that engage with food/body" is a harm vector, not value. When unsure, the calmer/quieter/smaller option is usually right.
11. **Localized from the start (`docs/12`).** No hardcoded user-facing strings — ever. **RTL-correct from day one** (Urdu, Arabic). 7 languages (en default, ur, de, zh, hi, ar, es). Sensitive surfaces — **crisis resources, questionnaire, companion lines** — require **native-speaker, ED-informed review per language**; machine translation of these is not acceptable. The copy-lint runs **per language** (forbidden number/shame words in every language). Language (UI) and region (crisis registry) are independent — never infer one from the other.

> If I ever catch myself softening one of these to satisfy a request, that's the signal to **stop and ask the human**, not to proceed.

---

## Stack & architecture (see `docs/04`)

- **Kotlin Multiplatform + Compose Multiplatform.** Android first; **iOS target compiles in CI from day one** so iOS is port-and-polish, not rewrite.
- **Shared-first:** ~90% of the app (logic + Compose UI) lives in `:shared`. Platform-specific only where unavoidable.
- **The overlay companion is Android-only** (iOS forbids system overlays). The **in-app companion is the cross-platform baseline**.
- **Safety + consent live in `:shared:domain`** — isolated so feature churn can't break them.
- Module map and `expect/actual` boundaries: `docs/04` §4; data model: `docs/04` §5.
- **Encryption:** local = device/user-held keys; cloud (opt-in) = **true E2E** (server stores ciphertext it can't decrypt; export/import verification is **auth-only, never decryption**). See `docs/08` §2.

### Patterns to follow
Clean architecture (dependencies point inward; `:shared:domain` knows nothing about Android/UI/network). MVVM + unidirectional state in UI. Coroutines/Flow with structured concurrency — **no busy loops** (battery). DI via the project's chosen container. Encrypted local store. Lean modules, testable in isolation.

---

## Workflow: research → plan → approve → implement → verify

Every non-trivial change goes through this. **The two human-approval gates are real — wait for them.**

1. **Research** — understand the problem; check the relevant `docs/` spec; search for existing patterns/libraries before writing anything. Output findings. *No code yet.*
2. **Plan** — propose the change against the spec: files to touch, the tests, the Definition of Done, risks. **STOP and get human approval.**
3. **Approve** — human gate. Don't skip.
4. **Implement** — TDD where it makes sense (tests first for safety/consent/logic). Honour the non-negotiables in every line of code *and copy*.
5. **Verify** — run the gates (below). Don't call it done until they're green. **Human owns the final "done."**

Safety-bearing code (safety engine, crisis registry, AI output guard, consent) gets **extra scrutiny** — treat my own generated code there as a draft to be reviewed, never trusted.

### Verification gates (must pass before "done")
- Builds for **both** Android and iOS targets.
- Tests green; **no-compromise coverage on safety + consent** (untested branches there are release-blocking).
- **Copy-lint** passes (no forbidden number/shame/appearance tokens in strings).
- **Crisis-freshness gate** passes (no supported-locale resource unverified/stale; "NEDA" anywhere fails the build).
- Reduced-motion / a11y not regressed.

---

## Where to look in `docs/`

- `00_INDEX` — map + decision log + open items.
- `01` — clinical foundation + the non-negotiables' *why* + references.
- `03` — requirements (FR/NFR/SR).
- `04` — stack, architecture, data model, perf/battery.
- `05` — companion (interaction + overlay + guardrails).
- `06` — UI/UX + safety subsystem + flows.
- `07` — phased plan + timeline.
- `08` — identity, E2E, consent, clinician-linkage roadmap.
- `09` — **Phase 2 spec (current build target): safety subsystem + consent primitive.**

---

## Current phase

**Phase 2 — safety subsystem + consent primitive** (`docs/09`). Build the spine before features. Scaffolding order in `docs/09` §6. Crisis registry *mechanism* now; *content* stays `TODO-VERIFY` until advisors sign it.

Keep `docs/STATUS.md` updated at the end of each working session (what's done, what's next, what's blocked) so any session — or my teammate — can resume cold.

---

## Team

Two devs (me + one partner, both on Claude Pro) + one non-technical teammate who owns trust-and-safety ops (crisis-registry curation/verification, copy review, advisor coordination). Don't generate real crisis numbers in code — that's an advisor-verified content task.
