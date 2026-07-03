package app.aspen.server

import app.aspen.server.ai.AnthropicMessagesProvider
import app.aspen.server.ai.FakeModelProvider
import app.aspen.server.ai.ModelProvider
import app.aspen.server.ai.OpenAiCompatProvider
import app.aspen.server.ai.ProviderConfig
import app.aspen.server.ai.ReflectionSystemPrompt
import io.ktor.client.HttpClient

/**
 * All deployment configuration comes from the environment — no credential, endpoint, or model
 * name exists in this repository (CLAUDE.md security rules). With NO environment set, the server
 * runs fully offline on [FakeModelProvider] and in-memory stores: the testable-without-API mode.
 *
 *   ASPEN_PORT          server port (default 8080)
 *   ASPEN_DATA_DIR      directory for durable file stores (absent → in-memory, ephemeral)
 *   ASPEN_AI_PROVIDER   "anthropic" | "openai" | "fake" (default "fake")
 *   ASPEN_AI_BASE_URL   vendor base URL (e.g. https://api.anthropic.com)
 *   ASPEN_AI_MODEL      vendor model id — ANY model behind either API shape, not just Claude
 *   ASPEN_AI_KEY        vendor API key
 *   ASPEN_AI_MAX_TOKENS reply cap (default 512)
 */
data class ServerConfig(
    val port: Int,
    val dataDir: String?,
    val aiProvider: String,
    val aiBaseUrl: String?,
    val aiModel: String?,
    val aiKey: String?,
    val aiMaxTokens: Int,
) {
    companion object {
        const val PROVIDER_FAKE = "fake"
        const val PROVIDER_ANTHROPIC = "anthropic"
        const val PROVIDER_OPENAI = "openai"

        fun fromEnv(env: (String) -> String? = System::getenv): ServerConfig = ServerConfig(
            port = env("ASPEN_PORT")?.toIntOrNull() ?: 8080,
            dataDir = env("ASPEN_DATA_DIR")?.takeIf { it.isNotBlank() },
            aiProvider = env("ASPEN_AI_PROVIDER")?.lowercase()?.takeIf { it.isNotBlank() } ?: PROVIDER_FAKE,
            aiBaseUrl = env("ASPEN_AI_BASE_URL")?.takeIf { it.isNotBlank() },
            aiModel = env("ASPEN_AI_MODEL")?.takeIf { it.isNotBlank() },
            aiKey = env("ASPEN_AI_KEY")?.takeIf { it.isNotBlank() },
            aiMaxTokens = env("ASPEN_AI_MAX_TOKENS")?.toIntOrNull() ?: ProviderConfig.DEFAULT_MAX_TOKENS,
        )
    }
}

/**
 * Selects the model adapter. Fail-safe: an unknown provider name or ANY missing vendor value
 * (url/model/key) falls back to the fake — a misconfigured deployment degrades to offline calm,
 * it never half-calls a vendor.
 */
fun buildModelProvider(config: ServerConfig, http: HttpClient): ModelProvider {
    val vendor = when {
        config.aiBaseUrl == null || config.aiModel == null || config.aiKey == null -> null
        else -> ProviderConfig(config.aiBaseUrl, config.aiKey, config.aiModel, config.aiMaxTokens)
    }
    return when {
        config.aiProvider == ServerConfig.PROVIDER_ANTHROPIC && vendor != null ->
            AnthropicMessagesProvider(vendor, http, ReflectionSystemPrompt.text)
        config.aiProvider == ServerConfig.PROVIDER_OPENAI && vendor != null ->
            OpenAiCompatProvider(vendor, http, ReflectionSystemPrompt.text)
        else -> FakeModelProvider()
    }
}
