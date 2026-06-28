package app.aspen.data.crisis

import app.aspen.domain.safety.model.Contact
import app.aspen.domain.safety.model.ContactMethod
import app.aspen.domain.safety.model.CrisisResource
import app.aspen.domain.safety.model.LocaleKey
import app.aspen.domain.safety.model.Purpose

/**
 * The in-code crisis registry — the offline source of truth the app actually resolves against
 * (docs/09 §2.1). It is a faithful MIRROR of the canonical, advisor-editable JSON under
 * `config/safety/crisis/` JSON files; [app.aspen.data.crisis.CrisisRegistryParityTest] (JVM) fails the
 * build if the two drift. Edit BOTH together.
 *
 * Why in-code and not a runtime resource reader (deviation from docs/09's "bundled JSON + reader"):
 * compiling the registry in makes the resolver's hard contract — **never empty, never throws, fully
 * offline** — a compile-time guarantee on every platform, with no `actual` file-IO that could fail
 * at runtime or be unverifiable from a CI host without the platform. Advisors still edit plain JSON;
 * the build-time freshness gate (SR-2) reads that JSON; the parity test keeps them identical.
 *
 * Provenance: every contact value and every [CrisisResource.verifiedOn]/[CrisisResource.verifiedBy]
 * here is the placeholder `TODO-VERIFY`. That is deliberate — the SR-2 release gate FAILS the build
 * for any launch-locale resource still unverified, so unverified content physically cannot ship
 * (docs/10 §7). Anchor org NAMES come from research (docs/10); no real numbers are invented in code.
 * NEDA appears nowhere (CLAUDE.md #7).
 */
object CrisisRegistry {

    /** Sentinel used for every not-yet-advisor-verified field. The SR-2 gate keys on this. */
    const val UNVERIFIED: String = "TODO-VERIFY"

    /**
     * PROVISIONAL provenance for launch locales (PK/DE/UK): a deliberate, self-documenting marker so
     * local dev / the non-strict `crisisGate` isn't halted before advisors sign off. Contact VALUES
     * stay [UNVERIFIED] (non-actionable in the UI), and the release gate `crisisGateStrict` still
     * rejects this — real advisor verification is required before ship (docs/PRE_SHIP_VERIFICATION.md).
     * Must mirror config/safety/crisis/{pk,de,uk}.json exactly (parity test).
     */
    const val PROVISIONAL_BY: String = "PROVISIONAL-UNVERIFIED"
    const val PROVISIONAL_ON: String = "2026-06-28"
    private val PROVISIONAL_LOCALES = setOf(LocaleKey.PK, LocaleKey.DE, LocaleKey.UK)

    /**
     * Regions whose registry is wired live. US is present in [byLocale] but intentionally NOT here
     * until US advisors sign off; resolving US therefore falls back to INTL. INTL is always usable
     * and is the universal fallback (it is not a "launch locale", it is the safety net).
     */
    val enabledLocales: Set<LocaleKey> = setOf(LocaleKey.PK, LocaleKey.DE, LocaleKey.UK)

    private fun phone(label: String) = Contact(ContactMethod.PHONE, label, UNVERIFIED)
    private fun url(label: String) = Contact(ContactMethod.URL, label, UNVERIFIED)
    private fun inApp(label: String) = Contact(ContactMethod.IN_APP, label, UNVERIFIED)

    private fun resource(
        id: String,
        locale: LocaleKey,
        name: String,
        purpose: Purpose,
        contacts: List<Contact>,
        languages: List<String>,
        notes: String?,
    ) = CrisisResource(
        id = id,
        locale = locale,
        name = name,
        purpose = purpose,
        contacts = contacts,
        hours = null,
        languages = languages,
        notes = notes,
        verifiedOn = if (locale in PROVISIONAL_LOCALES) PROVISIONAL_ON else UNVERIFIED,
        verifiedBy = if (locale in PROVISIONAL_LOCALES) PROVISIONAL_BY else UNVERIFIED,
    )

    /** All resources by region. Mirrors `config/safety/crisis/<locale>.json`. */
    val byLocale: Map<LocaleKey, List<CrisisResource>> = mapOf(
        LocaleKey.INTL to listOf(
            resource(
                "intl-findahelpline", LocaleKey.INTL, "Find A Helpline", Purpose.ED_SUPPORT,
                listOf(url("Find a verified helpline near you")), listOf("en"),
                "ThroughLine — verified crisis helplines across 175+ countries with an eating/body-image topic filter; the international backbone (docs/10 §6).",
            ),
            resource(
                "intl-findahelpline-finder", LocaleKey.INTL, "Find local eating-disorder help", Purpose.TREATMENT_FINDER,
                listOf(url("Ways to find support near you")), listOf("en"),
                "Honest fallback: Aspen does not yet have a verified local ED registry for every country; route users to vetted ways to find support.",
            ),
        ),
        LocaleKey.UK to listOf(
            resource(
                "uk-beat", LocaleKey.UK, "Beat (Beat Eating Disorders)", Purpose.ED_SUPPORT,
                listOf(phone("Helpline"), url("Online support")), listOf("en"),
                "UK national eating-disorder charity; primary anchor.",
            ),
            resource(
                "uk-nhs-ed", LocaleKey.UK, "NHS eating-disorder services", Purpose.TREATMENT_FINDER,
                listOf(url("NHS eating disorders"), phone("NHS 111 (urgent, non-emergency)")), listOf("en"),
                "Route into NHS specialist services.",
            ),
            resource(
                "uk-samaritans", LocaleKey.UK, "Samaritans", Purpose.ACUTE_CRISIS,
                listOf(phone("Samaritans (24/7)")), listOf("en"),
                "General distress / suicide support, 24/7.",
            ),
            resource(
                "uk-emergency", LocaleKey.UK, "Emergency services", Purpose.ACUTE_CRISIS,
                listOf(phone("Emergency (life-threatening only)")), listOf("en"),
                "Life-threatening situations only.",
            ),
        ),
        LocaleKey.DE to listOf(
            resource(
                "de-bzga", LocaleKey.DE, "BZgA — Essstörungen", Purpose.ED_SUPPORT,
                listOf(phone("Beratungstelefon"), url("Beratung online")), listOf("de"),
                "Federal anchor; maintains the national counselling-centre directory.",
            ),
            resource(
                "de-anad", LocaleKey.DE, "ANAD e.V.", Purpose.ED_SUPPORT,
                listOf(phone("Beratung")), listOf("de"),
                "Munich specialist ED organisation (distinct from the US 'ANAD').",
            ),
            resource(
                "de-treatment-finder", LocaleKey.DE, "Spezialisierte Behandlung finden", Purpose.TREATMENT_FINDER,
                listOf(url("Behandlungsverzeichnis")), listOf("de"),
                "Provider directories (DGESS / BFE) for finding specialist clinicians.",
            ),
            resource(
                "de-telefonseelsorge", LocaleKey.DE, "Telefonseelsorge", Purpose.ACUTE_CRISIS,
                listOf(phone("Telefonseelsorge (24/7)")), listOf("de"),
                "General 24/7 crisis support.",
            ),
            resource(
                "de-emergency", LocaleKey.DE, "Notruf", Purpose.ACUTE_CRISIS,
                listOf(phone("Notruf (nur bei Lebensgefahr)")), listOf("de"),
                "Life-threatening situations only.",
            ),
        ),
        LocaleKey.PK to listOf(
            resource(
                "pk-partner-clinicians", LocaleKey.PK, "Partner clinicians", Purpose.ED_SUPPORT,
                listOf(inApp("Aspen partner clinical contacts")), listOf("ur", "en"),
                "Most reliable ED-specific route locally; supplied by the advisor network.",
            ),
            resource(
                "pk-umang", LocaleKey.PK, "Umang Helpline", Purpose.ACUTE_CRISIS,
                listOf(phone("Umang (emotional support)")), listOf("ur", "en"),
                "General emotional / mental-health support; advisor to confirm currently operating.",
            ),
            resource(
                "pk-findahelpline", LocaleKey.PK, "Find A Helpline — Pakistan", Purpose.ED_SUPPORT,
                listOf(url("Find a verified helpline")), listOf("ur", "en"),
                "Verified cross-check / fallback while the local registry is thin.",
            ),
            resource(
                "pk-emergency", LocaleKey.PK, "Emergency services", Purpose.ACUTE_CRISIS,
                listOf(phone("Emergency (life-threatening only)")), listOf("ur", "en"),
                "Life-threatening situations only.",
            ),
        ),
        LocaleKey.US to listOf(
            resource(
                "us-alliance", LocaleKey.US, "National Alliance for Eating Disorders", Purpose.ED_SUPPORT,
                listOf(phone("Helpline (licensed clinicians)")), listOf("en"),
                "Primary US ED line — staffed by licensed clinicians (the correct US ED line to use).",
            ),
            resource(
                "us-anad", LocaleKey.US, "ANAD", Purpose.ED_SUPPORT,
                listOf(phone("Helpline (peer support + referrals)")), listOf("en", "es"),
                "Peer support and referrals.",
            ),
            resource(
                "us-988", LocaleKey.US, "988 Suicide & Crisis Lifeline", Purpose.ACUTE_CRISIS,
                listOf(phone("988 (call or text)")), listOf("en", "es"),
                "General acute crisis, 24/7.",
            ),
            resource(
                "us-emergency", LocaleKey.US, "Emergency services", Purpose.ACUTE_CRISIS,
                listOf(phone("Emergency (life-threatening only)")), listOf("en", "es"),
                "Life-threatening situations only.",
            ),
        ),
    )
}
