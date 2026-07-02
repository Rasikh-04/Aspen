# 06 — UI/UX & Safety Specification

> The design system, the three core flows, accessibility, and the safety subsystem treated as first-class. The visual language inherits the *intent* of the old UI spec (calm, warm, unhurried) but is re-grounded for native Android and re-anchored to the evidence-based non-negotiables.

---

## 1. Design language

Aspen should feel like **a quiet room, not a dashboard.** Four rules:

1. **Softness over sharpness** — rounded corners, soft shadows, no hard dividers, no raw black-on-white.
2. **Space over density** — generous whitespace; when in doubt, add padding not content. Recovery is not a checklist.
3. **Warmth over neutrality** — earth tones, sage, warm whites; *not* clinical greys/blues.
4. **Support over performance** — no streaks, scores, ranks, or green/red pass-fail. Progress is shown as **presence**, never as a metric.

### Anti-patterns (banned, enforced — ties to `01` §6 / `03` SR-1,4)
Calorie/weight/body fields or labels · red error states for minor actions (use soft amber) · progress bars tied to food/exercise · comparison or ranking language · bright/urgent notification styling · dense walls of content · numbered "steps of recovery" implying a right order · the words *fail/failed/missed/incomplete* in user copy.

---

## 2. Design tokens (native)

> Re-expressed as platform-neutral tokens (consumed by Compose theme in `:core:design`), not Tailwind. Values illustrative; finalise with the companion art.

**Color** (soft only — no pure red anywhere)
```
sage/50 … sage/700        primary, calm green family
sand/50 … sand/500        warm secondary
warm-white / warm-offwhite surfaces/background
text/primary, secondary, muted, inverse
surface, border
caution  = soft amber      (used instead of error-red)
crisis   = a calm, serious but non-alarming tone (NOT bright red)
```
*Rationale:* even the crisis/safety surface must be **serious without being alarming** — alarm-red can itself escalate distress.

**Typography**
```
display: a warm humanist serif (e.g. Fraunces) — headings, gentle moments
body:    a soft humanist sans (e.g. Plus Jakarta Sans) — content
scale:   12 / 14 / 16 / 18 / 22 / 26 / 32
line-height: 1.2 tight · 1.5 normal · 1.8 loose (favour loose for body)
```
All sizes honour the OS **dynamic type** setting (a11y).

**Spacing** — 4/8/12/16/20/24/32/40/48 dp scale; default to the generous end.
**Radius** — small 8 / medium 16 / large 24; pills for chips.
**Motion** — slow, eased, calming (200–400ms); a **reduced-motion** mode replaces transitions with fades and disables companion/breathing animation.

### 2.1 The component layer — calm is a design, not an absence of one (mandatory from Phase 3)

**Decision (2026-07-02):** Aspen's UI is deliberately quiet, but it must never read as *unfinished* — a
screen of raw text and default buttons tells a user in a hard moment that nobody cared about this room.
"Do less" (CLAUDE.md #10) governs *what* is on a screen, not *how well* it is made. Every surface gets
the same craft: ambient light, soft depth, eased motion, whole-target touch areas.

That craft lives in one place: **`app.aspen.design.components`** in `:shared:core-design`. All feature
UI — current screens and everything from Phase 4 on — composes these primitives. **Raw Material
widgets (`Button`, `OutlinedButton`, `FilterChip`, `NavigationBar`, ad-hoc `Surface` cards) are not
acceptable on shipped surfaces**; if a screen needs something the layer doesn't have, extend the layer,
don't hand-roll it in the feature. This is what keeps the app coherent as it grows and keeps the
non-negotiables (soft amber only, presence not progress, reduced-motion parity) enforced once.

The layer (each honours the tokens above and `LocalReducedMotion`):

| Component | Role |
|---|---|
| `AspenPrimaryButton` | The single forward action of a screen (soft pill, gentle press settle) |
| `AspenQuietButton` | Secondary routes (faint sage fill; replaces outlined buttons) |
| `AspenTextAction` | Low-key inline actions (back / skip / close / delete) |
| `AspenCard` | Grouped content; soft edge + whisper of shadow; optionally whole-card tappable |
| `AspenChoiceChip` / `AspenTagPill` | Pill toggle (calm tint shift, re-tonable for Flow C) / read-only tag |
| `AspenScreenHeader` | Serif display title + loose subtitle, one shared rhythm |
| `AspenAmbientBackground` | The quiet light behind every screen — static `drawBehind`, zero battery cost |
| `AspenPresenceDots` | Progress as presence (filled vs. soft dots), never a number or bar |

Rules of the layer: press feedback is a shared subtle settle (disabled under reduced motion); selection
states are eased tint fades (fades are the reduced-motion-safe transition); route changes cross-fade with
the motion tokens; ambience is drawn statically (no animation loops outside the breathing tool). Screens
stay text-first and near-empty — the craft is in the light, spacing, and settle, not in ornament.

---

## 3. The flows

### Flow 0 — Onboarding (disorder-adaptive, personalises not diagnoses)
1. A warm, plain welcome that sets expectations: *"A few gentle questions so Aspen fits you. This isn't a test or a diagnosis."*
2. **Numberless, skippable questions** that infer a **support profile** (`01` §5a / `03` FR-0). No calorie/weight/frequency interrogation. Soft language, one question per screen, generous pacing.
3. Closes by routing toward real assessment/support (*"this isn't a diagnosis — here's how to reach real help"*) and into the Calm Home.
4. Profile is **editable/re-runnable** later in Settings. The profile silently shapes the rest of the app (which logging appears, companion tone, tool emphasis).
5. **Clinical-advisor sign-off gate** on the question set + mapping before release.

### Flow A — Support ("I'm having a hard moment")
The most important non-safety flow.
1. **Calm Home** — near-empty; one or two soft entry points; no metrics, no food/number prompts.
2. Tap *"hard moment"* → a quiet chooser (large, few options, no scrolling): **Breathe · Ground (5-4-3-2-1) · Ride the urge · Talk · Reach someone.**
3. Each tool is full-screen, single-purpose, offline, 60fps, exit always visible.
4. Persistent, low-key **"reach a person"** affordance throughout (never red, never pushy) → Flow C.
5. On exit: a gentle, non-scoring close ("glad you took a moment") — *never* "great job," streaks, or stats.

### Flow B — Reflection + optional logging (the notebook)
1. From Home: *"Write something down"* (reflection) and, *if the profile allows*, gentle optional **food/feeling logging**.
2. Plain, warm text space. **Logging is numberless** — text + feeling tags, never a number (`01` §6 rule 1). Optional, non-enforced; **empty days are silent** (no "you missed…").
3. **No streaks, counts, or scores.** Entries are the user's; **one-tap permanent delete**. On-device, encrypted.
4. **Adaptive:** food logging is softened/reframed/**hidden** for presentations where it's contraindicated (profile-driven, advisor rules); feeling/behaviour logging stays broadly available.
5. If cloud AI is enabled: opt-in *"reflect with Aspen"* — validating, reflective, **never advising on eating/numbers** (§5).

### Flow C — Safety (its own subsystem; see §6)
1. Reachable in **≤2 taps from anywhere**, including from the companion and from Flow A.
2. Three calm options: **Crisis line (region-correct) · Trusted contact · Find treatment.**
3. Works offline (cached). No friction, no forms, no numbers. One tap to dial/connect.

---

## 4. Navigation & information architecture

- Flat and shallow. A simple bottom presence: **Home · Reflect · Calm/Tools · (Settings).** Safety is *not* buried in a tab — it's a persistent affordance + reachable from Home and the hard-moment flow.
- No notifications by default (`03` FR-8). If ever enabled, they are gentle, opt-in, never streak/guilt/urgency.
- Community (later) would be its own clearly-bounded space, never a feed.

---

## 5. AI companion UX & constraints (the notebook, not the therapist)

- **Tone:** warm, brief, validating, human. Reflects feelings back; helps the user name things; sits with them.
- **Hard constraints (SR-3), enforced by system prompt + output guard in `:domain`:**
  - Never gives eating/meal/diet/calorie/exercise advice.
  - Never outputs numbers about food/weight/body; never comments on appearance (any direction).
  - Never diagnoses, never claims to treat/cure.
  - On any sign of crisis/self-harm: shifts to validation + **warm hand-off** to Flow C and a human; does not attempt to manage risk itself; does not interrogate with assessment questions.
  - Keeps the user oriented to real, specialist support; never positions itself as a substitute.
- **Failure mode design:** if the model produces anything off-limits, the output guard replaces it with a safe, validating fallback + the route to human support. A **red-team test suite** (adversarial prompts) is part of release gating.
- **Offline:** degrades gracefully (queue/disable); grounding + notebook still work.

---

## 6. Safety architecture (first-class subsystem)

Safety is **not a screen** — it's an always-on subsystem (`04` §4 safety engine). Design principles:

**6.1 Crisis routing (FR-4 / SR-2)**
- **Region-aware resource registry**, data-driven (not hardcoded), with a curated entry per supported locale + an **international fallback**. Each entry: name, what it's for, contact method(s), hours if relevant.
- **Launch regions (confirmed):** **Pakistan, Germany, UK** primary; **USA** when ready (you have ED-informed medical contacts in US + Pakistan).
- **Current and correct, advisor-verified before every release.** **Never NEDA** (disconnected). Anchor organisations identified by research (all contact details to be **verified with your clinical advisors before shipping** — a wrong number in a crisis flow is the worst possible bug):

  | Region | Anchor organisations (verify details before ship) | Notes |
  |---|---|---|
  | **UK** | **Beat** (national ED charity, helplines); NHS routes; **Samaritans** for acute distress | Mature ED infrastructure. |
  | **Germany** | **BZgA** (Bundeszentrale für gesundheitliche Aufklärung — federal ED counselling info); **ANAD e.V.** (Munich); **DGESS** / **BFE** (professional networks); university centres (e.g. Tübingen KOMET) | Strong specialist network; German-language resources required. |
  | **USA** *(later)* | **National Alliance for Eating Disorders** helpline (1-866-662-1235, *staffed by licensed clinicians*); **988** Suicide & Crisis Lifeline for acute crisis; **ANAD** peer helpline | Use these — **not NEDA**. Curate with US advisors. |
  | **Pakistan** | *No established ED-specific helpline exists* — lean on **general mental-health crisis lines**, **your own clinical contacts**, and the **international fallback**; build out with your Pakistan advisors | **Be honest in-app** about limited local ED-specific services; this is exactly where your local clinical network matters most. |
  | **International fallback** | A vetted global directory/route + "find local help" guidance | Always present for any locale. |

- **Offline-capable:** cached so a number is dialable with no connectivity.
- **Calm, serious styling** — never alarm-red; never makes the user feel they've triggered an emergency by tapping.
- **Localisation:** German (and Urdu/regional, as advisors recommend) for the relevant locales — crisis copy especially must be in the user's language.

**6.2 Trusted contact**
- User pre-selects a person; the safety flow offers a one-tap way to reach them. Clinically grounded (carer involvement improves outcomes, `01` §4). Stored as a reference, not harvested.

**6.3 Crisis vs. distress — don't over-trigger**
- Most hard moments are *distress*, not acute emergency. The design must not treat every hard moment as a 911 event (that's why Flow A exists *before* Flow C). But the human exit is always one tap away. The app **does not** run clinical risk-assessment questionnaires (can feel interrogative and is outside its role).

**6.4 Copy & content safety (SR-1)**
- A **build-time lint** scans string resources for forbidden tokens (calorie, kcal, BMI, weight, macro, "failed," "missed," etc.) → flagged for human review.
- All user-facing copy passes a review checklist against `01` §6 before release.

**6.5 Release gating**
- The crisis flow and AI output guard are **release-blockers**: a build cannot ship if the crisis registry isn't verified for supported regions or the AI red-team suite regresses (`03` NFR-reli, SR-2/3).

---

## 7. Accessibility (NFR-a11y → WCAG 2.2 AA-equivalent)

- **Dynamic type** throughout; layouts reflow, never clip.
- **Screen-reader** labels on every interactive element; meaningful reading order; the companion and its summon button are properly labelled/ignorable.
- **Contrast** sufficient within the calm palette (verify each token pair).
- **Reduced-motion / calm mode** disables companion animation, breathing motion, and transitions (SR-6) — some motion is activating for anxious users.
- **Hit targets ≥ 48dp**; generous spacing already helps.
- **No reliance on color alone** (e.g. caution states pair amber with text/icon).
- **PIN/biometric lock** option for privacy (sensitive content).

---

## 8. Tone of voice (copy principles)

- Warm, plain, unhurried, adult. Never clinical, never chirpy-gamified, never patronising.
- Validate feelings; never evaluate the person. ("That sounds really hard." — not "Don't worry!")
- No imperatives that imply compliance. Offer, don't instruct.
- Modesty about what Aspen is: a companion for the gap, not a treatment.
- Banned words in user copy: fail/failed/missed/incomplete; any food/body number; any appearance descriptor.

---

## 9. Relationship to the old UI spec

The old spec's *visual intent* (the four rules, the anti-patterns, the sage/sand palette, the warm serif + humanist sans pairing) was good and is **kept**. What changes: it's re-expressed as **native Compose design tokens** (not Tailwind/Radix/MUI), safety is elevated from a section to a **subsystem with release-gating**, and the non-negotiables are now **enforced in code/CI**, not just documented.
