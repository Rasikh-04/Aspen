package app.aspen.data.sync

import app.aspen.api.AspenApi
import app.aspen.data.local.EncryptedBlobStore
import app.aspen.data.local.LocalCipher
import app.aspen.domain.sync.BackupManager
import app.aspen.domain.sync.BackupNowOutcome
import app.aspen.domain.sync.EnableBackupOutcome
import app.aspen.domain.sync.RestoreOutcome
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * [BackupManager] over the Aspen server's ciphertext-blob endpoint (docs/08 §2). The E2E chain:
 * random data key K → payload sealed with K → K wrapped by passphrase AND by the once-shown
 * recovery code → only the resulting [SyncEnvelope] ever leaves the device. Locally, K persists
 * sealed under the DEVICE cipher (so "back up now" needs no retyping), and the wrap header is
 * kept alongside so every upload reuses the same secrets. Total: every failure path is a calm
 * outcome, never an exception.
 */
class ServerBackupManager(
    private val baseUrl: String,
    private val sessionToken: () -> String?,
    private val http: HttpClient,
    private val crypto: SyncCrypto,
    private val localCipher: LocalCipher,
    private val bundle: LocalStoreBundle,
    private val keyBlob: EncryptedBlobStore,
    private val metaBlob: EncryptedBlobStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : BackupManager {

    override fun isConfigured(): Boolean = loadKey() != null && loadMeta() != null

    override suspend fun enable(passphrase: String): EnableBackupOutcome {
        if (passphrase.length < MIN_PASSPHRASE_LENGTH) return EnableBackupOutcome.WeakPassphrase
        if (sessionToken() == null) return EnableBackupOutcome.Unavailable

        val dataKey = crypto.randomBytes(KEY_BYTES)
        val recoveryCode = RecoveryCode.generate(crypto)
        val passSalt = crypto.randomBytes(SALT_BYTES)
        val recoverySalt = crypto.randomBytes(SALT_BYTES)
        val meta = SyncKeyMeta(
            passSalt = SyncEnvelope.encode(passSalt),
            recoverySalt = SyncEnvelope.encode(recoverySalt),
            keyByPass = SyncEnvelope.encode(crypto.seal(crypto.deriveKey(passphrase, passSalt), dataKey)),
            keyByRecovery = SyncEnvelope.encode(
                crypto.seal(crypto.deriveKey(RecoveryCode.normalize(recoveryCode), recoverySalt), dataKey),
            ),
        )

        if (!upload(dataKey, meta)) return EnableBackupOutcome.Unavailable

        // Persist only after the first upload succeeded — no half-configured state.
        keyBlob.save(localCipher.encrypt(dataKey))
        metaBlob.save(localCipher.encrypt(json.encodeToString(SyncKeyMeta.serializer(), meta).encodeToByteArray()))
        return EnableBackupOutcome.Enabled(recoveryCode)
    }

    override suspend fun backUpNow(): BackupNowOutcome {
        val dataKey = loadKey() ?: return BackupNowOutcome.NotConfigured
        val meta = loadMeta() ?: return BackupNowOutcome.NotConfigured
        return if (upload(dataKey, meta)) BackupNowOutcome.Done else BackupNowOutcome.Unavailable
    }

    override suspend fun restore(secret: String): RestoreOutcome {
        val token = sessionToken() ?: return RestoreOutcome.Unavailable
        val envelope = runCatching {
            val response = http.get(url()) { bearer(token) }
            when {
                response.status == HttpStatusCode.NotFound -> return RestoreOutcome.NoBackup
                !response.status.isSuccess() -> return RestoreOutcome.Unavailable
                else -> json.decodeFromString(SyncEnvelope.serializer(), response.readRawBytes().decodeToString())
            }
        }.getOrElse { return RestoreOutcome.Unavailable }

        val dataKey = unwrapKey(secret, envelope) ?: return RestoreOutcome.WrongSecret
        val payload = crypto.open(dataKey, SyncEnvelope.decode(envelope.payload))
            ?: return RestoreOutcome.WrongSecret

        runCatching { bundle.importPlaintext(payload) }.getOrElse { return RestoreOutcome.Unavailable }

        // This device is now configured with the same secrets — future backups just work.
        keyBlob.save(localCipher.encrypt(dataKey))
        val meta = SyncKeyMeta(envelope.passSalt, envelope.recoverySalt, envelope.keyByPass, envelope.keyByRecovery)
        metaBlob.save(localCipher.encrypt(json.encodeToString(SyncKeyMeta.serializer(), meta).encodeToByteArray()))
        return RestoreOutcome.Restored
    }

    override suspend fun disable(): Boolean {
        val token = sessionToken() ?: return false
        val gone = runCatching {
            val status = http.delete(url()) { bearer(token) }.status
            status.isSuccess() || status == HttpStatusCode.NotFound
        }.getOrDefault(false)
        // Forget the key only once the server copy is gone — mirrors delete-account semantics.
        if (gone) {
            keyBlob.clear()
            metaBlob.clear()
        }
        return gone
    }

    private suspend fun upload(dataKey: ByteArray, meta: SyncKeyMeta): Boolean {
        val token = sessionToken() ?: return false
        val envelope = SyncEnvelope(
            passSalt = meta.passSalt,
            recoverySalt = meta.recoverySalt,
            keyByPass = meta.keyByPass,
            keyByRecovery = meta.keyByRecovery,
            payload = SyncEnvelope.encode(crypto.seal(dataKey, bundle.exportPlaintext())),
        )
        return runCatching {
            http.put(url()) {
                bearer(token)
                setBody(json.encodeToString(SyncEnvelope.serializer(), envelope).encodeToByteArray())
            }.status.isSuccess()
        }.getOrDefault(false)
    }

    /** Tries the secret as passphrase, then as recovery code — one undifferentiated failure. */
    private fun unwrapKey(secret: String, envelope: SyncEnvelope): ByteArray? {
        val byPass = crypto.open(
            crypto.deriveKey(secret, SyncEnvelope.decode(envelope.passSalt)),
            SyncEnvelope.decode(envelope.keyByPass),
        )
        if (byPass != null) return byPass
        return crypto.open(
            crypto.deriveKey(RecoveryCode.normalize(secret), SyncEnvelope.decode(envelope.recoverySalt)),
            SyncEnvelope.decode(envelope.keyByRecovery),
        )
    }

    private fun loadKey(): ByteArray? =
        runCatching { keyBlob.load()?.let(localCipher::decrypt) }.getOrNull()

    private fun loadMeta(): SyncKeyMeta? = runCatching {
        val bytes = metaBlob.load()?.let(localCipher::decrypt) ?: return null
        json.decodeFromString(SyncKeyMeta.serializer(), bytes.decodeToString())
    }.getOrNull()

    private fun url() = baseUrl.trimEnd('/') + AspenApi.SYNC_BLOB

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    companion object {
        const val MIN_PASSPHRASE_LENGTH = 8
        private const val KEY_BYTES = 32
        private const val SALT_BYTES = 16
    }
}
