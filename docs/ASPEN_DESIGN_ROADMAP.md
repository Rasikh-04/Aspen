# Aspen — Design & Asset Overhaul: Master Roadmap

*A living reference for turning Aspen from "text blocks with no icons" into the quiet, crafted room `06_UI_UX_AND_SAFETY_SPEC.md` describes — without drifting from the rules the app was built on. Update this file as phases complete; it's meant to outlive any single Claude Code session.*

---

## 0. Where things actually stand (updated post-audit — 2026-07-10)

*The original pre-audit framing assumed the component layer and tokens were either unbuilt or tangled up with an abandoned repo attempt. Phase 0's actual audit found otherwise. This section is now the source of truth — the phase list in §3 reflects it.*

**Audit findings:**
- No abandoned design-system branch to reconcile — the untracked `redundant/` directory turned out to be old l10n exports, unrelated.
- `shared/core-design` is real and ~80% adopted: color/type/spacing/radius/motion tokens match `06` §2 exactly, are unit-tested (contrast + no-alarm-red), and fonts are bundled OFL binaries. Only gaps: no formal sign-off pass logged, and the 22sp type-scale step is unused.
- All 8 component-layer primitives from `06` §2.1 exist **and are in active use** app-wide (shipped in `1e347a5`) — not merely scoped, as the pre-audit text assumed.
- The raw-widget gap is narrow, not a full migration: zero raw `Button`/`OutlinedButton`/`FilterChip`/`NavigationBar`/`Switch` anywhere. What's left: `AlertDialog`+`TextButton` in 7 dialogs and `OutlinedTextField` in 4 files — neither `AspenDialog` nor `AspenTextField` was ever scoped.
- **Icons — done (2026-07-10, Phase 3)** — was the one genuinely untouched surface (zero `Icon(`/`Icons.*`/`painterResource` calls anywhere); now wired via `io.github.dev778g-me:phosphoricon-compose` for nav/hard-moment/5-of-7-Settings-groups, with Safety & Support + About explicitly deferred to Phase 6 (see §3 for the full writeup).
- The mascot/sprite system is further along than assumed: a working 3-species procedural pixel-art lineup (`SpriteSheets.kt`), palette-integrity-tested against alarm-red, already shipped from earlier companion work.

**Net effect:** Phases 1, 2, and 3 (visual identity, tokens-as-code, icon pack) are done. Phase 5 (component layer) is ~90% done. Phase 4 (mascot) is real and ahead of plan but is being deliberately paused and re-scoped, not treated as blocking. **Phase 6 (Settings IA + remaining icon wiring for Safety & Support/About) is next.**

**A note on tool choice for the mascot attempt:** Claude Design produced a generic soft-blob illustration ("Fluffy Aspen") that doesn't match `SpriteSheets.kt` at all — different medium entirely, smooth vector illustration vs. a fixed 12×12 character-grid with an exact palette map. That's a tool/task mismatch, not a dead end: pixel art at this precision (`PixelFrame` grids, deliberate alarm-red exclusion) is fundamentally a *code generation* problem, and Claude Code — Fable specifically handled this well — is the right tool for it whenever the mascot phase resumes.

**Standing principle going forward:** default to existing, off-the-shelf assets (icon libraries, font files, established tools) over anything LLM-generated. The mascot is a deliberate, narrow exception — a fixed-grid procedural format nothing off-the-shelf covers, so code generation earns its place there and nowhere else by default. Icons are exactly the kind of thing a mature open ecosystem already solves — see the revised Phase 3 below, which uses a published icon library instead of the split-path architecture originally proposed here. That original proposal was over-engineering a problem that didn't need solving; worth keeping visible as a correction rather than quietly editing it away.

---

## 1. Two tracks — keep these separate in every session

This matters because Aspen isn't a generic app — it's a support tool for people navigating eating-disorder-adjacent territory, and `06` itself treats safety as release-gated for good reason.

| Track | What it covers | Who can change it | 
|---|---|---|
| **Visual/asset layer** | Colors, type, icons, logo, mascot, spacing, component polish, screen layout, information architecture (e.g. grouping Settings) | Freely yours to redesign — this whole roadmap lives here |
| **Product-safety architecture** | Numberless logging, no streaks/scores, crisis routing (§6), AI companion hard constraints (§5, SR-3), copy lint banned-word list | Treat as **fixed constraints**, not redesign material, unless you have a specific clinical reason — and then it goes through the same advisor sign-off `06` already requires before shipping |

You mentioned wanting to "touch the architecture and hard rules... from the start." If you mean *how they're implemented* (cleaner code, better lint tooling, a nicer crisis screen) — yes, that's exactly Phase 7 below. If you mean *what they enforce* (e.g. loosening the no-numbers rule) — that's a clinical-advisor conversation, not a design one. Worth being explicit with yourself about which one you mean each time you open a session.

---

## 2. Tool & model map

| Task | Best tool | Why / notes |
|---|---|---|
| Reference gathering, mood-boarding, "what does X look like" | **This chat** (image search) | Fast, no setup, good for inspiration before you commit to a direction |
| Quick throwaway mockups to test a layout idea in minutes | **This chat** (Visualizer — I can render these inline right now) | Cheapest way to sanity-check a screen concept before touching code |
| Full prototypes across multiple screens, using your actual tokens, exportable to Claude Code | **Claude Design** (claude.ai/design or the sidebar in Claude Desktop) | You upload `06_UI_UX_AND_SAFETY_SPEC.md`, screenshots, and your old logo once — Claude builds a reusable design system from them and every project after that inherits it automatically. It can also export a bundle straight into Claude Code, or as PDF/HTML/PPTX for sharing with your dev partner. Currently in beta on Pro/Max/Team plans. |
| Sending a Claude Design output into Canva for manual polish | **Claude Design's "Send to Canva" export** | Anthropic and Canva announced this integration; one independent reviewer noted it's occasionally unreliable in the current beta, so have a PDF/HTML export as a fallback. If you'd rather work with Canva directly in *this* chat instead of Claude Design, a Canva MCP connector exists — say the word and I'll pull up the connect option. |
| Standalone app/launcher icon generation (not the in-app icon pack — see Phase 3, which now uses an existing library) | **`ecc:ios-icon-gen`** (Claude Code) | You already have this skill installed. Pulls from SF Symbols or Iconify and writes ready-to-use asset catalogs — useful for the still-open logo/launcher-icon question in Phase 1, not for in-app UI icons |
| Original static art (poster-style concepts for logo exploration only — not mascot pixel art, see the Phase 4 lesson learned) | **`canvas-design`** skill | Design-philosophy-driven original art generation, useful for early logo concept sheets |
| Reading an uploaded reference (your old logo) and reasoning about it | **This chat** — just upload the file | I can view it directly and reason about palette/shape compatibility with the sage/sand tokens |
| Actual Compose implementation, wiring tokens/components into the real app | **Claude Code**, in the Aspen repo | Ground truth — needs your actual codebase context |
| Auditing whether the design system is being followed consistently as you build | **`ecc:design-system`** skill | Generates/audits design systems, flags drift |
| The "craft" pass — spacing, hit areas, motion feel, icon alignment | **`ecc:make-interfaces-feel-better`** | Concrete polish details, not vibes |
| Production-UI judgment calls specific to your stack | **`ecc:frontend-design-direction`** | Sets a direction rather than generic defaults |
| Motion tokens / reduced-motion correctness | **`ecc:motion-foundations`** + **`ecc:motion-patterns`** | Matches §2's 200–400ms eased motion + reduced-motion requirement |
| Accessibility pass (WCAG 2.2 AA, dynamic type, screen reader labels) | **`ecc:accessibility`** | Matches §7 directly |
| Enforcing the banned-word / raw-widget rules automatically | **`ecc:hookify-rules`** or **`ecc:gateguard`** (both installed) | See Phase 7 — this is your anti-drift layer |
| Tracking spend across a long, multi-session project | **`ecc:cost-tracking`** + **`ecc:context-budget`** | Both installed already |
| Recording *why* a design decision was made (so a future session doesn't relitigate it) | **`ecc:architecture-decision-records`** | Cheap insurance against re-debating settled calls |

**Model tiers, in practice:**
- **Opus-tier** (Opus 4.8) — the handful of high-stakes calls: locking the final token values, reviewing the finished component layer, resolving genuine architecture disagreements. Use `/model` to switch for just that message, then switch back.
- **Sonnet-tier** (Sonnet 5) — your daily driver in Claude Code for the actual implementation grind (screens, components, icon wiring). It's the balance of quality and cost for this kind of work.
- **Haiku-tier** (Haiku 4.5) — cheap, fast, mechanical subagent work: renaming, running the lint script, generating icon-set boilerplate. If you set up subagents, give them `model: haiku` explicitly.
- **Fable-tier** (Fable 5) — worth keeping in mind for creative-generative *code* specifically, based on what actually worked for you: it produced the whole `SpriteSheets.kt` pixel-art system cleanly where a general visual-design tool did not. Scoped narrowly to the mascot phase when it resumes — not a general substitute for picking existing libraries elsewhere (icons, fonts, and similar all default to off-the-shelf per §0's standing principle).

**Session hygiene that actually saves money over a multi-month project:**
- Use **plan mode** (Shift+Tab) before any phase that touches more than one file — Claude proposes the approach first, so you're not paying for a wrong-direction rewrite.
- `/clear` between unrelated phases (e.g. don't carry "icon pack" context into a "Settings IA" session); `/rename` first if you'll want to find it again.
- Keep `CLAUDE.md` lean (under ~200 lines) — put the detailed, phase-specific rules (like the lint spec in §5 below) into a **skill** instead, so they only load when relevant rather than sitting in every session's context.
- Delegate noisy operations (running the whole test suite, grepping a big log) to a subagent so only the summary lands in your main conversation.

---

## 3. The phases

Each phase is designed to be one-to-a-few Claude Code sessions. No deadlines — but each has a clear "done" line so you're not guessing whether to move on.

### Phase 0 — Audit ✅ done (2026-07-10)
- Result: no abandoned attempt to reconcile; `shared/core-design` tokens + component layer are real and largely adopted. Full findings logged in `docs/STATUS.md` and summarized in §0 above.
- Decision made: keep everything, no rebuild. Extend with `AspenDialog` + `AspenTextField`, then move straight to icons.

### Phase 1 — Lock the visual identity ✅ done
- Palette, typography, and (still open, low-urgency) the standalone app logo/launcher icon. Palette and typography are locked and implemented in `AspenColors`/`Palette` and bundled OFL fonts (Fraunces / Plus Jakarta Sans / Noto Nastaliq Urdu).
- The old logo decision is still technically open but doesn't block anything below — it's a standalone branding/launcher-icon question, worth folding into whichever future session revisits the mascot or does app-store assets, not this pass.

### Phase 2 — Formalize design tokens as code ✅ done
- Color/type/spacing/radius/motion all match `06` §2 exactly and are unit-tested (contrast + no-alarm-red).
- Remaining loose end (low priority): no formal sign-off pass logged, and the 22sp type-scale step is defined but unused by any role yet.

### Phase 3 — Icon pack ✅ done (2026-07-10) — style: **Duotone**

- **Library locked in:** `io.github.dev778g-me:phosphoricon-compose:1.0.5` — chosen over `com.adamglin:phosphor-icon` after the due-diligence check below confirmed it (recent activity, 0 open issues, confirmed Duotone weight + iOS targets; see `docs/STATUS.md` Phase 3 entry for the full comparison).
- **Wired: 14 of the 16 scoped icons** — all 4 nav tabs, all 5 hard-moment tools, and the 5 Settings groups that already had rows to attach an icon to (You/Companion/AI & Privacy/Language/Account & Data).
- **Not wired — Safety & Support and About:** neither exists as Settings content yet (no crisis-region/trusted-contact row, no version/credits/feedback screen). Rather than invent placeholder cards to hang an icon on, these 2 are explicitly deferred to **Phase 6** below, which already owns the Settings IA/grouping work — they'll get their icon when that content is actually built.
- **"Done when" line 3 (lint hook watch-list) not yet done:** no PreToolUse anti-drift hook exists in this repo yet (checked `.claude/settings.json`) — that's genuinely **Phase 7**'s job, not this one's. Carrying it forward rather than claiming it here.
- Safety cross-check: every icon tint used is an existing `AspenTheme.colors.*` token, already covered by the `noTokenIsAlarmRed` unit test — no new explicit tint, no new test needed.

<details>
<summary>Original phase spec (for reference)</summary>

- **Correction:** this is icon *selection*, not icon design — no custom split-path architecture, no mirroring `SpriteSheets.kt`. That pattern exists because the mascot has no off-the-shelf equivalent; icons do, and treating them the same way was over-engineering it.
- **What Phosphor duotone actually is, technically:** each icon SVG already ships as two paths sharing one `currentColor` reference — a full-opacity detail path and a 20%-opacity background path. One tint, applied once, already produces the duotone look. There's nothing to hand-split.
- **Pack, not pipeline:** use an existing, published Compose Multiplatform library rather than converting SVGs yourself:
  - `io.github.dev778g-me:phosphoricon-compose` — full Phosphor set as `ImageVector`s, exposes `PhIcons.Duotone.*`/`PhIcons.Light.*`/etc., API modeled on Material Icons: `Icon(imageVector = PhIcons.Duotone.House, tint = ...)`.
  - `com.adamglin:phosphor-icon` (has separate Android/iOS platform artifacts) as an alternative if its coverage or maintenance looks better on inspection.
  - Both are small community libraries, not the official Phosphor project — have Claude Code check current version, last-updated date, and issue activity before locking one in, same due diligence as any new dependency.
- **Optional, thin, not mandatory:** if call sites should read as app concepts rather than pack internals (`AspenIcons.Breathe` vs `PhIcons.Duotone.Wind`), a one-line re-export per icon is fine — that's aliasing, not designing. Skip it entirely if calling `PhIcons.Duotone.*` directly everywhere is fine with you.
- **Scope for v1 (~16 icons):** bottom nav (Home/Reflect/Calm/Settings), the 5 hard-moment tools (Breathe/Ground/Ride the urge/Write it down/Reach someone), and the 7 Settings groups from §5.
- **Safety cross-check still applies:** wherever a tint is passed explicitly (rather than left at its default), assert it against `Argb.isAlarmRed` for anything under Settings → Safety & Support.
- **Done when:** every item in scope renders via the chosen library's duotone weight, its provenance is noted in `docs/STATUS.md`, and the lint hook in §4.3 has `Icons.*`/raw XML added to its watch list.

**Claude Code handoff prompt** (ready to paste):
```
Add the Phosphor duotone icon pack to the app via an existing library — do not
hand-author icon assets or split icon paths yourself.

1. Evaluate io.github.dev778g-me:phosphoricon-compose vs com.adamglin:phosphor-icon
   on Maven Central — check last release date, issue activity, and whether the
   Duotone weight covers all 16 icons below. Pick one and add it as a dependency.
2. Wire these in via PhIcons.Duotone.*, using AspenCard/AspenScreenHeader as
   already established, into the existing nav bar, the "hard moment" chooser,
   and each Settings card:
   Nav — Home, Reflect, Calm, Settings.
   Hard-moment tools — Breathe, Ground, Ride the urge, Write it down, Reach someone.
   Settings groups — You, Companion, AI & Privacy, Language, Account & Data,
   Safety & Support, About.
3. Do not introduce any raw widget while doing this.
4. For any icon tint passed explicitly under Settings → Safety & Support, assert
   it against the existing Argb.isAlarmRed guard.
5. Confirm zero remaining painterResource/Icons.* calls afterward with a
   repo-wide grep.
```
</details>

### Phase 4 — Mascot / companion sprites — **delayed, rescoped, not blocking**
- Deliberately paused: this pass is UI design first; mascot expansion gets its own dedicated phase later.
- Already shipped and ahead of the original plan: a 3-species procedural pixel-art lineup (`SpriteSheets.kt`) with ambient/playful/gentle animation states, alarm-red-tested palette, `LocalReducedMotion`-aware.
- **Lesson learned:** Claude Design isn't the right tool for this medium (see §0). Claude Code — Fable specifically handled it well — is the right tool when this phase resumes.
- **Likely scope when resumed:** additional states/species or higher-fidelity frames, authored the same way — as `PixelFrame` grids in Claude Code, not through a design tool.

### Phase 5 — Component layer ✅ ~90% done — two components remain
- All 8 original primitives exist and are adopted app-wide (verified by usage, not just presence).
- Remaining gap: `AspenDialog` (replacing raw `AlertDialog`+`TextButton` in 7 dialogs) and `AspenTextField` (replacing raw `OutlinedTextField` in 4 files, which currently hand-copy the same color wiring each time).
- **Done when:** both new primitives exist, the 7 dialogs and 4 text fields are migrated, and the lint hook in §4.3 has `AlertDialog`/`OutlinedTextField` added to its banned-widget list.

### Phase 6 — Screen-by-screen migration — now mostly an IA + icon-wiring pass
- The raw-widget swap is essentially already done (per the audit). What's left is applying icons (once Phase 3 ships) and the Settings grouping from §5, then rolling the same grouped-section treatment out to any other flat screens.
- **Done when:** Settings renders as the 7 grouped sections in §5's table with icons, and no other screen is flatter than Settings was.

### Phase 7 — Anti-drift guardrails
- Wire up the lint/hook rules in §5 so future sessions (yours or a fresh Claude Code instance) can't quietly reintroduce raw widgets or banned words.
- **Done when:** the hook actually blocks a deliberate test violation.

### Phase 8 — Accessibility & motion QA
- Run `ecc:accessibility` against the finished screens; verify dynamic type reflow, contrast, hit targets ≥48dp, reduced-motion parity per `06` §7.
- **Done when:** the checklist in §7 of `06` is fully checked, not assumed.

### Template — every future feature gets this shape
1. **Scope note** (1 paragraph): what's new, which track it belongs to (visual vs. safety-architecture — see §1), which existing components it reuses.
2. **Plan-mode pass** in Claude Code before writing anything.
3. **Build** using only the Phase-5 component layer — never a raw widget.
4. **Lint pass** (Phase 7's hook) before commit.
5. **ADR entry** (`ecc:architecture-decision-records`) if any non-obvious call was made.
6. **If it touches Flow C, logging, or the AI companion:** clinical-advisor sign-off before it ships, per `06` §6.5 — no exceptions, this one's non-negotiable by the doc's own rules.

---

## 4. Exact rules to hand Claude Code (the anti-drift layer)

### 4.1 Project rule (add to `CLAUDE.md`, keep it short)
```markdown
## Design system — non-negotiable
- Never use raw Compose Material widgets on a shipped screen: Button, OutlinedButton,
  TextButton, FilterChip, NavigationBar, Card (raw), Switch (raw). Use the
  app.aspen.design.components equivalent instead (see design-system-components skill).
- If a needed primitive doesn't exist yet, STOP and ask — extend the component layer,
  don't hand-roll a one-off in the feature.
- Never introduce: calorie, kcal, BMI, weight (as a number), macro, streak, score,
  fail/failed/missed/incomplete in user-facing strings.
- Any change to Flow C (crisis), the AI companion's system prompt, or the logging
  data model requires an explicit "clinically reviewed" confirmation from the user
  before it's considered done — do not mark it complete otherwise.
```

### 4.2 Component contract (put in a skill, not CLAUDE.md, so it only loads when building UI)
| If the task needs... | Use | Not |
|---|---|---|
| The forward action of a screen | `AspenPrimaryButton` | `Button` |
| A secondary route | `AspenQuietButton` | `OutlinedButton` |
| Back / skip / close / delete | `AspenTextAction` | `TextButton` |
| Any grouped content block | `AspenCard` | raw `Surface`/`Card` |
| A pill toggle | `AspenChoiceChip` | `FilterChip` |
| A read-only tag | `AspenTagPill` | `AssistChip` |
| A screen title | `AspenScreenHeader` | ad-hoc `Text` + `Column` |
| Background ambience | `AspenAmbientBackground` | any animated background loop |
| Any progress indicator | `AspenPresenceDots` | `LinearProgressIndicator` / numeric counter |

### 4.3 A pre-commit / PreToolUse hook to make this enforceable, not just documented
Save as `.claude/hooks/aspen-design-lint.sh`, wired to `PreToolUse` on `Edit`/`Write` for `*.kt` files. It doesn't block everything — it flags and asks you to confirm, since some raw-widget matches will be false positives (e.g. inside the component layer itself, which is allowed to use Material primitives internally):

```bash
#!/bin/bash
# aspen-design-lint.sh — flags raw Material widgets and banned copy outside
# the design-system module itself.
input=$(cat)
file=$(echo "$input" | jq -r '.tool_input.file_path // empty')

[[ "$file" != *.kt ]] && { echo "{}"; exit 0; }
[[ "$file" == *"/design/components/"* ]] && { echo "{}"; exit 0; }  # layer itself is exempt

content=$(echo "$input" | jq -r '.tool_input.content // .tool_input.new_string // empty')

banned_widgets='(\bButton\(|\bOutlinedButton\(|\bTextButton\(|\bFilterChip\(|\bNavigationBar\()'
banned_words='(calorie|kcal|\bBMI\b|macro[s]?\b|\bstreak|\bscore\b|fail(ed)?\b|missed\b|incomplete\b)'

hit=""
if echo "$content" | grep -qEi "$banned_widgets"; then
  hit="raw Material widget (use the app.aspen.design.components equivalent)"
elif echo "$content" | grep -qEi "$banned_words"; then
  hit="a word banned from user-facing copy by 06_UI_UX_AND_SAFETY_SPEC.md §1/§8"
fi

if [[ -n "$hit" ]]; then
  echo "{\"hookSpecificOutput\":{\"hookEventName\":\"PreToolUse\",\"permissionDecision\":\"ask\",\"permissionDecisionReason\":\"This edit appears to introduce $hit. Confirm this is intentional and reviewed.\"}}"
else
  echo "{}"
fi
```

Register it in `.claude/settings.json`:
```json
{
  "hooks": {
    "PreToolUse": [
      { "matcher": "Edit|Write", "hooks": [{ "type": "command", "command": ".claude/hooks/aspen-design-lint.sh" }] }
    ]
  }
}
```

This is a starting point, not a finished linter — you'll want to widen the widget list as the component layer grows, and eventually promote the banned-word half into the real build-time lint `06` §6.4 already calls for (a release-blocker, not just a nudge).

---

## 5. Settings — an information architecture proposal

Right now (Settings.jpeg) everything is one flat stack of cards. Here's a grouping that matches what's actually there, ready for icons from Phase 3:

| Group | Contains | Suggested icon direction |
|---|---|---|
| **You** | Revisit the questions (support profile) | a soft person/leaf mark |
| **Companion** | A small companion (pixel friend on/off) | the mascot's own idle glyph |
| **AI & Privacy** | Deeper reflection (on/off, on-device note) | a quiet spark/leaf, not a robot |
| **Language** | Match my device / English / اردو / Deutsch / 中文 / हिन्दी / العربية / Español | a globe outline |
| **Account & Data** | Account ID, sign-in state, (add: export data, delete data) | a soft shield or key |
| **Safety & Support** *(consider surfacing this even in Settings, not only via the persistent affordance)* | Crisis line region, trusted contact | a calm heart or lifebuoy — never red |
| **About** | Version, credits, feedback | an info circle |

Each group becomes an `AspenScreenHeader`-style section label + a set of `AspenCard`s, rather than one undifferentiated column — this alone will make Settings.jpeg feel dramatically more organized before a single icon is drawn.

---

## 6. Suggested order of operations

~~Original pre-audit order: Phase 0 → 1 → 2 → 3 → 5 → 6 → 4 → 7 → 8.~~ **Updated, now that 0/1/2/3 are done and 5 is nearly done:** **5's remainder (`AspenDialog`/`AspenTextField`, current) → 6 (Settings IA + the Safety & Support/About icons Phase 3 deferred) → 7 (guardrails) → 8 (a11y) → 4 (mascot, resumes whenever you're ready, fully decoupled from the rest).** Anything new that comes up mid-way still gets its own phase using the template in §3, inserted wherever it makes sense rather than forcing a fixed sequence.

---

## 7. Sources consulted for the product-tool guidance above
- Claude Design — https://support.claude.com/en/articles/14604416-get-started-with-claude-design
- Claude Design design-system setup — https://support.claude.com/en/articles/14604397-set-up-your-design-system-in-claude-design
- Claude Design launch (Canva export) — https://www.anthropic.com/news/claude-design-anthropic-labs
- Claude Code cost management — https://code.claude.com/docs/en/costs.md