package com.example.samsungzh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.Layout
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
    private var screenReceiverRegistered = false

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

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT,
                Intent.ACTION_DREAMING_STOPPED -> reattachOverlay()

                Intent.ACTION_SCREEN_OFF -> {
                    if (overlayPreferences.autoHideEnabled) {
                        hideOverlayForAutoHide()
                    }
                }
            }
        }
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshOverlayAndNotification()
            scheduleNextRefresh()
        }
    }

    private val autoHideRunnable = Runnable {
        hideOverlayForAutoHide()
    }

    override fun onCreate() {
        super.onCreate()
        repository = WordRepository(this)
        overlayPreferences = OverlayPreferences(this)
        displayManager = getSystemService(DisplayManager::class.java)
        createNotificationChannel()
        displayManager.registerDisplayListener(displayListener, handler)
        registerScreenReceiver()
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
        handler.removeCallbacks(autoHideRunnable)
        displayManager.unregisterDisplayListener(displayListener)
        unregisterScreenReceiver()
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
            scheduleAutoHideIfNeeded()
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun buildOverlayView(context: Context): View {
        val maxTextWidth = overlayTextMaxWidth(context)
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
            overlayTextView(
                context = context,
                id = R.id.overlay_hanzi,
                textSizeSp = overlayPreferences.hanziSizeSp,
                textColor = overlayPreferences.hanziColor,
                maxTextWidth = maxTextWidth,
                typeface = Typeface.create(SERIF_FAMILY, Typeface.BOLD),
            ),
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        root.addView(
            overlayTextView(
                context = context,
                id = R.id.overlay_pinyin,
                textSizeSp = overlayPreferences.pinyinSizeSp,
                textColor = overlayPreferences.pinyinColor,
                maxTextWidth = maxTextWidth,
                topPadding = 3,
                typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL),
            ),
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        root.addView(
            overlayTextView(
                context = context,
                id = R.id.overlay_english,
                textSizeSp = overlayPreferences.englishSizeSp,
                textColor = overlayPreferences.englishColor,
                maxTextWidth = maxTextWidth,
                topPadding = 3,
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD),
            ),
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )

        return root
    }

    private fun overlayTextView(
        context: Context,
        id: Int,
        textSizeSp: Int,
        textColor: Int,
        maxTextWidth: Int,
        topPadding: Int = 0,
        typeface: Typeface,
    ): TextView =
        TextView(context).apply {
            this.id = id
            setTextColor(textColor)
            textSize = textSizeSp.toFloat()
            this.typeface = typeface
            includeFontPadding = false
            maxWidth = maxTextWidth
            setHorizontallyScrolling(false)
            isSingleLine = false
            breakStrategy = Layout.BREAK_STRATEGY_BALANCED
            hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
            if (topPadding > 0) {
                setPadding(0, dp(context, topPadding), 0, 0)
            }
        }

    private fun refreshOverlayAndNotification() {
        val phrase = currentOverlayPhrase()
        renderOverlayPhrase(phrase)
        applyOverlayTextStyles()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(phrase))
    }

    private fun currentOverlayPhrase(): OverlayPhrase {
        val word = repository.currentWord()
        return OverlayPhrase(
            hanzi = word.hanzi,
            pinyin = PinyinToneFormatter.format(word),
            english = word.english,
        )
    }

    private fun renderOverlayPhrase(phrase: OverlayPhrase) {
        overlayView?.findViewById<TextView>(R.id.overlay_hanzi)?.text = phrase.hanzi
        overlayView?.findViewById<TextView>(R.id.overlay_pinyin)?.text = phrase.pinyinDisplay
        overlayView?.findViewById<TextView>(R.id.overlay_english)?.text = phrase.english
    }

    private fun scheduleNextRefresh() {
        val delayMillis = (repository.rotationInfo().nextRotationMillis - System.currentTimeMillis())
            .coerceAtLeast(MIN_REFRESH_DELAY_MILLIS) + REFRESH_GRACE_MILLIS
        handler.postDelayed(refreshRunnable, delayMillis)
    }

    private fun applyOverlayTextStyles() {
        val maxTextWidth = overlayView?.context?.let { overlayTextMaxWidth(it) }
        overlayView?.findViewById<TextView>(R.id.overlay_hanzi)?.apply {
            textSize = overlayPreferences.hanziSizeSp.toFloat()
            setTextColor(overlayPreferences.hanziColor)
            if (maxTextWidth != null) maxWidth = maxTextWidth
        }
        overlayView?.findViewById<TextView>(R.id.overlay_pinyin)?.apply {
            textSize = overlayPreferences.pinyinSizeSp.toFloat()
            setTextColor(overlayPreferences.pinyinColor)
            if (maxTextWidth != null) maxWidth = maxTextWidth
        }
        overlayView?.findViewById<TextView>(R.id.overlay_english)?.apply {
            textSize = overlayPreferences.englishSizeSp.toFloat()
            setTextColor(overlayPreferences.englishColor)
            if (maxTextWidth != null) maxWidth = maxTextWidth
        }
    }

    private fun stopOverlay() {
        handler.removeCallbacks(autoHideRunnable)
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

    private fun hideOverlayForAutoHide() {
        val hadOverlay = overlayView != null
        val hideSeconds = overlayPreferences.autoHideSeconds
        stopOverlay()
        if (!hadOverlay) return

        displayLabel = "auto-hidden"
        overlayPreferences.overlayStatus =
            "Overlay status: auto-hidden after ${hideSeconds}s. Notification fallback active."
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun scheduleAutoHideIfNeeded() {
        handler.removeCallbacks(autoHideRunnable)
        if (overlayPreferences.autoHideEnabled) {
            handler.postDelayed(autoHideRunnable, overlayPreferences.autoHideMillis)
        }
    }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_DREAMING_STOPPED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenReceiver, filter)
        }
        screenReceiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return

        try {
            unregisterReceiver(screenReceiver)
        } catch (_: RuntimeException) {
            // The service can be torn down while Android is also cleaning up receivers.
        }
        screenReceiverRegistered = false
    }

    private fun preferredCoverDisplay(): Display? {
        return displayManager.displays.firstOrNull { it.displayId == COVER_DISPLAY_ID }
            ?: displayManager.displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
    }

    private fun buildNotification(phrase: OverlayPhrase = currentOverlayPhrase()): Notification {
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
            .setContentTitle("${phrase.hanzi} ${phrase.pinyinDisplay}")
            .setContentText("${phrase.english} - $displayLabel")
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

    private fun overlayTextMaxWidth(context: Context): Int =
        (context.resources.displayMetrics.widthPixels - dp(context, 32)).coerceAtLeast(dp(context, 180))

    private data class OverlayPhrase(
        val hanzi: String,
        val pinyin: String,
        val english: String,
    ) {
        val pinyinDisplay: String = "[$pinyin]"
    }

    companion object {
        private const val CHANNEL_ID = "cover_word_overlay"
        private const val NOTIFICATION_ID = 2106
        private const val COVER_DISPLAY_ID = 1
        private const val MIN_REFRESH_DELAY_MILLIS = 1_000L
        private const val REFRESH_GRACE_MILLIS = 250L
        private const val SERIF_FAMILY = "Noto Serif TC"
        private const val SANS_FAMILY = "sans-serif"

        fun permissionSettingsIntent(context: Context): Intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
    }
}
