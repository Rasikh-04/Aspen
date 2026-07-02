# DEV_SETUP — Claude Code + ECC for Aspen

> How the repo is wired for development, and how to run the **research → plan → approve → implement → verify** pipeline using Claude Code together with ECC. Adjust ECC skill/command names to match your actual ECC install — treat the mapping below as "use the ECC piece that does X," since ECC evolves.

---

## 1. What's in the repo for Claude Code

```
Aspen/
├── CLAUDE.md                  ← always-on rules: non-negotiables, stack, workflow, gates. Read every session.
├── .claude/
│   ├── settings.json          ← permissions / project settings (starter)
│   └── commands/
│       ├── research.md        ← /research <topic>
│       ├── plan.md            ← /plan <feature>   (stops for approval)
│       └── verify.md          ← /verify <change>  (runs the gates)
└── docs/                      ← the specs (00–09) + STATUS.md (boot-resume)
```

`CLAUDE.md` is the keystone — Claude Code loads it automatically and it pins the non-negotiables into every session so generated code/copy can't quietly drift into a forbidden pattern (a calorie field, a broken-streak message, an alarm-red crisis screen).

---

## 2. The pipeline, and where ECC plugs in

Your path is **research → plan → approve → implement → verify**. ECC's `orch-*` skills already implement a gated *Research → Plan → TDD → Review → Commit* pipeline with **two human gates** — which is essentially your path. So: use the lightweight `/research` `/plan` `/verify` commands as Aspen-flavoured entry points, and lean on ECC to do the heavy orchestration.

| Stage | Claude Code | ECC skill to lean on (match to your install) |
|---|---|---|
| **Research** | `/research <topic>` | `search-first` (find existing tools/patterns before coding), `documentation-lookup` (current lib docs), `research-ops` / `deep-research` |
| **Plan** | `/plan <feature>` → **stops for approval** | `blueprint` (multi-session construction plan) or the plan phase of `orch-add-feature`; `plan-orchestrate` |
| **Approve** | human gate (you) | ECC `orch-*` has the human gate built in — don't auto-advance |
| **Implement** | code in focused sessions | **`orch-add-feature`** (new capability) / **`orch-build-mvp`** (bootstrapping) / `orch-fix-defect` (bugs) — these run the gated TDD pipeline; plus pattern skills: **`kotlin-patterns`, `compose-multiplatform-patterns`, `android-clean-architecture`, `kotlin-coroutines-flows`, `kotlin-testing`** |
| **Verify** | `/verify <change>` | **`verification-loop`**, `tdd-workflow`; for safety code: **`security-review`**, **`safety-guard`** (block destructive ops), `gateguard` (fact-forcing gate before edits) |

**Cross-cutting ECC pieces worth turning on for Aspen:**
- **`ck`** — persistent per-project memory; auto-loads context on session start, tracks sessions with git activity. Pairs with `docs/STATUS.md` for boot-resume.
- **`strategic-compact`** — manual compaction at phase boundaries so you don't lose context mid-feature.
- **`cost-tracking`** — you're on 1–2 Pro accounts; track burn by phase.
- **`safety-guard` / `gateguard`** — guardrails on autonomous edits; especially valuable on the safety/consent modules.

---

## 3. Recommended loop (per feature)

```
1. /research <feature>          → findings; confirm the spec + non-negotiables touched
2. /plan <feature>              → plan + tests + DoD; STOPS
3. (you) approve or adjust
4. implement                    → drive ecc:orch-add-feature (gated TDD); honour CLAUDE.md every line
5. /verify <feature>            → run all gates; fix/flag failures
6. (you) sign off → commit
7. update docs/STATUS.md        → so the next session/teammate resumes cold
```

For **Phase 2 specifically**, follow the scaffolding order in `docs/09` §6. Safety + consent are the modules where you do **not** compromise on test coverage — let `verification-loop` / `tdd-workflow` enforce it, and give the safety engine / crisis registry / consent code extra human review (treat generated code there as a draft).

---

## 4. Two-person, two-account setup

- **You:** Phase 1 (skeleton + CI both targets) → Phase 2 spine (`docs/09`). Don't bring your partner on until the shared safety+consent foundation is green — everything leans on it.
- **Partner (second Pro account):** joins at **Phase 3** (onboarding + grounding + reflection/logging), building against the shared foundation. Module boundaries (`docs/04` §4) are clean, so independent work parallelises without collisions.
- **Non-technical teammate:** owns the crisis-registry **content** (advisor-verified), copy review against the non-negotiables, and advisor coordination — in parallel with your mechanism work. Code generates the registry *machinery*; humans fill *verified numbers*.
- Use `ck` + `STATUS.md` so both accounts share a coherent picture of state.

---

## 5. Guardrails that matter most here

Because this app is for a vulnerable population, two failure modes are worth wiring against explicitly:

1. **Generated code/copy drifting into a non-negotiable violation.** Mitigation: `CLAUDE.md` pins them; the **copy-lint** + **crisis-freshness** CI gates are the backstop (`docs/09` §2.5). Keep them in CI from Phase 1.
2. **Autonomous edits to safety-critical code without scrutiny.** Mitigation: `safety-guard` / `gateguard`; and the rule in `CLAUDE.md` that safety/consent code is reviewed, never trusted.

---

## 6. First commands to run

```
# at the Aspen repo root, in Claude Code:
/research kmp cmp skeleton with both targets in CI
# then:
/plan kmp cmp skeleton + CI gates (copy-lint, crisis-freshness)
# approve, implement (lean on ecc:orch-build-mvp), then:
/verify skeleton builds both targets, CI gates active
# then update docs/STATUS.md and move to docs/09 §6 step 2
```

> Reminder: `docs/09` is your current build target. `docs/00_INDEX.md` is the map. `CLAUDE.md` wins over everything.

---

## 7. Optional: companion ranker model (Phase 4, Tier-1 AI)

The Tier-1 companion voice works fully without any model (deterministic selection over the curated
library). To exercise the LiteRT/MediaPipe **ranker** on Android — a small text-EMBEDDER that only
re-orders the approved lines, never generates — drop the model asset in place:

```
# ~4 MB, Apache-2.0, from the MediaPipe model zoo ("Average Word Embedding" text embedder):
mkdir -p androidApp/src/main/assets
curl -L -o androidApp/src/main/assets/companion_ranker.tflite \
  https://storage.googleapis.com/mediapipe-models/text_embedder/average_word_embedder/float32/latest/average_word_embedder.tflite
```

- The asset is **git-ignored** (binary; each dev fetches it) and **optional by design**: absent or
  unloadable → `platformLineRanker()` returns null → deterministic selection, asserted by tests.
- Verify in a debug build: Settings → "Companion preview (debug)" shows which line the ranker picks
  per moment; without the asset the picks rotate deterministically by variant instead.
- Model zoo page (for updates/checksums): https://ai.google.dev/edge/mediapipe/solutions/text/text_embedder
- Ship decision (bundle vs on-demand download) is a Phase-7 item — docs/PRE_SHIP_VERIFICATION.md.
