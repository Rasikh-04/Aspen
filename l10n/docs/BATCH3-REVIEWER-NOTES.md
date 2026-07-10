# Batch 3 — Grounding tools (25 keys × 7 languages)

All rows `PENDING`; same merge path as batches 1–2, then `bulk_approve.py` after sign-off.

## The decisions your reviewers should check first

**"Ride the urge" is not translated literally anywhere.** It's the urge-surfing
technique, and a literal "ride" reads absurd or aggressive in most of these languages.
Every draft renders it as a *let-it-pass / wave* phrasing (لہر کو گزرنے دیں · लहर को
गुज़रने दें · 让冲动过去 · Den Drang vorbeiziehen lassen · Dejar pasar el impulso ·
دَع الرغبة تمرّ · Laisser passer l'envie), which also keeps the menu label and the
`ride_urge_body` wave metaphor telling one coherent story. `grounding_ride_urge` and
`ride_urge_title` are kept **identical** per language — they're the same feature.

**The word for "urge" is the sharpest choice in this batch.** German *Drang*, Spanish
*impulso*, Arabic *رغبة*, French *envie*, Chinese 冲动, Urdu/Hindi *خواہش/इच्छा* — each
was picked to stay behaviour-neutral (not naming what the urge is toward). French
reviewers may debate *envie* vs *pulsion*; the draft uses *envie* because *pulsion*
carries clinical/psychoanalytic weight that can feel pathologising. Flag if your
ED-informed reviewer disagrees.

**Digits stay digits.** `Ground (5-4-3-2-1)` keeps the numerals in every language —
they're the technique's name, not a countable quantity. One thing to check when the
missing safety lexicons get filled: make sure the per-language forbidden-token lists
don't accidentally lint the technique label.

**Breathing pacer words (`breathe_in`/`hold`/`out`) are kept to one or two short words**
since they presumably sync with an animation: شہیق/أمسِك/زفير, Inhala/Mantén/Exhala,
吸气/屏住/呼气, Einatmen/Halten/Ausatmen. If the animation timing is tight, these are the
strings to test on-device first — Urdu/Hindi "سانس اندر لیں / साँस अंदर लें" are the
longest and could be shortened to "اندر / अंदर" style cues if they overflow.

**The 5-4-3-2-1 counts were hand-checked** — five/see, four/hear, three/touch, two/smell,
one/taste survive in all seven languages with correct number–noun agreement (including
the Arabic dual شيئان for "two things").

**Gender-neutrality carried over from batch 2.** French `grounding_gentle_close` and
`ride_urge_body` were rephrased to avoid `content·e` / `obligé·e`; Urdu/Hindi first-person
lines ("میں ابھی ٹھیک ہوں" / "मैं अभी ठीक हूँ") are naturally neutral; Arabic keeps the
masculine-default convention pending your house-style decision.

**One deliberate divergence:** `grounding_talk`'s English is "Write it down" (despite the
key name saying "talk") — drafts translate the *text*, not the key name.

## Running total
Batches 1–3 now cover 98 of 244 keys per language: Home, Navigation, Common, the full
Crisis/safety screen, the full Onboarding questionnaire, and all Grounding tools — i.e.
every SENSITIVE surface a user meets before the companion/AI features. Next natural
batch: Companion messages + AI reflection/consent/fallback (the remaining SENSITIVE
surfaces), or the STANDARD bulk (Reflection, Feeling tags, Notifications, Settings,
Account/Backup) if you'd rather clear volume first.
