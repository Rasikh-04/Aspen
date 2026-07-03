package app.aspen.server.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing for account credentials (docs/08 §1). PBKDF2-HMAC-SHA256 with a per-password
 * random salt — chosen over an external Argon2 dependency deliberately: it ships in the JDK,
 * has no native-library surface, and at OWASP-recommended iteration counts is an accepted
 * password KDF. Self-describing storage format so parameters can be raised later without
 * invalidating existing hashes:
 *
 *     pbkdf2-sha256$<iterations>$<saltB64>$<hashB64>
 *
 * Verification is constant-time ([MessageDigest.isEqual]) and total: any malformed or foreign
 * stored value verifies false, never throws.
 */
object PasswordHasher {

    /** OWASP (2023+) recommended floor for PBKDF2-HMAC-SHA256. */
    const val DEFAULT_ITERATIONS = 600_000

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2-sha256"
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256

    private val random = SecureRandom()

    fun hash(password: String, iterations: Int = DEFAULT_ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val derived = derive(password, salt, iterations)
        val b64 = Base64.getEncoder()
        return "$PREFIX$$iterations$${b64.encodeToString(salt)}$${b64.encodeToString(derived)}"
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != PREFIX) return false
        return runCatching {
            val iterations = parts[1].toInt()
            if (iterations < 1) return false
            val salt = Base64.getDecoder().decode(parts[2])
            val expected = Base64.getDecoder().decode(parts[3])
            MessageDigest.isEqual(expected, derive(password, salt, iterations))
        }.getOrDefault(false)
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
