package app.aspen.data.consent

import app.aspen.domain.consent.model.ConsentEvent
import app.aspen.domain.consent.model.ConsentEventType
import app.aspen.domain.consent.model.ConsentGrant
import app.aspen.domain.consent.model.DataCategory
import app.aspen.domain.consent.model.Recipient
import app.aspen.domain.consent.model.RecipientType
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Serialization DTOs for consent persistence. The domain models stay pure (no `@Serializable`); these
 * mirrors carry the wire shape and map back and forth. Enums and instants are stored as strings so an
 * unrecognised value fails to map (→ skipped) rather than silently coercing — see [toDomainOrNull].
 */
@Serializable
data class ConsentStateDto(
    val grants: List<ConsentGrantDto> = emptyList(),
    val events: List<ConsentEventDto> = emptyList(),
)

@Serializable
data class ConsentGrantDto(
    val id: String,
    val recipientId: String,
    val recipientType: String,
    val recipientDisplayName: String,
    val categories: List<String>,
    val grantedAt: String,
    val expiresAt: String?,
    val revokedAt: String?,
    val purpose: String,
)

@Serializable
data class ConsentEventDto(
    val type: String,
    val at: String,
    val grantId: String?,
    val recipientId: String?,
    val category: String?,
    val allowed: Boolean?,
)

fun ConsentGrant.toDto(): ConsentGrantDto = ConsentGrantDto(
    id = id,
    recipientId = recipient.id,
    recipientType = recipient.type.name,
    recipientDisplayName = recipient.displayName,
    categories = categories.map { it.name },
    grantedAt = grantedAt.toString(),
    expiresAt = expiresAt?.toString(),
    revokedAt = revokedAt?.toString(),
    purpose = purpose,
)

/** Map back to the domain grant, or null if any enum/instant is unrecognised (corrupt → skip). */
fun ConsentGrantDto.toDomainOrNull(): ConsentGrant? = runCatching {
    ConsentGrant(
        id = id,
        recipient = Recipient(
            id = recipientId,
            type = RecipientType.valueOf(recipientType),
            displayName = recipientDisplayName,
        ),
        categories = categories.map { DataCategory.valueOf(it) }.toSet(),
        grantedAt = Instant.parse(grantedAt),
        expiresAt = expiresAt?.let { Instant.parse(it) },
        revokedAt = revokedAt?.let { Instant.parse(it) },
        purpose = purpose,
    )
}.getOrNull()

fun ConsentEvent.toDto(): ConsentEventDto = ConsentEventDto(
    type = type.name,
    at = at.toString(),
    grantId = grantId,
    recipientId = recipientId,
    category = category?.name,
    allowed = allowed,
)

/** Map back to the domain event, or null if any enum/instant is unrecognised (corrupt → skip). */
fun ConsentEventDto.toDomainOrNull(): ConsentEvent? = runCatching {
    ConsentEvent(
        type = ConsentEventType.valueOf(type),
        at = Instant.parse(at),
        grantId = grantId,
        recipientId = recipientId,
        category = category?.let { DataCategory.valueOf(it) },
        allowed = allowed,
    )
}.getOrNull()
