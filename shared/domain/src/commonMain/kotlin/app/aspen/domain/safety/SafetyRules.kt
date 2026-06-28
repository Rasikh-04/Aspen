package app.aspen.domain.safety

/**
 * A category of forbidden content, tied to the non-negotiables (CLAUDE.md #1/#2/#5/#8, docs/01 §6).
 * EATING_ADVICE is runtime-only (the AI output guard, Phase 4); the build-time copy-lint uses the
 * first three.
 */
enum class TokenCategory { NUMBERS_FOOD_BODY, SHAME, APPEARANCE, EATING_ADVICE }

/**
 * Per-language forbidden token lists plus a script-neutral universal set. This is the RUNTIME mirror
 * of the canonical `config/safety/forbidden_tokens.json`; a JVM parity test asserts the two never
 * drift (decision #3 — buildSrc can't be a runtime dependency, so the single source is enforced by
 * test, not by sharing a class). The authoritative, complete per-language lists are advisor- and
 * translator-supplied trust-and-safety content (docs/12 §3); what ships here is a reviewed starter.
 */
data class ForbiddenLexicon(
    val byLanguage: Map<String, Map<TokenCategory, List<String>>>,
    val universal: Map<TokenCategory, List<String>>,
) {
    /** All tokens for [category] across every language plus the universal set (deduped, lowercased). */
    fun allTokens(category: TokenCategory): List<String> =
        (byLanguage.values.flatMap { it[category].orEmpty() } + universal[category].orEmpty())
            .map { it.lowercase() }
            .distinct()
}

/**
 * The non-negotiables encoded as detection predicates (docs/09 §2.3). These are HEURISTIC backstops
 * for the AI output guard and lints — never the front line (the front line is curation + human
 * review). Whole-word, Unicode-aware, case-insensitive matching so it works across Latin and
 * non-Latin scripts (matches the copy-lint's matching exactly).
 */
class SafetyRules(private val lexicon: ForbiddenLexicon) {

    private fun wholeWordRegex(token: String): Regex =
        Regex("(?<!\\p{L})" + Regex.escape(token.trim()) + "(?!\\p{L})", RegexOption.IGNORE_CASE)

    private fun containsAny(text: String, category: TokenCategory): Boolean =
        lexicon.allTokens(category).any { it.isNotBlank() && wholeWordRegex(it).containsMatchIn(text) }

    /** Detect forbidden numeric-about-food/body content (calories, BMI, weight units…). */
    fun containsForbiddenNumbers(text: String): Boolean = containsAny(text, TokenCategory.NUMBERS_FOOD_BODY)

    /** Detect appearance commentary, in any direction (CLAUDE.md #2). */
    fun containsAppearanceComment(text: String): Boolean = containsAny(text, TokenCategory.APPEARANCE)

    /** Detect eating/diet/exercise advice — the AI is a notebook, not an authority (CLAUDE.md #8). */
    fun containsEatingAdvice(text: String): Boolean = containsAny(text, TokenCategory.EATING_ADVICE)

    /** Detect shame/failure language forbidden in user-facing copy (CLAUDE.md #5). */
    fun containsShameLanguage(text: String): Boolean = containsAny(text, TokenCategory.SHAME)

    /** True if any non-negotiable predicate trips — the single check the output guard calls. */
    fun violatesAny(text: String): Boolean =
        containsForbiddenNumbers(text) ||
            containsAppearanceComment(text) ||
            containsEatingAdvice(text) ||
            containsShameLanguage(text)
}
