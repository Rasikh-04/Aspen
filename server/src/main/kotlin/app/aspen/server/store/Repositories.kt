package app.aspen.server.store

/**
 * Server-side storage ports (repository pattern). v1 ships an in-memory implementation (tests,
 * ephemeral dev) and a file-backed one (durable dev) — a production database is a drop-in behind
 * these interfaces later (docs/07 Phase 6.9).
 *
 * Deliberate absence: there is NO message/content repository. The AI relay is stateless by
 * construction — nothing a user writes can be persisted server-side because no store for it
 * exists (CLAUDE.md #8/#10; docs/08 §2 "the server handles identity and access control — not
 * content").
 */

/** One app-native account (docs/08 §1). [email] is optional, for login + recovery only. */
data class AccountRecord(
    val id: String,
    val email: String?,
    val passwordHash: String,
    val createdAtEpochMs: Long,
)

interface AccountRepository {
    fun create(record: AccountRecord)
    fun byId(id: String): AccountRecord?
    fun byEmail(email: String): AccountRecord?

    /** Returns false when the account no longer exists. */
    fun updatePassword(id: String, newHash: String): Boolean
    fun delete(id: String): Boolean
}

/** Opaque bearer-token sessions; server-side so revocation is immediate (docs/08 §3 spirit). */
interface SessionRepository {
    fun put(token: String, accountId: String)
    fun accountFor(token: String): String?
    fun revoke(token: String)
    fun revokeAll(accountId: String)
}

/**
 * The E2E sync store: one opaque ciphertext blob per account (docs/04 §5 `synced_data`).
 * Bytes go in and come out verbatim — the server has no key and nothing here can decrypt.
 */
interface BlobRepository {
    fun put(accountId: String, ciphertext: ByteArray)
    fun get(accountId: String): ByteArray?

    /** FR-11 delete-means-delete: purge, not tombstone-forever. Returns false when absent. */
    fun delete(accountId: String): Boolean
}

/** Single-use, expiring email-recovery tokens (restores LOGIN, never the data key — docs/08 §2). */
interface RecoveryTokenRepository {
    fun put(token: String, accountId: String, expiresAtEpochMs: Long)

    /** Atomically consumes the token: valid+unexpired → its accountId, else null. */
    fun consume(token: String, nowEpochMs: Long): String?
    fun revokeAll(accountId: String)
}
