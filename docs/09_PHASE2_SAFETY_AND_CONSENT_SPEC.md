# 09 — Phase 2 Spec: Safety Subsystem + Consent Primitive

> Implementation-ready spec for the first real build phase. Both pieces live in **shared** code (`:shared:domain` + `:shared:data`), so they exist on Android and iOS from day one. Build these **before** features, because everything leans on them. Nothing here ships to users without advisor sign-off on the crisis registry content.

> **Scope note:** this is interface- and contract-level, KMP-ready (Kotlin, `expect/actual` where platform-specific). It is deliberately not full code — it's what you scaffold against. Treat type names as proposals.

---

## 0. Why Phase 2 is safety + consent together

- **Safety** is the highest-stakes path in the app; build and harden it first so every later feature can rely on it.
- **Consent primitive** built now (even with zero recipients) means clinician-linkage, export, and any future sharing become *"issue a grant,"* not a refactor (`08` §3). Building it later would force breaking changes — exactly what you said you want to avoid.

---

## 1. Module placement

```
:shared:domain
  safety/
    SafetyEngine.kt            ← façade: output guard + copy guard hooks + crisis resolve
    CrisisResolver.kt          ← locale → resources
    SafetyRules.kt             ← the non-negotiable predicates (numbers/appearance/etc.)
    model/ (CrisisResource, Locale, SafetyVerdict, RedactedOutput…)
  consent/
    ConsentManager.kt          ← grant / revoke / check / audit
    model/ (ConsentGrant, DataCategory, Recipient, Scope…)
:shared:data
  crisis/
    CrisisRegistry.kt          ← data source (bundled JSON per locale) + offline cache
    CrisisRegistryRepo.kt
  consent/
    ConsentStore.kt            ← encrypted local persistence of grants
:androidApp / :iosApp
  thin UI for Flow C (safety) — calls SafetyEngine; no logic here
```

Safety + consent logic is **platform-agnostic** and **unit-testable without a device**.

---

## 2. Safety subsystem

### 2.1 `CrisisResource` model

```kotlin
data class CrisisResource(
  val id: String,
  val locale: LocaleKey,            // PK, DE, UK, US, INTL
  val name: String,                 // e.g. "Beat", "BZgA", "National Alliance for Eating Disorders"
  val purpose: Purpose,             // ED_SUPPORT, ACUTE_CRISIS, TRUSTED_PERSON, TREATMENT_FINDER
  val contacts: List<Contact>,      // phone / text / url, each with label + value
  val hours: String?,               // human-readable, localized
  val languages: List<String>,      // e.g. ["de"], ["en","ur"]
  val notes: String?,               // e.g. "staffed by licensed clinicians"
  val verifiedOn: String,           // ISO date — REQUIRED; release gate checks freshness
  val verifiedBy: String            // advisor/initials — provenance for the SR-2 checklist
)

enum class Purpose { ED_SUPPORT, ACUTE_CRISIS, TREATMENT_FINDER, TRUSTED_PERSON }
data class Contact(val method: ContactMethod, val label: String, val value: String)
enum class ContactMethod { PHONE, SMS, URL, IN_APP }
```

**Registry data** lives as bundled, versioned JSON per locale (so it works offline and is reviewable in git). The **draft content** to load is in `docs/10_CRISIS_REGISTRY_DRAFT.md` (anchor orgs grounded in research; every number `⚠VERIFY`). The registry is keyed by **country**, separate from UI **language** (`docs/12` §6). Example shape (content is placeholder — **advisor-verified before ship**):

```json
{
  "locale": "DE",
  "version": "2026-06-01",
  "resources": [
    { "id": "de-bzga", "name": "BZgA Essstörungen", "purpose": "ED_SUPPORT",
      "contacts": [{ "method": "PHONE", "label": "Beratungstelefon", "value": "TODO-VERIFY" }],
      "languages": ["de"], "verifiedOn": "TODO", "verifiedBy": "TODO" }
  ]
}
```

> **Hard rule:** no resource ships with `verifiedOn = TODO`. The release gate (SR-2) **fails the build** if any supported-locale resource is unverified or stale beyond a threshold. **Never include NEDA.** For US, use the National Alliance for Eating Disorders (clinician-staffed) + 988 for acute crisis.

### 2.2 `CrisisResolver`

```kotlin
interface CrisisResolver {
  /** Always returns a usable set: requested locale if available, else INTL fallback. Never empty, never throws. */
  fun resolve(locale: LocaleKey): CrisisResourceSet
}

data class CrisisResourceSet(
  val locale: LocaleKey,
  val edSupport: List<CrisisResource>,
  val acuteCrisis: List<CrisisResource>,
  val treatmentFinder: List<CrisisResource>,
  val isFallback: Boolean        // true if we fell back to INTL
)
```

Contracts:
- **Never returns empty / never throws** — there is always at least the INTL fallback.
- **Works fully offline** from bundled/cached data (no network on the crisis path).
- Pure/deterministic → trivially unit-testable for every locale.

### 2.3 `SafetyRules` — the non-negotiables as code

These predicates encode `01` §6. Used by the AI output guard (Phase 4) and the build-time copy lint (`06` §6.4), and available to any feature.

```kotlin
object SafetyRules {
  /** Detect forbidden numeric-about-food/body content. */
  fun containsForbiddenNumbers(text: String): Boolean
  /** Detect appearance commentary (any direction). */
  fun containsAppearanceComment(text: String): Boolean
  /** Detect eating/diet/exercise advice. */
  fun containsEatingAdvice(text: String): Boolean
  /** Forbidden shame/failure tokens in user-facing copy. */
  fun containsShameLanguage(text: String): Boolean
}

// Token/pattern lists live in a reviewed resource file, not inline:
//   forbidden_numbers: kcal, calorie(s), BMI, kg, lbs, macro(s), portion counts, weight, "goal weight"…
//   appearance: "you look…", thin, fat, skinny, "healthy weight"…
//   shame: fail, failed, missed, incomplete, "you didn't"…
// Lists are advisor-reviewed and localized (DE/UR too).
```

> These are **detection heuristics for guards/lints**, not a guarantee — the real guarantee for AI output is curation (companion library) + human review. Heuristics are the backstop, not the front line.

### 2.4 `SafetyEngine` (façade)

```kotlin
interface SafetyEngine {
  fun crisis(locale: LocaleKey): CrisisResourceSet
  /** Phase 4 hook: vet AI output; returns safe text or a validated fallback + handoff. */
  fun guardOutput(candidate: String, ctx: OutputContext): SafetyVerdict
}

sealed interface SafetyVerdict {
  data class Pass(val text: String) : SafetyVerdict
  data class Rewrite(val safeText: String, val reason: String) : SafetyVerdict  // replaced w/ validation + Flow C handoff
}
```

In Phase 2, implement `crisis()` fully; stub `guardOutput()` with the rule checks + a safe fallback (wired for real in Phase 4).

### 2.5 Build-time release gates (SR-2 / SR-1)

A CI step that **fails the build** if:
1. Any supported-locale (`PK, DE, UK` now; `US` when enabled) crisis resource is missing `verifiedOn`/`verifiedBy` or is older than the freshness threshold.
2. The string-resource copy lint finds a forbidden token (numbers/shame/appearance) not explicitly allow-listed with reviewer note.
3. "NEDA" appears anywhere in crisis data (explicit deny).

---

## 3. Consent primitive

Built now with **no recipients required** — it just has to exist and be correct so later features attach to it.

### 3.1 Model

```kotlin
data class ConsentGrant(
  val id: String,
  val recipient: Recipient,           // who can see (clinician, trusted person, "self-export")
  val categories: Set<DataCategory>,  // scoped: what they can see
  val grantedAt: Instant,
  val expiresAt: Instant?,            // optional time-box
  val revokedAt: Instant?,           // null = active
  val purpose: String                 // human-readable why
)

enum class DataCategory { REFLECTIONS, FOOD_LOGS, BEHAVIOUR_LOGS, PROFILE, SAFETY_EVENTS }

data class Recipient(
  val id: String,
  val type: RecipientType,            // LINKED_CLINICIAN, TRUSTED_PERSON, SELF_EXPORT, AFFILIATED_DOCTOR(future)
  val displayName: String
)
```

**Data-category tagging now:** even though linkage is later, tag stored data by `DataCategory` from the start (reflections vs. logs vs. profile vs. safety events), so a future grant can be scoped precisely without migration.

### 3.2 API

```kotlin
interface ConsentManager {
  fun grant(recipient: Recipient, categories: Set<DataCategory>,
            expiresAt: Instant? = null, purpose: String): ConsentGrant
  fun revoke(grantId: String)                       // immediate; one tap from UI
  fun activeGrants(): List<ConsentGrant>
  fun canAccess(recipientId: String, category: DataCategory): Boolean  // default-DENY
  fun auditLog(): List<ConsentEvent>                // user can always see who-saw-what-when
}
```

Contracts (the invariants that make this safe to build on):
- **Default deny:** `canAccess` is false unless a *non-expired, non-revoked* grant covers that exact category.
- **Revoke is immediate and total** for future access; reflected in audit.
- **Auditable:** every grant/revoke/access-check is logged for the user's eyes.
- **Persisted encrypted** (`ConsentStore`, local, device-key) — consent state is sensitive.
- **No recipient infrastructure needed yet** — `SELF_EXPORT` is the only recipient type wired in Phase 2 (used later by export); clinician types are defined but dormant.

---

## 4. Test requirements (this phase is heavily tested — it's the foundation)

**Crisis / safety (highest coverage in the app):**
- `resolve()` returns non-empty for **every** supported locale **and** for an unknown locale (→ INTL fallback).
- Offline path: with network forced off, crisis resources still resolve from bundle/cache.
- Gate test: a resource with `verifiedOn = null` **fails** the gate; a "NEDA" entry **fails** the deny check.
- `SafetyRules` predicates: table-driven tests over known-bad and known-good strings (incl. DE/UR samples).
- Property: `resolve()` never throws, never returns empty, for any input.

**Consent:**
- `canAccess` is **false** with no grant; **true** only within an active, in-scope, non-expired grant; **false** immediately after `revoke`; **false** after `expiresAt`.
- Audit log records grant/revoke/access.
- Round-trip persistence (encrypted store) preserves grants; corrupted store fails safe (deny).

**Coverage target:** safety + consent are the modules where you do *not* compromise on coverage. Treat untested branches here as release-blocking.

---

## 5. Definition of Done (Phase 2)

- [ ] Shared `:shared:domain/safety` + `:shared:domain/consent` and `:shared:data/{crisis,consent}` build for **both** Android and iOS targets.
- [ ] `CrisisResolver.resolve()` returns correct, **offline**, non-empty sets for PK/DE/UK (+ INTL fallback); US scaffolded, disabled until verified.
- [ ] Flow C UI (thin) reaches a region-correct resource in **≤2 taps** from a placeholder home; calm styling, never alarm-red.
- [ ] Trusted-contact set/reach works (stored as reference, not harvested).
- [ ] `ConsentManager` implements grant/revoke/check/audit with **default-deny** and immediate revoke; data tagged by `DataCategory`.
- [ ] CI release gates active: crisis-freshness gate, copy-lint, NEDA-deny — and they actually fail the build when violated (tested).
- [ ] Test coverage on safety + consent meets the no-compromise bar.
- [ ] **Crisis registry content marked `TODO-VERIFY` everywhere it isn't yet advisor-verified** — i.e. the *mechanism* ships in Phase 2, but no locale is marked shippable until your PK/US advisors sign the content. (Don't let real numbers land unverified.)

---

## 6. Scaffolding order (practical, for your first sessions)

1. KMP + CMP project skeleton with **both targets compiling in CI** (Phase 1 carry-in).
2. `:shared:domain` safety models + `SafetyRules` (+ tests) — pure, fast.
3. `:shared:data:crisis` bundled JSON (TODO-VERIFY content) + `CrisisResolver` (+ offline tests).
4. CI gates (freshness, copy-lint, NEDA-deny).
5. `:shared:domain/consent` + `:shared:data:consent` encrypted store (+ tests).
6. Thin Flow C UI in `:androidApp` wired to `SafetyEngine.crisis()`.
7. Hand the verified-content task to your non-technical teammate + advisors in parallel — they fill the registry; you build the mechanism.

> When this is green, you've got the spine. Then bring your dev partner onto Phase 3 (onboarding + grounding + reflection/logging) against the shared foundation, and the consent primitive is already there for linkage later.
