package app.aspen.domain.ai

import app.aspen.domain.consent.ConsentManager
import app.aspen.domain.consent.model.DataCategory
import app.aspen.domain.safety.CrisisSignals
import app.aspen.domain.safety.OutputContext
import app.aspen.domain.safety.SafetyEngine
import app.aspen.domain.safety.SafetyVerdict
import kotlin.time.Clock

/** What the reflection surface renders after one exchange. Copy for each case lives in the UI. */
sealed interface ReflectionOutcome {
    /** No active cloud-AI consent — the client was never touched (default-deny, CLAUDE.md #10). */
    data object Disabled : ReflectionOutcome

    /**
     * The INPUT suggested crisis (docs/06 §5): nothing left the device, nothing was stored, no AI
     * replied. The UI shows a validating line + the warm route to Flow C and a human (CLAUDE.md #8) —
     * this is a hand-off, never a label or an assessment (CLAUDE.md #9).
     */
    data object CrisisHandOff : ReflectionOutcome

    /** A guarded reply. [wasGuarded] = the raw candidate was withheld and replaced (SR-3). */
    data class Reply(val text: String, val wasGuarded: Boolean) : ReflectionOutcome

    /** Offline/error — calm degradation; the notebook itself keeps working (docs/03 FR-5). */
    data object Unavailable : ReflectionOutcome
}

/**
 * The ONE pipeline through which any user text may reach the cloud tier and any AI text may reach
 * the user (docs/04 ADR-003, docs/03 SR-3). Order is load-bearing:
 *
 *   consent (default-deny, revocable, audited)
 *     → crisis-sign check on the INPUT (warm hand-off BEFORE anything leaves the device)
 *       → client call (on-demand only)
 *         → [SafetyEngine.guardOutput] on the OUTPUT (withhold + replace; never echo unsafe text)
 *           → persist the exchange (encrypted; only what was actually shown).
 *
 * Features depend on this class, never on [AiClient] — so the gates cannot be bypassed
 * (same pattern as LoggingService for food-logging suppression).
 */
class ReflectionCompanion(
    private val consent: ConsentManager,
    private val client: AiClient,
    private val safetyEngine: SafetyEngine,
    private val crisisSignals: CrisisSignals,
    private val store: AiMessageStore,
    private val newId: () -> String,
    private val clock: Clock = Clock.System,
) {

    /** Whether the cloud surface should be offered at all right now (drives UI visibility). */
    fun isEnabled(): Boolean = consent.canAccess(AI_RECIPIENT_ID, DataCategory.AI_MESSAGES)

    suspend fun reflect(userText: String, ctx: OutputContext = OutputContext()): ReflectionOutcome {
        if (userText.isBlank()) return ReflectionOutcome.Unavailable
        if (!isEnabled()) return ReflectionOutcome.Disabled
        if (crisisSignals.suggestsCrisis(userText)) return ReflectionOutcome.CrisisHandOff

        val result = runCatching { client.reply(userText, store.history()) }
            .getOrElse { return ReflectionOutcome.Unavailable }

        return when (result) {
            is AiClientResult.Reply -> when (val verdict = safetyEngine.guardOutput(result.text, ctx)) {
                is SafetyVerdict.Pass -> {
                    persistExchange(userText, verdict.text)
                    ReflectionOutcome.Reply(verdict.text, wasGuarded = false)
                }
                is SafetyVerdict.Rewrite -> {
                    // The raw candidate is withheld and NEVER persisted — only the safe text is.
                    persistExchange(userText, verdict.safeText)
                    ReflectionOutcome.Reply(verdict.safeText, wasGuarded = true)
                }
            }
            AiClientResult.Disabled -> ReflectionOutcome.Disabled
            AiClientResult.Unavailable -> ReflectionOutcome.Unavailable
        }
    }

    fun history(): List<AiMessage> = if (isEnabled()) store.history() else emptyList()

    /** FR-11: permanent, immediate deletion of the whole AI conversation. */
    fun deleteEverything() = store.clearAll()

    private fun persistExchange(userText: String, shownReply: String) {
        val now = clock.now()
        store.append(AiMessage(id = newId(), role = AiRole.USER, text = userText, createdAt = now))
        store.append(AiMessage(id = newId(), role = AiRole.COMPANION, text = shownReply, createdAt = now))
    }

    companion object {
        /** The consent recipient id for the cloud AI service (docs/08 §3 — one recipient, one grant). */
        const val AI_RECIPIENT_ID = "ai-cloud"
    }
}
