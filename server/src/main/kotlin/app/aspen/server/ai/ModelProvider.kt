package app.aspen.server.ai

/**
 * The server-side model port. The app never sees this — it speaks Aspen's own wire shape
 * (`:shared:server-api`); which vendor answers is server configuration only. Two adapter
 * shapes cover effectively every hosted or self-hosted model today:
 *
 *  - [AnthropicMessagesProvider] — the Anthropic Messages API.
 *  - [OpenAiCompatProvider] — the OpenAI-compatible Chat Completions shape (OpenAI, Gemini,
 *    Mistral, Groq, Together, OpenRouter, Ollama/vLLM, ...).
 *
 * Every implementation is total: transport or API failure maps to [ProviderResult.Unavailable],
 * never an exception upward. The raw reply is relayed to the DEVICE, whose SafetyEngine guard
 * decides what the user sees (CLAUDE.md #8) — the server does not judge content, and it stores
 * none (no content repository exists; see `store/Repositories.kt`).
 */
interface ModelProvider {
    suspend fun complete(userText: String, history: List<ChatMessage>): ProviderResult
}

enum class ChatRole { USER, ASSISTANT }

data class ChatMessage(val role: ChatRole, val text: String)

sealed interface ProviderResult {
    data class Reply(val text: String) : ProviderResult

    /** Offline, misconfigured, refused, or errored — one calm degradation token (FR-5). */
    data object Unavailable : ProviderResult
}

/**
 * The zero-config default: deterministic, offline, no key, no network — keeps the whole stack
 * testable and runnable with no live API (dev only; a deployment picks a real provider by env).
 */
class FakeModelProvider : ModelProvider {
    override suspend fun complete(userText: String, history: List<ChatMessage>): ProviderResult =
        ProviderResult.Reply(FIXED_REPLY)

    companion object {
        /** Dev-only text; still written within the copy rules (no numbers/shame/appearance). */
        const val FIXED_REPLY =
            "Thank you for writing that down. Taking a moment for yourself counts, and this space is yours."
    }
}
