# 12 — Localization & Internationalization (first-class requirement)

> **This is a hard, cross-cutting requirement, not a feature.** It is written separately and referenced from `CLAUDE.md`, `docs/03`, `docs/04`, `docs/06`, `docs/09`, `docs/10`, `docs/11` precisely so we **do not drift away from it** mid-build. Retrofitting RTL and complex-script support after building English-only is expensive and error-prone — so it goes in from Phase 1.

---

## 1. The requirement (decided)

Aspen ships from first release in **7 languages**, available worldwide, **auto-selected by system default**, English as the worldwide fallback:

| Language | Code | Script / direction | Notes |
|---|---|---|---|
| **English** | `en` | Latin, LTR | **Default / worldwide fallback.** |
| **Urdu** | `ur` | Nastaliq (Arabic script), **RTL** | Pakistan launch; needs Nastaliq font + RTL. |
| **German** | `de` | Latin, LTR | Germany launch. |
| **Mandarin Chinese** | `zh` | Han (CJK) | Confirm Simplified vs Traditional (`zh-Hans` / `zh-Hant`). |
| **Hindi** | `hi` | Devanagari | Complex shaping. |
| **Arabic** | `ar` | Arabic, **RTL** | Complex shaping + RTL. |
| **Spanish** | `es` | Latin, LTR | Spain + Latin America. |

**Behaviour:** detect the OS locale on first run → pick the matching language if supported, else **English**. User can override in Settings anytime. Region (for crisis routing) is **separate** from language (see §6).

---

## 2. Why this must be wired from the start (the drift risks)

These are the things that are cheap on day 1 and brutal to add on day 200:

1. **No hardcoded strings — ever.** Every user-facing string lives in a localized resource from the first screen. A single hardcoded English string is a bug. (CI lint can flag string literals in UI code.)
2. **RTL layout (Urdu, Arabic).** Compose Multiplatform must run in RTL: mirrored layouts, start/end (not left/right) paddings, mirrored icons/back-arrows/progress, text alignment. **Build at least one RTL language in from Phase 1** so the layout system is RTL-correct from the beginning, not patched later.
3. **Complex script rendering.** Nastaliq (Urdu), Arabic shaping, Devanagari (Hindi), CJK (Mandarin) need proper fonts and shaping. Bundle/verify fonts per script early; don't assume the platform default renders Nastaliq well (it often doesn't).
4. **Text expansion/contraction.** German runs long; CJK is compact; Urdu/Arabic have different line-height needs. Layouts must flex (no fixed-width text containers) — verify with the longest and the tallest scripts early.
5. **Locale-aware formatting.** Dates, times, lists — via locale APIs, never manual. (Aspen has *no* numbers about food/body, but it still has dates/times.)
6. **Pluralization & gender.** Use proper plural/grammar resources (ICU-style), not string concatenation — Arabic has multiple plural forms; Urdu/Hindi gender agreement matters.

---

## 3. What must be localized (it's more than UI chrome)

Localization here is **safety-critical**, not cosmetic — three of the most sensitive surfaces are localized content:

| Surface | Localization requirement |
|---|---|
| **UI strings** | All 7 languages. No hardcoded text. |
| **Crisis resources (`docs/10`)** | Per-**country** data + copy in the user's language. **Crisis copy mistranslated is a safety bug** — native-speaker review mandatory. |
| **Onboarding questionnaire (`docs/11`)** | Native-speaker, ED-informed review per language; **machine translation not acceptable** for screening copy. Cultural/stigma adaptation per region. |
| **Companion message library (`docs/04` ADR-003)** | The curated companion lines must exist **per language**, reviewed by native speakers — not auto-translated. The companion's warmth doesn't survive machine translation, and a tone-deaf line to a vulnerable user is harmful. |
| **AI output (cloud tier)** | Must respond in the user's language; the output guard's forbidden-token lists (`docs/09` §2.3) exist **per language** (e.g. German/Urdu/Arabic number & shame words). |
| **Grounding tools / copy** | Localized; check idioms (e.g. "5-4-3-2-1" works, but instructional copy must translate naturally). |

> **Hard rule:** the **forbidden-token copy-lint (`docs/09` §2.5) runs per language.** "kcal"/"calorie" in English is not enough — it needs the German, Urdu, Arabic, Hindi, Mandarin, Spanish equivalents too, advisor/translator-supplied. A numberless app must be numberless in every language.

---

## 4. Architecture (how it's built — fits KMP + CMP, `docs/04`)

- **String resources** via Compose Multiplatform's resource system (shared `composeResources`), keyed and per-locale; no literals in code.
- **Locale provider** in `:shared` — resolves active locale (system default → supported? → else `en`), exposes it to UI + data layers; user override persisted in settings.
- **RTL** handled by CMP layout direction driven from the locale; use logical start/end everywhere; provide mirrored assets where needed.
- **Fonts** per script bundled/loaded (Nastaliq for Urdu, Arabic, Devanagari, CJK, Latin) with fallback chains; verify rendering on-device per script.
- **Content registries** (crisis, questionnaire, companion library) are **locale-keyed data**, loaded like the crisis registry (`docs/09` §2.1) — so adding/curating a language is a data + review task, not a code change.
- **Translation workflow:** keys → translation files → **native-speaker + (for sensitive surfaces) ED-informed review** → in. Track per-key review status for the sensitive surfaces; a sensitive string can't ship "machine-translated, unreviewed."

---

## 5. Testing & gates

- **Pseudo-localization** + longest/tallest-script pass in CI to catch truncation/overflow early.
- **RTL screenshot tests** for Urdu/Arabic on key screens (home, hard-moment, safety, questionnaire).
- **Per-language copy-lint** (forbidden tokens) — release-blocking (`docs/09` §2.5).
- **Sensitive-surface review gate:** crisis copy, questionnaire, and companion lines cannot ship in a language until marked native-speaker-reviewed (analogous to the crisis-freshness gate).
- **A11y per language:** screen-reader pronunciation, dynamic type with each script (`docs/06` §7).

---

## 6. Language ≠ region (important distinction)

- **Language** = how the UI reads (user/system choice; 7 supported worldwide).
- **Region/country** = which **crisis registry** applies (`docs/10`) and which directory/linkage data shows.
- These are **independent.** An Arabic-speaking user could be in Germany; a user with English UI could be in Pakistan. So: detect/confirm **country** for crisis routing separately from **language** for UI, and never infer one from the other (e.g. don't assume "Arabic UI → some Arab country's crisis line"). When country is unknown, use the International Fallback (`docs/10` §6).

---

## 7. Phasing (so it's present without blocking everything)

- **Phase 1:** wire the i18n architecture (resource system, locale provider, **RTL from day one with at least one RTL language stubbed**, font/shaping setup, no-hardcoded-string lint). Ship UI in **`en`** first, with the *machinery* for all 7.
- **Phase 2–3:** as safety + onboarding land, their **content** is authored localizable and the priority languages for launch regions (`en`, `ur`, `de`) get real translations + native review.
- **Pre-launch:** all 7 UI languages complete; sensitive surfaces (crisis/questionnaire/companion) native-reviewed for at least the launch-region languages; others can follow but must pass the sensitive-surface gate before being offered.
- **Honest staging option:** if all 7 *sensitive-surface* reviews aren't ready at launch, it's acceptable to ship UI in 7 languages but **gate the questionnaire/companion in a given language** behind its native review — never ship unreviewed sensitive content in any language.

---

## 8. CLAUDE.md hook (added so we don't drift)

`CLAUDE.md` now carries a localization rule in its non-negotiables region: *no hardcoded user-facing strings; RTL-correct from the start; sensitive surfaces (crisis/questionnaire/companion) require native-speaker review per language; the copy-lint runs per language.* Every coding session inherits it.
