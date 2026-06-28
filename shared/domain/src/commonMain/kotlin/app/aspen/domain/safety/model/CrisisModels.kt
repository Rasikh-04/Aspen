package app.aspen.domain.safety.model

/**
 * Crisis-registry region key (docs/09 §2.1). This is a COUNTRY/region key, deliberately separate
 * from the UI language (app.aspen.core.i18n.SupportedLanguage). A user's UI language must NEVER be
 * used to infer their crisis region (CLAUDE.md #11, docs/12 §6): an Arabic UI does not imply any
 * particular country. [INTL] is the always-present international fallback (docs/10 §6).
 */
enum class LocaleKey {
    PK,
    DE,
    UK,
    US,
    INTL,
    ;

    companion object {
        /**
         * Map an ISO-3166 alpha-2 country code to a [LocaleKey], or null if unsupported.
         * Accepts the common "GB" alias for the UK. Never throws.
         */
        fun fromCountryCode(code: String?): LocaleKey? {
            if (code.isNullOrBlank()) return null
            return when (code.trim().uppercase()) {
                "PK" -> PK
                "DE" -> DE
                "UK", "GB" -> UK
                "US" -> US
                else -> null
            }
        }
    }
}

/** What a crisis resource is for (docs/09 §2.1). */
enum class Purpose { ED_SUPPORT, ACUTE_CRISIS, TREATMENT_FINDER, TRUSTED_PERSON }

/** How a resource is reached. IN_APP routes to an in-app destination (e.g. trusted contact). */
enum class ContactMethod { PHONE, SMS, URL, IN_APP }

/** A single way to reach a resource: a method plus a localized label and the dialable/openable value. */
data class Contact(
    val method: ContactMethod,
    val label: String,
    val value: String,
)

/**
 * One crisis/support resource (docs/09 §2.1). Provenance fields [verifiedOn]/[verifiedBy] are
 * REQUIRED and drive the release-gating freshness check (SR-2): no resource ships to a launch
 * locale while either is a TODO/placeholder. Never include NEDA (CLAUDE.md #7).
 */
data class CrisisResource(
    val id: String,
    val locale: LocaleKey,
    val name: String,
    val purpose: Purpose,
    val contacts: List<Contact>,
    val hours: String?,
    val languages: List<String>,
    val notes: String?,
    val verifiedOn: String,
    val verifiedBy: String,
)

/**
 * The resolved set of resources for a region (docs/09 §2.2). Grouped by purpose so a calm Flow C UI
 * can show "support" separately from "acute crisis". [isFallback] is true when the requested region
 * had no enabled registry and we fell back to [LocaleKey.INTL].
 *
 * Contract: a resolver never returns an empty set — there is always at least the INTL fallback.
 */
data class CrisisResourceSet(
    val locale: LocaleKey,
    val edSupport: List<CrisisResource>,
    val acuteCrisis: List<CrisisResource>,
    val treatmentFinder: List<CrisisResource>,
    val isFallback: Boolean,
) {
    /** True when this set carries no resources at all — a state the resolver must never produce. */
    fun isEmpty(): Boolean = edSupport.isEmpty() && acuteCrisis.isEmpty() && treatmentFinder.isEmpty()
}
