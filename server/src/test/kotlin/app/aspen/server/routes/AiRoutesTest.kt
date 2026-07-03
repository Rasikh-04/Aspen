package app.aspen.server.routes

import app.aspen.api.ApiChatMessage
import app.aspen.api.AspenApi
import app.aspen.api.ReflectRequest
import app.aspen.api.ReflectResponse
import app.aspen.server.RateLimiter
import app.aspen.server.ai.ChatMessage
import app.aspen.server.ai.FakeModelProvider
import app.aspen.server.ai.ModelProvider
import app.aspen.server.ai.ProviderResult
import app.aspen.server.aspenServer
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiRoutesTest {

    private suspend fun HttpClient.reflect(token: String?, request: ReflectRequest) =
        post(AspenApi.AI_REFLECT) {
            token?.let { bearer(it) }
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(ReflectRequest.serializer(), request))
        }

    @Test
    fun `relay requires auth`() = TestServer().run { client ->
        assertEquals(HttpStatusCode.Unauthorized, client.reflect(null, ReflectRequest("hello")).status)
    }

    @Test
    fun `fake provider answers deterministically - the whole stack runs with no live API`() =
        TestServer().run { client ->
            val auth = client.registerAccount()
            val response = client.reflect(auth.token, ReflectRequest("today was hard"))
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.body(ReflectResponse.serializer())
            assertEquals(ReflectResponse.STATUS_REPLY, body.status)
            assertEquals(FakeModelProvider.FIXED_REPLY, body.text)
        }

    @Test
    fun `history roles are mapped and passed through to the provider`() {
        var seen: List<ChatMessage> = emptyList()
        val spy = object : ModelProvider {
            override suspend fun complete(userText: String, history: List<ChatMessage>): ProviderResult {
                seen = history
                return ProviderResult.Reply("ok")
            }
        }
        TestServer(provider = spy).run { client ->
            val auth = client.registerAccount()
            client.reflect(
                auth.token,
                ReflectRequest(
                    "now",
                    history = listOf(
                        ApiChatMessage(ApiChatMessage.ROLE_USER, "before"),
                        ApiChatMessage(ApiChatMessage.ROLE_COMPANION, "reflected"),
                    ),
                ),
            )
            assertEquals(2, seen.size)
            assertEquals(app.aspen.server.ai.ChatRole.USER, seen[0].role)
            assertEquals(app.aspen.server.ai.ChatRole.ASSISTANT, seen[1].role)
        }
    }

    @Test
    fun `provider unavailability relays as the calm token, never an error body`() {
        val down = object : ModelProvider {
            override suspend fun complete(userText: String, history: List<ChatMessage>) =
                ProviderResult.Unavailable
        }
        TestServer(provider = down).run { client ->
            val auth = client.registerAccount()
            val response = client.reflect(auth.token, ReflectRequest("hello"))
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ReflectResponse.STATUS_UNAVAILABLE, response.body(ReflectResponse.serializer()).status)
        }
    }

    @Test
    fun `blank and oversized inputs are rejected at the boundary`() = TestServer().run { client ->
        val auth = client.registerAccount()
        assertEquals(HttpStatusCode.BadRequest, client.reflect(auth.token, ReflectRequest("   ")).status)
        assertEquals(
            HttpStatusCode.BadRequest,
            client.reflect(auth.token, ReflectRequest("x".repeat(8_001))).status,
        )
    }

    @Test
    fun `relay is rate limited per account`() {
        val server = TestServer(aiLimiter = RateLimiter(maxPerWindow = 2, windowMs = 60_000))
        server.run { client ->
            val auth = client.registerAccount()
            assertEquals(HttpStatusCode.OK, client.reflect(auth.token, ReflectRequest("one")).status)
            assertEquals(HttpStatusCode.OK, client.reflect(auth.token, ReflectRequest("two")).status)
            assertEquals(HttpStatusCode.TooManyRequests, client.reflect(auth.token, ReflectRequest("three")).status)
        }
    }

    @Test
    fun `relay is stateless - nothing a user writes ever lands in the data directory`() {
        val dataDir = java.nio.file.Files.createTempDirectory("aspen-stateless-proof")
        val config = app.aspen.server.ServerConfig.fromEnv { key ->
            if (key == "ASPEN_DATA_DIR") dataDir.toString() else null
        }
        val deps = app.aspen.server.buildDeps(config)
        val marker = "a-very-distinctive-sentence-that-must-never-be-stored"

        io.ktor.server.testing.testApplication {
            application { aspenServer(deps) }
            val auth = client.registerAccount()
            val response = client.reflect(auth.token, ReflectRequest(marker))
            assertEquals(HttpStatusCode.OK, response.status)
        }

        val filesContainingMarker = java.nio.file.Files.walk(dataDir)
            .filter { java.nio.file.Files.isRegularFile(it) }
            .filter { String(java.nio.file.Files.readAllBytes(it)).contains(marker) }
            .count()
        assertTrue(filesContainingMarker == 0L, "user text must never be persisted server-side")
    }
}
