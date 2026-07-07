#!/usr/bin/env python3
"""
Aspen localisation & advisor-review round-trip tool  (docs/12 §3–§4, CLAUDE.md #11).

One dependency-free script that:

  generate   Reads the CANONICAL string sources in the repo (Compose/Android string
             resources, crisis registry JSON, safety lexicons, companion library, the
             AI system prompt) and (re)writes the review worksheets under l10n/worksheets/
             plus the human-readable master catalog l10n/catalog.md.
             It MERGES: existing translations / review status / reviewer / date / notes are
             preserved; new keys arrive as PENDING; keys whose English source changed after
             being APPROVED are flagged RECHECK. Nothing a reviewer typed is ever clobbered.

  import     Reads APPROVED worksheets and writes them back into the codebase
             (per-locale strings.xml, crisis JSON, safety-lexicon JSON). Enforces the
             sensitive-surface gate: a SENSITIVE row is only emitted into a shipping
             resource when its status is APPROVED (docs/12 §5). Run per surface:
                 import ui [--lang ur]      import crisis      import lexicon

This tool is BUILD/OPS tooling, not shipped app code. It touches only string CONTENT;
it never edits logic. Run from the repo root:  python3 l10n/tools/l10n_review.py generate
"""
from __future__ import annotations

import csv
import json
import os
import re
import sys
from datetime import date

# --------------------------------------------------------------------------------------
# Repo layout
# --------------------------------------------------------------------------------------
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
L10N = os.path.join(ROOT, "l10n")
WORK = os.path.join(L10N, "worksheets")
PROMPT_DIR = os.path.join(WORK, "ai-prompt")
CATALOG = os.path.join(L10N, "catalog.md")

# Languages: en is the authored source; the rest are translation targets (docs/12 §1).
SOURCE_LANG = "en"
TARGET_LANGS = ["ur", "de", "zh", "hi", "ar", "es"]
ALL_LANGS = [SOURCE_LANG] + TARGET_LANGS
LANG_NAMES = {
    "en": "English", "ur": "Urdu", "de": "German", "zh": "Mandarin Chinese",
    "hi": "Hindi", "ar": "Arabic", "es": "Spanish",
}

# UI string sources. Each key is written back into the file for its `source`, so import is
# unambiguous. `base` is the English file; `locale` is the per-language path template.
UI_SOURCES = [
    {
        "id": "shared-ui",
        "base": "shared/ui/src/commonMain/composeResources/values/strings.xml",
        "locale": "shared/ui/src/commonMain/composeResources/values-{lang}/strings.xml",
    },
    {
        "id": "overlay",
        "base": "companion-overlay-android/src/main/res/values/strings.xml",
        "locale": "companion-overlay-android/src/main/res/values-{lang}/strings.xml",
    },
]

CRISIS_DIR = os.path.join(ROOT, "config", "safety", "crisis")
FORBIDDEN_TOKENS = os.path.join(ROOT, "config", "safety", "forbidden_tokens.json")
CRISIS_SIGNALS = os.path.join(ROOT, "config", "safety", "crisis_signals.json")
COMPANION_LIB = os.path.join(ROOT, "config", "companion", "library.json")
PROMPT_KT = os.path.join(ROOT, "server", "src", "main", "kotlin", "app", "aspen", "server", "ai",
                         "ReflectionSystemPrompt.kt")

# --------------------------------------------------------------------------------------
# Surface + sensitivity classification (docs/12 §3).
#   SENSITIVE   -> ED-informed NATIVE-SPEAKER review mandatory; machine translation not
#                  acceptable; may not ship in a language until APPROVED (docs/12 §5).
#   STANDARD    -> native translation + standard review.
#   DEV_ONLY    -> debug builds only, never shipped; translation not required.
#   DO_NOT_TRANSLATE -> brand / proper noun; identical in every language.
# Rules are matched IN ORDER; first hit wins. Keep most-specific prefixes first.
# --------------------------------------------------------------------------------------
CLASSIFY = [
    ("app_name_android", "Brand", "DO_NOT_TRANSLATE"),
    ("app_name", "Brand", "DO_NOT_TRANSLATE"),
    ("nav_", "Navigation", "STANDARD"),
    ("onb_", "Onboarding questionnaire", "SENSITIVE"),
    ("breathe_", "Grounding tools", "SENSITIVE"),
    ("ground_54321_", "Grounding tools", "SENSITIVE"),
    ("ride_urge_", "Grounding tools", "SENSITIVE"),
    ("grounding_", "Grounding tools", "SENSITIVE"),
    ("safety_ai_fallback", "AI safety fallback", "SENSITIVE"),
    ("safety_", "Crisis / safety screen", "SENSITIVE"),
    ("reflect_companion_", "AI reflection companion", "SENSITIVE"),
    ("companion_", "Companion messages", "SENSITIVE"),
    ("settings_ai_", "AI consent (deeper reflection)", "SENSITIVE"),
    ("settings_debug_", "Debug (not shipped)", "DEV_ONLY"),
    ("debug_", "Debug (not shipped)", "DEV_ONLY"),
    ("settings_companion_", "Companion presence settings", "STANDARD"),
    ("companion_species_", "Companion presence settings", "STANDARD"),
    ("settings_overlay_", "Overlay settings", "STANDARD"),
    ("overlay_", "Overlay notification", "STANDARD"),
    ("settings_notify_", "Notifications", "STANDARD"),
    ("notify_", "Notifications", "STANDARD"),
    ("settings_account_", "Account", "STANDARD"),
    ("account_", "Account", "STANDARD"),
    ("backup_", "Backup", "STANDARD"),
    ("settings_language_", "Language settings", "STANDARD"),
    ("language_", "Language settings", "STANDARD"),
    ("settings_", "Settings", "STANDARD"),
    ("reflect_", "Reflection & logging", "STANDARD"),
    ("feeling_", "Feeling tags", "STANDARD"),
    ("home_", "Home", "STANDARD"),
    ("back", "Common", "STANDARD"),
]

# Order surfaces appear in the catalog / generated locale files.
SURFACE_ORDER = [
    "Home", "Navigation", "Common",
    "Onboarding questionnaire",
    "Grounding tools", "Crisis / safety screen",
    "Reflection & logging", "Feeling tags",
    "Companion messages", "AI reflection companion", "AI safety fallback",
    "AI consent (deeper reflection)",
    "Companion presence settings", "Overlay settings", "Overlay notification",
    "Notifications",
    "Account", "Backup",
    "Language settings", "Settings",
    "Brand", "Debug (not shipped)",
]

UI_HEADER = ["source", "key", "surface", "sensitivity", "en_source",
             "translation", "status", "reviewer", "date", "notes"]

# ======================================================================================
# escaping helpers (Android/Compose string resources)
# ======================================================================================
def unescape(v: str) -> str:
    v = v.replace("\\'", "'").replace('\\"', '"')
    v = v.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
    return v.strip()

def escape(v: str) -> str:
    v = v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    v = v.replace("'", "\\'")
    return v

STRING_RE = re.compile(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', re.DOTALL)

def read_resources(path: str) -> "list[tuple[str, str]]":
    """Ordered (key, unescaped-value) pairs, mirroring the build's StringResourceParser."""
    if not os.path.exists(path):
        return []
    text = open(path, encoding="utf-8").read()
    return [(m.group(1), unescape(m.group(2))) for m in STRING_RE.finditer(text)]

def classify(key: str) -> "tuple[str, str]":
    for prefix, surface, sensitivity in CLASSIFY:
        if key == prefix or key.startswith(prefix):
            return surface, sensitivity
    return "Other", "STANDARD"

def default_status(sensitivity: str) -> str:
    if sensitivity in ("DEV_ONLY", "DO_NOT_TRANSLATE"):
        return "SKIP"
    return "PENDING"

# ======================================================================================
# GENERATE
# ======================================================================================
def load_worksheet(path: str) -> dict:
    """Existing worksheet rows keyed by (source, key) so we can preserve reviewer work."""
    if not os.path.exists(path):
        return {}
    out = {}
    with open(path, encoding="utf-8", newline="") as fh:
        for row in csv.DictReader(fh):
            out[(row.get("source", ""), row["key"])] = row
    return out

def build_ui_catalog() -> list:
    """Authoritative list of UI entries from the English sources, in file order."""
    entries = []
    seen = set()
    for src in UI_SOURCES:
        for key, en_val in read_resources(os.path.join(ROOT, src["base"])):
            if (src["id"], key) in seen:
                continue
            seen.add((src["id"], key))
            surface, sensitivity = classify(key)
            entries.append({
                "source": src["id"], "key": key, "surface": surface,
                "sensitivity": sensitivity, "en_source": en_val,
            })
    return entries

def generate_ui(entries: list) -> None:
    os.makedirs(WORK, exist_ok=True)
    for lang in ALL_LANGS:
        path = os.path.join(WORK, f"ui.{lang}.csv")
        prior = load_worksheet(path)
        # Pre-seed target translations from an existing locale file, but ONLY when the value
        # actually differs from English (skips English placeholders left in stub locale files).
        locale_vals = {}
        if lang != SOURCE_LANG:
            for src in UI_SOURCES:
                lp = os.path.join(ROOT, src["locale"].format(lang=lang))
                for k, v in read_resources(lp):
                    if v and v.strip():
                        locale_vals[(src["id"], k)] = v

        rows = []
        for e in entries:
            k = (e["source"], e["key"])
            old = prior.get(k)
            if lang == SOURCE_LANG:
                translation = e["en_source"]
                status = old["status"] if old else (
                    "PENDING" if e["sensitivity"] == "SENSITIVE" else "SOURCE")
                if e["sensitivity"] in ("DEV_ONLY", "DO_NOT_TRANSLATE"):
                    status = old["status"] if old else "SKIP"
                reviewer = old.get("reviewer", "") if old else ""
                date_s = old.get("date", "") if old else ""
                notes = old.get("notes", "") if old else ""
            else:
                if old and old.get("translation"):
                    translation = old["translation"]
                    status = old.get("status") or default_status(e["sensitivity"])
                    reviewer = old.get("reviewer", "")
                    date_s = old.get("date", "")
                    notes = old.get("notes", "")
                elif k in locale_vals and locale_vals[k] != e["en_source"]:
                    translation = locale_vals[k]
                    status = "NEEDS_REVIEW"   # imported from an existing locale file, unverified
                    reviewer, date_s = "", ""
                    notes = "seeded from existing locale file"
                else:
                    translation = ""
                    status = default_status(e["sensitivity"])
                    reviewer, date_s, notes = "", "", ""
                # Flag English-source drift on previously-approved rows.
                if old and old.get("en_source", e["en_source"]) != e["en_source"] \
                        and status == "APPROVED":
                    status = "RECHECK"
                    notes = (notes + "; " if notes else "") + "en source changed since approval"

            rows.append({
                "source": e["source"], "key": e["key"], "surface": e["surface"],
                "sensitivity": e["sensitivity"], "en_source": e["en_source"],
                "translation": translation, "status": status,
                "reviewer": reviewer, "date": date_s, "notes": notes,
            })
        write_csv(path, UI_HEADER, rows)
    print(f"  ui: {len(entries)} keys x {len(ALL_LANGS)} languages -> l10n/worksheets/ui.<lang>.csv")

def write_csv(path: str, header: list, rows: list) -> None:
    with open(path, "w", encoding="utf-8", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=header, extrasaction="ignore")
        w.writeheader()
        for r in rows:
            w.writerow(r)

# ---- crisis registry -----------------------------------------------------------------
CRISIS_HEADER = ["country", "resource_id", "name", "purpose", "contact_index", "method",
                 "label", "value", "verifiedBy", "verifiedOn", "status", "notes"]

def generate_crisis() -> None:
    os.makedirs(WORK, exist_ok=True)
    countries = sorted(f[:-5] for f in os.listdir(CRISIS_DIR) if f.endswith(".json"))
    for country in countries:
        data = json.load(open(os.path.join(CRISIS_DIR, f"{country}.json"), encoding="utf-8"))
        path = os.path.join(WORK, f"crisis.{country}.csv")
        prior = {}
        if os.path.exists(path):
            with open(path, encoding="utf-8", newline="") as fh:
                for row in csv.DictReader(fh):
                    prior[(row["resource_id"], row["contact_index"])] = row
        rows = []
        for res in data.get("resources", []):
            for i, c in enumerate(res.get("contacts", [])):
                old = prior.get((res["id"], str(i)), {})
                value = c.get("value", "")
                verified = value not in ("", "TODO-VERIFY") and old.get("value") == value
                rows.append({
                    "country": data.get("locale", country.upper()),
                    "resource_id": res["id"], "name": res.get("name", ""),
                    "purpose": res.get("purpose", ""), "contact_index": i,
                    "method": c.get("method", ""), "label": c.get("label", ""),
                    "value": old.get("value") or value,
                    "verifiedBy": old.get("verifiedBy") or res.get("verifiedBy", ""),
                    "verifiedOn": old.get("verifiedOn") or res.get("verifiedOn", ""),
                    "status": old.get("status") or ("APPROVED" if verified else "PENDING"),
                    "notes": old.get("notes", ""),
                })
        write_csv(path, CRISIS_HEADER, rows)
    print(f"  crisis: {len(countries)} countries -> l10n/worksheets/crisis.<country>.csv")

# ---- safety lexicons (advisor-supplied per language) ---------------------------------
LEXICON_HEADER = ["list", "category", "term", "status", "notes"]

def generate_lexicon() -> None:
    os.makedirs(WORK, exist_ok=True)
    ft = json.load(open(FORBIDDEN_TOKENS, encoding="utf-8")).get("languages", {})
    cs = json.load(open(CRISIS_SIGNALS, encoding="utf-8")).get("languages", {})
    for lang in ALL_LANGS:
        path = os.path.join(WORK, f"safety-lexicon.{lang}.csv")
        prior = {}
        if os.path.exists(path):
            with open(path, encoding="utf-8", newline="") as fh:
                for row in csv.DictReader(fh):
                    prior[(row["list"], row["category"], row["term"])] = row
        rows = []
        for category, terms in (ft.get(lang) or {}).items():
            for term in terms:
                old = prior.get(("forbidden_tokens", category, term), {})
                rows.append({"list": "forbidden_tokens", "category": category, "term": term,
                             "status": old.get("status") or "APPROVED",
                             "notes": old.get("notes", "")})
        for term in (cs.get(lang) or []):
            old = prior.get(("crisis_signals", "", term), {})
            rows.append({"list": "crisis_signals", "category": "", "term": term,
                         "status": old.get("status") or "APPROVED", "notes": old.get("notes", "")})
        # keep any extra rows a reviewer added for a not-yet-seeded language
        seeded = {(r["list"], r["category"], r["term"]) for r in rows}
        for kkey, old in prior.items():
            if kkey not in seeded:
                rows.append({"list": old["list"], "category": old["category"], "term": old["term"],
                             "status": old.get("status", "PENDING"), "notes": old.get("notes", "")})
        write_csv(path, LEXICON_HEADER, rows)
    print(f"  lexicon: {len(ALL_LANGS)} languages -> l10n/worksheets/safety-lexicon.<lang>.csv")

# ---- AI system prompt ----------------------------------------------------------------
def read_prompt() -> "tuple[str, str]":
    text = open(PROMPT_KT, encoding="utf-8").read()
    rev = re.search(r'REVISION[^"]*"([^"]+)"', text)
    body = re.search(r'val text: String =\s*"""(.*?)"""\.trimIndent\(\)', text, re.DOTALL)
    if not body:
        return (rev.group(1) if rev else "unknown", "")
    lines = body.group(1).splitlines()
    while lines and not lines[0].strip():
        lines.pop(0)
    indent = min((len(l) - len(l.lstrip()) for l in lines if l.strip()), default=0)
    dedented = "\n".join(l[indent:] for l in lines).strip()
    return (rev.group(1) if rev else "unknown", dedented)

def generate_prompt() -> None:
    os.makedirs(PROMPT_DIR, exist_ok=True)
    rev, body = read_prompt()
    header = (f"# Aspen AI reflection system prompt — SOURCE (English)\n"
              f"# Revision: {rev}  (server: ReflectionSystemPrompt.kt)\n"
              f"# SENSITIVE / advisor-review surface (docs/07 Phase 4 [APPROVE], docs/12 §3).\n"
              f"# Per-language versions live beside this as <lang>.txt, native ED-informed review\n"
              f"# required. Editing the shipped prompt is a server change, done AFTER sign-off.\n\n")
    with open(os.path.join(PROMPT_DIR, "en.txt"), "w", encoding="utf-8") as fh:
        fh.write(header + body + "\n")
    print(f"  prompt: revision {rev} -> l10n/worksheets/ai-prompt/en.txt")

# ---- master catalog ------------------------------------------------------------------
def generate_catalog(entries: list) -> None:
    # per-language completion, read back from the just-written worksheets
    def counts(lang):
        path = os.path.join(WORK, f"ui.{lang}.csv")
        c = {"APPROVED": 0, "PENDING": 0, "other": 0, "translatable": 0}
        with open(path, encoding="utf-8", newline="") as fh:
            for r in csv.DictReader(fh):
                if r["sensitivity"] in ("DEV_ONLY", "DO_NOT_TRANSLATE"):
                    continue
                c["translatable"] += 1
                st = r["status"]
                if st == "APPROVED":
                    c["APPROVED"] += 1
                elif st in ("PENDING", "SOURCE"):
                    c["PENDING"] += 1
                else:
                    c["other"] += 1
        return c

    lines = []
    lines.append("# Aspen — advisor-review & translation master catalog\n")
    lines.append("> **Generated** by `l10n/tools/l10n_review.py generate` — do not hand-edit; "
                 "edit the source strings or the worksheets and re-run. "
                 f"Last generated {date.today().isoformat()}.\n")
    lines.append("\nEvery user-facing string in Aspen that needs advisor sign-off and/or "
                 "translation. See [README](README.md) for the workflow.\n")

    lines.append("\n## Sensitivity tiers\n")
    lines.append("| Tier | Meaning |")
    lines.append("|---|---|")
    lines.append("| **SENSITIVE** | ED-informed **native-speaker** review mandatory; machine "
                 "translation not acceptable; cannot ship in a language until `APPROVED` "
                 "(docs/12 §3/§5). |")
    lines.append("| STANDARD | Native translation + standard review. |")
    lines.append("| DEV_ONLY | Debug builds only, never shipped; translation not required. |")
    lines.append("| DO_NOT_TRANSLATE | Brand / proper noun; identical every language. |")

    lines.append("\n## Per-language UI coverage\n")
    lines.append("| Language | Approved | Pending | Other | Translatable total |")
    lines.append("|---|--:|--:|--:|--:|")
    for lang in ALL_LANGS:
        c = counts(lang)
        lines.append(f"| {LANG_NAMES[lang]} (`{lang}`) | {c['APPROVED']} | {c['PENDING']} | "
                     f"{c['other']} | {c['translatable']} |")

    lines.append("\n## UI strings by surface\n")
    by_surface = {}
    for e in entries:
        by_surface.setdefault(e["surface"], []).append(e)
    ordered = [s for s in SURFACE_ORDER if s in by_surface] + \
              [s for s in by_surface if s not in SURFACE_ORDER]
    for surface in ordered:
        es = by_surface[surface]
        sens = es[0]["sensitivity"]
        badge = "🔴 SENSITIVE" if sens == "SENSITIVE" else sens
        lines.append(f"\n### {surface}  ·  {badge}  ·  {len(es)} keys\n")
        lines.append("| key | English source |")
        lines.append("|---|---|")
        for e in es:
            en = e["en_source"].replace("|", "\\|")
            lines.append(f"| `{e['key']}` | {en} |")

    # crisis
    lines.append("\n## Crisis registry (verify data + translate copy, per country)\n")
    lines.append("Real phone/URL values are `TODO-VERIFY` until advisors verify them — that is a "
                 "release gate (`crisisGateStrict`, docs/10 §7). Worksheets: "
                 "`l10n/worksheets/crisis.<country>.csv`.\n")
    lines.append("| Country | Resources | Contacts verified |")
    lines.append("|---|--:|--:|")
    for f in sorted(os.listdir(CRISIS_DIR)):
        if not f.endswith(".json"):
            continue
        data = json.load(open(os.path.join(CRISIS_DIR, f), encoding="utf-8"))
        res = data.get("resources", [])
        total = sum(len(r.get("contacts", [])) for r in res)
        verified = sum(1 for r in res for c in r.get("contacts", [])
                       if c.get("value") not in ("", "TODO-VERIFY"))
        lines.append(f"| {data.get('locale', f[:-5].upper())} | {len(res)} | {verified}/{total} |")

    # lexicons + prompt
    lines.append("\n## Safety lexicons (advisor-supplied per language)\n")
    lines.append("Forbidden-token lists (numberless/anti-shame copy-lint) and crisis-sign phrases "
                 "(hand-off trigger). These are **equivalents supplied per language**, not "
                 "translations. Worksheets: `l10n/worksheets/safety-lexicon.<lang>.csv`.\n")
    ft = json.load(open(FORBIDDEN_TOKENS, encoding="utf-8")).get("languages", {})
    cs = json.load(open(CRISIS_SIGNALS, encoding="utf-8")).get("languages", {})
    lines.append("| Language | Forbidden tokens | Crisis-sign phrases |")
    lines.append("|---|--:|--:|")
    for lang in ALL_LANGS:
        nft = sum(len(v) for v in (ft.get(lang) or {}).values())
        lines.append(f"| {LANG_NAMES[lang]} (`{lang}`) | {nft} | {len(cs.get(lang) or [])} |")

    rev, _ = read_prompt()
    lines.append("\n## AI reflection system prompt\n")
    lines.append(f"Revision `{rev}` — English source at `l10n/worksheets/ai-prompt/en.txt`. "
                 "SENSITIVE; per-language versions (`<lang>.txt`) need native ED-informed review "
                 "before the server can route them.\n")

    with open(CATALOG, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")
    print("  catalog -> l10n/catalog.md")

def cmd_generate() -> None:
    print("Generating advisor-review worksheets + catalog…")
    entries = build_ui_catalog()
    generate_ui(entries)
    generate_crisis()
    generate_lexicon()
    generate_prompt()
    generate_catalog(entries)
    print("Done. Review under l10n/worksheets/ ; overview in l10n/catalog.md.")

# ======================================================================================
# IMPORT  (worksheets -> codebase).  Only APPROVED sensitive rows are emitted.
# ======================================================================================
def cmd_import_ui(only_lang: str | None) -> None:
    langs = [only_lang] if only_lang else TARGET_LANGS
    for lang in langs:
        path = os.path.join(WORK, f"ui.{lang}.csv")
        if not os.path.exists(path):
            print(f"  (skip {lang}: no worksheet)")
            continue
        rows = list(csv.DictReader(open(path, encoding="utf-8", newline="")))
        for src in UI_SOURCES:
            emit, held = [], 0
            for r in rows:
                if r["source"] != src["id"]:
                    continue
                if r["sensitivity"] in ("DEV_ONLY", "DO_NOT_TRANSLATE"):
                    continue
                tr = (r.get("translation") or "").strip()
                if not tr:
                    continue
                if r["sensitivity"] == "SENSITIVE" and r.get("status") != "APPROVED":
                    held += 1
                    continue
                emit.append((r["key"], r["surface"], tr))
            if not emit:
                continue
            out = os.path.join(ROOT, src["locale"].format(lang=lang))
            os.makedirs(os.path.dirname(out), exist_ok=True)
            write_locale_xml(out, lang, emit)
            note = f" ({held} sensitive rows held: not APPROVED)" if held else ""
            print(f"  wrote {len(emit)} strings -> {os.path.relpath(out, ROOT)}{note}")

def write_locale_xml(path: str, lang: str, emit: list) -> None:
    by_surface = {}
    for key, surface, tr in emit:
        by_surface.setdefault(surface, []).append((key, tr))
    ordered = [s for s in SURFACE_ORDER if s in by_surface] + \
              [s for s in by_surface if s not in SURFACE_ORDER]
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           f"<!-- {LANG_NAMES.get(lang, lang)} ({lang}). GENERATED from "
           f"l10n/worksheets/ui.{lang}.csv by l10n_review.py import. Do not hand-edit; edit the",
           "     worksheet and re-import. Only reviewed strings are present; absent keys fall back",
           "     to English at runtime. SENSITIVE surfaces appear only when APPROVED (docs/12 §5). -->",
           "<resources>"]
    for surface in ordered:
        out.append(f"    <!-- {surface} -->")
        for key, tr in by_surface[surface]:
            out.append(f'    <string name="{key}">{escape(tr)}</string>')
    out.append("</resources>")
    open(path, "w", encoding="utf-8").write("\n".join(out) + "\n")

def cmd_import_crisis() -> None:
    for f in sorted(os.listdir(CRISIS_DIR)):
        if not f.endswith(".json"):
            continue
        country = f[:-5]
        ws = os.path.join(WORK, f"crisis.{country}.csv")
        if not os.path.exists(ws):
            continue
        rows = {(r["resource_id"], r["contact_index"]): r
                for r in csv.DictReader(open(ws, encoding="utf-8", newline=""))}
        data = json.load(open(os.path.join(CRISIS_DIR, f), encoding="utf-8"))
        changed = 0
        for res in data.get("resources", []):
            for i, c in enumerate(res.get("contacts", [])):
                r = rows.get((res["id"], str(i)))
                if not r or r.get("status") != "APPROVED":
                    continue
                if r.get("value") and r["value"] != "TODO-VERIFY":
                    c["value"] = r["value"]
                    changed += 1
                if r.get("verifiedBy"):
                    res["verifiedBy"] = r["verifiedBy"]
                if r.get("verifiedOn"):
                    res["verifiedOn"] = r["verifiedOn"]
        if changed:
            json.dump(data, open(os.path.join(CRISIS_DIR, f), "w", encoding="utf-8"),
                      ensure_ascii=False, indent=2)
            open(os.path.join(CRISIS_DIR, f), "a", encoding="utf-8").write("\n")
            print(f"  {country}: {changed} verified contact value(s) written")

def cmd_import_lexicon() -> None:
    ft_doc = json.load(open(FORBIDDEN_TOKENS, encoding="utf-8"))
    cs_doc = json.load(open(CRISIS_SIGNALS, encoding="utf-8"))
    for lang in ALL_LANGS:
        ws = os.path.join(WORK, f"safety-lexicon.{lang}.csv")
        if not os.path.exists(ws):
            continue
        ft, cs = {}, []
        for r in csv.DictReader(open(ws, encoding="utf-8", newline="")):
            if r.get("status") != "APPROVED" or not r.get("term", "").strip():
                continue
            if r["list"] == "forbidden_tokens":
                ft.setdefault(r["category"], []).append(r["term"])
            elif r["list"] == "crisis_signals":
                cs.append(r["term"])
        if ft:
            ft_doc.setdefault("languages", {})[lang] = ft
        if cs:
            cs_doc.setdefault("languages", {})[lang] = cs
    json.dump(ft_doc, open(FORBIDDEN_TOKENS, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    open(FORBIDDEN_TOKENS, "a", encoding="utf-8").write("\n")
    json.dump(cs_doc, open(CRISIS_SIGNALS, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    open(CRISIS_SIGNALS, "a", encoding="utf-8").write("\n")
    print("  lexicons imported (APPROVED terms only). Re-run the copy-lint / parity tests.")

# ======================================================================================
def main() -> int:
    args = sys.argv[1:]
    if not args or args[0] in ("-h", "--help"):
        print(__doc__)
        return 0
    cmd = args[0]
    if cmd == "generate":
        cmd_generate()
        return 0
    if cmd == "import":
        what = args[1] if len(args) > 1 else ""
        lang = None
        if "--lang" in args:
            lang = args[args.index("--lang") + 1]
        if what == "ui":
            cmd_import_ui(lang)
        elif what == "crisis":
            cmd_import_crisis()
        elif what == "lexicon":
            cmd_import_lexicon()
        else:
            print("usage: import ui [--lang <code>] | import crisis | import lexicon")
            return 2
        return 0
    print(f"unknown command: {cmd}\n{__doc__}")
    return 2

if __name__ == "__main__":
    raise SystemExit(main())
