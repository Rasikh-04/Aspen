package app.aspen.data.local

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Process-wide storage anchor for the Android blob actuals. The data layer can't reach a [Context]
 * on its own (and must stay Koin-optional — platform entries still construct stores by hand,
 * docs/STATUS.md), so the app initialises this ONCE at launch, before any store is built:
 *
 *     AspenLocalStorage.init(applicationContext)   // MainActivity.onCreate / Application.onCreate
 *
 * Failing fast on a missed init is deliberate: silently falling back to memory would look like it
 * works and then lose the user's writing on restart — the one failure mode a private notebook must
 * never have.
 */
object AspenLocalStorage {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    internal fun requireContext(): Context =
        checkNotNull(appContext) {
            "AspenLocalStorage.init(context) must be called at app start before any local store is created"
        }
}

/**
 * Android [BlobFileIo]: files under `filesDir/aspen_blobs` — private app storage, no permissions,
 * cleared with the app. Bytes are ciphertext from the AndroidKeyStore-backed [LocalCipher]. Writes
 * are temp-then-atomic-rename (same crash-safety contract as the JVM actual).
 */
private class AndroidBlobFileIo(context: Context) : BlobFileIo {
    private val directory = File(context.filesDir, "aspen_blobs")

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
}

actual fun platformBlobFileIo(): BlobFileIo = AndroidBlobFileIo(AspenLocalStorage.requireContext())
