# 03 — Requirements & Feasibility

> What the system must do (functional), how well it must do it (non-functional), the safety requirements that override everything, and whether a free, Android-first, donation-funded build is actually viable on the technical, operational, and financial axes.

---

## Part A — Requirements

Requirements use **MoSCoW** priority (Must / Should / Could / Won't-for-now) and map to the principles in `01` §6. Every "Must" is testable.

### A.1 Functional requirements

**FR-1 — Calm home (the "quiet room")** · *Must*
The first screen *after onboarding* is near-empty, low-stimulation, with one or two gentle entry points (e.g. "I'm having a hard moment," "Write something down"). No dashboard, no metrics, no prompts about food/numbers. Cold-launch to interactive < 1s on a mid-range device (NFR-perf).

**FR-0 — Disorder-adaptive onboarding questionnaire** · *Must*
First-run questionnaire that infers a **support profile** (restriction-/binge-/purge-leaning, avoidance-sensory/ARFID, body-image-distress, mixed/unsure) to tailor the app. **Personalises, never diagnoses**; never shows a clinical label; routes toward real assessment at the end. Questions are gentle, plain, skippable, **numberless** (no calorie/weight/frequency interrogation), and obey `01` §6. Profile stored locally, editable, re-runnable. Drives which logging is offered/suppressed (FR-3b), companion language, and tool emphasis. **Question set + profile→behaviour mapping require clinical-advisor sign-off** before release (`01` §5a).

**FR-3b — Optional, numberless meal/food + behaviour/feeling logging** · *Should*
Notebook-style, **not enforced**, framed as gentle encouragement/self-knowledge. **Qualitative and numberless only** — text + feeling tags; **no calories, portions, weight, macros, or any number** (SR-1 unchanged). **No streaks, no scores, no "missed meal," no shame.** Empty days are silent. **Adaptive:** food logging is softened/reframed/**suppressed** for presentations where it's contraindicated (per FR-0 profile + advisor rules); behaviour/feeling logging is lower-risk and broadly available. On-device, encrypted. One-tap permanent delete. **Clinical-advisor sign-off required per disorder type.**

**FR-3 — Private reflection (unstructured)** · *Must*
A plain, free-text space to externalise thoughts. **On-device by default, encrypted at rest.** No structure imposed, no scoring, no streaks, no "you wrote N times." Optional gentle prompts, never about eating/numbers. One-tap permanent delete.

**FR-4 — Safety / crisis routing** · *Must (highest)*
Always-reachable (≤2 taps from anywhere) path to: (a) region-appropriate ED/crisis lines, (b) a user-set **trusted contact**, (c) a treatment-finder link. Region-aware resource registry. Works offline for cached numbers. **Never NEDA.** See `06` Safety spec.

**FR-5 — AI reflection companion (notebook, not therapist)** · *Should*
An optional conversational surface that validates, reflects, and helps the user articulate feelings. **Hard-constrained**: no eating advice, no meal/calorie content, no diagnosis, no appearance comments, no crisis-handling beyond warm hand-off to FR-4. Must degrade gracefully offline (queue or local fallback). Local-first/cloud-optional per `04`.

**FR-6 — On-screen overlay companion** · *Should*
The animated character that can live on the screen (home-screen-only or system-wide, configurable), be summoned/dismissed, dragged, and offer low-pressure presence and optional gentle check-ins. Fully optional, off by default. Full spec + guardrails in `05`.

**FR-7 — Text-only community** · *Could (later phase)*
Moderated, text-only peer support. No images, no feeds, no likes/followers, no comparison. Pro-recovery only; trigger-aware moderation. Deferred until the single-user experience is solid and moderation capacity exists.

**FR-8 — Settings & consent** · *Must*
Granular control over: companion behaviour/scope, AI on/off and cloud-consent, notifications (off by default), data location, trusted contact, PIN/biometric lock. Plain-language privacy explanation.

**FR-9 — Anonymous use + optional app-native account** · *Must*
Core features (FR-0–6) usable with **no account, no email**. Optional account is **Aspen-native** (not federated-dependent): email and/or "continue with Google/Apple" as *convenience* auth into an account that exists in Aspen's own DB; user can **add a password or change method later** even after a social login. Full model in `08`.

**FR-12 — Scoped, revocable consent primitive** · *Must (build early)*
A single consent mechanism: grants are **scoped** (data category), **directed** (recipient), optionally **time-boxed**, **instantly revocable**, **auditable**, **default-deny**. Powers all current/future sharing (clinician linkage, export). Build the primitive early even before recipients exist (`08` §3) so later features don't require breaking changes.

**FR-13 — Clinician/support linkage (phased, consent-gated)** · *Could / later*
Tier 1 directory (find help) → Tier 3 link-your-own-clinician (consent-scoped data share or encrypted export) → *(separate future phase)* Tier 2 affiliated real-time telehealth. All consent-gated via FR-12; Tier 2 requires its own legal/clinical governance (`08` §4). Not in v1 beyond architectural readiness.

**FR-10 — Donation surface** · *Should*
A non-intrusive "support Aspen" option. **No feature is paywalled.** Never shown during a hard-moment/crisis flow.

**FR-11 — Data export & delete** · *Must*
User can export their own reflections (plain format) and **permanently delete all data** locally and in cloud (if used). No dark patterns.

### A.2 Non-functional requirements

**NFR-perf — Performance**
- Cold start to interactive: **< 1s** (target), < 1.5s (ceiling) on a mid-range device (e.g. Snapdragon 6-series / 4GB RAM).
- Companion overlay sustains **60fps** while active; must not jank the foreground app.
- Grounding animations: smooth 60fps, no dropped frames during breathing cycles (these are watched closely by anxious users; jank is anti-therapeutic).

**NFR-batt — Battery / resource**
- Overlay companion idle drain target: **< 1%/hr** additional; achieved via render-only-when-visible, no busy loops, frame-throttling when static, and suspending entirely when a fullscreen app is foregrounded.
- No background polling. No always-on network. AI calls are on-demand only.
- App APK size lean (target < 30MB base; companion sprite packs downloaded on demand, like Shimeji's model).

**NFR-priv — Privacy / security**
- Reflections and AI history encrypted at rest (SQLCipher or equivalent). Device-lock / biometric optional gate.
- No third-party analytics SDKs in core flows; no ad SDKs ever; no data sale ever.
- If cloud is enabled, end-to-end posture where feasible; explicit, revocable consent; data minimisation.
- Crash reporting (if any) must be privacy-preserving and must never capture reflection content.

**NFR-a11y — Accessibility** (full detail in `06`)
- WCAG 2.2 AA equivalent: dynamic type, screen-reader labels, sufficient contrast (within the calm palette), reduced-motion mode (disables companion animation and breathing motion), large hit targets (≥48dp).

**NFR-reli — Reliability**
- Safety routing (FR-4) must function **offline** and must be the most-tested path in the app. Target: zero defects shipped on crisis flow; release-blocking.
- No data loss on reflections across crashes/updates.

**NFR-i18n — Localisation (first-class; see `docs/12`)**
- **7 languages from first release**, worldwide, auto-selected by system default, English as fallback: **English, Urdu, German, Mandarin, Hindi, Arabic, Spanish.**
- **RTL from day one** (Urdu, Arabic) — layouts mirror, logical start/end, mirrored assets. Complex-script fonts (Nastaliq, Arabic, Devanagari, CJK) bundled/verified.
- **No hardcoded user-facing strings** — all externalised from Phase 1.
- **Sensitive surfaces** (crisis resources, questionnaire, companion lines, AI output) require **native-speaker, ED-informed review per language**; machine translation not acceptable for these.
- Crisis resources are per-**country** data; **language (UI) and region (crisis registry) are independent** — never infer one from the other.
- **Copy-lint runs per language** (forbidden number/shame tokens in every language) — release-blocking.

### A.3 Safety requirements (override all others)

These are requirements, not guidelines, and they **block release** if unmet:

- **SR-1** No user-facing numbers for food/weight/body/calories/macros anywhere — enforced by a lint/CI check on strings + code review (`06`).
- **SR-2** Crisis resources are current, region-correct, and never NEDA; verified by a checklist before each release.
- **SR-3** AI output is constrained by system prompt + output filter; any eating-advice/number/appearance content is blocked and replaced with validation + hand-off. Red-team suite required.
- **SR-4** Companion and notifications can never use guilt, streaks, urgency, or appearance language.
- **SR-5** No flow may trap a user away from a human exit.
- **SR-6** Reduced-motion and "calm mode" fully disable potentially activating animation.

---

## Part B — Feasibility study

### B.1 Technical feasibility — **Feasible**

- **Overlay companion** is well-established on Android via the `SYSTEM_ALERT_WINDOW` permission; multiple shipping apps (Shimeji family) prove the pattern, including drag, tap-to-interact, pass-through mode, hide-on-fullscreen, and lightweight footprints [Shimeji apps, Google Play / Softonic]. No novel platform capability is required — the novelty is in *purpose and restraint*, not in the technical mechanism.
- **On-device encrypted storage, offline tools, native animation** are all standard Android (Jetpack, Room+SQLCipher, Compose/Canvas).
- **AI**: cloud LLM via API is trivial; on-device small-model is feasible but quality-limited — hence the local-first/cloud-optional design (`04`).
- **Risk areas (manageable):** OEM-specific overlay/battery restrictions (Xiaomi/Samsung aggressive killers), overlay permission UX friction, and accessibility-service scope creep (we intend to *avoid* AccessibilityService where possible — see `05` for why). All are known, documented problems with known mitigations.

### B.2 Operational feasibility — **Feasible with caveats**

- **Single-user experience (v1)** is operationally light: no servers required if local-first, no moderation load, no clinician relationships.
- **The real operational cost is trust & safety**: keeping the crisis registry current per region, reviewing all user-facing copy against the non-negotiables, and (if community ships) staffing moderation. **Caveat:** community (FR-7) should not launch until there is a sustainable moderation answer; otherwise it becomes a liability. This is why it's deferred.
- **Clinical sign-off:** before public launch, the safety flows and copy should be reviewed by a qualified ED clinician/advisor. Budget for an advisory relationship, even pro-bono via an ED charity.

### B.3 Financial feasibility — **Feasible at small scale; depends on funding model, not users**

Because the app is free, costs must be covered by donations/grants, and the architecture is chosen to keep costs near-zero at low scale:

| Cost driver | Design choice that controls it | Rough scale behaviour |
|---|---|---|
| Backend servers | **Local-first**; no server needed for core v1 | ~$0 until cloud/community |
| AI inference | On-demand only; cloud-optional; can cap / queue; can fall back to on-device | Scales with *active AI use*, not installs; capable of a hard monthly budget |
| Storage | On-device | ~$0 |
| Distribution | Google Play one-time fee | ~$25 one-time |
| Clinical advisory | Pro-bono / charity partnership where possible | low / in-kind |
| Moderation (if community) | Deferred until funded | the main future cost |

**Conclusion:** a free, Android-first, local-first Aspen is financially viable to *build and run at small scale on near-zero infrastructure cost*, with AI as the only usage-scaling cost — and that cost is controllable (budget caps, on-device fallback). The binding constraint is **engineering time and trust-and-safety diligence**, not infrastructure spend. Donations/grants need to cover AI inference + (later) moderation, not a server fleet.

### B.4 Legal / ethical feasibility — **Feasible, with required diligence**

- Not a medical device if positioned as *support, not treatment/diagnosis* — but positioning must be disciplined (no efficacy/treatment claims) and reviewed.
- Privacy law (GDPR/UK-GDPR if EU/UK users; local equivalents): local-first + data-minimisation makes compliance dramatically easier. A clear privacy policy and consent flow are required.
- **Duty-of-care reality:** an app this population relies on in crisis carries moral (and potentially legal) weight. The crisis flow's reliability is therefore a release-blocker, not a feature.

---

## Part C — Out of scope for v1 (explicit)

- iOS app (architecture leaves the door open; not built now).
- Clinician portal / passive monitoring (export-only if ever).
- Community (deferred until moderation is funded).
- Any food/weight/calorie capability (permanently out of scope, not "later").
- Wearables / sensors.
