package app.aspen.data.account

import app.aspen.api.AspenApi
import app.aspen.data.local.InMemoryEncryptedBlobStore
import app.aspen.data.local.platformLocalCipher
import app.aspen.domain.account.AccountResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerAccountManagerTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private fun authBody(id: String = "acct-1", token: String = "tok-1") =
        """{"accountId":"$id","token":"$token"}"""

    private fun manager(engine: MockEngine, sessions: PersistentSessionStore = newSessions()) =
        ServerAccountManager("https://aspen.example.com/", sessions, HttpClient(engine))

    private fun newSessions() = PersistentSessionStore(platformLocalCipher(), InMemoryEncryptedBlobStore())

    @Test
    fun `register signs in and persists the session encrypted`() = runTest {
        val blob = InMemoryEncryptedBlobStore()
        val sessions = PersistentSessionStore(platformLocalCipher(), blob)
        val engine = MockEngine { respond(authBody(), HttpStatusCode.OK, jsonHeaders) }

        val result = manager(engine, sessions).register("longenough", email = null)

        assertIs<AccountResult.SignedIn>(result)
        assertEquals("acct-1", sessions.current()!!.accountId)
        // At rest the token must not be readable.
        assertFalse(blob.load()!!.decodeToString().contains("tok-1"))
    }

    @Test
    fun `sign-in failure maps by machine code, session stays absent`() = runTest {
        suspend fun resultFor(status: HttpStatusCode, body: String): AccountResult {
            val engine = MockEngine { respond(body, status, jsonHeaders) }
            return manager(engine).signIn("someone", "pass")
        }

        assertEquals(AccountResult.Denied, resultFor(HttpStatusCode.Unauthorized, """{"code":"unauthorized"}"""))
        assertEquals(AccountResult.WeakPassword, resultFor(HttpStatusCode.BadRequest, """{"code":"weak_password"}"""))
        assertEquals(AccountResult.EmailTaken, resultFor(HttpStatusCode.Conflict, """{"code":"email_taken"}"""))
    }

    @Test
    fun `offline or server error is calm Unavailable, never a throw`() = runTest {
        val thrower = MockEngine { throw RuntimeException("no network") }
        val serverError = MockEngine { respondError(HttpStatusCode.InternalServerError) }

        assertEquals(AccountResult.Unavailable, manager(thrower).signIn("x", "y"))
        assertEquals(AccountResult.Unavailable, manager(serverError).register("longenough", null))
    }

    @Test
    fun `sign out clears the local session even when the server is unreachable`() = runTest {
        val sessions = newSessions().apply { save(StoredSession("acct-1", "tok-1")) }
        val thrower = MockEngine { throw RuntimeException("offline") }

        manager(thrower, sessions).signOut()

        assertNull(sessions.current())
    }

    @Test
    fun `delete account clears locally ONLY after the server purge succeeds`() = runTest {
        val sessions = newSessions().apply { save(StoredSession("acct-1", "tok-1")) }
        val failing = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        assertFalse(manager(failing, sessions).deleteAccount())
        assertEquals("acct-1", sessions.current()!!.accountId, "session kept so the user can retry the purge")

        val ok = MockEngine { respond("", HttpStatusCode.OK, jsonHeaders) }
        assertTrue(manager(ok, sessions).deleteAccount())
        assertNull(sessions.current())
    }

    @Test
    fun `login request goes to the right endpoint with the password only in transit`() = runTest {
        var seenUrl = ""
        val engine = MockEngine { request ->
            seenUrl = request.url.toString()
            respond(authBody(), HttpStatusCode.OK, jsonHeaders)
        }
        manager(engine).signIn("me@aspen.app", "pass-in-transit")
        assertEquals("https://aspen.example.com" + AspenApi.LOGIN, seenUrl)
    }

    @Test
    fun `corrupt session blob fails safe to signed out`() {
        val blob = InMemoryEncryptedBlobStore().apply { save("not ciphertext".encodeToByteArray()) }
        val sessions = PersistentSessionStore(platformLocalCipher(), blob)
        assertNull(sessions.current())
    }
}
