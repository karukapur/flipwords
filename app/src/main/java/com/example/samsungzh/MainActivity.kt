package com.example.samsungzh

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.Display
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private lateinit var repository: WordRepository
    private lateinit var overlayPreferences: OverlayPreferences
    private lateinit var hanziView: TextView
    private lateinit var pinyinView: TextView
    private lateinit var englishView: TextView
    private lateinit var previewHanziView: TextView
    private lateinit var previewPinyinView: TextView
    private lateinit var previewEnglishView: TextView
    private lateinit var permissionStatusView: TextView
    private lateinit var rotationStatusView: TextView
    private lateinit var displayStatusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = WordRepository(this)
        overlayPreferences = OverlayPreferences(this)
        WordUpdateScheduler.schedule(this)
        buildLayout()
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::permissionStatusView.isInitialized) {
            render()
        }
    }

    private fun buildLayout() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START or Gravity.TOP
            setBackgroundColor(getColor(R.color.background))
            setPadding(dp(24), dp(26), dp(24), dp(24))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.background))
            addView(root)
        }

        hanziView = TextView(this).apply {
            setTextColor(getColor(R.color.text_primary))
            textSize = 86f
            includeFontPadding = false
        }

        pinyinView = TextView(this).apply {
            setTextColor(getColor(R.color.text_secondary))
            textSize = 28f
            includeFontPadding = false
            setPadding(0, dp(12), 0, 0)
        }

        englishView = TextView(this).apply {
            setTextColor(getColor(R.color.accent))
            textSize = 32f
            includeFontPadding = false
            setPadding(0, dp(18), 0, 0)
        }

        val nextButton = Button(this).apply {
            text = getString(R.string.refresh_word)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener {
                repository.advanceWord()
                refreshOverlay()
                render()
            }
        }

        val permissionButton = Button(this).apply {
            text = getString(R.string.overlay_permission)
            setOnClickListener {
                startActivity(CoverOverlayService.permissionSettingsIntent(this@MainActivity))
            }
        }

        val startOverlayButton = Button(this).apply {
            text = getString(R.string.overlay_start)
            setOnClickListener {
                maybeRequestNotificationPermission()
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startForegroundService(
                        Intent(this@MainActivity, CoverOverlayService::class.java)
                            .setAction(CoverOverlayActions.ACTION_START),
                    )
                } else {
                    startActivity(CoverOverlayService.permissionSettingsIntent(this@MainActivity))
                }
                render()
            }
        }

        val stopOverlayButton = Button(this).apply {
            text = getString(R.string.overlay_stop)
            setOnClickListener {
                overlayPreferences.overlayEnabled = false
                startService(
                    Intent(this@MainActivity, CoverOverlayService::class.java)
                        .setAction(CoverOverlayActions.ACTION_STOP),
                )
                render()
            }
        }

        permissionStatusView = infoTextView()
        rotationStatusView = infoTextView()
        displayStatusView = infoTextView()
        previewHanziView = TextView(this).apply {
            setTextColor(getColor(R.color.text_primary))
            includeFontPadding = false
        }
        previewPinyinView = TextView(this).apply {
            setTextColor(getColor(R.color.text_secondary))
            includeFontPadding = false
        }
        previewEnglishView = TextView(this).apply {
            setTextColor(getColor(R.color.accent))
            includeFontPadding = false
        }

        root.addView(hanziView)
        root.addView(pinyinView)
        root.addView(englishView)
        root.addView(
            nextButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(32)
            },
        )
        root.addView(permissionButton, buttonLayoutParams(topMargin = 18))
        root.addView(startOverlayButton, buttonLayoutParams(topMargin = 8))
        root.addView(stopOverlayButton, buttonLayoutParams(topMargin = 8))
        root.addView(sectionTextView("Overlay preview"), textLayoutParams(topMargin = 24))
        root.addView(previewHanziView, textLayoutParams(topMargin = 8))
        root.addView(previewPinyinView, textLayoutParams(topMargin = 4))
        root.addView(previewEnglishView, textLayoutParams(topMargin = 4))
        addSizeControl(
            root = root,
            labelPrefix = "Hanzi size",
            min = OverlayPreferences.MIN_HANZI_SIZE,
            max = OverlayPreferences.MAX_HANZI_SIZE,
            current = overlayPreferences.hanziSizeSp,
        ) { overlayPreferences.hanziSizeSp = it }
        addSizeControl(
            root = root,
            labelPrefix = "Pinyin size",
            min = OverlayPreferences.MIN_PINYIN_SIZE,
            max = OverlayPreferences.MAX_PINYIN_SIZE,
            current = overlayPreferences.pinyinSizeSp,
        ) { overlayPreferences.pinyinSizeSp = it }
        addSizeControl(
            root = root,
            labelPrefix = "English size",
            min = OverlayPreferences.MIN_ENGLISH_SIZE,
            max = OverlayPreferences.MAX_ENGLISH_SIZE,
            current = overlayPreferences.englishSizeSp,
        ) { overlayPreferences.englishSizeSp = it }
        addIntervalControl(root)
        root.addView(permissionStatusView, textLayoutParams(topMargin = 22))
        root.addView(rotationStatusView, textLayoutParams(topMargin = 12))
        root.addView(displayStatusView, textLayoutParams(topMargin = 12))
        setContentView(scrollView)
    }

    private fun render() {
        val info = repository.rotationInfo()
        val word = info.currentWord
        hanziView.text = word.hanzi
        pinyinView.text = "[${word.pinyin}]"
        englishView.text = word.english
        previewHanziView.text = word.hanzi
        previewPinyinView.text = "[${word.pinyin}]"
        previewEnglishView.text = word.english
        previewHanziView.textSize = overlayPreferences.hanziSizeSp.toFloat()
        previewPinyinView.textSize = overlayPreferences.pinyinSizeSp.toFloat()
        previewEnglishView.textSize = overlayPreferences.englishSizeSp.toFloat()
        permissionStatusView.text = overlayPermissionText()
        rotationStatusView.text =
            "Interval: ${formatInterval(info.intervalMillis / 1000L)}\nNext rotation: ${
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(info.nextRotationMillis))
            }"
        displayStatusView.text = displayDebugText()
    }

    private fun overlayPermissionText(): String =
        if (Settings.canDrawOverlays(this)) {
            "Overlay permission: granted"
        } else {
            "Overlay permission: not granted"
        }

    private fun displayDebugText(): String {
        val displayManager = getSystemService(DisplayManager::class.java)
        val displays = displayManager.displays.joinToString(separator = "\n") { display ->
            val marker = when (display.displayId) {
                Display.DEFAULT_DISPLAY -> "main"
                1 -> "cover candidate"
                else -> "secondary"
            }
            "Display ${display.displayId} ($marker): ${display.name}"
        }
        return "${overlayPreferences.overlayStatus}\nMain display fallback disabled.\nDetected displays:\n$displays"
    }

    private fun addSizeControl(
        root: LinearLayout,
        labelPrefix: String,
        min: Int,
        max: Int,
        current: Int,
        onValueChanged: (Int) -> Unit,
    ) {
        val label = infoTextView()
        val seekBar = SeekBar(this).apply {
            this.max = max - min
            progress = current - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = min + progress
                    label.text = "$labelPrefix: ${value}sp"
                    if (fromUser) {
                        onValueChanged(value)
                        refreshOverlay()
                        render()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }

        label.text = "$labelPrefix: ${current}sp"
        root.addView(label, textLayoutParams(topMargin = 16))
        root.addView(seekBar, textLayoutParams(topMargin = 4))
    }

    private fun addIntervalControl(root: LinearLayout) {
        val label = infoTextView()
        val min = OverlayPreferences.MIN_ROTATION_INTERVAL_SECONDS
        val max = OverlayPreferences.MAX_ROTATION_INTERVAL_SECONDS
        val step = OverlayPreferences.ROTATION_INTERVAL_STEP_SECONDS
        val seekBar = SeekBar(this).apply {
            this.max = (max - min) / step
            progress = (overlayPreferences.rotationIntervalSeconds - min) / step
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = min + (progress * step)
                    label.text = "Word change frequency: ${formatInterval(value.toLong())}"
                    if (fromUser) {
                        val currentWord = repository.currentWord()
                        overlayPreferences.rotationIntervalSeconds = value
                        repository.pinWord(currentWord)
                        WordUpdateScheduler.schedule(this@MainActivity)
                        refreshOverlay()
                        render()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }

        label.text = "Word change frequency: ${formatInterval(overlayPreferences.rotationIntervalSeconds.toLong())}"
        root.addView(sectionTextView("Word timing"), textLayoutParams(topMargin = 24))
        root.addView(label, textLayoutParams(topMargin = 8))
        root.addView(seekBar, textLayoutParams(topMargin = 4))
    }

    private fun refreshOverlay() {
        if (overlayPreferences.overlayEnabled) {
            startService(
                Intent(this, CoverOverlayService::class.java)
                    .setAction(CoverOverlayActions.ACTION_REFRESH),
            )
        }
    }

    private fun formatInterval(totalSeconds: Long): String {
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return when {
            minutes == 0L -> "${seconds}s"
            seconds == 0L -> "${minutes}m"
            else -> "${minutes}m ${seconds}s"
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_POST_NOTIFICATIONS)
        }
    }

    private fun infoTextView(): TextView =
        TextView(this).apply {
            setTextColor(getColor(R.color.text_secondary))
            textSize = 15f
            includeFontPadding = true
        }

    private fun sectionTextView(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(getColor(R.color.text_primary))
            textSize = 18f
            includeFontPadding = true
        }

    private fun buttonLayoutParams(topMargin: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            this.topMargin = dp(topMargin)
        }

    private fun textLayoutParams(topMargin: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            this.topMargin = dp(topMargin)
        }

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 42
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
