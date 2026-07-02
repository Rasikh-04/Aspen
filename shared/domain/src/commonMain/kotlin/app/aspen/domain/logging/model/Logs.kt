package app.aspen.domain.logging.model

import kotlin.time.Instant

/**
 * Qualitative feeling tags for logs (docs/03 FR-3b, docs/04 §5 `felt_how`). These are **emotions, not
 * measurements** — there is deliberately no intensity scale, count, or rating, because any number tied
 * to a log is forbidden (CLAUDE.md #1, SR-1). The UI maps each tag to a localized label; the enum
 * itself carries no user-facing copy (CLAUDE.md #11).
 */
enum class FeelingTag {
    CALM,
    CONTENT,
    RELIEVED,
    HOPEFUL,
    TIRED,
    NUMB,
    ANXIOUS,
    OVERWHELMED,
    SAD,
    FRUSTRATED,
    GUILTY,
    ALONE,
}

/**
 * A private, unstructured reflection (docs/03 FR-3 `reflections`). Encrypted at rest; one-tap
 * permanent delete (FR-11). Text only — no structure to quantify.
 */
data class Reflection(
    val id: String,
    val text: String,
    val createdAt: Instant,
)

/**
 * A numberless food/meal log (docs/03 FR-3b, docs/04 §5 `food_logs`). **Qualitative text + feeling
 * tags ONLY** — there are no calorie/portion/weight/macro fields, by design, so numbers literally
 * cannot be logged even by a future careless change (SR-1, numberless by schema). Offered only when
 * the support profile permits it; see [app.aspen.domain.logging.LoggingService].
 */
data class FoodLog(
    val id: String,
    val note: String,
    val feelings: Set<FeelingTag>,
    val createdAt: Instant,
)

/**
 * A behaviour/feeling log (docs/03 FR-3b, docs/04 §5 `behaviour_logs`). Lower-risk than food logging
 * and broadly available across presentations. Numberless: qualitative note + feeling tags only.
 */
data class BehaviourLog(
    val id: String,
    val note: String,
    val feelings: Set<FeelingTag>,
    val createdAt: Instant,
)
