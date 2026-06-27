package app.aspen.core.i18n

/**
 * The seven languages Aspen ships from first release (docs/12 §1).
 * English is the worldwide fallback. [isRtl] drives layout direction from day one.
 *
 * NOTE: language is independent of region/country (docs/12 §6). This type carries
 * NO country information and must never be used to infer a crisis region.
 */
enum class SupportedLanguage(val code: String, val isRtl: Boolean) {
    EN("en", isRtl = false),
    UR("ur", isRtl = true),
    DE("de", isRtl = false),
    ZH("zh", isRtl = false),
    HI("hi", isRtl = false),
    AR("ar", isRtl = true),
    ES("es", isRtl = false),
    ;

    companion object {
        /** Worldwide default / fallback (docs/12 §1). */
        val DEFAULT: SupportedLanguage = EN

        /** Match a primary language subtag (e.g. "en" from "en-US"); null if unsupported. */
        fun fromCode(code: String?): SupportedLanguage? {
            if (code.isNullOrBlank()) return null
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
        }
    }
}
