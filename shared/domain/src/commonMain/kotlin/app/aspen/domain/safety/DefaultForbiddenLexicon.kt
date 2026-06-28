package app.aspen.domain.safety

/**
 * The runtime mirror of the canonical `config/safety/forbidden_tokens.json` (decision #3). Kept in
 * sync by `ForbiddenLexiconParityTest` (JVM), which fails the build if this drifts from the JSON.
 * Hand-edit BOTH this and the JSON together; the parity test is the guard, not a suggestion.
 *
 * Starter set (en/de/ur); authoritative per-language lists are advisor/translator-supplied content
 * (docs/12 §3). `EATING_ADVICE` is the runtime-only category the copy-lint does not use.
 */
object DefaultForbiddenLexicon {

    val lexicon: ForbiddenLexicon = ForbiddenLexicon(
        byLanguage = mapOf(
            "en" to mapOf(
                TokenCategory.NUMBERS_FOOD_BODY to listOf(
                    "calorie", "calories", "kcal", "bmi", "macro", "macros",
                    "kg", "kgs", "lbs", "pound", "pounds", "weight", "portion", "portions", "goal weight",
                ),
                TokenCategory.SHAME to listOf("fail", "failed", "fails", "failing", "missed", "incomplete", "you didn't"),
                TokenCategory.APPEARANCE to listOf(
                    "you look", "thin", "fat", "skinny", "chubby", "overweight", "underweight", "healthy weight",
                ),
                TokenCategory.EATING_ADVICE to listOf(
                    "you should eat", "eat more", "eat less", "skip the meal", "skip a meal", "restrict", "go on a diet",
                ),
            ),
            "de" to mapOf(
                TokenCategory.NUMBERS_FOOD_BODY to listOf("kalorie", "kalorien", "bmi", "gewicht", "kilo", "abnehmen", "zunehmen"),
                TokenCategory.SHAME to listOf("versagt", "gescheitert", "verpasst"),
                TokenCategory.APPEARANCE to listOf("dünn", "dick", "übergewicht", "untergewicht"),
                TokenCategory.EATING_ADVICE to emptyList(),
            ),
            "ur" to mapOf(
                TokenCategory.NUMBERS_FOOD_BODY to listOf("وزن", "کیلو", "کیلوری"),
                TokenCategory.SHAME to emptyList(),
                TokenCategory.APPEARANCE to emptyList(),
                TokenCategory.EATING_ADVICE to emptyList(),
            ),
        ),
        universal = mapOf(
            TokenCategory.NUMBERS_FOOD_BODY to listOf("kcal", "bmi"),
            TokenCategory.SHAME to emptyList(),
            TokenCategory.APPEARANCE to emptyList(),
            TokenCategory.EATING_ADVICE to emptyList(),
        ),
    )
}
