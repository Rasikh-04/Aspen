package app.aspen.companion.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowInsetsCompat
import app.aspen.data.local.AspenLocalStorage
import app.aspen.data.local.FileEncryptedBlobStore
import app.aspen.data.local.platformLocalCipher
import app.aspen.data.companion.PersistentCompanionPrefsStore
import app.aspen.ui.companion.CompanionController
import kotlin.math.roundToInt

/**
 * The Android-only system-overlay companion (docs/05 §6): a foreground service that adds one small
 * `ComposeView` to the `WindowManager`. Overlay-ONLY capabilities — `SYSTEM_ALERT_WINDOW`, never
 * AccessibilityService; it cannot see, read, or record anything on screen.
 *
 * Lifecycle honesty (docs/04 §6 "OEM reality"): if the OS or an aggressive OEM kills this service
 * the companion just disappears — nothing else in Aspen breaks, and re-enabling in Settings brings
 * it back ([START_NOT_STICKY]: we never fight the system to resurrect a decoration).
 *
 * All presence RULES live in the shared domain machine (the same [CompanionController] the in-app
 * layer uses): off-by-default, dismissed-stays-dismissed, fullscreen suspend, reduced-motion still.
 */
class CompanionOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "aspen_companion_overlay"
        private const val NOTIFICATION_ID = 501

        fun isPermissionGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

        /** Starts the overlay if (and only if) the permission is granted; otherwise a no-op. */
        fun start(context: Context) {
            if (!isPermissionGranted(context)) return
            val intent = Intent(context, CompanionOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CompanionOverlayService::class.java))
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val owner = OverlayLifecycleOwner()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // The permission can be revoked from system settings at any time; degrade silently.
        if (!isPermissionGranted(this)) {
            stopSelf()
            return
        }
        startInForeground()
        AspenLocalStorage.init(applicationContext)

        val prefs = PersistentCompanionPrefsStore(platformLocalCipher(), FileEncryptedBlobStore("companion_prefs"))
        val current = prefs.current()
        // Belt-and-braces: presence is opt-in twice over (docs/05 §3.1) — no prefs, companion off,
        // or overlay off all mean this service has no business running.
        if (current == null || !current.enabled || !current.overlayEnabled) {
            stopSelf()
            return
        }

        addOverlay(CompanionController(prefs))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        overlayView?.let { view ->
            owner.onDestroy()
            windowManager?.removeView(view)
        }
        overlayView = null
        super.onDestroy()
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_MIN, // silent, no sound, minimised — quiet by design
            ).apply { description = getString(R.string.overlay_channel_description) }
            manager.createNotificationChannel(channel)
        }

        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notification = builder
            .setSmallIcon(R.drawable.ic_overlay_companion)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_body))
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun addOverlay(controller: CompanionController) {
        val wm = getSystemService(WindowManager::class.java)
        windowManager = wm

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Never focusable: the companion can never intercept typing or block interaction
            // with whatever is underneath except its own small bounds (docs/05 §3.1).
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 0
            y = 0
        }

        val view = ComposeView(this).also { composeView ->
            owner.attachTo(composeView)
            owner.onCreate()
            composeView.setContent {
                OverlayCompanionContent(
                    controller = controller,
                    onMoveBy = { dx, dy ->
                        // Gravity is BOTTOM|END, so window x/y grow toward start/top — invert.
                        params.x = (params.x - dx.roundToInt()).coerceAtLeast(0)
                        params.y = (params.y - dy.roundToInt()).coerceAtLeast(0)
                        overlayView?.let { wm.updateViewLayout(it, params) }
                    },
                    onDismissed = { stopSelf() },
                )
            }
            composeView.setOnApplyWindowInsetsListener { v, insets ->
                val compat = WindowInsetsCompat.toWindowInsetsCompat(insets, v)
                controller.on(FullscreenSignals.eventFor(compat.isVisible(WindowInsetsCompat.Type.statusBars())))
                insets
            }
        }

        overlayView = view
        wm.addView(view, params)
    }
}
