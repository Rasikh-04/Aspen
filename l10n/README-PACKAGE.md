# Aspen l10n package — complete draft set (2026-07-07)

## Contents
- `crisis/` — crisis.{us,uk,de,pk,intl}.csv: all contact values researched against live
  sources (sources + advisor checkpoints in `notes`); status PENDING, verifiedBy PENDING-ADVISOR.
- `ui-drafts/` — batches 1–5 per language (ur, hi, zh, de, es, ar, fr):
  b1 crisis-screen+home/nav · b2 onboarding (51) · b3 grounding (25) · b4 companion+AI (30)
  · b5 STANDARD remainder (115). = full 244-key catalog, 7 languages, all PENDING.
- `docs/` — HANDOFF.md (crisis verification brief + workflow) and per-batch reviewer notes.
- `tools/bulk_approve.py` — drop into `l10n/tools/`; after an advisor reads a sheet and
  marks anything wrong NEEDS_WORK, one command flips the rest to APPROVED with their
  name+date. No manual cell editing; the human attestation stays real.

## Integration
1. Merge each `ui-drafts-batch*.LANG.csv` into `l10n/worksheets/ui.LANG.csv` by key
   (fill `translation`+`notes`; keep key/surface/sensitivity as generated).
2. Replace `l10n/worksheets/crisis.*.csv` with the researched versions.
3. Advisors review → `python3 l10n/tools/bulk_approve.py <sheet> --reviewer "Name"
   [--verified-by "T&S: Name"]` → `l10n_review.py import`. Importer/crisisGateStrict
   only ship APPROVED rows, so nothing reaches users before sign-off.

## Open items
UK Beat hours conflict + PK Umang number need call-tests · 5 safety lexicons empty ·
AI prompt per language · fr not in TARGET_LANGS · language-picker keys missing for
de/zh/hi/ar/es/fr · CLASSIFY prefix-order bug (companion_species_).
