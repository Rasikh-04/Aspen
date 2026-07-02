package app.aspen.domain.ai

import kotlin.time.Instant

/** Who said a stored AI-conversation line (docs/04 §5 `ai_messages`). */
enum class AiRole { USER, COMPANION }

/** One encrypted, hard-deletable AI-conversation entry (docs/04 §5; FR-11 delete applies). */
data class AiMessage(
    val id: String,
    val role: AiRole,
    val text: String,
    val createdAt: Instant,
)

/**
 * Persistence port for the (cloud-tier) AI conversation history. Exists ONLY when the user has
 * enabled cloud reflection; always encrypted at rest and permanently deletable (FR-11).
 */
interface AiMessageStore {
    fun append(message: AiMessage)
    fun history(): List<AiMessage>

    /** Permanent, immediate removal of the entire AI history (FR-11 "delete means delete"). */
    fun clearAll()
}

/** The outcome of one Tier-2 client call (docs/04 ADR-003). */
sealed interface AiClientResult {
    /** The raw model candidate — NEVER user-visible until it passes [app.aspen.domain.safety.SafetyEngine.guardOutput]. */
    data class Reply(val text: String) : AiClientResult

    /** The cloud tier is not wired/enabled (the shipped default until the live-endpoint decision). */
    data object Disabled : AiClientResult

    /** Offline or transient failure — the caller degrades calmly, never with an error state. */
    data object Unavailable : AiClientResult
}

/**
 * The Tier-2 cloud client port (docs/04 ADR-003). Implementations must be on-demand only (never
 * background), send NOTHING unless invoked, and carry no credentials in code — endpoint/auth are
 * injected configuration (the live endpoint is a deferred decision; docs/PRE_SHIP_VERIFICATION.md).
 *
 * Callers never use this directly: [ReflectionCompanion] is the single pipeline, which consent-gates
 * and safety-guards every call (CLAUDE.md #8).
 */
interface AiClient {
    suspend fun reply(userText: String, history: List<AiMessage>): AiClientResult
}
