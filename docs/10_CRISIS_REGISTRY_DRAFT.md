# 10 — Crisis & Support Resource Registry (DRAFT)

> **⚠ This is an UNVERIFIED draft for your advisors to verify, localise, and complete.** It names *real, credible anchor organisations* found in research, but **every specific phone number / contact detail is marked `⚠VERIFY`** and must be confirmed against the organisation's official source before shipping. A stale number in a crisis flow is the worst possible bug in this app.

---

## How to read & use this file

- This is the **content** that fills the `CrisisRegistry` mechanism specified in `docs/09` §2. The *schema* is built in Phase 2; the *content* below is the starting point for the human verification task.
- **Routing principle (safe by design):** prefer routing the user **to the organisation** (name + official site/app) and let the live, verified number come from your advisor-checked data. Organisations are stable; raw numbers drift.
- **Launch registries (curated, advisor-verified):** Pakistan, Germany, UK, USA.
- **Everywhere else (worldwide language release):** the **International Fallback** strategy (§6) — because *offering a language ≠ having a verified local registry*. A Hindi/Mandarin/Arabic/Spanish speaker outside a launch country still gets something safe.
- **The single best universal backbone:** **Find A Helpline (ThroughLine)** maintains *verified* crisis helplines across **175+ countries** with an **eating/body-image topic filter** — use it as the international fallback and as a cross-check for every locale. https://findahelpline.com/ (eating/body topic: `/topics/eating-body-image`).
- **Never include NEDA** (the US NEDA Helpline is disconnected). For acute suicide/self-harm crisis, pair ED resources with the country's general crisis line.

Each entry maps to the `CrisisResource` model (`docs/09` §2.1): `name`, `purpose`, `contacts[]`, `languages`, `hours`, `verifiedOn/By`.

---

## 1. 🇬🇧 United Kingdom (launch)

| Org | Purpose | Anchor (verify details) | Notes |
|---|---|---|---|
| **Beat** (Beat Eating Disorders) | ED_SUPPORT | Helplines + online support; official site `beateatingdisorders.org.uk` · phone/webchat `⚠VERIFY` | UK's national ED charity; the primary anchor. |
| **NHS 111 / NHS ED services** | TREATMENT_FINDER | `nhs.uk` ED pages; 111 for urgent non-emergency `⚠VERIFY` | Route into specialist services. |
| **Samaritans** | ACUTE_CRISIS | `116 123` `⚠VERIFY` (long-standing UK/IE number) | General distress/suicide, 24/7. |
| **999** | ACUTE_CRISIS (emergency) | Emergency services | Life-threatening only. |

Languages: `en`. (Beat resources predominantly English.)

---

## 2. 🇩🇪 Germany (launch)

| Org | Purpose | Anchor (verify details) | Notes |
|---|---|---|---|
| **BZgA** (Bundeszentrale für gesundheitliche Aufklärung) — Essstörungen | ED_SUPPORT / TREATMENT_FINDER | Federal ED counselling info + counselling-centre database; `bzga-essstoerungen.de` · Beratungstelefon `⚠VERIFY` | Federal anchor; maintains the national counselling-centre directory. |
| **ANAD e.V.** (Versorgungszentrum Essstörungen, Munich) | ED_SUPPORT | `anad.de` · helpline `⚠VERIFY` | Major specialist ED org. (Not the US "ANAD" — different entity.) |
| **DGESS / BFE** | TREATMENT_FINDER | Professional networks; provider directories `⚠VERIFY` | For finding specialist clinicians. |
| **Telefonseelsorge** | ACUTE_CRISIS | `0800 111 0 111` / `0800 111 0 222` `⚠VERIFY` (long-standing) | General 24/7 crisis support. |
| **112** | ACUTE_CRISIS (emergency) | EU emergency number | Life-threatening only. |

Languages: `de` (crisis copy **must** be in German).

---

## 3. 🇺🇸 United States (launch when ready; you have US advisors)

| Org | Purpose | Anchor (verify details) | Notes |
|---|---|---|---|
| **National Alliance for Eating Disorders** | ED_SUPPORT | Helpline **`1-866-662-1235`** `⚠VERIFY` — *staffed by licensed clinicians* | **Use this as the primary US ED line — NOT NEDA.** |
| **ANAD** (Nat'l Assoc. of Anorexia Nervosa & Assoc. Disorders) | ED_SUPPORT | Helpline `1-888-375-7767` `⚠VERIFY`; peer-led; referrals | Peer support + referrals; donation-based. |
| **988 Suicide & Crisis Lifeline** | ACUTE_CRISIS | `988` (call/text) `⚠VERIFY` | General acute crisis, 24/7. |
| **911** | ACUTE_CRISIS (emergency) | Emergency services | Life-threatening only. |

Languages: `en`, `es` (988 and many lines offer Spanish).
**Do NOT list the NEDA Helpline** — disconnected.

---

## 4. 🇵🇰 Pakistan (launch)

> **Honest caveat:** established **ED-specific** helplines in Pakistan are essentially absent — there is little dedicated ED infrastructure. This is exactly where **your local clinical contacts** and the **international fallback** carry the load. Be transparent in-app that local ED-specific services are limited, and lean on general mental-health crisis support + your advisor network + treatment-finder routing.

| Org | Purpose | Anchor (verify details) | Notes |
|---|---|---|---|
| **Your Pakistan clinical contacts / partner clinicians** | ED_SUPPORT / TREATMENT_FINDER | To be supplied by your network `⚠VERIFY` | Most reliable ED-specific route locally. |
| **Umang Helpline** (Pakistan mental-health support) | ACUTE_CRISIS / ED_SUPPORT | `⚠VERIFY` via official source | General emotional/mental-health support. |
| **Rozan / other vetted MH lines** | ACUTE_CRISIS | `⚠VERIFY` | Advisor to confirm currently-operating lines. |
| **Find A Helpline — Pakistan** | ED_SUPPORT (fallback) | `findahelpline.com/countries/pk` | Verified cross-check / fallback. |
| **1122 / local emergency** | ACUTE_CRISIS (emergency) | Emergency services | Life-threatening only. |

Languages: `ur`, `en` (crisis copy in Urdu — note RTL, see `docs/12`).
**Advisor action:** because the local landscape is thin and changing, treat Pakistan as the registry needing the most hands-on local curation.

---

## 5. Additional language regions (worldwide release — registries to research/curate)

The 7 launch *languages* reach far beyond the 4 launch *countries*. For users in these regions, route via **Find A Helpline** (verified per-country) + International Fallback until you build a curated registry. Credible anchors found so far:

| Region / language | Credible anchor(s) (verify + expand with advisors) | Status |
|---|---|---|
| **🇮🇳 India (Hindi + many)** | **Tele-MANAS** (Govt of India 24/7 MH) · **KIRAN** `1800-599-0019` `⚠VERIFY` · **Vandrevala Foundation** (24/7, multilingual) · ED treatment via Sangath, MINDS Foundation | Good anchors exist; ED-specific is limited — use MH lines + Find A Helpline `/in`. |
| **🇪🇸 Spain / Spanish (es)** | **FEACAB** (national federation of ED associations, `feacab.org`) · **ACAB** (`acab.org`, Barcelona) · **ADANER** · regional associations `⚠VERIFY` numbers | Strong, well-established ED network. |
| **Latin America (es)** | Per-country via **Find A Helpline**; advisor to identify national ED orgs | Registry needed per country. |
| **🇨🇳 China / Mandarin (zh)** | **Sparse dedicated ED infrastructure**; route via **Find A Helpline `/cn`** + general MH lines; advisor research required | **Honest gap** — do not fabricate; advisor-research task. |
| **Arabic-speaking regions (ar)** | **Varies enormously by country**; no single anchor. Route via **Find A Helpline** per country (e.g. `/eg`, `/sa`, `/ae`) + general MH lines; advisor research required | **Honest gap** — per-country research task. |

> **Design consequence:** the registry is keyed by **country/locale**, and **language is separate from country** (`docs/12`). A user's *UI language* (e.g. Arabic) does not determine their *crisis registry* (their country does). The app should detect/ask region for crisis routing, fall back to International, and never assume "Arabic UI → some default Arab country."

---

## 6. International Fallback (always present, every locale)

For any user whose country has no curated registry yet:
1. **Find A Helpline (ThroughLine)** — verified helplines in 175+ countries, eating/body-image filter, multi-language, free. Deep-link to the user's country where possible (`findahelpline.com/countries/<cc>`).
2. **"Find local help" guidance** — honest copy: *"Aspen doesn't yet have verified local eating-disorder services for your area. Here are vetted ways to find support near you."*
3. **General emergency guidance** — surface the local emergency number for the detected country (112 EU, 911 US, 999 UK, 1122/15 PK, 112 IN, etc.) **for life-threatening situations only**, clearly distinguished from ED support.
4. **Trusted contact** — the user's own pre-set person is always offered regardless of region (`docs/06` §6.2).

---

## 7. Verification checklist (gate before ANY locale ships — ties to `docs/09` §2.5 SR-2)

For each resource, before its locale is marked shippable:
- [ ] Organisation still operating and ED-relevant (or correctly categorised as general MH/emergency).
- [ ] Contact detail confirmed against the **organisation's own official source**, dated.
- [ ] Hours / languages confirmed.
- [ ] `verifiedOn` (date) + `verifiedBy` (advisor) recorded.
- [ ] Localised copy reviewed in the target language by a native-speaking, ED-informed reviewer.
- [ ] **NEDA appears nowhere.**
- [ ] Acute-crisis option present and clearly distinguished from ED support.
- [ ] Tested offline (cached) and reachable in ≤2 taps.

> **Build note:** ship the *mechanism* with all numbers as `⚠VERIFY`/`TODO-VERIFY`. The CI crisis-freshness gate (`docs/09` §2.5) **fails the build** if any supported-locale resource is unverified — so unverified content physically cannot ship to a launch locale. That's the safety backstop working as intended.

---

## 8. Sources (anchors; not for the live numbers — verify those)

- Beat (UK), NHS — UK ED infrastructure (this session's research).
- BZgA, ANAD e.V., DGESS, BFE (Germany) — PMC11965544 (DigiBEssst), edreferral.com/germany.
- National Alliance for Eating Disorders (US helpline, clinician-staffed); ANAD (US); 988 — eatingdisorderhope.com hotlines; anad.org.
- India: Tele-MANAS, KIRAN `1800-599-0019` (PMC7561607), Vandrevala, Sangath — findahelpline.com/countries/in; thelivelovelaughfoundation.org.
- Spain: FEACAB (`feacab.org`), ACAB (`acab.org`), ADANER — FEACAB/ACAB sites; ScienceDirect S1695403325001420.
- **Find A Helpline (ThroughLine)** — verified helplines, 175+ countries, eating/body-image topic — findahelpline.com.
- NEDA disconnection confirmed (this session + prior); **excluded**.

> Treat all numbers above as **starting pointers, not facts.** Your advisors and the non-technical teammate own turning this draft into verified, dated, localised registry data.
