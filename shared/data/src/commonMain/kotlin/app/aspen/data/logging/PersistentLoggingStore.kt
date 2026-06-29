package app.aspen.data.logging

import app.aspen.data.local.EncryptedBlobStore
import app.aspen.data.local.LocalCipher
import app.aspen.domain.logging.LoggingStore
import app.aspen.domain.logging.model.BehaviourLog
import app.aspen.domain.logging.model.FoodLog
import app.aspen.domain.logging.model.Reflection
import kotlinx.serialization.json.Json

/**
 * The encrypted, fail-safe [LoggingStore] (docs/04 §5). All logs serialize to one JSON state blob,
 * encrypted via [LocalCipher].
 *
 * **Fail-safe:** an unreadable/corrupt blob reads as empty lists — never an exception. **Delete means
 * delete** (FR-11): per-id deletes rewrite the blob without the entry; [clearAll] drops the blob
 * entirely. Suppression policy is NOT here — it lives in
 * [app.aspen.domain.logging.LoggingService], the single enforcement point.
 */
class PersistentLoggingStore(
    private val cipher: LocalCipher,
    private val blob: EncryptedBlobStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : LoggingStore {

    override fun addReflection(reflection: Reflection) = mutate {
        it.copy(reflections = it.reflections + reflection.toDto())
    }

    override fun reflections(): List<Reflection> = load().reflections.mapNotNull { it.toDomainOrNull() }

    override fun deleteReflection(id: String) = mutate {
        it.copy(reflections = it.reflections.filterNot { dto -> dto.id == id })
    }

    override fun addFoodLog(log: FoodLog) = mutate { it.copy(foodLogs = it.foodLogs + log.toDto()) }

    override fun foodLogs(): List<FoodLog> = load().foodLogs.mapNotNull { it.toFoodLogOrNull() }

    override fun deleteFoodLog(id: String) = mutate {
        it.copy(foodLogs = it.foodLogs.filterNot { dto -> dto.id == id })
    }

    override fun addBehaviourLog(log: BehaviourLog) = mutate {
        it.copy(behaviourLogs = it.behaviourLogs + log.toDto())
    }

    override fun behaviourLogs(): List<BehaviourLog> =
        load().behaviourLogs.mapNotNull { it.toBehaviourLogOrNull() }

    override fun deleteBehaviourLog(id: String) = mutate {
        it.copy(behaviourLogs = it.behaviourLogs.filterNot { dto -> dto.id == id })
    }

    override fun clearAll() = blob.clear()

    private fun load(): LogStateDto = try {
        val bytes = blob.load() ?: return LogStateDto()
        json.decodeFromString(LogStateDto.serializer(), cipher.decrypt(bytes).decodeToString())
    } catch (t: Throwable) {
        LogStateDto() // Fail SAFE: unreadable → empty (see class doc).
    }

    private inline fun mutate(transform: (LogStateDto) -> LogStateDto) {
        val next = transform(load())
        blob.save(cipher.encrypt(json.encodeToString(LogStateDto.serializer(), next).encodeToByteArray()))
    }
}
