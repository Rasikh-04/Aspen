package app.aspen.domain.sync

/** Outcome of turning backup on. [Enabled.recoveryCode] is shown ONCE and never stored readable. */
sealed interface EnableBackupOutcome {
    data class Enabled(val recoveryCode: String) : EnableBackupOutcome
    data object WeakPassphrase : EnableBackupOutcome
    data object Unavailable : EnableBackupOutcome
}

sealed interface BackupNowOutcome {
    data object Done : BackupNowOutcome
    data object NotConfigured : BackupNowOutcome
    data object Unavailable : BackupNowOutcome
}

sealed interface RestoreOutcome {
    data object Restored : RestoreOutcome

    /** Neither the passphrase nor a recovery code opened the backup — undifferentiated. */
    data object WrongSecret : RestoreOutcome
    data object NoBackup : RestoreOutcome
    data object Unavailable : RestoreOutcome
}

/**
 * Optional, user-initiated E2E backup of the user's written content (docs/08 §2 — decided: true
 * E2E, recovery code + email account-recovery). The honest contract this port encodes:
 *
 *  - The key is derived ON-DEVICE from the user's passphrase; the server only ever receives
 *    ciphertext it cannot decrypt. Enabling returns a one-time recovery code — the ONLY other
 *    way in. If both are lost, the backup is unrecoverable BY DESIGN; an email reset restores
 *    account access, never this key. UI copy must say this plainly (no false reassurance).
 *  - Everything is manual and foreground: no background sync exists (docs/04 §6 no background
 *    network), and none of this is required for any feature (FR-9).
 */
interface BackupManager {
    fun isConfigured(): Boolean

    /** Turns backup on and uploads the first backup. Returns the once-shown recovery code. */
    suspend fun enable(passphrase: String): EnableBackupOutcome

    suspend fun backUpNow(): BackupNowOutcome

    /** [secret] is the passphrase or a recovery code; restores onto THIS device and configures it. */
    suspend fun restore(secret: String): RestoreOutcome

    /** Deletes the server-side backup, then forgets the local key. False = server not reached. */
    suspend fun disable(): Boolean
}
