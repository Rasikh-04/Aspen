package app.aspen.server.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {

    // Low iteration count in tests only — correctness is iteration-independent, speed is not.
    private fun hash(password: String) = PasswordHasher.hash(password, iterations = 1_000)

    @Test
    fun `stored value never contains or equals the password`() {
        val stored = hash("correct horse battery")
        assertNotEquals("correct horse battery", stored)
        assertFalse(stored.contains("correct horse battery"))
    }

    @Test
    fun `verify accepts the right password and rejects a wrong one`() {
        val stored = hash("a-quiet-passphrase")
        assertTrue(PasswordHasher.verify("a-quiet-passphrase", stored))
        assertFalse(PasswordHasher.verify("a-quiet-passphrasE", stored))
        assertFalse(PasswordHasher.verify("", stored))
    }

    @Test
    fun `same password hashes differently every time - per-password salt`() {
        assertNotEquals(hash("same input"), hash("same input"))
    }

    @Test
    fun `verify is total - malformed stored values are false, never a throw`() {
        listOf(
            "",
            "not-a-hash",
            "pbkdf2-sha256",
            "pbkdf2-sha256\$abc\$notb64\$notb64",
            "pbkdf2-sha256\$-5\$AAAA\$AAAA",
            "argon2\$1\$AAAA\$AAAA",
            "pbkdf2-sha256\$1000\$AAAA\$AAAA\$extra",
        ).forEach { assertFalse(PasswordHasher.verify("anything", it), "should reject: $it") }
    }

    @Test
    fun `hash format is self-describing so parameters can be raised later`() {
        val parts = hash("x".repeat(8)).split('$')
        assertTrue(parts.size == 4 && parts[0] == "pbkdf2-sha256" && parts[1] == "1000")
    }
}
