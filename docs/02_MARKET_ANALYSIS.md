# 02 — Market Analysis

> Where the ED-support app space actually is, what the incumbents do, where they fail recovering users, and the niche Aspen occupies. Used to make sure the rebuild is differentiated for the right reasons, not just different.

---

## 1. Market context

- The digital mental-health / mHealth market is large and crowded: **10,000+ mHealth apps** exist, with a slow-growing body of clinical guidance on their use [MedCentral 2021].
- ED specifically is a **rising-burden, under-served** market: prevalence roughly doubled in a decade, **70%+ of sufferers never access specialist care**, and demand spiked post-COVID [GBD 2021; NEDA/Hart]. The unmet-need population is the addressable need, and it is enormous.
- Yet the *dedicated ED-app* shelf is surprisingly thin and stagnant. The two names that dominate every "best ED apps" list have for years been **Recovery Record** and **Rise Up + Recover**, both architected around the same CBT self-monitoring loop. Several other listed apps (Brighter Bite, Jourvie, Recovery Warriors content) are smaller or content-led.

**Read:** large and growing demand, dominated by a small number of monitoring-centric incumbents whose core loop the evidence shows is double-edged (see `01` §5). That is a real opening for a product built on a different premise.

---

## 2. Competitor teardown

### Recovery Record — the incumbent "platform"
- **What it is:** the most feature-complete ED self-monitoring app; patient app (free) + paid clinician portal. HIPAA-compliant. Clinician–patient linkage so providers monitor logs between sessions.
- **Core loop:** log meals (incl. photos), thoughts, triggers, behaviours; CBT/DBT/ACT/FBT coping content matched to current needs; questionnaires (EDE-Q); **meal plans, weight charting, BMI** in the clinician tooling; graphs/insights.
- **Strengths:** clinically embedded, evidence-cited adherence gains, mature, trusted by clinicians.
- **Where it fails the between-session user:** it is fundamentally a **clinician's monitoring instrument**. The patient-experience research on it [PMC6035344] found surveillance discomfort, "logging for logging's sake," and that its tracking surface can be recruited as a compulsion by users habituated to calorie apps. It can feel **overwhelming** (a recurring review theme). It contains the very numbers (weight, BMI, meal plans) Aspen forbids.

### Rise Up + Recover — the "simpler" alternative
- **What it is:** free, Android + iOS, no login required, optional PIN. Built by Recovery Warriors.
- **Core loop:** quick meal/behaviour/mood logs; **coping-skills library** (widely praised); affirmations/quotes; **PDF export** to share with a treatment team (no passive clinician surveillance — user-initiated); **ConnectED** in-app treatment directory.
- **Strengths:** lightweight, private-feeling, low-friction; the coping-skills + treatment-finder combo is genuinely good and well-loved; for ~⅓ of users it's their *first* contact with treatment-seeking.
- **Where it fails:** still **meal-log-centric**; limited longitudinal view; mood captured once/day; some expert-review notes the program had accessibility/availability gaps over time. Body/eating remains the organising axis.

### The rest of the shelf
- **Brighter Bite** — ACT/DBT "recovery buddy"; thought/mood + meal/behaviour tracking; smaller.
- **Jourvie** — ANAD-recommended; monitoring-oriented.
- **MindShift CBT / Cognitive Diary / Calm / Headspace** — *general* anxiety/CBT/meditation tools, not ED-specific; useful adjuncts but not built for ED triggers and contain no ED-specific safety design.
- **General social + "wellness" apps (MyFitnessPal-style, fitness trackers)** — actively contraindicated; these are the calorie-tracking apps the research flags as compulsion fuel.

### Adjacent but not competitors
- **Crisis lines / text lines** (region-specific), **NHS / specialist services**, **carer guides**. Aspen routes *to* these; it doesn't compete with them.

---

## 3. Feature comparison matrix

| Capability | Recovery Record | Rise Up + Recover | General wellness apps | **Aspen (proposed)** |
|---|---|---|---|---|
| Meal / food logging | ✅ core | ✅ core | ✅ (often calorie) | ❌ **never** |
| Calorie / BMI / weight | ✅ (clinician) | partial | ✅ | ❌ **never** |
| Streaks / scores / gamified progress | partial | partial | ✅ | ❌ **never** |
| Coping / grounding tools | ✅ | ✅ (strong) | ✅ (generic) | ✅ **core** |
| Unstructured private reflection | partial | partial | ✅ (generic) | ✅ **core, on-device** |
| Text-only, no-feed community | ❌ | ❌ | ❌ | ✅ (later phase) |
| AI reflection companion | ❌ | ❌ | some (generic) | ✅ (notebook, not therapist) |
| **On-screen overlay companion** | ❌ | ❌ | ❌ | ✅ **unique** |
| Crisis routing / trusted contact | partial (directory) | ✅ (ConnectED) | ❌ | ✅ **first-class, region-aware** |
| Clinician surveillance | ✅ (passive) | ❌ (export only) | ❌ | ❌ (export only, later) |
| On-device-first privacy | ❌ (cloud) | partial | ❌ | ✅ **core** |
| Price | free + paid clinician | free | freemium | **free, donation-funded** |
| Platform | iOS + Android | iOS + Android | both | **Android-first (native)** |

The matrix makes the positioning obvious: **every incumbent organises the product around food/body data.** Aspen organises it around *the emotional between-session gap* and *fast routes to humans*, and removes the food/body data surface entirely.

---

## 4. Positioning statement

> **For** people living with an eating disorder who need support in the hours between appointments,
> **Aspen is** a free, calm, text-first Android companion
> **that** helps them get through hard moments and reach real help fast,
> **without** tracking food, weight, or "compliance" — because those features feed the disorder.
> **Unlike** Recovery Record and Rise Up + Recover, which are built around clinical self-monitoring,
> **Aspen** is built around presence, reflection, and safety, and stores the most sensitive things on the user's own device.

---

## 5. Differentiation — and the honest risks of it

**Genuine, defensible differentiators**
1. **No food/body data surface at all** — evidence-grounded, not a gap.
2. **On-screen companion** — no ED app does this; it's a novel form of low-pressure presence (see `05`).
3. **On-device-first privacy** — the sensitive data never has to leave the phone.
4. **Free + donation** — removes the cost barrier entirely for an under-treated population.
5. **Safety as a first-class, region-aware subsystem** — most apps bolt on a directory; Aspen treats routing-to-humans as core.

**Risks I won't paper over**
- **"No logging" may reduce stickiness** by conventional metrics. *That is acceptable and arguably correct* — engagement is not Aspen's success metric (see `01` §8). But it means donation/grant funding, not engagement-driven growth, must sustain it (`03` feasibility).
- **No clinician portal initially** means no clinician-driven distribution (Recovery Record's growth engine). Aspen's early distribution is direct-to-user + advocacy orgs.
- **The companion could itself become a dependency or an intrusion** for some users. This is a real risk and is mitigated by hard design rules in `05` — but it must be user-tested with the population, not assumed safe.
- **Trust & safety liability**: any app touching this population must be impeccable on crisis handling and must never appear to give eating advice. This raises the bar on QA and copy review (`06`).

---

## 6. Funding / sustainability landscape (free + donations)

Since Aspen is free and donation-funded, "market" includes how it stays alive:
- **Individual donations** (in-app "support Aspen," no paywalled features behind it).
- **Grants**: mental-health foundations, digital-health funds, university/innovation grants (relevant given an academic context).
- **Partnerships / sponsorship** with ED charities and advocacy orgs — who can also drive trustworthy distribution.
- **Explicitly avoided:** ads (contraindicated for this audience), data monetisation (ethically off-limits), and engagement-based revenue (mis-aligned with the mission).

Detailed cost/sustainability modelling is in `03_REQUIREMENTS_AND_FEASIBILITY.md` §Financial.

---

## 7. Sources

- MedCentral, *Eating Disorders: Which Mobile Apps Help Patient Adherence* (NEDA/ANAD-recommended apps; privacy concerns; 10,000+ mHealth apps). https://www.medcentral.com/behavioral-mental/eating/eating-disorders-mhealth-apps-adherence
- Recovery Record. https://www.recoveryrecord.com/
- Rise Up + Recover / Recovery Warriors. https://recoverywarriors.com/8-reasons-the-rise-up-recover-app-is-the-perfect-companion-to-therapy/
- Lomborg et al., Recovery Record patient-experience qualitative study, PMC6035344.
- Healthline, *The Best Eating Disorder Recovery Apps.* https://www.healthline.com/health/top-eating-disorder-iphone-android-apps
- MindTools.io expert reviews (Rise Up + Recover; Recovery Record).
- Eating Disorder Hope, *Best Eating Disorder Apps.* (Brighter Bite, etc.)
- Prevalence/unmet-need sources as cited in `01` §9.
