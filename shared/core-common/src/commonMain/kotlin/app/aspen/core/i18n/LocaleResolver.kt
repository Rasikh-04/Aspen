package app.aspen.core.i18n

/**
 * Resolves the active UI language (docs/12 §4): system default -> supported? -> else English.
 * A user override always wins.
 *
 * This resolves LANGUAGE ONLY. It deliberately has no notion of region/country, so it can
 * never infer a crisis region from a UI language (docs/12 §6).
 */
object LocaleResolver {

    /**
     * @param systemLanguageTag a BCP-47-ish tag from the platform (e.g. "ur-PK", "de_DE"), or null.
     * @param override an explicit user choice from Settings, or null to follow the system.
     */
    fun resolve(systemLanguageTag: String?, override: SupportedLanguage? = null): SupportedLanguage {
        if (override != null) return override
        val primarySubtag = systemLanguageTag
            ?.trim()
            ?.substringBefore('-')
            ?.substringBefore('_')
        return SupportedLanguage.fromCode(primarySubtag) ?: SupportedLanguage.DEFAULT
    }

    /** Layout direction for a language — the single source of truth for RTL (docs/12 §2). */
    fun layoutDirFor(language: SupportedLanguage): LayoutDir =
        if (language.isRtl) LayoutDir.RTL else LayoutDir.LTR
}
