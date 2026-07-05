package com.example.samsungzh

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import java.text.DateFormat
import java.util.Date
import java.util.Locale

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
        configureSystemBars()
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
            setBackgroundColor(APP_BACKGROUND)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        applySystemBarPadding(root)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(APP_BACKGROUND)
            isFillViewport = true
            addView(root)
        }

        root.addView(buildHeroCard(), textLayoutParams())
        root.addView(buildPreviewCard(), textLayoutParams(topMargin = 14))
        root.addView(buildStyleCard(), textLayoutParams(topMargin = 14))
        root.addView(buildTimingCard(), textLayoutParams(topMargin = 14))
        root.addView(buildStatusCard(), textLayoutParams(topMargin = 14))
        setContentView(scrollView)
        root.requestApplyInsets()
    }

    private fun buildHeroCard(): LinearLayout {
        val card = card()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.flipwords_logo)
                contentDescription = getString(R.string.app_name)
                scaleType = ImageView.ScaleType.FIT_CENTER
            },
            LinearLayout.LayoutParams(dp(54), dp(54)),
        )
        header.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, 0, 0)
                addView(
                    TextView(this@MainActivity).apply {
                        text = getString(R.string.app_name)
                        setTextColor(APP_TEXT_PRIMARY)
                        textSize = 24f
                        typeface = Typeface.DEFAULT_BOLD
                        includeFontPadding = false
                    },
                )
                addView(
                    TextView(this@MainActivity).apply {
                        text = "Traditional Chinese on your Flip cover screen"
                        setTextColor(APP_TEXT_SECONDARY)
                        textSize = 15f
                        setPadding(0, dp(6), 0, 0)
                    },
                )
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        card.addView(header)

        hanziView = TextView(this).apply {
            setTextColor(APP_TEXT_PRIMARY)
            textSize = 82f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            setPadding(0, dp(24), 0, 0)
        }
        pinyinView = TextView(this).apply {
            setTextColor(APP_TEXT_SECONDARY)
            textSize = 26f
            includeFontPadding = false
            setPadding(0, dp(10), 0, 0)
        }
        englishView = TextView(this).apply {
            setTextColor(APP_ACCENT)
            textSize = 30f
            includeFontPadding = false
            setPadding(0, dp(14), 0, 0)
        }

        card.addView(hanziView)
        card.addView(pinyinView)
        card.addView(englishView)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(24), 0, 0)
        }
        actions.addView(
            actionButton(getString(R.string.overlay_start), primary = true).apply {
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
            },
            weightedButtonLayoutParams(),
        )
        actions.addView(
            actionButton(getString(R.string.refresh_word), primary = false),
            weightedButtonLayoutParams(startMargin = 10),
        )
        actions.getChildAt(1).setOnClickListener {
            repository.advanceWord()
            refreshOverlay()
            render()
        }
        card.addView(actions)

        val secondaryActions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        secondaryActions.addView(
            actionButton(getString(R.string.overlay_permission), primary = false).apply {
                setOnClickListener {
                    startActivity(CoverOverlayService.permissionSettingsIntent(this@MainActivity))
                }
            },
            textLayoutParams(),
        )
        secondaryActions.addView(
            actionButton(getString(R.string.overlay_stop), primary = false).apply {
                setOnClickListener {
                    overlayPreferences.overlayEnabled = false
                    startService(
                        Intent(this@MainActivity, CoverOverlayService::class.java)
                            .setAction(CoverOverlayActions.ACTION_STOP),
                    )
                    render()
                }
            },
            textLayoutParams(topMargin = 8),
        )
        card.addView(secondaryActions)

        return card
    }

    private fun buildPreviewCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(PREVIEW_BACKGROUND, radius = 22)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        card.addView(
            TextView(this).apply {
                text = "Overlay preview"
                setTextColor(Color.WHITE)
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
            },
        )

        previewHanziView = TextView(this).apply {
            includeFontPadding = false
            setPadding(0, dp(18), 0, 0)
        }
        previewPinyinView = TextView(this).apply {
            includeFontPadding = false
            setPadding(0, dp(6), 0, 0)
        }
        previewEnglishView = TextView(this).apply {
            includeFontPadding = false
            setPadding(0, dp(6), 0, 0)
        }
        card.addView(previewHanziView)
        card.addView(previewPinyinView)
        card.addView(previewEnglishView)
        return card
    }

    private fun buildStyleCard(): LinearLayout {
        val card = card()
        card.addView(sectionTextView("Text style"))
        addTextStyleControl(
            root = card,
            labelPrefix = "Hanzi",
            min = OverlayPreferences.MIN_HANZI_SIZE,
            max = OverlayPreferences.MAX_HANZI_SIZE,
            current = overlayPreferences.hanziSizeSp,
            currentColor = overlayPreferences.hanziColor,
            onSizeChanged = { overlayPreferences.hanziSizeSp = it },
            onColorChanged = { overlayPreferences.hanziColor = it },
        )
        addTextStyleControl(
            root = card,
            labelPrefix = "Pinyin",
            min = OverlayPreferences.MIN_PINYIN_SIZE,
            max = OverlayPreferences.MAX_PINYIN_SIZE,
            current = overlayPreferences.pinyinSizeSp,
            currentColor = overlayPreferences.pinyinColor,
            onSizeChanged = { overlayPreferences.pinyinSizeSp = it },
            onColorChanged = { overlayPreferences.pinyinColor = it },
        )
        addTextStyleControl(
            root = card,
            labelPrefix = "English",
            min = OverlayPreferences.MIN_ENGLISH_SIZE,
            max = OverlayPreferences.MAX_ENGLISH_SIZE,
            current = overlayPreferences.englishSizeSp,
            currentColor = overlayPreferences.englishColor,
            onSizeChanged = { overlayPreferences.englishSizeSp = it },
            onColorChanged = { overlayPreferences.englishColor = it },
        )
        return card
    }

    private fun buildTimingCard(): LinearLayout {
        val card = card()
        card.addView(sectionTextView("Word timing"))
        addIntervalControl(card)
        return card
    }

    private fun buildStatusCard(): LinearLayout {
        val card = card()
        card.addView(sectionTextView("Device status"))
        permissionStatusView = infoTextView()
        rotationStatusView = infoTextView()
        displayStatusView = infoTextView()
        card.addView(permissionStatusView, textLayoutParams(topMargin = 12))
        card.addView(rotationStatusView, textLayoutParams(topMargin = 10))
        card.addView(displayStatusView, textLayoutParams(topMargin = 10))
        return card
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
        previewHanziView.setTextColor(overlayPreferences.hanziColor)
        previewPinyinView.setTextColor(overlayPreferences.pinyinColor)
        previewEnglishView.setTextColor(overlayPreferences.englishColor)
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

    private fun addTextStyleControl(
        root: LinearLayout,
        labelPrefix: String,
        min: Int,
        max: Int,
        current: Int,
        currentColor: Int,
        onSizeChanged: (Int) -> Unit,
        onColorChanged: (Int) -> Unit,
    ) {
        val label = infoTextView()
        val colorButton = swatchView(currentColor, sizeDp = 34).apply {
            contentDescription = "Choose $labelPrefix color"
            isClickable = true
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, 0)
            addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(colorButton)
        }
        val palette = buildPalette(
            selectedColor = currentColor,
            onSelected = { color ->
                onColorChanged(color)
                colorButton.background = swatchBackground(color, selected = true)
                refreshOverlay()
                render()
            },
        )
        palette.visibility = View.GONE
        colorButton.setOnClickListener {
            palette.visibility = if (palette.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val seekBar = SeekBar(this).apply {
            this.max = max - min
            progress = current - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = min + progress
                    label.text = "$labelPrefix size: ${value}sp"
                    if (fromUser) {
                        onSizeChanged(value)
                        refreshOverlay()
                        render()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }

        label.text = "$labelPrefix size: ${current}sp"
        root.addView(header)
        root.addView(seekBar, textLayoutParams(topMargin = 4))
        root.addView(palette, textLayoutParams(topMargin = 8))
    }

    private fun buildPalette(selectedColor: Int, onSelected: (Int) -> Unit): LinearLayout {
        val palette = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(2))
        }
        palette.addView(
            TextView(this).apply {
                text = "Saved colors"
                setTextColor(APP_TEXT_SECONDARY)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
            },
        )
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        COLOR_PALETTE.forEach { color ->
            row.addView(
                swatchView(color, sizeDp = 30).apply {
                    isClickable = true
                    contentDescription = "Use color ${colorToHex(color)}"
                    background = swatchBackground(color, selected = color == selectedColor)
                    setOnClickListener {
                        onSelected(color)
                        for (index in 0 until row.childCount) {
                            val child = row.getChildAt(index)
                            val childColor = COLOR_PALETTE[index]
                            child.background = swatchBackground(childColor, selected = childColor == color)
                        }
                    }
                },
                LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                    marginEnd = dp(10)
                },
            )
        }
        palette.addView(
            HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(row)
            },
        )
        return palette
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
        root.addView(label, textLayoutParams(topMargin = 14))
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

    private fun card(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(APP_SURFACE, radius = 22)
            elevation = dp(1).toFloat()
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

    private fun actionButton(text: String, primary: Boolean): TextView =
        TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            setTextColor(if (primary) Color.WHITE else APP_TEXT_PRIMARY)
            background = roundedBackground(if (primary) APP_ACCENT else APP_MUTED_SURFACE, radius = 16)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            minHeight = dp(42)
            minWidth = 0
        }

    private fun infoTextView(): TextView =
        TextView(this).apply {
            setTextColor(APP_TEXT_SECONDARY)
            textSize = 15f
            includeFontPadding = true
        }

    private fun sectionTextView(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(APP_TEXT_PRIMARY)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = true
        }

    private fun swatchView(color: Int, sizeDp: Int): TextView =
        TextView(this).apply {
            background = swatchBackground(color, selected = true)
            minWidth = dp(sizeDp)
            minHeight = dp(sizeDp)
        }

    private fun swatchBackground(color: Int, selected: Boolean): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dp(if (selected) 3 else 1), if (selected) APP_ACCENT else APP_BORDER)
        }

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radius).toFloat()
            setColor(color)
        }

    private fun weightedButtonLayoutParams(startMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f,
        ).apply {
            marginStart = dp(startMargin)
        }

    private fun textLayoutParams(topMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            this.topMargin = dp(topMargin)
        }

    private fun colorToHex(color: Int): String =
        String.format(Locale.US, "#%06X", 0xFFFFFF and color)

    private fun configureSystemBars() {
        window.statusBarColor = APP_BACKGROUND
        window.navigationBarColor = APP_BACKGROUND
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.decorView.systemUiVisibility = flags
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
    }

    @Suppress("DEPRECATION")
    private fun applySystemBarPadding(root: View) {
        val baseLeft = dp(18)
        val baseTop = dp(18)
        val baseRight = dp(18)
        val baseBottom = dp(24)
        root.setPadding(baseLeft, baseTop, baseRight, baseBottom)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val topInset: Int
            val bottomInset: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                topInset = bars.top
                bottomInset = bars.bottom
            } else {
                topInset = insets.systemWindowInsetTop
                bottomInset = insets.systemWindowInsetBottom
            }
            view.setPadding(
                baseLeft,
                baseTop + topInset,
                baseRight,
                baseBottom + bottomInset,
            )
            insets
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 42

        private val APP_BACKGROUND = Color.parseColor("#F5F6F8")
        private val APP_SURFACE = Color.WHITE
        private val APP_MUTED_SURFACE = Color.parseColor("#EEF1F5")
        private val PREVIEW_BACKGROUND = Color.parseColor("#111214")
        private val APP_TEXT_PRIMARY = Color.parseColor("#20242A")
        private val APP_TEXT_SECONDARY = Color.parseColor("#66717F")
        private val APP_BORDER = Color.parseColor("#D6DCE3")
        private val APP_ACCENT = Color.parseColor("#7F56D9")

        private val COLOR_PALETTE = intArrayOf(
            OverlayPreferences.DEFAULT_HANZI_COLOR,
            OverlayPreferences.DEFAULT_PINYIN_COLOR,
            OverlayPreferences.DEFAULT_ENGLISH_COLOR,
            Color.WHITE,
            Color.parseColor("#20242A"),
            Color.parseColor("#7F56D9"),
            Color.parseColor("#2F80ED"),
            Color.parseColor("#12B76A"),
            Color.parseColor("#F97316"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#EC4899"),
        )
    }
}
