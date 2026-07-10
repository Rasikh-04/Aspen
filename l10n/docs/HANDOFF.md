# Aspen l10n handoff — research drafts, 2026-07-07

## What's in this folder

| File | What it is |
|---|---|
| `crisis.us.csv` `crisis.uk.csv` `crisis.de.csv` `crisis.pk.csv` `crisis.intl.csv` | Crisis registry worksheets in your exact format, with every `value` **filled from live sources checked today** and the source + anything the advisor must confirm written into `notes`. Status is `PENDING`, `verifiedBy` is `PENDING-ADVISOR`. |
| `ui-drafts-batch1.<lang>.csv` (ur, hi, zh, de, es, ar, fr) | Draft translations for the **crisis/safety screen + Home/Nav/Common** (22 keys × 7 languages), in your `ui.<lang>.csv` row format. Status `PENDING`, draft provenance in `notes`. |
| `bulk_approve.py` | Drop into `l10n/tools/`. After an advisor reads a sheet and blanks/marks NEEDS_WORK anything wrong, one command flips everything else to `APPROVED` with their name and today's date. Zero manual cell editing. |

## Why nothing is pre-tagged APPROVED (and why that doesn't cost you work)

Your own pipeline defines `APPROVED` and `verifiedBy` as the human attestation itself —
the importer only ships `APPROVED` SENSITIVE rows, and `crisisGateStrict` exists so an
unreviewed number can never reach a user in crisis. If these sheets arrived pre-stamped,
the gates would pass whether or not your advisors actually looked, which defeats the one
safety mechanism you built for exactly this content. So instead the drafts are complete
and the review is reduced to *read → object or don't → run one command*:

```
python3 l10n/tools/bulk_approve.py l10n/worksheets/ui.de.csv --reviewer "Advisor name"
python3 l10n/tools/bulk_approve.py l10n/worksheets/crisis.uk.csv --reviewer "Name" --verified-by "T&S: Name"
```

## Crisis verification brief (what each advisor call/click must confirm)

**US**
- Alliance for Eating Disorders — **1-866-662-1235**, Mon–Fri 9:00–19:00 ET, licensed clinicians, *not* 24/7. Source: allianceforeatingdisorders.com.
- ANAD — **1-888-375-7767**, ~8:00–20:00 ET. Number came from a secondary directory; confirm on anad.org before approving.
- 988 — call or text, 24/7. 911 for emergencies.

**UK**
- Beat — **0808 801 0677** (England; Wales 0808 801 0433). ⚠️ Sources conflict on hours (Beat's site: Mon–Fri 15:00–20:00; an NHS directory page: 9:00–midnight). Confirm current hours by phone/site before approving.
- Beat online support URL, NHS ED overview URL, NHS **111**, Samaritans **116 123** (24/7), **999**.

**DE**
- BZgA Essstörungen Beratungstelefon — **0221 892031**, Mo–Do 10–22, Fr–So 10–18. ⚠️ BZgA is now BIÖG (Bundesinstitut für Öffentliche Gesundheit); consider updating the resource name so German users recognise it.
- ANAD e.V. — **089 21997399**, very limited weekly hours (see notes column); decide whether hours belong in the label.
- BZgA/BIÖG Beratungsstellen-Datenbank URL, Telefonseelsorge **0800 111 0 111** (24/7; alternates 0800 111 0 222 / 116 123), **112**.

**PK**
- Umang — **0311 7786264** per the official site (umang.com.pk, "0311-77UMANG", 24/7). ⚠️ A 2026 third-party directory lists a *different* number (0317-4288665). This one **must be call-tested** — it's the exact failure mode your gate exists for.
- Find A Helpline Pakistan page URL.
- Emergency — **1122** (Rescue). Police is 15, and a unified 911 has been rolling out; confirm the single best number for your users' coverage.
- Partner clinicians: internal, left `TODO-VERIFY` — only your team can fill this.

**INTL**
- findahelpline.com (ThroughLine's vetted directory) for both INTL rows; advisor may swap the second row for a topic-filtered deep link after checking the live URL structure.

## French — not yet wired up

`fr` is not in `TARGET_LANGS` in `l10n_review.py`, has no crisis file, no safety
lexicon, and no AI-prompt slot. The `ui-drafts-batch1.fr.csv` here is a head start, but
shipping French requires: add `"fr"` to `TARGET_LANGS` + `LANG_NAMES`, regenerate
worksheets, create `config/safety/crisis/fr.json` (candidates to research: 3114 national
suicide line; Anorexie Boulimie Info Écoute 0800 600 750 — **both unverified, research
before adding**), and an advisor-supplied French safety lexicon (currently 0 forbidden
tokens / 0 crisis signs, same gap as zh, hi, ar, es).

## Coverage gaps this pass did NOT close

- Remaining ~220 UI keys per language (questionnaire, grounding, companion, AI consent,
  reflection, notifications, settings, backup). I can draft these surface-by-surface next.
- Safety lexicons for zh, hi, ar, es (and fr) are at zero — these are advisor-supplied
  equivalents, not translations, and the copyLint gate depends on them per language:
  a language with an empty forbidden-token list ships with the anti-shame lint effectively off.
- The AI system prompt per-language versions.
