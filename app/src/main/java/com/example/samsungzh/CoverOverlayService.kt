package com.example.samsungzh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class CoverOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repository: WordRepository
    private lateinit var overlayPreferences: OverlayPreferences
    private lateinit var displayManager: DisplayManager
    private var overlayView: View? = null
    private var overlayWindowManager: WindowManager? = null
    private var displayLabel: String = "not attached"

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            reattachOverlay()
        }

        override fun onDisplayRemoved(displayId: Int) {
            reattachOverlay()
        }

        override fun onDisplayChanged(displayId: Int) {
            reattachOverlay()
        }
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshOverlayAndNotification()
            scheduleNextRefresh()
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = WordRepository(this)
        overlayPreferences = OverlayPreferences(this)
        displayManager = getSystemService(DisplayManager::class.java)
        createNotificationChannel()
        displayManager.registerDisplayListener(displayListener, handler)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            CoverOverlayActions.ACTION_STOP -> {
                overlayPreferences.overlayEnabled = false
                stopOverlay()
                stopSelf()
                return START_NOT_STICKY
            }

            CoverOverlayActions.ACTION_START -> {
                overlayPreferences.overlayEnabled = true
            }

            CoverOverlayActions.ACTION_NEXT_WORD -> {
                repository.advanceWord()
            }

            CoverOverlayActions.ACTION_REFRESH -> {
                if (!overlayPreferences.overlayEnabled) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            overlayPreferences.overlayStatus = "Overlay status: permission missing"
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        showOverlay()
        refreshOverlayAndNotification()
        handler.removeCallbacks(refreshRunnable)
        scheduleNextRefresh()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshRunnable)
        displayManager.unregisterDisplayListener(displayListener)
        stopOverlay()
        overlayPreferences.overlayStatus = OverlayPreferences.STATUS_IDLE
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        if (overlayView != null) return

        val preferredDisplay = preferredCoverDisplay()
        if (preferredDisplay == null) {
            overlayPreferences.overlayStatus = OverlayPreferences.STATUS_WAITING
            displayLabel = "waiting for cover display"
            return
        }

        if (!tryAttachOverlay(preferredDisplay)) {
            overlayPreferences.overlayStatus =
                "Overlay status: cover display found but attachment was blocked. Main display fallback disabled."
            displayLabel = "cover display blocked"
        }
    }

    private fun reattachOverlay() {
        stopOverlay()
        showOverlay()
        refreshOverlayAndNotification()
    }

    private fun tryAttachOverlay(display: Display?): Boolean {
        if (display == null || display.displayId == Display.DEFAULT_DISPLAY) return false

        val displayContext = createDisplayContext(display)
        val windowManager = displayContext.getSystemService(WindowManager::class.java)
        val view = buildOverlayView(displayContext)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(displayContext, 12)
            y = dp(displayContext, 12)
        }

        return try {
            windowManager.addView(view, params)
            overlayWindowManager = windowManager
            overlayView = view
            displayLabel = "display ${display.displayId}: ${display.name}"
            overlayPreferences.overlayStatus =
                "Overlay status: attached to $displayLabel. Main display fallback disabled."
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun buildOverlayView(context: Context): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4))
            isClickable = true
            setOnClickListener {
                repository.advanceWord()
                refreshOverlayAndNotification()
            }
        }

        root.addView(
            TextView(context).apply {
                id = R.id.overlay_hanzi
                setTextColor(overlayPreferences.hanziColor)
                textSize = overlayPreferences.hanziSizeSp.toFloat()
                includeFontPadding = false
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        root.addView(
            TextView(context).apply {
                id = R.id.overlay_pinyin
                setTextColor(overlayPreferences.pinyinColor)
                textSize = overlayPreferences.pinyinSizeSp.toFloat()
                includeFontPadding = false
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        root.addView(
            TextView(context).apply {
                id = R.id.overlay_english
                setTextColor(overlayPreferences.englishColor)
                textSize = overlayPreferences.englishSizeSp.toFloat()
                includeFontPadding = false
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )

        return root
    }

    private fun refreshOverlayAndNotification() {
        val word = repository.currentWord()
        val pinyin = PinyinToneFormatter.format(word)
        overlayView?.findViewById<TextView>(R.id.overlay_hanzi)?.text = word.hanzi
        overlayView?.findViewById<TextView>(R.id.overlay_pinyin)?.text = "[$pinyin]"
        overlayView?.findViewById<TextView>(R.id.overlay_english)?.text = word.english
        applyOverlayTextStyles()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun scheduleNextRefresh() {
        val delayMillis = (repository.rotationInfo().nextRotationMillis - System.currentTimeMillis())
            .coerceAtLeast(MIN_REFRESH_DELAY_MILLIS) + REFRESH_GRACE_MILLIS
        handler.postDelayed(refreshRunnable, delayMillis)
    }

    private fun applyOverlayTextStyles() {
        overlayView?.findViewById<TextView>(R.id.overlay_hanzi)?.apply {
            textSize = overlayPreferences.hanziSizeSp.toFloat()
            setTextColor(overlayPreferences.hanziColor)
        }
        overlayView?.findViewById<TextView>(R.id.overlay_pinyin)?.apply {
            textSize = overlayPreferences.pinyinSizeSp.toFloat()
            setTextColor(overlayPreferences.pinyinColor)
        }
        overlayView?.findViewById<TextView>(R.id.overlay_english)?.apply {
            textSize = overlayPreferences.englishSizeSp.toFloat()
            setTextColor(overlayPreferences.englishColor)
        }
    }

    private fun stopOverlay() {
        val view = overlayView ?: return
        try {
            overlayWindowManager?.removeView(view)
        } catch (_: RuntimeException) {
            // The system may already have detached the overlay during display changes.
        }
        overlayView = null
        overlayWindowManager = null
        displayLabel = "not attached"
    }

    private fun preferredCoverDisplay(): Display? {
        return displayManager.displays.firstOrNull { it.displayId == COVER_DISPLAY_ID }
            ?: displayManager.displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
    }

    private fun buildNotification(): Notification {
        val word = repository.currentWord()
        val pinyin = PinyinToneFormatter.format(word)
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val nextIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CoverOverlayService::class.java).setAction(CoverOverlayActions.ACTION_NEXT_WORD),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, CoverOverlayService::class.java).setAction(CoverOverlayActions.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("${word.hanzi} [$pinyin]")
            .setContentText("${word.english} - $displayLabel")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_launcher, getString(R.string.refresh_word), nextIntent)
            .addAction(R.drawable.ic_launcher, getString(R.string.overlay_stop), stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps the Chinese word overlay running."
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL_ID = "cover_word_overlay"
        private const val NOTIFICATION_ID = 2106
        private const val COVER_DISPLAY_ID = 1
        private const val MIN_REFRESH_DELAY_MILLIS = 1_000L
        private const val REFRESH_GRACE_MILLIS = 250L

        fun permissionSettingsIntent(context: Context): Intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
    }
}
