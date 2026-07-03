package app.aspen.server.routes

import app.aspen.api.ApiError
import app.aspen.api.AspenApi
import app.aspen.api.AuthResponse
import app.aspen.api.LoginRequest
import app.aspen.api.RecoveryCompleteRequest
import app.aspen.api.RecoveryRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthRoutesTest {

    @Test
    fun `health endpoint answers without auth`() = TestServer().run { client ->
        assertEquals(HttpStatusCode.OK, client.get(AspenApi.HEALTH).status)
    }

    @Test
    fun `register issues a working session token`() = TestServer().run { client ->
        val auth = client.registerAccount()
        val response = client.get(AspenApi.SYNC_BLOB) { bearer(auth.token) }
        // 404 (no blob yet) proves the token authenticated; an invalid token would be 401.
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `login with wrong password is an undifferentiated 401`() = TestServer().run { client ->
        val auth = client.registerAccount()
        val response = client.post(AspenApi.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(LoginRequest.serializer(), LoginRequest(auth.accountId, "wrong-pass")))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(ApiError.CODE_UNAUTHORIZED, response.body(ApiError.serializer()).code)
    }

    @Test
    fun `weak password is refused with a machine code`() = TestServer().run { client ->
        val response = client.post(AspenApi.REGISTER) {
            contentType(ContentType.Application.Json)
            setBody("""{"password":"short"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ApiError.CODE_WEAK_PASSWORD, response.body(ApiError.serializer()).code)
    }

    @Test
    fun `logout revokes the session immediately`() = TestServer().run { client ->
        val auth = client.registerAccount()
        client.post(AspenApi.LOGOUT) { bearer(auth.token) }
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get(AspenApi.SYNC_BLOB) { bearer(auth.token) }.status,
        )
    }

    @Test
    fun `delete account purges the blob and the session`() = TestServer().run { client ->
        val auth = client.registerAccount()
        client.put(AspenApi.SYNC_BLOB) { bearer(auth.token); setBody(byteArrayOf(9, 9)) }

        assertEquals(HttpStatusCode.OK, client.delete(AspenApi.ACCOUNT) { bearer(auth.token) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get(AspenApi.SYNC_BLOB) { bearer(auth.token) }.status)
    }

    @Test
    fun `recovery request is always 202 - account existence never leaks`() = TestServer().run { client ->
        val response = client.post(AspenApi.RECOVERY_REQUEST) {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(RecoveryRequest.serializer(), RecoveryRequest("nobody@nowhere.x")))
        }
        assertEquals(HttpStatusCode.Accepted, response.status)
    }

    @Test
    fun `email recovery round trip over the wire`() {
        val server = TestServer()
        server.run { client ->
            client.registerAccount(email = "me@aspen.app")
            client.post(AspenApi.RECOVERY_REQUEST) {
                contentType(ContentType.Application.Json)
                setBody(testJson.encodeToString(RecoveryRequest.serializer(), RecoveryRequest("me@aspen.app")))
            }
            val token = server.sentMail.single().second

            val response = client.post(AspenApi.RECOVERY_COMPLETE) {
                contentType(ContentType.Application.Json)
                setBody(
                    testJson.encodeToString(
                        RecoveryCompleteRequest.serializer(),
                        RecoveryCompleteRequest(token, "brand-new-pass"),
                    ),
                )
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val auth = response.body(AuthResponse.serializer())
            // The fresh session works.
            assertEquals(HttpStatusCode.NotFound, client.get(AspenApi.SYNC_BLOB) { bearer(auth.token) }.status)
        }
    }

    @Test
    fun `login brute force is rate limited per identifier`() {
        val server = TestServer(
            credentialLimiter = app.aspen.server.RateLimiter(maxPerWindow = 2, windowMs = 60_000),
        )
        server.run { client ->
            suspend fun attempt(): HttpStatusCode = client.post(AspenApi.LOGIN) {
                contentType(ContentType.Application.Json)
                setBody(testJson.encodeToString(LoginRequest.serializer(), LoginRequest("target-id", "guess")))
            }.status

            assertEquals(HttpStatusCode.Unauthorized, attempt())
            assertEquals(HttpStatusCode.Unauthorized, attempt())
            assertEquals(HttpStatusCode.TooManyRequests, attempt())
        }
    }
}
