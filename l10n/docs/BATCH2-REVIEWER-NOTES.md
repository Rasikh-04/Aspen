# Batch 2 — Onboarding questionnaire (51 keys × 7 languages)

All rows `PENDING`; provenance and per-row flags are in the `notes` column. Same merge
path as batch 1: paste rows into `l10n/worksheets/ui.<lang>.csv` (or replace the matching
key rows), advisors review, then `bulk_approve.py`.

## Translation decisions your reviewers should check first

**The two euphemism-critical items (q5, q2_undo_after).** The English deliberately never
names compensatory behaviours ("make up for it somehow", "something I try to undo
afterwards"). Every draft preserves that indirection — e.g. Urdu تلافی, Hindi भरपाई,
Chinese 弥补, German wiedergutmachen, Spanish compensarlo, Arabic تعويض, French compenser.
A reviewer tempted to make these "clearer" would be making them more harmful; the vague
register is the clinical design. These rows carry an explicit note.

**q6 (ARFID screen).** The question's whole job is separating sensory/consequence-based
difficulty from body/shape concern. The drafts keep the two clauses structurally distinct
in every language, but this is the sentence most likely to collapse in translation —
flagged in notes for a careful read.

**Grammatical gender.**
- Urdu/Hindi first-person verbs are gendered. Where the English is first-person
  ("I try to undo…", "how I feel in my body"), the drafts use neutral rephrasings
  (e.g. ur "مٹانے کی کوشش ہوتی ہے", hi "मिटाने की कोशिश होती है") so no user is
  misgendered by default. `onb_prefer_not` uses the colloquial-neutral
  "مجھے یہ نہیں بتانا" / "मुझे यह नहीं बताना". Reviewer should set a house style.
- Arabic second person is gendered; drafts use the masculine-default convention
  (خذ وقتك, لست متأكدًا). Reviewer should decide: masculine default, feminine,
  or dual string variants.

**Register.** Matches batch 1: German du, Spanish tú, French tu, formal-warm آپ/आप for
Urdu/Hindi. If your advisors prefer Sie/usted/vous, both batches need the same flip.

**"Aspen"** kept untranslated everywhere (DO_NOT_TRANSLATE brand rule).

**Likert scales.** `onb_likert_*` (Not really/Sometimes/Often) is shared across q3–q5;
the same triple is used consistently so the scale reads identically across questions.
q7/q8 have their own answer sets ("A lot" appears in both — translated identically:
بہت زیادہ / बहुत ज़्यादा / 很大 / Sehr / Mucho / كثيرًا / Beaucoup).

## Remaining after this batch
Grounding tools, Crisis-screen leftovers if any, Reflection & logging, Feeling tags,
Companion messages, AI consent + reflection + fallback, Notifications, Overlay,
Account/Backup/Settings, and the AI system prompt per language. Say the word and I'll
do batch 3 (grounding tools — breathe_/ground_54321_/ride_urge_ keys) next.
