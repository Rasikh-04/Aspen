package app.aspen.server.routes

import app.aspen.api.AspenApi
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SyncRoutesTest {

    @Test
    fun `blob round-trips verbatim - opaque ciphertext in, identical bytes out`() = TestServer().run { client ->
        val auth = client.registerAccount()
        val ciphertext = ByteArray(512) { (it * 7).toByte() }

        assertEquals(
            HttpStatusCode.OK,
            client.put(AspenApi.SYNC_BLOB) { bearer(auth.token); setBody(ciphertext) }.status,
        )
        val fetched = client.get(AspenApi.SYNC_BLOB) { bearer(auth.token) }
        assertEquals(HttpStatusCode.OK, fetched.status)
        assertContentEquals(ciphertext, fetched.readRawBytes())
    }

    @Test
    fun `every sync verb requires auth`() = TestServer().run { client ->
        assertEquals(HttpStatusCode.Unauthorized, client.put(AspenApi.SYNC_BLOB) { setBody(byteArrayOf(1)) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get(AspenApi.SYNC_BLOB).status)
        assertEquals(HttpStatusCode.Unauthorized, client.delete(AspenApi.SYNC_BLOB).status)
    }

    @Test
    fun `accounts are isolated - one account can never read another's blob`() = TestServer().run { client ->
        val alice = client.registerAccount()
        val bob = client.registerAccount()
        client.put(AspenApi.SYNC_BLOB) { bearer(alice.token); setBody(byteArrayOf(1, 2, 3)) }

        // Bob's own view is empty; there is no parameter through which he could name Alice's blob.
        assertEquals(HttpStatusCode.NotFound, client.get(AspenApi.SYNC_BLOB) { bearer(bob.token) }.status)
    }

    @Test
    fun `delete purges - subsequent get is 404`() = TestServer().run { client ->
        val auth = client.registerAccount()
        client.put(AspenApi.SYNC_BLOB) { bearer(auth.token); setBody(byteArrayOf(5)) }

        assertEquals(HttpStatusCode.OK, client.delete(AspenApi.SYNC_BLOB) { bearer(auth.token) }.status)
        assertEquals(HttpStatusCode.NotFound, client.get(AspenApi.SYNC_BLOB) { bearer(auth.token) }.status)
        assertEquals(HttpStatusCode.NotFound, client.delete(AspenApi.SYNC_BLOB) { bearer(auth.token) }.status)
    }

    @Test
    fun `oversized blob is refused`() = TestServer().run { client ->
        val auth = client.registerAccount()
        val tooBig = ByteArray(5 * 1024 * 1024 + 1)
        assertEquals(
            HttpStatusCode.PayloadTooLarge,
            client.put(AspenApi.SYNC_BLOB) { bearer(auth.token); setBody(tooBig) }.status,
        )
    }
}
