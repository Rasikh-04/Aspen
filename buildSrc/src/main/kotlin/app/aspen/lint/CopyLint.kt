package app.aspen.lint

/** A single copy-lint finding. */
data class Violation(
    val language: String,
    val stringName: String,
    val token: String,
    val category: TokenCategory,
)

/**
 * Pure, unit-testable copy-lint core. Matches forbidden tokens as whole words (Unicode-aware, so
 * it works for Latin and non-Latin scripts), case-insensitively. Allow-listed tokens (reviewed,
 * with a note) are suppressed.
 */
object CopyLint {

    private fun wholeWordRegex(token: String): Regex =
        Regex("(?<!\\p{L})" + Regex.escape(token.trim()) + "(?!\\p{L})", RegexOption.IGNORE_CASE)

    /** Scan one string value against a token set; returns every forbidden token it contains. */
    fun scanValue(
        language: String,
        stringName: String,
        value: String,
        tokens: TokenSet,
        allowList: Set<String> = emptySet(),
    ): List<Violation> {
        val findings = mutableListOf<Violation>()
        for (category in TokenCategory.entries) {
            for (token in tokens.byCategory(category)) {
                if (token.lowercase() in allowList) continue
                if (wholeWordRegex(token).containsMatchIn(value)) {
                    findings += Violation(language, stringName, token, category)
                }
            }
        }
        return findings
    }

    /**
     * Scan a parsed resource file. [language] is derived from the resource folder
     * (e.g. values-ur -> "ur", values -> "en"). The universal set is always applied.
     */
    fun scanStrings(
        language: String,
        strings: List<Pair<String, String>>,
        tokensByLanguage: Map<String, TokenSet>,
        universal: TokenSet,
        allowList: Set<String> = emptySet(),
    ): List<Violation> {
        val langTokens = tokensByLanguage[language]
        val findings = mutableListOf<Violation>()
        for ((name, value) in strings) {
            findings += scanValue(language, name, value, universal, allowList)
            if (langTokens != null) {
                findings += scanValue(language, name, value, langTokens, allowList)
            }
        }
        return findings.distinct()
    }
}
