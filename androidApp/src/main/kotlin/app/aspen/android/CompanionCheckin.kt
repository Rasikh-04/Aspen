package app.aspen.android

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.aspen.data.ai.local.LibraryCompanionVoice
import app.aspen.data.companion.PersistentCompanionPrefsStore
import app.aspen.data.local.AspenLocalStorage
import app.aspen.data.local.EncryptedBlobStore
import app.aspen.data.local.FileEncryptedBlobStore
import app.aspen.data.local.LocalCipher
import app.aspen.data.local.platformLocalCipher
import app.aspen.data.onboarding.PersistentProfileStore
import app.aspen.domain.ai.CompanionMoment
import app.aspen.domain.companion.NotificationPolicy
import app.aspen.domain.onboarding.AppConfigProvider
import app.aspen.ui.companion.companionLineResource
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.notify_channel_description
import app.aspen.ui.generated.resources.notify_channel_name
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.compose.resources.getString

/**
 * Delivery plumbing for the companion's rare check-in (FR-8; docs/05 §3.5; deferred here from
 * Phase 4 by approval). ALL judgement lives in the domain [NotificationPolicy] — this worker can
 * only ask "may I, right now?" and quietly do nothing otherwise. The wording comes exclusively
 * from the reviewed library's NOTIFICATION_PHRASING moment via the Phase-4 voice.
 */
object CompanionCheckinScheduler {
    private const val WORK_NAME = "aspen_companion_checkin"

    /** Daily probe; the 72h policy cadence + quiet hours decide whether anything is shown. */
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CompanionCheckinWorker>(24, TimeUnit.HOURS).build(),
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

/** Encrypted last-delivery timestamp — the policy's cadence memory. Fail-safe: unreadable → null. */
private class CheckinTimestampStore(
    private val cipher: LocalCipher,
    private val blob: EncryptedBlobStore = FileEncryptedBlobStore("companion_checkin"),
) {
    fun load(): Instant? = try {
        blob.load()?.let { Instant.fromEpochMilliseconds(cipher.decrypt(it).decodeToString().toLong()) }
    } catch (t: Throwable) {
        null
    }

    fun save(at: Instant) {
        blob.save(cipher.encrypt(at.toEpochMilliseconds().toString().encodeToByteArray()))
    }
}

class CompanionCheckinWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        AspenLocalStorage.init(applicationContext)
        val cipher = platformLocalCipher()

        val prefs = PersistentCompanionPrefsStore(cipher, FileEncryptedBlobStore("companion_prefs")).current()
            ?: return Result.success()
        val timestamps = CheckinTimestampStore(cipher)
        val now = Clock.System.now()
        val localHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        if (!NotificationPolicy.shouldDeliver(prefs, timestamps.load(), now, localHour)) return Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success() // Opted in but permission since revoked: stay silent, never re-ask.
        }

        val config = AppConfigProvider(PersistentProfileStore(cipher, FileEncryptedBlobStore("profile"))).current()
        val dayVariant = (now.toEpochMilliseconds() / MILLIS_PER_DAY).toInt()
        val line = LibraryCompanionVoice().line(CompanionMoment.NOTIFICATION_PHRASING, config, variant = dayVariant)
        val key = NotificationPolicy.phrasingKeyOrNull(line) ?: return Result.success()

        postNotification(getString(companionLineResource(key)))
        timestamps.save(now)
        return Result.success()
    }

    private suspend fun postNotification(text: String) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(Res.string.notify_channel_name),
                NotificationManager.IMPORTANCE_LOW, // visible, silent — a hello, not an alert
            ).apply { description = getString(Res.string.notify_channel_description) }
            manager.createNotificationChannel(channel)
        }

        val openApp = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?.let { intent ->
                android.app.PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                )
            }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(applicationContext, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(applicationContext)
        }
        manager.notify(
            NOTIFICATION_ID,
            builder
                .setSmallIcon(app.aspen.companion.overlay.R.drawable.ic_overlay_companion)
                .setContentText(text)
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build(),
        )
    }

    private companion object {
        const val CHANNEL_ID = "aspen_companion_checkin"
        const val NOTIFICATION_ID = 502
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
