package app.aspen.domain.account

/** The signed-in identity, if any. Knowing it changes nothing about local features (FR-9). */
data class AccountState(val accountId: String)

/** Outcome of one account action. Calm and total — no exception ever reaches the UI. */
sealed interface AccountResult {
    data class SignedIn(val state: AccountState) : AccountResult
    data object EmailTaken : AccountResult
    data object WeakPassword : AccountResult

    /** Wrong credentials or invalid input — one undifferentiated denial (mirrors the server). */
    data object Denied : AccountResult

    /** Offline or server unreachable — the app degrades quietly, never with an error state. */
    data object Unavailable : AccountResult
}

/**
 * The OPTIONAL Aspen-native account (docs/08 §1, FR-9). Anonymous use is the default and every
 * feature works without this; an account only adds the option of backup/sync later. The UI offers
 * it quietly in Settings — it is never proposed during onboarding or any arrival flow
 * (CLAUDE.md #10: the calmer option). Implementations persist the session encrypted so sign-in
 * survives restarts, and [signOut]/[deleteAccount] clear it immediately.
 */
interface AccountManager {
    fun current(): AccountState?

    /** [email] is optional — an account can exist with no email at all (login by account id). */
    suspend fun register(password: String, email: String?): AccountResult

    /** [identifier] is the account id or an attached email. */
    suspend fun signIn(identifier: String, password: String): AccountResult

    /** Revokes the server session (best-effort) and always clears the local one. */
    suspend fun signOut()

    /** Server-side purge (account, sessions, backed-up blob) + local session clear. */
    suspend fun deleteAccount(): Boolean
}
