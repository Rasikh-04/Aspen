# 01 — Research & Clinical Foundation

> The evidence base for every product decision that follows. If a later document makes a claim about "what users need" or "what's harmful," it traces back to here.

---

## 1. The problem, stated precisely

Eating disorders (EDs) are serious mental illnesses with one of the highest mortality rates of any psychiatric condition. They are not lifestyle choices, vanity, or phases. Anorexia nervosa in particular carries a standardised mortality ratio around 5× the general population, and EDs as a category sit near the top of psychiatric mortality, behind opioid use disorder [GBD 2021; Arcelus 2011].

But mortality is only the headline. The lived reality that Aspen addresses is narrower and more specific:

**The gap between appointments.** Specialist ED treatment — the thing that actually works — happens in scheduled blocks: a weekly therapy hour, a fortnightly dietitian session, a monthly review. The disorder does not keep that schedule. Urges, intrusive thoughts, post-meal distress, and loneliness peak in the evenings, at night, on weekends, around meals, and in social situations — precisely the times no clinician is reachable [Recovery Record qualitative study, PMC6035344, on context-dependence of distress]. This is the **between-session gap**, and it is where most of a person's recovery time is actually spent.

**The help that exists doesn't fit the gap.**
- *General social platforms* actively harm recovering users: visual feeds, comparison mechanics, "what I eat in a day" content, and unmoderated communities amplify exactly the cognitions the disorder runs on.
- *Clinical care* is essential but scarce, waitlisted, and absent between sessions.
- *Existing ED apps* (analysed in `02_MARKET_ANALYSIS.md`) are largely built around **self-monitoring / meal logging** — and the evidence shows that this core loop is double-edged for a meaningful fraction of users.

So the problem Aspen targets is not "treat eating disorders" (it can't and shouldn't). It is: **make the between-session hours less dangerous and less lonely, without introducing anything that feeds the disorder.**

---

## 2. Scope of conditions — and a correction to the original brief

The DSM-5 / ICD-11 feeding-and-eating-disorder spectrum Aspen should consider:

| Condition | Notes for product |
|---|---|
| Anorexia nervosa (AN) | Highest mortality; restriction-driven; comparison/numbers especially dangerous. |
| Bulimia nervosa (BN) | Binge–purge cycles; shame and secrecy are central; needs low-friction, private support. |
| Binge-eating disorder (BED) | Most prevalent ED; often co-occurs with higher body weight and weight stigma in healthcare. |
| OSFED (Other Specified) | The largest real-world group; "atypical" presentations, often under-diagnosed. |
| ARFID | Avoidant/restrictive intake; **not** body-image driven; needs distinct framing. |

**Correction:** The original brief listed all of the above as covered by **NICE guideline NG69**. NG69 in fact covers **anorexia nervosa, bulimia nervosa, binge-eating disorder, and OSFED — and explicitly excludes ARFID, pica, and rumination disorder** [NICE NG69, 2017, updated Dec 2020]. This matters for Aspen because ARFID is sensory/avoidance-driven, not body-image-driven, so generic "recovery" framing and body-acceptance language can be irrelevant or alienating to ARFID users. **Design implication:** the product must not assume every user's ED is body-image-based. Onboarding and companion dialogue must be condition-agnostic and never presume the user fears weight gain.

---

## 3. Epidemiology (why this is worth building)

All figures cited with sources in §9. Rounded, directional, not precise claims.

- Global ED prevalence has roughly **doubled over the last decade** (cited ranges from ~3.4% to ~7.8% of population across the 2000s–2020s), with sharp rises in high-income Asian and European countries [SingleCare 2026; Rehab Seekers 2025].
- The **Global Burden of Disease 2021** analysis found age-standardised incidence rising (≈107 → ≈124 per 100,000) and DALYs rising (≈37 → ≈43 per 100,000) from 1990–2021, with bulimia nervosa growing faster than anorexia [GBD 2021, PMC12375436].
- **~22% of children and adolescents worldwide** show disordered-eating behaviours (meta-analysis) [López-Gil et al., JAMA Pediatrics 2023].
- **The majority of people with an ED never receive specialist treatment** — frequently cited as 70%+ unmet need [Hart et al. 2011; NEDA statistics page]. This is the single most important number for Aspen: *the gap Aspen serves is the normal case, not the exception.*
- EDs affect all genders, ages, body sizes, and ethnicities; the "thin young white girl" stereotype causes under-diagnosis, and **higher-weight individuals are diagnosed at roughly half the rate** despite higher risk of disordered behaviours [Ramaswamy 2023, AMA J Ethics]. **Design implication:** zero body-size assumptions anywhere in the product.

> **A note on numbers in this document vs. in the product.** This file is for *you*, the builder. It is appropriate to reason about prevalence and mortality here. The *product* itself must contain none of this — no statistics shown to users, and above all no calorie/BMI/weight/macro inputs or outputs anywhere. The research justifies the build; it never becomes user-facing content.

---

## 4. What the clinical guidance actually recommends (and what it implies for an app)

Synthesised from NICE NG69, NIMH, and Mayo Clinic reference material.

| Clinical principle | What it means for Aspen |
|---|---|
| **Early intervention + specialist, community-based care** is the evidence-based path [NG69]. | Aspen's job is to *shorten the distance to specialist help*, not substitute for it. Every flow surfaces a route to real treatment. |
| **Family/carer involvement** improves outcomes, especially for young people [NG69]. | The "trusted contact" safety flow is clinically grounded, not just a nice-to-have. Make it first-class. |
| **Psychological therapy is first-line; medication alone is not effective for AN** [NG69 surveillance 2024]. | Aspen must not imply it is a treatment or that its tools "work" like therapy. Language stays modest: support, not cure. |
| **Self-monitoring (CBT homework) helps *some* people *some* of the time** — and is the basis of most existing apps. | This is the contested core. See §5. Aspen treats reflection as optional and never as compliance-tracking. |
| **Avoid anything that reinforces preoccupation** with food, weight, shape, or control. | The non-negotiables (§6). |

---

## 5. The central evidence-based decision: why Aspen does *not* center meal/calorie logging

This is the most important research finding for the rebuild, so it gets its own section.

The dominant ED apps (Recovery Record, Rise Up + Recover) are built on **self-monitoring**: log meals, behaviours, moods; share with a clinician. This is a legitimate CBT technique. But the qualitative evidence on how patients *actually experience* it is mixed in a way that matters:

A study of 41 patients using Recovery Record in a Danish ED service [Lomborg et al., PMC6035344] found that the same features sat on a **"supportive ↔ obstructive" continuum** depending on context:
- Some users felt **surveilled** by clinician monitoring and disengaged.
- Some found logging **pointless and burdensome** when a meal hadn't caused distress, so they logged "for the sake of logging."
- Critically: several users had previously been **habituated to weight-loss / calorie-tracking apps**, which they described as **"addictive" because easy calorie tracking gave immediate stress relief by reducing fear of weight gain.** In other words, a tracking UI can be *recruited by the disorder* as a compulsion.

The implication is precise, and it is **not** "logging is bad." Self-monitoring is a legitimate, NICE-endorsed CBT technique that helps many people. The evidence points at **three specific harm vectors**, not at logging in general:
1. **Numbers** — calories, macros, portions, weight, BMI. This is what gets recruited as a compulsion ("easy calorie tracking gave relief by reducing fear of weight gain"). *This is the line that must never be crossed.*
2. **Surveillance** — passive clinician monitoring that makes users feel watched.
3. **Enforcement / compliance framing** — logging that becomes an obligation, with shame on a "missed" entry, streaks that break, scores that judge.

Remove those three, and qualitative logging is not only safe but clinically mainstream.

> **Decision (you + team), reflected here:** Aspen **will** include optional **meal/food logging and behaviour/feeling logging**, notebook-style, *not enforced*, framed as gentle encouragement and self-knowledge — and crucially **adaptive to the person** (different disorders are not managed alike; see §5a and the onboarding questionnaire). This revises the earlier draft's blanket "no logging." It is a defensible middle path **on three strict conditions** below.

**Aspen's resolution (revised):**
- The **core loop is still emotional/cognitive**, not nutritional. Logging is a *supporting* feature, never the centre of gravity, never the home screen, never the thing the app nags you about.
- **Logging is qualitative and numberless — always.** "I had lunch; it felt hard but okay" / "felt anxious after" — **never** a number, calorie, portion, macro, weight, or BMI. The no-numbers rule (§6 rule 1) is *not* relaxed by this decision; it is the precondition for it.
- **Optional, non-enforced, no streaks, no shame.** No "you missed a meal," no compliance score, no broken-streak guilt. An empty day is fine and silent.
- **Disorder-adaptive (the key safeguard).** Because onboarding tailors the app to the user's presentation, food logging can be **softened, reframed, or fully suppressed** for presentations where it is contraindicated (e.g. restrictive AN, where even numberless meal-checking can become a control/compliance ritual), while behaviour/feeling logging — lower-risk across the board — stays available. ARFID gets a different framing again (sensory, not body-image). *This adaptivity is what makes "include logging" defensible rather than reckless.*
- **Clinician sharing is consent-based and revocable** (see `08_IDENTITY_LINKAGE_AND_CONSENT.md`), never passive surveillance.

**This needs your clinicians' sign-off, per disorder type.** You have ED-informed medical contacts (US + Pakistan). The exact question for them is not "logging yes/no" but: *for each presentation, is numberless qualitative meal logging encouraging or risky, and how should it be framed or suppressed?* Their answer should drive the adaptive rules. Treat this as a clinical-review gate, not a settled design.

---

## 5a. Disorder-adaptive onboarding (the personalisation that makes the above safe)

**Decision (you + team):** Aspen opens with a **questionnaire** that helps the app understand the kind of difficulty the person is facing, so support is tailored — *"no disorder is managed similarly."* This is one of the strongest decisions in the rebuild, because it is exactly what the clinical reality demands (`§2`: ARFID is not body-image-driven; restrictive vs. binge presentations need opposite handling). It also makes the logging decision (§5) safe, by letting the app withhold contraindicated features.

**How to do it without harm — the framing rules:**
- **It personalises; it does not diagnose.** The app must never tell the user "you have anorexia." It is a *"help us tailor Aspen to you"* tool that infers a **support profile** (e.g. restriction-leaning / binge-leaning / purge-leaning / avoidance-sensory / body-image-distress / mixed-or-unsure), used internally to choose framing and tools. No clinical label is shown.
- **It routes toward, never replaces, professional assessment.** The end of onboarding always surfaces "this isn't a diagnosis — here's how to reach real assessment/support."
- **The questions themselves obey the non-negotiables.** No "how many calories," no weight, no numeric frequency interrogations that feel clinical/triggering. Gentle, plain, skippable. Can be inspired by validated screening tools (e.g. SCOFF-style domains) but **softened and numberless**, and re-checked against §6.
- **The profile is editable and re-runnable.** People's needs change; the user can revisit it. Stored locally, private.
- **Drives adaptivity everywhere:** which logging is offered/suppressed (§5), companion language (`05`), tool emphasis, and copy.
- **Clinical-review gate.** The question set and the profile→behaviour mapping must be reviewed by your ED-informed advisors before shipping. This is the same gate as §5.

> Why this matters: a one-size app is *unsafe* here — body-acceptance language can alienate an ARFID user; meal-logging can harm a restrictive AN user; binge-focused framing can shame a BED user. Adaptivity isn't a nicety; it's a safety mechanism.

---

## 6. The non-negotiable design rules (derived, not invented)

Each rule below traces to evidence above. These are **hard constraints** enforced in code, copy review, and QA — not aspirations.

1. **No numbers tied to food, body, or weight — ever.** No calories, macros, BMI, weight, portion counts, "goal weight," numeric meal frequencies, or any quantification of eating, *anywhere*, including in logging and onboarding. Qualitative, numberless logging is permitted (§5); the moment a number attaches to food/body, the line is crossed. *(Derived from §5; reinforced by the appended clinical-safety guidance: "Don't introduce numbers — calories, BMI, weights, macros.")*
2. **No comparison mechanisms.** No leaderboards, no "others like you ate X," no visible peer metrics, no follower counts. *(General-social-platform harm, §1.)*
3. **No visual food/body feeds.** Text-first community; no image feed of meals or bodies. *(§1.)*
4. **No streaks, scores, or pass/fail states.** Progress is shown as *presence*, never as a metric that can be "broken." A broken streak is a relapse trigger. *(§5, compulsion risk.)*
5. **No harsh/failure language.** Never "failed," "missed," "incomplete," "you didn't." Caution states use soft amber, never alarm red. *(NICE emphasis on not reinforcing shame.)*
6. **No appearance comments, in any direction.** The companion and AI never say "you look healthy/good/better." *(Appended guidance: "you look healthy" can land as "you look fat.")*
7. **Never position any tool as treatment or cure.** Modest language only. *(NG69: therapy is first-line and specialist-led.)*
8. **Every flow has a human exit.** Crisis line, trusted contact, or treatment-finder is always ≤2 taps away. *(70%+ unmet need; the app is a bridge.)*
9. **Region-correct crisis resources, never NEDA.** The NEDA helpline is disconnected; the registry must use live, region-appropriate lines. *(Appended guidance; corroborated by search.)*
10. **The AI is a notebook, not an authority.** It reflects and supports; it does not advise on eating, diagnose, or instruct. *(§4.)*

---

## 7. Case study: the between-session evening (composite, illustrative)

*A composite scenario, not a real person, used to anchor design.*

> It's 9:40pm. **M** had a hard dinner — ate it, but the thoughts started immediately and haven't stopped. Their therapist is on Thursday, three days away. The urge to compensate is loud. Scrolling Instagram makes it worse; a "what I eat in a day" reel just made it much worse. They don't want to wake anyone. They don't think this is a "911 emergency," so a crisis line feels like too much. They just need the next twenty minutes to be survivable.

What Aspen should let M do, in order of escalation, with **no numbers and no judgement**:
1. Open the app to a calm, near-empty home — not a dashboard, not a prompt to log dinner.
2. Tap one thing: a grounding/urge-surfing exercise (timed breathing, 5-4-3-2-1, "ride the wave" — DBT distress-tolerance style, no food content).
3. If they want to externalise the thought: write it into a private, unstructured note that *stays on their device*. No scoring, no "great job logging."
4. Optionally summon the companion for low-pressure presence — something alive on the screen that doesn't demand anything.
5. If the AI is engaged, it reflects and validates ("that sounds really hard; the urge is loud right now and it will pass") — it does **not** problem-solve the eating.
6. A persistent, low-key route to *"talk to a person"* — trusted contact or region's ED/crisis line — is always visible but never pushed.

The design test for every screen: **"Would this help M at 9:40pm, or give the disorder something to grab?"**

---

## 8. What Aspen is *not* (boundaries that protect users)

- Not a treatment program, therapy replacement, or diagnostic tool.
- Not a meal/calorie/weight tracker.
- Not a social network with feeds, likes, or followers.
- Not an engagement-maximising product. Success is *the user closing the app feeling steadier*, not session length or DAU.
- Not a crisis service itself — it is a fast, reliable *router* to crisis services.

---

## 9. References

*Clinical & epidemiological*
1. NICE. *Eating disorders: recognition and treatment.* NICE guideline NG69. Published 23 May 2017, updated 16 Dec 2020. https://www.nice.org.uk/guidance/ng69
2. NICE. *2024 exceptional surveillance of eating disorders (NG69).* NCBI Bookshelf NBK607987. https://www.ncbi.nlm.nih.gov/books/NBK607987/
3. Global Burden of Disease 2021 — *Global, regional, and national burdens of eating disorders 1990–2021 and projection to 2035.* PMC12375436. https://www.ncbi.nlm.nih.gov/pmc/articles/PMC12375436/
4. Arcelus J, Mitchell AJ, Wales J, et al. *Mortality rates in patients with anorexia nervosa and other eating disorders: a meta-analysis of 36 studies.* Arch Gen Psychiatry. 2011;68(7):724–731.
5. López-Gil JF, et al. *Global Proportion of Disordered Eating in Children and Adolescents.* JAMA Pediatrics. 2023.
6. Hart LM, et al. *Unmet need for treatment in the eating disorders: a systematic review.* Clinical Psychology Review. 2011;31:727–735.
7. Galmiche M, et al. *Prevalence of eating disorders over the 2000–2018 period: a systematic literature review.* Am J Clin Nutr. 2019;109(5):1402–1413.
8. Ramaswamy N. *Overreliance on BMI and Delayed Care for Patients With Higher BMI and Disordered Eating.* AMA J Ethics. 2023;25(7):E540–544.
9. National Alliance for Eating Disorders — statistics overview (2024). https://www.allianceforeatingdisorders.com/eating-disorder-statistics-an-updated-view-for-2024/

*Product / app evidence*
10. Lomborg/Pedersen et al. *Patient Experiences Using a Self-Monitoring App (Recovery Record) in Eating Disorder Treatment: Qualitative Study.* PMC6035344. https://pmc.ncbi.nlm.nih.gov/articles/PMC6035344/
11. Recovery Record — official site. https://www.recoveryrecord.com/
12. Rise Up + Recover (Recovery Warriors). https://recoverywarriors.com/
13. MedCentral. *Eating Disorders: Which Mobile Apps Help Patient Adherence.* (Notes NEDA/ANAD-recommended apps and privacy concerns.)

*Prevalence aggregators (directional, lower-tier; used only for trend direction)*
14. SingleCare, *Eating disorder statistics and facts 2026.*
15. Rehab Seekers, *Eating Disorder Treatment and Prevalence Statistics 2025–2026.*

> **Source-quality note.** Items 1–10 are primary/peer-reviewed/official and carry the load-bearing claims. Items 14–15 are secondary aggregators used only to indicate *direction* (e.g., "prevalence is rising"), never for precise figures. Treat any single percentage as approximate; the *pattern* is robust.
