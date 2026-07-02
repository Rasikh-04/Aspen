package app.aspen.domain.logging

import app.aspen.domain.logging.model.BehaviourLog
import app.aspen.domain.logging.model.FeelingTag
import app.aspen.domain.logging.model.FoodLog
import app.aspen.domain.logging.model.Reflection
import app.aspen.domain.onboarding.AppConfigProvider
import app.aspen.domain.onboarding.model.FoodLoggingMode
import kotlin.time.Clock

/** Result of a write attempt. Food logging may be refused for a contraindicated profile. */
sealed interface LogOutcome {
    /** The entry was stored; [id] is its identifier. */
    data class Saved(val id: String) : LogOutcome

    /**
     * A food-log write was refused because the active support profile suppresses food logging
     * (restriction / avoidance-sensory — docs/01 §5, docs/11 §4). Not an error and not a failure
     * state shown to the user (CLAUDE.md #5): the feature simply does not offer food logging here.
     */
    data object SuppressedFoodLogging : LogOutcome
}

/**
 * The use-case surface the feature layer calls to log (docs/03 FR-3, FR-3b). This is the **single
 * enforcement point** for adaptive food-logging suppression: a food-log write is allowed only when the
 * live [AppConfigProvider] config permits it. Behaviour/feeling logs and reflections are lower-risk
 * and always available (docs/03 FR-3b).
 *
 * Everything is **numberless** by construction — the entities have no numeric fields (SR-1). [Clock]
 * and [newId] are injected for deterministic tests.
 */
class LoggingService(
    private val store: LoggingStore,
    private val appConfig: AppConfigProvider,
    private val newId: () -> String,
    private val clock: Clock = Clock.System,
) {

    /** Private reflection — always available (docs/03 FR-3). */
    fun logReflection(text: String): LogOutcome.Saved {
        val reflection = Reflection(id = newId(), text = text, createdAt = clock.now())
        store.addReflection(reflection)
        return LogOutcome.Saved(reflection.id)
    }

    /** Behaviour/feeling log — lower-risk, broadly available (docs/03 FR-3b). */
    fun logBehaviour(note: String, feelings: Set<FeelingTag>): LogOutcome.Saved {
        val log = BehaviourLog(id = newId(), note = note, feelings = feelings, createdAt = clock.now())
        store.addBehaviourLog(log)
        return LogOutcome.Saved(log.id)
    }

    /**
     * Food/meal log — **gated by the active profile**. Refused (no write) when the config suppresses
     * food logging; otherwise stored (still numberless). The caller renders nothing punitive on a
     * [LogOutcome.SuppressedFoodLogging] — the feature is simply absent for this profile.
     */
    fun logFood(note: String, feelings: Set<FeelingTag>): LogOutcome {
        if (appConfig.current().foodLoggingMode == FoodLoggingMode.OFF) {
            return LogOutcome.SuppressedFoodLogging
        }
        val log = FoodLog(id = newId(), note = note, feelings = feelings, createdAt = clock.now())
        store.addFoodLog(log)
        return LogOutcome.Saved(log.id)
    }

    /** Whether the food-logging feature should be offered at all for the active profile. */
    fun isFoodLoggingOffered(): Boolean = appConfig.current().foodLoggingMode != FoodLoggingMode.OFF

    fun reflections(): List<Reflection> = store.reflections()
    fun foodLogs(): List<FoodLog> = store.foodLogs()
    fun behaviourLogs(): List<BehaviourLog> = store.behaviourLogs()

    fun deleteReflection(id: String) = store.deleteReflection(id)
    fun deleteFoodLog(id: String) = store.deleteFoodLog(id)
    fun deleteBehaviourLog(id: String) = store.deleteBehaviourLog(id)

    /** Permanent wipe of all logs and reflections (FR-11). */
    fun deleteEverything() = store.clearAll()
}
