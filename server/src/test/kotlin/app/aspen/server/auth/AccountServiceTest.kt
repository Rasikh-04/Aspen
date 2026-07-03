package app.aspen.server.auth

import app.aspen.server.store.InMemoryAccountRepository
import app.aspen.server.store.InMemoryBlobRepository
import app.aspen.server.store.InMemoryRecoveryTokenRepository
import app.aspen.server.store.InMemorySessionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountServiceTest {

    private class Fixture(var nowMs: Long = 0L) {
        val accounts = InMemoryAccountRepository()
        val sessions = InMemorySessionRepository()
        val recovery = InMemoryRecoveryTokenRepository()
        val blobs = InMemoryBlobRepository()
        val sentMail = mutableListOf<Pair<String, String>>()
        var idCounter = 0
        val service = AccountService(
            accounts = accounts,
            sessions = sessions,
            recoveryTokens = recovery,
            blobs = blobs,
            mailer = { email, token -> sentMail.add(email to token) },
            now = { nowMs },
            newId = { "acct-${idCounter++}" },
            hashIterations = 1_000,
        )
    }

    @Test
    fun `register then login by account id works with no email at all`() {
        val f = Fixture()
        val registered = assertIs<RegisterOutcome.Registered>(f.service.register("longenough", email = null))
        val loggedIn = assertIs<LoginOutcome.LoggedIn>(f.service.login(registered.accountId, "longenough"))
        assertEquals(registered.accountId, loggedIn.accountId)
    }

    @Test
    fun `login by email works and is case-insensitive`() {
        val f = Fixture()
        f.service.register("longenough", email = "Someone@Example.com")
        assertIs<LoginOutcome.LoggedIn>(f.service.login("someone@example.COM", "longenough"))
    }

    @Test
    fun `wrong password and unknown identifier are one undifferentiated denial`() {
        val f = Fixture()
        val r = assertIs<RegisterOutcome.Registered>(f.service.register("longenough", "a@b.c"))
        assertEquals(LoginOutcome.Denied, f.service.login(r.accountId, "wrong-password"))
        assertEquals(LoginOutcome.Denied, f.service.login("nobody@nowhere.x", "longenough"))
    }

    @Test
    fun `short password is refused`() {
        val f = Fixture()
        assertEquals(RegisterOutcome.WeakPassword, f.service.register("short", null))
    }

    @Test
    fun `duplicate email is refused`() {
        val f = Fixture()
        f.service.register("longenough", "same@aspen.app")
        assertEquals(RegisterOutcome.EmailTaken, f.service.register("different-pass", "SAME@aspen.app"))
    }

    @Test
    fun `stored hash is never the plaintext password`() {
        val f = Fixture()
        val r = assertIs<RegisterOutcome.Registered>(f.service.register("longenough", null))
        val record = f.accounts.byId(r.accountId)!!
        assertNotEquals("longenough", record.passwordHash)
        assertTrue(!record.passwordHash.contains("longenough"))
    }

    @Test
    fun `session token resolves the account and logout revokes it immediately`() {
        val f = Fixture()
        val r = assertIs<RegisterOutcome.Registered>(f.service.register("longenough", null))
        assertEquals(r.accountId, f.service.accountFor(r.token))
        f.service.logout(r.token)
        assertNull(f.service.accountFor(r.token))
    }

    @Test
    fun `delete account purges sessions, blob, recovery tokens and the record`() {
        val f = Fixture()
        val r = assertIs<RegisterOutcome.Registered>(f.service.register("longenough", "x@y.z"))
        f.blobs.put(r.accountId, byteArrayOf(1, 2, 3))
        f.service.requestRecovery("x@y.z")

        f.service.deleteAccount(r.accountId)

        assertNull(f.service.accountFor(r.token))
        assertNull(f.blobs.get(r.accountId))
        assertNull(f.accounts.byId(r.accountId))
        val staleToken = f.sentMail.single().second
        assertEquals(RecoveryOutcome.Denied, f.service.completeRecovery(staleToken, "newpassword"))
    }

    @Test
    fun `recovery round trip - old password out, new password in, old sessions revoked`() {
        val f = Fixture()
        val r = assertIs<RegisterOutcome.Registered>(f.service.register("longenough", "me@aspen.app"))
        f.service.requestRecovery("me@aspen.app")
        val token = f.sentMail.single().second

        val recovered = assertIs<RecoveryOutcome.Recovered>(f.service.completeRecovery(token, "brand-new-pass"))
        assertEquals(r.accountId, recovered.accountId)
        assertNull(f.service.accountFor(r.token), "pre-recovery session must be revoked")
        assertEquals(LoginOutcome.Denied, f.service.login(r.accountId, "longenough"))
        assertIs<LoginOutcome.LoggedIn>(f.service.login(r.accountId, "brand-new-pass"))
    }

    @Test
    fun `recovery token is single-use and expires`() {
        val f = Fixture()
        f.service.register("longenough", "me@aspen.app")
        f.service.requestRecovery("me@aspen.app")
        val token = f.sentMail.single().second

        assertIs<RecoveryOutcome.Recovered>(f.service.completeRecovery(token, "brand-new-pass"))
        assertEquals(RecoveryOutcome.Denied, f.service.completeRecovery(token, "another-pass"))

        f.service.requestRecovery("me@aspen.app")
        val second = f.sentMail.last().second
        f.nowMs = AccountService.RECOVERY_TOKEN_TTL_MS + 1
        assertEquals(RecoveryOutcome.Denied, f.service.completeRecovery(second, "another-pass"))
    }

    @Test
    fun `recovery request for an unknown or absent email sends nothing and does not throw`() {
        val f = Fixture()
        f.service.register("longenough", email = null)
        f.service.requestRecovery("unknown@aspen.app")
        assertTrue(f.sentMail.isEmpty())
    }
}
