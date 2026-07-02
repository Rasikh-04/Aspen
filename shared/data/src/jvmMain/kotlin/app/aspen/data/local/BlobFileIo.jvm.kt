package app.aspen.data.local

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * JVM [BlobFileIo]: files under an app-scoped directory in the system temp dir (dev/test grade —
 * pairs with the process-ephemeral JVM [LocalCipher]; production durability is the Android/iOS
 * actuals' job). Writes are temp-then-atomic-rename so a crash mid-write can't corrupt a good blob.
 */
class JvmBlobFileIo(private val directory: File = defaultDirectory()) : BlobFileIo {

    override fun read(name: String): ByteArray? {
        val file = directory.resolve("$name.blob")
        return if (file.isFile) file.readBytes() else null
    }

    override fun write(name: String, bytes: ByteArray) {
        directory.mkdirs()
        val target = directory.resolve("$name.blob")
        val temp = File.createTempFile("$name.", ".tmp", directory)
        try {
            temp.writeBytes(bytes)
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            temp.delete()
        }
    }

    override fun delete(name: String) {
        directory.resolve("$name.blob").delete()
    }

    companion object {
        fun defaultDirectory(): File = File(System.getProperty("java.io.tmpdir"), "aspen-local-store")
    }
}

actual fun platformBlobFileIo(): BlobFileIo = JvmBlobFileIo()
