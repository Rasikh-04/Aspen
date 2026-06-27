# 11 — Onboarding Questionnaire (DRAFT)

> **⚠ Unverified draft for clinical sign-off and per-language/per-region cultural adaptation.** This personalises Aspen to the person; it **does not diagnose**. It must be reviewed by your ED-informed advisors (US + PK) before shipping, in every language, by native-speaking reviewers.

---

## 0. Read this first — what this is and isn't

**What it is:** a short, gentle, **numberless** set of questions that produces an internal **support profile** so Aspen can tailor itself — which tools to surface, whether food logging is offered or suppressed, how the companion speaks, what framing to use.

**What it is NOT — and the honest limit on "accuracy":**
You asked for it to find the disorder type "with some accuracy." Important caveat: the validated ED screeners (SCOFF, ESP, EDE-Q) achieve their accuracy **using exactly the things Aspen forbids** — numeric weight-loss thresholds, frequency counts, body-image-shame items. The moment we soften them to be safe and numberless, **they are no longer validated instruments and their accuracy figures do not carry over.** So treat this as a **tailoring heuristic, not a diagnostic test.** It should be:
- *good enough* to choose a sensible support profile,
- **never** shown to the user as a diagnosis or a confidence score,
- refined over time by the optional device/remote agent (it observes gently and adjusts the profile — it doesn't "confirm a diagnosis"),
- always backed by *"this isn't an assessment — here's how to reach real help."*

Over-trusting this would be a mistake. Its job is to stop Aspen being one-size, not to label anyone.

---

## 1. Design rules (all enforced; tie to `docs/01` §5a, `docs/06`)

1. **Numberless.** No "how much," "how many times," "how many kg," no BMI, no weight. (`docs/01` §6 rule 1.)
2. **Personalise, never diagnose.** Output a *support profile*, never a clinical label shown to the user.
3. **Gentle, plain, skippable.** One question per screen, generous pacing, "prefer not to say" on every item, "skip all" always available (→ a safe neutral/"mixed-or-unsure" profile).
4. **Non-triggering framing.** Ask about *feelings and relationships with eating*, not behaviours-to-quantify. No graphic specifics.
5. **Routes to real help at the end**, always.
6. **Re-runnable & editable** any time in Settings; needs change.
7. **Stored locally, private** (device-key encrypted).
8. **Condition-agnostic.** Must work for restriction, binge, purge, **and ARFID** (sensory/avoidance, *not* body-image) — don't presume everyone fears weight gain.

---

## 2. The support profiles (internal only — never user-visible labels)

The questionnaire scores toward one or more of these. They map to the adaptivity rules in `docs/01` §5a and the logging rules in `docs/03` FR-3b.

| Profile (internal) | Loosely corresponds to | Key adaptivity |
|---|---|---|
| `RESTRICTION_LEANING` | AN-spectrum restriction | **Food logging suppressed/reframed**; companion avoids food focus; grounding emphasised. |
| `BINGE_LEANING` | BED-spectrum | Logging optional, shame-free; urge-surfing + self-compassion tools. |
| `PURGE_COMPENSATORY` | BN-spectrum | Logging gentle; post-eating distress tools; strong human-exit. |
| `AVOIDANCE_SENSORY` | ARFID | **No body-image framing at all**; sensory-aware, non-weight language; different tool set. |
| `BODY_IMAGE_DISTRESS` | cross-cutting | No-comparison emphasis; self-worth/values tools; appearance-talk banned hard. |
| `MIXED_OR_UNSURE` | default / skip | Safest, most neutral configuration; food logging off by default. |

A person can carry **more than one** profile (common). When in doubt, default to the **more protective** configuration (e.g. any restriction signal → suppress food logging).

---

## 3. The questions (DRAFT — English master; advisors to refine, translators to localise)

> Each maps to one or more profiles. Phrasing here is the *master/English* version. Tone: warm, second-person, no clinical jargon. Response style: soft Likert ("Not really / Sometimes / Often / I'd rather not say") unless noted.

**Intro screen (not a question):**
> "A few gentle questions so Aspen can fit you better. There are no right answers, nothing here is a test or a diagnosis, and you can skip anything. Take your time."

**Q1 — opening, low-stakes (orientation)**
*"What would feel most helpful from Aspen right now?"* — multi-select: *A calmer moment in hard times · Somewhere private to put my thoughts · Company that doesn't ask much of me · A way to reach real help · I'm not sure yet.*
→ tunes first-run emphasis; not scored to a disorder.

**Q2 — relationship with eating (broad, non-judgemental)**
*"How would you describe your relationship with eating lately?"* — options: *Tense or full of rules · Out of my control at times · Something I try to undo afterwards · Hard because of how foods feel/taste/seem · Mostly about how I feel in my body · It varies · I'd rather not say.*
→ R1 maps: rules→`RESTRICTION`; out of control→`BINGE`; undo→`PURGE`; feel/taste→`AVOIDANCE_SENSORY`; body→`BODY_IMAGE`.

**Q3 — restriction signal**
*"Do you find yourself holding back from eating, even when part of you wants or needs to?"* — Likert.
→ `RESTRICTION_LEANING`.

**Q4 — loss-of-control signal**
*"Do you ever feel that once you start eating, it's hard to feel in control?"* — Likert.
→ `BINGE_LEANING`.

**Q5 — compensatory signal (gentle, no methods named)**
*"After eating, do you often feel a strong urge to make up for it somehow?"* — Likert.
→ `PURGE_COMPENSATORY`. *(Deliberately does not name or ask about methods — that would be unsafe and triggering.)*

**Q6 — ARFID/sensory signal (crucial, distinguishes non-body-image presentation)**
*"Is eating hard for you mainly because of how foods feel, taste, or smell — or worry about what eating might do (like choking or feeling sick) — rather than worry about weight or shape?"* — Yes / Somewhat / No / Rather not say.
→ Strong `AVOIDANCE_SENSORY`; if Yes, **down-weight** body-image framing entirely.

**Q7 — body-image salience (asked gently, numberless)**
*"How much do thoughts about your body or shape affect your day?"* — *Not much · Sometimes · A lot · Rather not say.*
→ `BODY_IMAGE_DISTRESS`. (No "how do you feel about your weight" — too direct/triggering.)

**Q8 — impact / functioning (severity-ish, without numbers)**
*"How much is this getting in the way of your everyday life right now?"* — *A little · Some · A lot · Rather not say.*
→ not a profile; tunes how strongly to surface the human-exit / treatment routing. "A lot" → foreground support routes more.

**Q9 — support context (clinically grounded — carer involvement helps)**
*"Do you have anyone — a professional or someone you trust — supporting you with this?"* — *Yes, a professional · Yes, someone I trust · Not right now · Rather not say.*
→ if "Not right now" → emphasise Tier-1 directory / treatment-finder later; offer trusted-contact setup.

**Q10 — safety-adjacent, careful (NOT a risk assessment)**
*"On the hardest days, would it help to have a quick way to reach a person?"* — *Yes, set that up · Maybe later · No.*
→ offers trusted-contact / crisis-route setup. **Not** a suicide-risk screen — Aspen does not run clinical risk questionnaires (`docs/06` §6.3). Just makes the human-exit easy.

**Closing screen (not a question):**
> "Thank you for sharing. Aspen will gently shape itself around what you've said — and you can change any of this anytime. One thing worth saying clearly: this isn't a diagnosis or an assessment. If you'd like to find real, specialist support, here's where to start." → links to region resources (`docs/10`).

---

## 4. Scoring → profile (heuristic, transparent, conservative)

- Simple weighted tally per profile from Q2–Q7; Q8–Q10 tune routing, not diagnosis.
- **Conservative bias:** any meaningful `RESTRICTION` or `AVOIDANCE_SENSORY` signal triggers the protective configuration (food logging off/reframed) even if another profile scores higher.
- **Ties / low signal / many skips → `MIXED_OR_UNSURE`** (safest config).
- Profiles are **soft weights**, not exclusive — store all, act on the dominant + any protective flag.
- The optional **device/remote agent** may, over time and with consent, gently adjust weights from in-app behaviour — but it **never** announces a disorder and never overrides the user's explicit settings.

> Pseudocode contract (for `:shared:domain`):
> `OnboardingResult { profiles: Map<Profile, Weight>, protectiveFlags: Set<Flag>, routingHints: RoutingHints }`
> `deriveConfig(result) -> AppConfig { foodLoggingMode, companionTone, toolEmphasis, supportRoutingStrength }`

---

## 5. Region & language adaptation (works with `docs/12`)

- **Clinical content is largely universal** — disorders present similarly across regions; the *core* question set is shared.
- **What localises:** language (all 7), **cultural framing & examples**, idiom, and **stigma-sensitivity** (in some regions, mental-health/ED stigma is higher — phrasing must be even gentler; advisors guide this per locale).
- **RTL:** Urdu & Arabic render right-to-left (`docs/12`) — questionnaire layout must mirror.
- **Native-speaker ED-informed review per language is mandatory** — machine translation of sensitive screening copy is not acceptable. (Especially Q5/Q6/Q7.)
- Region does **not** change a person's profile logic, only its wording — keep scoring locale-independent for consistency.

---

## 6. Sign-off checklist (gate before shipping the questionnaire)

- [ ] Advisors confirm the question set is safe, non-triggering, and non-diagnostic in framing.
- [ ] Profile→behaviour mapping (esp. **logging suppression rules**) signed off per disorder type.
- [ ] ARFID distinction (Q6) validated — does not force body-image framing.
- [ ] Each language reviewed by a native-speaking, ED-informed reviewer.
- [ ] "Not a diagnosis / here's real help" routing present at the end in every locale.
- [ ] No numbers anywhere; copy-lint passes on all localised strings.
- [ ] Skip/"rather not say" paths all resolve to a safe profile.

---

## 7. Sources / basis

- Screening-instrument context (SCOFF, ESP, EDE-Q) — used only to understand *domains*; **deliberately not reproduced** because their accuracy depends on numeric/body-shame items Aspen forbids (NICE NG69 context; standard ED screening literature).
- ARFID-as-distinct (not body-image-driven) — `docs/01` §2 (DSM-5/ICD-11; NG69 excludes ARFID).
- Carer-involvement value (Q9) — NICE NG69 (`docs/01` §4).
- Personalise-not-diagnose + numberless rules — `docs/01` §5a/§6, and the clinical-safety guidance: do less not more; no numbers; point toward region-appropriate ED support.

> Bottom line: ship this as a **tailoring tool with a clinical gate**, not a screener. Its accuracy claim is "helps Aspen fit you," never "tells you what you have."
