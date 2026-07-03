package app.aspen.api

import kotlinx.serialization.Serializable

/**
 * The Aspen server wire contract (docs/08 §1–2, docs/07 Phase 6) — the ONE set of DTOs both the
 * `:server` routes and the `:shared:data` clients compile against, so the two sides cannot drift.
 *
 * Privacy invariants encoded here, not just documented:
 *  - No DTO carries a key, passphrase, or recovery code: the E2E data key is derived and kept
 *    on-device (docs/08 §2); the server verifies AUTHORIZATION, never content.
 *  - Sync payloads are raw ciphertext bytes (HTTP body), deliberately absent from these types —
 *    there is no field a careless feature could put plaintext into.
 *  - [ApiError.code] is a machine token, never a sentence — user-facing copy stays in the app's
 *    localized resources (CLAUDE.md #11), and error detail can't leak account existence.
 */
object AspenApi {
    const val VERSION_PREFIX = "/v1"

    const val REGISTER = "$VERSION_PREFIX/account/register"
    const val LOGIN = "$VERSION_PREFIX/account/login"
    const val LOGOUT = "$VERSION_PREFIX/account/logout"
    const val ACCOUNT = "$VERSION_PREFIX/account"
    const val RECOVERY_REQUEST = "$VERSION_PREFIX/account/recovery/request"
    const val RECOVERY_COMPLETE = "$VERSION_PREFIX/account/recovery/complete"
    const val SYNC_BLOB = "$VERSION_PREFIX/sync/blob"
    const val AI_REFLECT = "$VERSION_PREFIX/ai/reflect"
    const val HEALTH = "$VERSION_PREFIX/health"
}

/** Email is optional (FR-9 spirit: the account itself asks for as little as possible). */
@Serializable
data class RegisterRequest(val password: String, val email: String? = null)

/** [identifier] is the account id or, when one was attached, the email (docs/08 §1 layerable auth). */
@Serializable
data class LoginRequest(val identifier: String, val password: String)

@Serializable
data class AuthResponse(val accountId: String, val token: String)

/** Always accepted outwardly (no account enumeration); a mail is sent only if the email is known. */
@Serializable
data class RecoveryRequest(val email: String)

/** Restores LOGIN only — never the E2E data key (docs/08 §2 honest trade-off). */
@Serializable
data class RecoveryCompleteRequest(val recoveryToken: String, val newPassword: String)

@Serializable
data class ReflectRequest(val text: String, val history: List<ApiChatMessage> = emptyList())

/** [role] is one of [ApiChatMessage.ROLE_USER] / [ApiChatMessage.ROLE_COMPANION]. */
@Serializable
data class ApiChatMessage(val role: String, val text: String) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_COMPANION = "companion"
    }
}

@Serializable
data class ReflectResponse(val status: String, val text: String? = null) {
    companion object {
        const val STATUS_REPLY = "reply"

        /** Calm degradation token — the app renders its own localized quiet copy (FR-5). */
        const val STATUS_UNAVAILABLE = "unavailable"
    }
}

/** Machine-readable error token; the app maps codes to localized copy. */
@Serializable
data class ApiError(val code: String) {
    companion object {
        const val CODE_INVALID = "invalid_request"
        const val CODE_UNAUTHORIZED = "unauthorized"
        const val CODE_EMAIL_TAKEN = "email_taken"
        const val CODE_WEAK_PASSWORD = "weak_password"
        const val CODE_RATE_LIMITED = "rate_limited"
        const val CODE_NOT_FOUND = "not_found"
        const val CODE_TOO_LARGE = "too_large"
    }
}
