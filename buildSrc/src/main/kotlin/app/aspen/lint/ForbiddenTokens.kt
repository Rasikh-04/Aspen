package app.aspen.lint

/** A category of banned copy, tied to the non-negotiables (CLAUDE.md #1/#2/#5, docs/01 §6). */
enum class TokenCategory { NUMBERS_FOOD_BODY, SHAME, APPEARANCE }

/**
 * Forbidden tokens for one language. Per docs/09 §2.3 these are detection heuristics for the
 * build-time copy lint, and per docs/12 §3 the lists are PER LANGUAGE — a numberless app must be
 * numberless in every language. The lists here are a STARTER set (en/de/ur); the authoritative,
 * complete per-language lists are advisor/translator-supplied trust-and-safety content.
 */
data class TokenSet(
    val language: String,
    val numbersFoodBody: List<String>,
    val shame: List<String>,
    val appearance: List<String>,
) {
    fun byCategory(category: TokenCategory): List<String> = when (category) {
        TokenCategory.NUMBERS_FOOD_BODY -> numbersFoodBody
        TokenCategory.SHAME -> shame
        TokenCategory.APPEARANCE -> appearance
    }
}

object ForbiddenTokens {

    /** Starter token sets. Language code "*" applies to every language (script-neutral loanwords). */
    fun defaults(): Map<String, TokenSet> = mapOf(
        "en" to TokenSet(
            language = "en",
            numbersFoodBody = listOf(
                "calorie", "calories", "kcal", "bmi", "macro", "macros",
                "kg", "kgs", "lbs", "pound", "pounds", "weight", "portion", "portions",
                "goal weight",
            ),
            shame = listOf("fail", "failed", "fails", "failing", "missed", "incomplete", "you didn't"),
            appearance = listOf(
                "you look", "thin", "fat", "skinny", "chubby", "overweight", "underweight", "healthy weight",
            ),
        ),
        "de" to TokenSet(
            language = "de",
            numbersFoodBody = listOf("kalorie", "kalorien", "bmi", "gewicht", "kilo", "abnehmen", "zunehmen"),
            shame = listOf("versagt", "gescheitert", "verpasst"),
            appearance = listOf("dünn", "dick", "übergewicht", "untergewicht"),
        ),
        "ur" to TokenSet(
            // Starter only — authoritative Urdu list requires native, ED-informed review (docs/12 §3).
            language = "ur",
            numbersFoodBody = listOf("وزن", "کیلو", "کیلوری"),
            shame = emptyList(),
            appearance = emptyList(),
        ),
    )

    /** Tokens that are banned in EVERY language (NEDA deny is crisis-data and lands in Phase 2). */
    fun universal(): TokenSet = TokenSet(
        language = "*",
        numbersFoodBody = listOf("kcal", "bmi"),
        shame = emptyList(),
        appearance = emptyList(),
    )
}
