package app.aspen.domain.logging

import app.aspen.domain.logging.model.BehaviourLog
import app.aspen.domain.logging.model.FoodLog
import app.aspen.domain.logging.model.Reflection

/**
 * Persistence port for on-device, encrypted, numberless logs (docs/04 §5). The domain depends only on
 * this interface; the encrypted implementation lives in `:shared:data`.
 *
 * **Fail-safe:** reads never throw — an unreadable store yields empty lists, never an exception.
 * **Delete means delete** (FR-11): the per-id deletes and [clearAll] hard-remove data.
 *
 * This is raw storage with no policy. Food-logging *suppression* is enforced one layer up in
 * [LoggingService] so the safety rule lives in exactly one place and can't be bypassed by a feature
 * calling the store directly — features depend on [LoggingService], not this port.
 */
interface LoggingStore {
    fun addReflection(reflection: Reflection)
    fun reflections(): List<Reflection>
    fun deleteReflection(id: String)

    fun addFoodLog(log: FoodLog)
    fun foodLogs(): List<FoodLog>
    fun deleteFoodLog(id: String)

    fun addBehaviourLog(log: BehaviourLog)
    fun behaviourLogs(): List<BehaviourLog>
    fun deleteBehaviourLog(id: String)

    /** Permanently remove ALL logs and reflections (account delete / wipe — FR-11). */
    fun clearAll()
}
