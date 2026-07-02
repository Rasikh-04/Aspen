package app.aspen.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

/**
 * iOS [BlobFileIo]: files under Application Support/aspen_blobs (backed up, app-private). Writes use
 * `writeToFile(atomically = true)` — the platform's temp-then-rename — matching the crash-safety
 * contract of the JVM/Android actuals.
 *
 * NOT YET ENABLED (see [platformBlobFileIo] below): the iOS [LocalCipher] is still the passthrough
 * placeholder (docs/STATUS.md), so writing its "ciphertext" to disk would persist PLAINTEXT — a
 * violation of the encrypted-local-store rule the in-memory default used to prevent. This class is
 * ready and compiles in CI; it is switched on together with the Keychain/CryptoKit cipher.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosBlobFileIo : BlobFileIo {

    private fun directoryPath(): String {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: error("no Application Support directory")
        return "$base/aspen_blobs"
    }

    private fun filePath(name: String): String = "${directoryPath()}/$name.blob"

    override fun read(name: String): ByteArray? {
        val data = NSData.dataWithContentsOfFile(filePath(name)) ?: return null
        val length = data.length.toInt()
        if (length == 0) return ByteArray(0)
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        return bytes
    }

    override fun write(name: String, bytes: ByteArray) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            directoryPath(),
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val data = if (bytes.isEmpty()) {
            NSData()
        } else {
            bytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
            }
        }
        check(data.writeToFile(filePath(name), atomically = true)) { "failed to write blob $name" }
    }

    override fun delete(name: String) {
        NSFileManager.defaultManager.removeItemAtPath(filePath(name), error = null)
    }
}

/**
 * Process-wide in-memory stand-in so iOS keeps the "nothing sensitive persists" guarantee until the
 * real cipher exists. Shared across instances (one map per process) so the common contract —
 * a fresh [FileEncryptedBlobStore] over the same name sees prior saves — still holds in-process.
 */
private object SharedInMemoryBlobFileIo : BlobFileIo {
    private val blobs = mutableMapOf<String, ByteArray>()
    override fun read(name: String): ByteArray? = blobs[name]
    override fun write(name: String, bytes: ByteArray) {
        blobs[name] = bytes.copyOf()
    }
    override fun delete(name: String) {
        blobs.remove(name)
    }
}

/** Flip to [IosBlobFileIo] together with the Keychain/CryptoKit [LocalCipher] actual (tracked). */
private const val IOS_CIPHER_IS_REAL = false

actual fun platformBlobFileIo(): BlobFileIo =
    if (IOS_CIPHER_IS_REAL) IosBlobFileIo() else SharedInMemoryBlobFileIo

