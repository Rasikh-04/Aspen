package app.aspen.data.logging

import app.aspen.domain.logging.model.BehaviourLog
import app.aspen.domain.logging.model.FeelingTag
import app.aspen.domain.logging.model.FoodLog
import app.aspen.domain.logging.model.Reflection
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Serialization DTOs for the encrypted log store. Domain models stay pure; these mirrors carry the
 * wire shape. **There are no numeric fields** — qualitative note + feeling tags (as strings) + an
 * instant only (SR-1, numberless by schema). Unrecognised tags/instants drop leniently via
 * [toDomainOrNull] so one bad record can't take the rest down.
 */
@Serializable
data class LogStateDto(
    val reflections: List<ReflectionDto> = emptyList(),
    val foodLogs: List<EntryDto> = emptyList(),
    val behaviourLogs: List<EntryDto> = emptyList(),
)

@Serializable
data class ReflectionDto(val id: String, val text: String, val createdAt: String)

@Serializable
data class EntryDto(
    val id: String,
    val note: String,
    val feelings: List<String> = emptyList(),
    val createdAt: String,
)

fun Reflection.toDto() = ReflectionDto(id = id, text = text, createdAt = createdAt.toString())

fun ReflectionDto.toDomainOrNull(): Reflection? = runCatching {
    Reflection(id = id, text = text, createdAt = Instant.parse(createdAt))
}.getOrNull()

fun FoodLog.toDto() = EntryDto(id, note, feelings.map { it.name }, createdAt.toString())
fun BehaviourLog.toDto() = EntryDto(id, note, feelings.map { it.name }, createdAt.toString())

private fun EntryDto.feelingTags(): Set<FeelingTag> =
    feelings.mapNotNull { runCatching { FeelingTag.valueOf(it) }.getOrNull() }.toSet()

fun EntryDto.toFoodLogOrNull(): FoodLog? = runCatching {
    FoodLog(id = id, note = note, feelings = feelingTags(), createdAt = Instant.parse(createdAt))
}.getOrNull()

fun EntryDto.toBehaviourLogOrNull(): BehaviourLog? = runCatching {
    BehaviourLog(id = id, note = note, feelings = feelingTags(), createdAt = Instant.parse(createdAt))
}.getOrNull()
