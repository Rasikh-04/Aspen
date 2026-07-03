package app.aspen.data.sync

import app.aspen.data.local.EncryptedBlobStore
import app.aspen.data.local.InMemoryEncryptedBlobStore
import app.aspen.data.local.LocalCipher
import app.aspen.data.local.platformLocalCipher
import app.aspen.domain.sync.BackupNowOutcome
import app.aspen.domain.sync.EnableBackupOutcome
import app.aspen.domain.sync.RestoreOutcome
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerBackupManagerTest {

    private val crypto = platformSyncCrypto()!!

    // ---- crypto seam ----

    @Test
    fun `seal-open roundtrip, wrong key and tampered bytes open to null`() {
        val key = crypto.deriveKey("a quiet passphrase", crypto.randomBytes(16))
        val sealed = crypto.seal(key, "hello".encodeToByteArray())

        assertContentEquals("hello".encodeToByteArray(), crypto.open(key, sealed))
        assertNull(crypto.open(crypto.deriveKey("wrong", crypto.randomBytes(16)), sealed))
        sealed[sealed.size - 1] = (sealed.last() + 1).toByte()
        assertNull(crypto.open(key, sealed))
    }

    @Test
    fun `recovery codes are well-formed and normalization forgives formatting`() {
        val code = RecoveryCode.generate(crypto)
        assertTrue(Regex("^[0-9A-HJKMNP-TV-Z]{5}(-[0-9A-HJKMNP-TV-Z]{5}){3}$").matches(code), code)
        assertEquals(RecoveryCode.normalize(code), RecoveryCode.normalize(code.lowercase().replace("-", " ")))
    }

    // ---- manager over a stateful fake blob endpoint ----

    /** One "device": its own device cipher, content stores, and sync-key blobs. */
    private class Device {
        val cipher: LocalCipher = platformLocalCipher()
        val content: Map<String, EncryptedBlobStore> =
            LocalStoreBundle.CONTENT_STORE_NAMES.associateWith { InMemoryEncryptedBlobStore() }
        val bundle = LocalStoreBundle(cipher, content)
        val keyBlob = InMemoryEncryptedBlobStore()
        val metaBlob = InMemoryEncryptedBlobStore()

        fun writeContent(name: String, plaintext: String) =
            content.getValue(name).save(cipher.encrypt(plaintext.encodeToByteArray()))

        fun readContent(name: String): String? =
            runCatching { content.getValue(name).load()?.let(cipher::decrypt)?.decodeToString() }.getOrNull()
    }

    /** The server side: verbatim blob storage, like the real (already-tested) :server routes. */
    private class FakeBlobServer {
        var stored: ByteArray? = null
        val engine = MockEngine { request ->
            when (request.method) {
                HttpMethod.Put -> {
                    stored = request.body.toByteArray()
                    respond("", HttpStatusCode.OK)
                }
                HttpMethod.Get -> stored?.let { respond(it, HttpStatusCode.OK) }
                    ?: respondError(HttpStatusCode.NotFound)
                HttpMethod.Delete -> {
                    stored = null
                    respond("", HttpStatusCode.OK)
                }
                else -> respondError(HttpStatusCode.MethodNotAllowed)
            }
        }
    }

    private fun manager(device: Device, server: FakeBlobServer, token: String? = "session-token") =
        ServerBackupManager(
            baseUrl = "https://aspen.example.com",
            sessionToken = { token },
            http = HttpClient(server.engine),
            crypto = crypto,
            localCipher = device.cipher,
            bundle = device.bundle,
            keyBlob = device.keyBlob,
            metaBlob = device.metaBlob,
        )

    @Test
    fun `enable uploads ciphertext only - the written content never appears in what the server holds`() = runTest {
        val device = Device().apply { writeContent("logs", "a-very-distinctive-reflection-sentence") }
        val server = FakeBlobServer()

        val outcome = manager(device, server).enable("a quiet passphrase")

        assertIs<EnableBackupOutcome.Enabled>(outcome)
        val held = server.stored!!.decodeToString()
        assertFalse(held.contains("distinctive"), "server must hold nothing readable")
        assertFalse(held.contains(outcome.recoveryCode.take(5)), "secrets never leave the device")
    }

    @Test
    fun `full journey - enable on one device, restore on a NEW device with the passphrase`() = runTest {
        val old = Device().apply {
            writeContent("logs", "the reflections")
            writeContent("profile", "the profile")
        }
        val server = FakeBlobServer()
        assertIs<EnableBackupOutcome.Enabled>(manager(old, server).enable("a quiet passphrase"))

        val fresh = Device()
        assertEquals(RestoreOutcome.Restored, manager(fresh, server).restore("a quiet passphrase"))

        assertEquals("the reflections", fresh.readContent("logs"))
        assertEquals("the profile", fresh.readContent("profile"))
        assertTrue(manager(fresh, server).isConfigured(), "restored device can back up without re-enabling")
    }

    @Test
    fun `restore also works with the recovery code, however it is typed`() = runTest {
        val old = Device().apply { writeContent("logs", "kept safe") }
        val server = FakeBlobServer()
        val code = (manager(old, server).enable("a quiet passphrase") as EnableBackupOutcome.Enabled).recoveryCode

        val fresh = Device()
        val sloppy = code.lowercase().replace("-", "  ")
        assertEquals(RestoreOutcome.Restored, manager(fresh, server).restore(sloppy))
        assertEquals("kept safe", fresh.readContent("logs"))
    }

    @Test
    fun `wrong secret, missing backup and offline map to their calm outcomes`() = runTest {
        val device = Device().apply { writeContent("logs", "content") }
        val server = FakeBlobServer()
        manager(device, server).enable("a quiet passphrase")

        assertEquals(RestoreOutcome.WrongSecret, manager(Device(), server).restore("not the passphrase"))
        assertEquals(RestoreOutcome.NoBackup, manager(Device(), FakeBlobServer()).restore("whatever"))

        val offline = MockEngine { throw RuntimeException("no network") }
        val offlineManager = ServerBackupManager(
            "https://aspen.example.com", { "t" }, HttpClient(offline), crypto,
            device.cipher, device.bundle, device.keyBlob, device.metaBlob,
        )
        assertEquals(RestoreOutcome.Unavailable, offlineManager.restore("a quiet passphrase"))
        assertEquals(BackupNowOutcome.Unavailable, offlineManager.backUpNow())
    }

    @Test
    fun `weak passphrase refused, unconfigured backUpNow refused, no session is Unavailable`() = runTest {
        val device = Device()
        val server = FakeBlobServer()

        assertEquals(EnableBackupOutcome.WeakPassphrase, manager(device, server).enable("short"))
        assertEquals(BackupNowOutcome.NotConfigured, manager(device, server).backUpNow())
        assertEquals(EnableBackupOutcome.Unavailable, manager(device, server, token = null).enable("a quiet passphrase"))
    }

    @Test
    fun `disable deletes the server copy first, then forgets the local key`() = runTest {
        val device = Device().apply { writeContent("logs", "content") }
        val server = FakeBlobServer()
        manager(device, server).enable("a quiet passphrase")

        assertTrue(manager(device, server).disable())
        assertNull(server.stored)
        assertFalse(manager(device, server).isConfigured())
    }
}
