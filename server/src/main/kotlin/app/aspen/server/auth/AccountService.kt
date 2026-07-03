package app.aspen.server.auth

import app.aspen.server.store.AccountRecord
import app.aspen.server.store.AccountRepository
import app.aspen.server.store.BlobRepository
import app.aspen.server.store.RecoveryTokenRepository
import app.aspen.server.store.SessionRepository
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * Delivers a recovery token to the account's email. v1 dev implementation logs to stdout;
 * a real mail sender is deployment configuration (docs/07 Phase 6.9). The token restores
 * LOGIN only — the E2E data key never touches the server (docs/08 §2).
 */
fun interface RecoveryMailer {
    fun send(email: String, recoveryToken: String)
}

sealed interface RegisterOutcome {
    data class Registered(val accountId: String, val token: String) : RegisterOutcome
    data object EmailTaken : RegisterOutcome
    data object WeakPassword : RegisterOutcome
}

sealed interface RecoveryOutcome {
    data class Recovered(val accountId: String, val token: String) : RecoveryOutcome
    data object WeakPassword : RecoveryOutcome

    /** Invalid, expired, or already-used token — undifferentiated on purpose. */
    data object Denied : RecoveryOutcome
}

sealed interface LoginOutcome {
    data class LoggedIn(val accountId: String, val token: String) : LoginOutcome

    /** One undifferentiated denial: callers can never distinguish "no such account" from "wrong password". */
    data object Denied : LoginOutcome
}

/**
 * Account rules in one place, routes stay thin (docs/08 §1: the Aspen account is the root;
 * auth methods attach to it — password now, federated later in Phase 6.9).
 */
class AccountService(
    private val accounts: AccountRepository,
    private val sessions: SessionRepository,
    private val recoveryTokens: RecoveryTokenRepository,
    private val blobs: BlobRepository,
    private val mailer: RecoveryMailer,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    /** Production default; tests lower it — correctness is iteration-independent, speed is not. */
    private val hashIterations: Int = PasswordHasher.DEFAULT_ITERATIONS,
) {

    fun register(password: String, email: String?): RegisterOutcome {
        if (password.length < MIN_PASSWORD_LENGTH) return RegisterOutcome.WeakPassword
        val normalizedEmail = email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (normalizedEmail != null && accounts.byEmail(normalizedEmail) != null) return RegisterOutcome.EmailTaken

        val account = AccountRecord(
            id = newId(),
            email = normalizedEmail,
            passwordHash = PasswordHasher.hash(password, hashIterations),
            createdAtEpochMs = now(),
        )
        accounts.create(account)
        return RegisterOutcome.Registered(account.id, issueSession(account.id))
    }

    fun login(identifier: String, password: String): LoginOutcome {
        val account = accounts.byId(identifier.trim())
            ?: accounts.byEmail(identifier.trim().lowercase())
            ?: return LoginOutcome.Denied
        if (!PasswordHasher.verify(password, account.passwordHash)) return LoginOutcome.Denied
        return LoginOutcome.LoggedIn(account.id, issueSession(account.id))
    }

    fun accountFor(token: String): String? = sessions.accountFor(token)

    fun logout(token: String) = sessions.revoke(token)

    /** FR-11 delete-means-delete: account, sessions, recovery tokens, and the sync blob all purge. */
    fun deleteAccount(accountId: String) {
        sessions.revokeAll(accountId)
        recoveryTokens.revokeAll(accountId)
        blobs.delete(accountId)
        accounts.delete(accountId)
    }

    /** Outwardly always succeeds (no account enumeration); mails only a known address. */
    fun requestRecovery(email: String) {
        val account = accounts.byEmail(email.trim().lowercase()) ?: return
        val token = newToken()
        recoveryTokens.put(token, account.id, now() + RECOVERY_TOKEN_TTL_MS)
        mailer.send(requireNotNull(account.email), token)
    }

    /** Consumes the token, resets the password, and revokes every existing session. */
    fun completeRecovery(recoveryToken: String, newPassword: String): RecoveryOutcome {
        if (newPassword.length < MIN_PASSWORD_LENGTH) return RecoveryOutcome.WeakPassword
        val accountId = recoveryTokens.consume(recoveryToken, now()) ?: return RecoveryOutcome.Denied
        return if (accounts.updatePassword(accountId, PasswordHasher.hash(newPassword, hashIterations))) {
            sessions.revokeAll(accountId)
            RecoveryOutcome.Recovered(accountId, issueSession(accountId))
        } else {
            RecoveryOutcome.Denied
        }
    }

    private fun issueSession(accountId: String): String =
        newToken().also { sessions.put(it, accountId) }

    private fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(secureRandom::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        const val RECOVERY_TOKEN_TTL_MS: Long = 30L * 60 * 1000
        private const val TOKEN_BYTES = 32
        private val secureRandom = SecureRandom()
    }
}
