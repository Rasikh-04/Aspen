# Aspen — advisor review & translation

This folder is where **every user-facing string in Aspen** is reviewed, signed off, and
translated into all supported languages, then imported back into the app. It exists so this
work is a **content task** for advisors and native-speaker translators — not a code change
(docs/12 §3–§4, CLAUDE.md #11).

Nothing here is shipped app code. It is the review surface between the codebase and the
people who own trust-and-safety and translation.

---

## What's in scope

Everything a user can read or hear: onboarding questionnaire, grounding tools, the crisis /
safety screen, reflection & feeling-log copy, **companion messages**, **notifications**,
**AI "deeper reflection" consent + the AI system prompt**, account & backup copy, settings.
Plus two safety data surfaces: the **crisis registry** (real phone/URL numbers) and the
per-language **safety lexicons** (numberless/anti-shame copy-lint tokens + crisis-sign
phrases).

See **[catalog.md](catalog.md)** for the full generated inventory and current coverage.

---

## Sensitivity tiers (drives the review bar)

| Tier | Bar |
|---|---|
| 🔴 **SENSITIVE** | ED-informed **native-speaker** review is mandatory. **Machine translation is not acceptable.** A SENSITIVE string **cannot ship in a language until its status is `APPROVED`** — the importer refuses to emit it otherwise (docs/12 §5). Surfaces: questionnaire, grounding, crisis/safety screen, companion messages, AI reflection/consent/fallback, AI system prompt. |
| STANDARD | Native translation + a normal review pass. |
| DEV_ONLY | Debug-only strings, never shipped; no translation needed. |
| DO_NOT_TRANSLATE | Brand / proper nouns (e.g. "Aspen"); identical in every language. |

---

## The files

```
l10n/
  README.md                      ← you are here (the workflow)
  catalog.md                     ← GENERATED master inventory + coverage matrix
  worksheets/
    ui.<lang>.csv                ← all UI strings, one sheet per language (en = source of truth)
    crisis.<country>.csv         ← crisis registry: verify the real numbers + review copy
    safety-lexicon.<lang>.csv    ← forbidden tokens + crisis-sign phrases, per language
    ai-prompt/en.txt             ← the AI system prompt (English source); add <lang>.txt per language
  tools/
    l10n_review.py               ← generate the worksheets / import approved ones back
```

### `ui.<lang>.csv` columns

| column | meaning |
|---|---|
| `source` | which resource file it belongs to (`shared-ui` / `overlay`) — leave as-is |
| `key` | the string key — **never change** |
| `surface` | where it appears (e.g. "Companion messages") |
| `sensitivity` | `SENSITIVE` / `STANDARD` / `DEV_ONLY` / `DO_NOT_TRANSLATE` |
| `en_source` | the English source text (read-only reference) |
| **`translation`** | ← **the translated text goes here** |
| **`status`** | `PENDING` → set to **`APPROVED`** when reviewed, or `NEEDS_WORK` |
| `reviewer` | who approved it |
| `date` | when |
| `notes` | anything the reviewer wants to flag |

Open these in Excel / Google Sheets / LibreOffice. Fill `translation`, set `status` to
`APPROVED`. Leave a row blank to fall back to English at runtime.

Status values the tool understands: `PENDING`, `APPROVED`, `NEEDS_WORK`, `NEEDS_REVIEW`
(auto-seeded from an existing locale file — confirm before trusting), `RECHECK` (English
changed after this row was approved — re-review), `SOURCE`/`SKIP` (English source / not
translatable).

---

## Workflow

### 1. Generate / refresh the worksheets
Run whenever app strings change; it **merges** — your translations, statuses and notes are
preserved, new keys arrive as `PENDING`, and a row whose English changed after approval is
flagged `RECHECK`.

```
python3 l10n/tools/l10n_review.py generate
```

### 2. Review & translate
- **Advisors** sign off the English **SENSITIVE** copy (companion lines, questionnaire, AI
  prompt, crisis/safety screen) — set `status = APPROVED` in `ui.en.csv` and
  `ai-prompt/en.txt`.
- **Native-speaker translators** fill each `ui.<lang>.csv`; SENSITIVE surfaces need an
  ED-informed native reviewer, not machine translation.
- **Crisis registry** (`crisis.<country>.csv`): the T&S teammate fills the **real** phone /
  URL `value`, `verifiedBy`, `verifiedOn`, then `status = APPROVED`. Never invent numbers.
- **Safety lexicons** (`safety-lexicon.<lang>.csv`): advisor/translator supplies the
  per-language number/shame/appearance tokens and crisis-sign phrases (equivalents, not
  translations), `status = APPROVED`.

### 3. Import approved content back into the app
```
python3 l10n/tools/l10n_review.py import ui        # → per-locale strings.xml
python3 l10n/tools/l10n_review.py import ui --lang de
python3 l10n/tools/l10n_review.py import crisis     # → config/safety/crisis/*.json
python3 l10n/tools/l10n_review.py import lexicon    # → config/safety/*.json
```

The importer **only emits `APPROVED` SENSITIVE rows** and reports how many it held back.
Generated locale files carry a "do not hand-edit" banner.

### 4. Verify (the release gates catch mistakes)
After importing, run the project gates — a mistranslation that reintroduces a forbidden
number/shame word is caught **per language**:

```
./gradlew copyLint          # numberless/anti-shame, every language
./gradlew crisisGateStrict  # real, verified, current crisis content (no TODO-VERIFY, never NEDA)
./gradlew check             # tests incl. lexicon/companion parity
```

The AI system prompt is a server change: after advisor sign-off, its English text is updated
in `server/.../ReflectionSystemPrompt.kt` (and its `REVISION` bumped). Per-language prompt
routing is a downstream server feature; `ai-prompt/<lang>.txt` holds the reviewed text until
then.

---

## Notes & current limits

- `import` is **not run** by default and touches only string **content**, never logic.
- Crisis worksheets are **verification-first** (the numbers are still `TODO-VERIFY`).
  Per-language translation of crisis *labels* is a follow-on once the data is verified.
- `zh` is treated as one locale for now — confirm Simplified vs Traditional
  (`zh-Hans` / `zh-Hant`) with the translator before shipping (docs/12 §1).
- Re-run `generate` after any string change so `catalog.md` and the worksheets stay in sync.
