# Batch 5 — STANDARD remainder (115 keys × 7 languages)

Covers: Reflection & logging (15), Feeling tags (12), remaining companion keys (5),
Companion presence settings (4), Overlay settings (7), Overlay notification (4),
Notifications (5), Account (24), Backup (26), Language settings (4), Settings (9).
**This completes the full 244-key catalog in all 7 languages.** All rows `PENDING`.

## Flags found while drafting

**CLASSIFY ordering bug (worth fixing in `l10n_review.py`).** `companion_species_*`,
`companion_a11y_label`, and `companion_rest_action` match prefix `companion_` (SENSITIVE)
before ever reaching `companion_species_` (STANDARD) — the comment says "most-specific
prefixes first" but they aren't ordered that way. The drafts mark these 5 rows SENSITIVE
to match what the generator actually produces; move `companion_species_` above
`companion_` if STANDARD was the intent.

**Language picker gap.** Only `language_en` and `language_ur` exist — there are no
`language_de/zh/hi/ar/es` (or `fr`) keys, so users can't actually *select* the other
five shipped languages. New keys needed; values are endonyms (Deutsch, 中文, हिन्दी,
العربية, Español, Français) and shouldn't vary by UI language.

**Feeling tags rendered as nouns** in ur/hi/es/ar/fr (سکون، थकान, Calma, هدوء,
Fatigue…) so no tag misgenders the user — adjectives are gendered in these languages.
German and Chinese keep adjectives (ungendered there). Two judgment calls flagged
per-row: es/fr "Vacío"/"Vide" for *Numb* (alternative: Entumecimiento/Engourdissement).

**Security copy translated literally, flagged per-row.** `backup_note_keymodel` ("we
store only what we cannot read") and `backup_code_body` ("email reset brings back your
sign-in, never your backup") are zero-knowledge claims — reviewers must not soften them.
`account_id_caption` keeps the `%1$s` placeholder verbatim in every language.

**Android permission quote.** The overlay dialog quotes "display over other apps" — each
draft uses the common OS phrasing (de „über anderen Apps angezeigt", zh
"显示在其他应用上层", es «muestre sobre otras aplicaciones»…) but reviewers should match
the exact system string on a device in that language.

**Anti-shame framing preserved:** "Around eating" / "a moment around eating" stays
indirect in every language (کھانے کے آس پاس / 与吃有关 / Rund ums Essen / En torno a la
comida / حول الأكل / Autour du repas) — never "food log" or "meal diary".

## Full-catalog status: 244/244 keys drafted × 7 languages, all PENDING.
Still open (non-UI-string work): advisor verification of the batch-1 crisis sheets
(UK hours, PK Umang call-test), the five empty safety lexicons, per-language AI system
prompts, French TARGET_LANGS wiring, the language-picker keys above, and the CLASSIFY fix.
