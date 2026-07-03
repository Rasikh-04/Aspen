package app.aspen.server.store

import java.util.concurrent.ConcurrentHashMap

/** In-memory repositories: the test doubles and the zero-config dev default (data lost on stop). */

class InMemoryAccountRepository : AccountRepository {
    private val byId = ConcurrentHashMap<String, AccountRecord>()

    override fun create(record: AccountRecord) {
        byId[record.id] = record
    }

    override fun byId(id: String): AccountRecord? = byId[id]

    override fun byEmail(email: String): AccountRecord? =
        byId.values.firstOrNull { it.email.equals(email, ignoreCase = true) }

    override fun updatePassword(id: String, newHash: String): Boolean =
        byId.computeIfPresent(id) { _, existing -> existing.copy(passwordHash = newHash) } != null

    override fun delete(id: String): Boolean = byId.remove(id) != null
}

class InMemorySessionRepository : SessionRepository {
    private val accountByToken = ConcurrentHashMap<String, String>()

    override fun put(token: String, accountId: String) {
        accountByToken[token] = accountId
    }

    override fun accountFor(token: String): String? = accountByToken[token]

    override fun revoke(token: String) {
        accountByToken.remove(token)
    }

    override fun revokeAll(accountId: String) {
        accountByToken.entries.removeIf { it.value == accountId }
    }
}

class InMemoryBlobRepository : BlobRepository {
    private val blobs = ConcurrentHashMap<String, ByteArray>()

    override fun put(accountId: String, ciphertext: ByteArray) {
        blobs[accountId] = ciphertext.copyOf()
    }

    override fun get(accountId: String): ByteArray? = blobs[accountId]?.copyOf()

    override fun delete(accountId: String): Boolean = blobs.remove(accountId) != null
}

class InMemoryRecoveryTokenRepository : RecoveryTokenRepository {
    private data class Entry(val accountId: String, val expiresAtEpochMs: Long)

    private val entries = ConcurrentHashMap<String, Entry>()

    override fun put(token: String, accountId: String, expiresAtEpochMs: Long) {
        entries[token] = Entry(accountId, expiresAtEpochMs)
    }

    override fun consume(token: String, nowEpochMs: Long): String? {
        val entry = entries.remove(token) ?: return null
        return entry.accountId.takeIf { nowEpochMs < entry.expiresAtEpochMs }
    }

    override fun revokeAll(accountId: String) {
        entries.entries.removeIf { it.value.accountId == accountId }
    }
}
