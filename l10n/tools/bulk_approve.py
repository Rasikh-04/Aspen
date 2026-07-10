#!/usr/bin/env python3
"""One-command status flip AFTER advisors sign off — no manual cell editing.

Usage (from repo root):
  python3 l10n/tools/bulk_approve.py l10n/worksheets/ui.de.csv --reviewer "Dr. X" 
  python3 l10n/tools/bulk_approve.py l10n/worksheets/crisis.pk.csv --reviewer "T&S: Name" --verified-by "T&S: Name"

Flips every PENDING row to APPROVED and stamps reviewer + today's date.
Rows the advisor marked NEEDS_WORK / left blank are untouched, so the advisor's
review pass is just: read each row, blank or NEEDS_WORK anything wrong, then run this.
"""
import csv, sys, argparse, datetime

p = argparse.ArgumentParser()
p.add_argument("worksheet")
p.add_argument("--reviewer", required=True)
p.add_argument("--verified-by", default=None, help="crisis sheets: stamp verifiedBy")
a = p.parse_args()

today = datetime.date.today().isoformat()
rows = list(csv.DictReader(open(a.worksheet, encoding="utf-8", newline="")))
header = rows[0].keys() if rows else []
flipped = 0
for r in rows:
    if r.get("status") == "PENDING" and (r.get("translation") or r.get("value")):
        if r.get("value") == "TODO-VERIFY":
            continue
        r["status"] = "APPROVED"
        if "reviewer" in r: r["reviewer"] = a.reviewer
        if "date" in r: r["date"] = today
        if a.verified_by and "verifiedBy" in r:
            r["verifiedBy"] = a.verified_by
            r["verifiedOn"] = today
        flipped += 1
with open(a.worksheet, "w", encoding="utf-8", newline="") as fh:
    w = csv.DictWriter(fh, fieldnames=header)
    w.writeheader()
    for r in rows: w.writerow(r)
print(f"{flipped} rows APPROVED in {a.worksheet}")
