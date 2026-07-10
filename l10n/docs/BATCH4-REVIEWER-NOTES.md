# Batch 4 — Companion messages + AI surfaces (30 keys × 7 languages)

Covers: Companion messages (14), AI reflection companion (7), AI safety fallback (1),
AI consent / deeper reflection (8). **This completes all SENSITIVE surfaces** — every
remaining untranslated key in the app is STANDARD tier. All rows `PENDING`; same merge
path, then `bulk_approve.py` after sign-off.

## Highest-priority rows for review

**The crisis-handoff trio** (`safety_ai_fallback`, `reflect_companion_handoff`,
`reflect_companion_handoff_button`) is the most consequential copy in this batch — it's
what a user sees when the AI steps back because the conversation crossed the safety
threshold. Each draft was checked against three constraints: validate the feeling, don't
promise the AI's ongoing presence, and route firmly to a real human. These rows are
marked CRISIS-HANDOFF in the notes column and deserve their own review pass.

**The consent copy** (`settings_ai_dialog_body`, `_subtitle_off`, `_local_note`) makes
literal privacy claims — "sent securely to an AI service", "never leave this device",
"can be deleted". These must survive translation as *factual statements*, not vibes, and
should be seen by whoever owns privacy language, not only the ED reviewer. Flagged
per-row.

## Voice decisions

**The companion speaks in tiny, non-demanding sentences** and every language keeps that
shape — no sentence grew clauses in translation. Two recurring companion moves were
protected: it *offers* rather than instructs (every "Want to…?" stays a genuine
question — چاہیں تو / चाहें तो / 想…吗 / Magst du / ¿Quieres…? / أتحب / Envie de…?), and
it never claims to fix anything (`companion_hard_no_fixing` stays presence-only in all
seven).

**Companion grammatical gender is a new open decision for Urdu/Hindi.** The companion
speaks as "I", and Urdu/Hindi first-person verbs are gendered. The drafts dodge it with
neutral constructions (subjunctive لے چلوں / ले चलूँ, stative ساتھ ہوں / साथ हूँ, and
shifting agreement onto objects — "یہ محفوظ رہیں گے"). That works for all 14 messages
here, but your advisors should make an explicit house-style call, because the AI
reflection companion generates free text at runtime and the system prompt will need the
same convention. Add this to the existing Arabic-gender decision.

**The weather metaphor** (`companion_hard_passing`) translated cleanly everywhere and
stays non-instructive — it describes transience without telling the user what to do.

**Terminology continuity:** the "reflection" word matches batch 1's `nav_reflect` in
every language (غور و فکر / चिंतन / 反思 / Reflexion / Reflexión / تأمّل / Réflexion), so
the consent dialog, the companion screen, and the nav tab all name the same feature.

**`reflect_companion_thinking`** stays an ellipsis in every language (fullwidth …… in zh).

## Status after batch 4

| Surface | Keys | State |
|---|--:|---|
| Crisis/safety screen, Home, Nav, Common | 23 | ✅ batch 1 |
| Onboarding questionnaire | 51 | ✅ batch 2 |
| Grounding tools | 25 | ✅ batch 3 |
| Companion + AI surfaces | 30 | ✅ batch 4 |
| **All SENSITIVE copy** | **~128** | **drafted, pending advisor review** |
| STANDARD remainder (Reflection & logging, Feeling tags, Notifications, Overlay, Companion-presence & Language settings, Account, Backup, Settings) | ~115 | next |

Not yet started (outside UI strings): per-language AI system prompt (`ai-prompt/<lang>.txt`),
the five empty safety lexicons, French `TARGET_LANGS` wiring, and the crisis-sheet
advisor verifications from batch 1.

Batch 5 would be the STANDARD remainder — bigger in key count but faster to review
(normal translation bar, no ED-informed native-speaker requirement).
