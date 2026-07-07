package com.example.samsungzh

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.MotionEvent
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repository: WordRepository
    private lateinit var overlayPreferences: OverlayPreferences
    private lateinit var aiLabPreferences: AiLabPreferences
    private lateinit var aiModelManager: AiModelManager
    private lateinit var hanziView: TextView
    private lateinit var pinyinView: TextView
    private lateinit var englishView: TextView
    private lateinit var previewHanziView: TextView
    private lateinit var previewPinyinView: TextView
    private lateinit var previewEnglishView: TextView
    private lateinit var overlayToggleButton: TextView
    private lateinit var overlayPermissionButton: TextView
    private lateinit var overlayPermissionContainer: LinearLayout
    private lateinit var overlayStatusView: TextView
    private lateinit var nextWordMetricView: TextView
    private lateinit var bucketMetricView: TextView
    private lateinit var frequencyMetricView: TextView
    private lateinit var intervalStatusView: TextView
    private lateinit var intervalChangeButton: TextView
    private lateinit var autoHideStatusView: TextView
    private lateinit var autoHideDurationButton: TextView
    private lateinit var autoHideSwitch: Switch
    private lateinit var permissionStatusView: TextView
    private lateinit var rotationStatusView: TextView
    private lateinit var displayStatusView: TextView
    private lateinit var aiModelStatusView: TextView
    private lateinit var aiGeneratedStatusView: TextView
    private lateinit var aiScheduleStatusView: TextView
    private lateinit var aiSourceStatusView: TextView
    private lateinit var aiDownloadProgressBar: ProgressBar
    private lateinit var aiDownloadProgressView: TextView
    private lateinit var aiGenerationProgressBar: ProgressBar
    private lateinit var aiGenerationStatusView: TextView
    private lateinit var aiModelActionButton: TextView
    private lateinit var aiDeleteModelButton: TextView
    private lateinit var aiGenerateButton: TextView
    private lateinit var aiTimeButton: TextView
    private lateinit var aiAlarmPermissionButton: TextView
    private lateinit var aiDailySwitch: Switch
    private lateinit var aiSourceButton: TextView
    private lateinit var aiHskButton: TextView
    private lateinit var aiGenerationCountButton: TextView
    private lateinit var aiModelPillView: TextView
    private lateinit var aiPackPillView: TextView
    private lateinit var learnSection: LinearLayout
    private lateinit var styleSection: LinearLayout
    private lateinit var aiLabSection: LinearLayout
    private lateinit var deviceSection: LinearLayout
    private val tabButtons = mutableMapOf<AppTab, TextView>()
    private var activeTab = AppTab.LEARN
    private var updatingUi = false
    private var aiFailurePromptShowing = false
    private var lastRenderedHanzi: String? = null

    private val downloadProgressRunnable = object : Runnable {
        override fun run() {
            if (::aiDownloadProgressBar.isInitialized) {
                render()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = WordRepository(this)
        overlayPreferences = OverlayPreferences(this)
        aiLabPreferences = AiLabPreferences(this)
        aiModelManager = AiModelManager(this)
        WordUpdateScheduler.schedule(this)
        configureSystemBars()
        buildLayout()
        render()
        maybePromptForAiFailureLog()
    }

    override fun onResume() {
        super.onResume()
        if (::permissionStatusView.isInitialized) {
            render()
            maybePromptForAiFailureLog()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(downloadProgressRunnable)
        super.onDestroy()
    }

    private fun buildLayout() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(APP_BACKGROUND)
            clipChildren = false
            clipToPadding = false
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        applySystemBarPadding(root)

        val contentShell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = true
            clipToPadding = true
        }
        val contentRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(0, dp(18), 0, dp(112))
        }
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(APP_BACKGROUND)
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            addView(contentRoot)
        }

        contentShell.addView(buildTopAppBar(), textLayoutParams())

        learnSection = sectionContainer().apply {
            addView(buildHeroCard(), textLayoutParams())
        }
        styleSection = sectionContainer().apply {
            addView(buildPreviewCard(), textLayoutParams())
            addView(buildStyleCard(), textLayoutParams(topMargin = 16))
            addView(buildTimingCard(), textLayoutParams(topMargin = 16))
        }
        aiLabSection = sectionContainer().apply {
            addView(buildAiLabHeroCard(), textLayoutParams())
            addView(buildAiModelCard(), textLayoutParams(topMargin = 16))
            addView(buildAiGenerationCard(), textLayoutParams(topMargin = 16))
            addView(buildAiSourceCard(), textLayoutParams(topMargin = 16))
            addView(buildAiAutomationCard(), textLayoutParams(topMargin = 16))
        }
        deviceSection = sectionContainer().apply {
            addView(buildStatusCard(), textLayoutParams())
        }

        contentRoot.addView(learnSection, textLayoutParams())
        contentRoot.addView(styleSection, textLayoutParams())
        contentRoot.addView(aiLabSection, textLayoutParams())
        contentRoot.addView(deviceSection, textLayoutParams())
        contentShell.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
        root.addView(
            contentShell,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        root.addView(
            buildBottomNavigation(),
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).apply {
                leftMargin = dp(10)
                rightMargin = dp(10)
                bottomMargin = dp(12)
            },
        )
        setContentView(root)
        root.requestApplyInsets()
        updateVisibleSection()
    }

    private fun buildHeroCard(): LinearLayout {
        val card = card()

        card.addView(
            TextView(this).apply {
                text = "Today's word"
                setTextColor(APP_TEXT_SECONDARY)
                textSize = 14f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                includeFontPadding = false
            },
        )

        hanziView = TextView(this).apply {
            setTextColor(APP_TEXT_PRIMARY)
            textSize = 60f
            gravity = Gravity.CENTER
            typeface = Typeface.create(SERIF_FAMILY, Typeface.BOLD)
            includeFontPadding = false
            setPadding(0, dp(34), 0, 0)
        }
        pinyinView = TextView(this).apply {
            setTextColor(APP_TEXT_SECONDARY)
            textSize = 22f
            gravity = Gravity.CENTER
            typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
            includeFontPadding = false
            setPadding(0, dp(10), 0, 0)
        }
        englishView = TextView(this).apply {
            setTextColor(APP_PRIMARY)
            textSize = 26f
            gravity = Gravity.CENTER
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = false
            setPadding(0, dp(14), 0, 0)
        }

        card.addView(hanziView)
        card.addView(pinyinView)
        card.addView(englishView)
        val metricsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, 0)
        }
        nextWordMetricView = metricPill()
        bucketMetricView = metricPill()
        frequencyMetricView = metricPill()
        metricsRow.addView(nextWordMetricView, weightedButtonLayoutParams())
        metricsRow.addView(bucketMetricView, weightedButtonLayoutParams(startMargin = 8))
        metricsRow.addView(frequencyMetricView, weightedButtonLayoutParams(startMargin = 8))
        card.addView(metricsRow)
        overlayStatusView = infoTextView().apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, 0)
        }
        card.addView(overlayStatusView)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(30), 0, 0)
        }
        overlayToggleButton = actionButton(getString(R.string.overlay_start), primary = true, iconRes = R.drawable.ic_play).apply {
            setOnClickListener { handleOverlayToggle() }
        }
        actions.addView(
            overlayToggleButton,
            weightedButtonLayoutParams(),
        )
        actions.addView(
            actionButton(getString(R.string.refresh_word), primary = false, iconRes = R.drawable.ic_next),
            weightedButtonLayoutParams(startMargin = 10),
        )
        actions.getChildAt(1).setOnClickListener {
            repository.advanceWord()
            refreshOverlay()
            render()
        }
        card.addView(actions)

        overlayPermissionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        overlayPermissionButton = actionButton(
            getString(R.string.overlay_permission),
            primary = false,
            iconRes = R.drawable.ic_permission,
        ).apply {
            setOnClickListener {
                startActivity(CoverOverlayService.permissionSettingsIntent(this@MainActivity))
            }
        }
        overlayPermissionContainer.addView(
            overlayPermissionButton,
            textLayoutParams(),
        )
        card.addView(overlayPermissionContainer)

        return card
    }

    private fun buildPreviewCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(PREVIEW_BACKGROUND, radius = 22)
            applySoftElevation(this, CARD_ELEVATION_DP)
            setPadding(dp(22), dp(22), dp(22), dp(22))
        }
        card.addView(
            TextView(this).apply {
                text = "Overlay preview"
                setTextColor(APP_INVERSE_TEXT)
                textSize = 17f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            },
        )

        previewHanziView = TextView(this).apply {
            includeFontPadding = false
            setPadding(0, dp(18), 0, 0)
            typeface = Typeface.create(SERIF_FAMILY, Typeface.BOLD)
        }
        previewPinyinView = TextView(this).apply {
            includeFontPadding = false
            setPadding(0, dp(6), 0, 0)
            typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
        }
        previewEnglishView = TextView(this).apply {
            includeFontPadding = false
            setPadding(0, dp(6), 0, 0)
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
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
        addAutoHideControl(card)
        return card
    }

    private fun buildAiLabHeroCard(): LinearLayout {
        val card = card()
        card.addView(
            TextView(this).apply {
                text = "AI Lab"
                setTextColor(APP_TEXT_PRIMARY)
                textSize = 32f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                includeFontPadding = false
            },
        )
        card.addView(
            infoTextView().apply {
                text = "Optional local vocabulary generation with flexible pack sizes."
            },
            textLayoutParams(topMargin = 10),
        )
        val aiPillRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, 0)
        }
        aiModelPillView = statusPill()
        aiPackPillView = statusPill()
        aiPillRow.addView(aiModelPillView, weightedButtonLayoutParams())
        aiPillRow.addView(aiPackPillView, weightedButtonLayoutParams(startMargin = 8))
        card.addView(aiPillRow)
        return card
    }

    private fun buildAiModelCard(): LinearLayout {
        val card = card()
        card.addView(sectionTextView("Model setup"))
        aiModelStatusView = infoTextView()
        aiDownloadProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                progressTintList = ColorStateList.valueOf(APP_AMBER)
                progressBackgroundTintList = ColorStateList.valueOf(APP_SURFACE_CONTAINER_HIGH)
            }
            minHeight = dp(8)
        }
        aiDownloadProgressView = infoTextView()
        card.addView(
            TextView(this).apply {
                text = "gemma-4-E2B-it.litertlm"
                setTextColor(APP_PRIMARY)
                textSize = 11f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                includeFontPadding = false
                background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 14, strokeColor = APP_OUTLINE_VARIANT)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setButtonIcon(this, R.drawable.ic_device, APP_PRIMARY)
            },
            textLayoutParams(topMargin = 14),
        )
        card.addView(aiModelStatusView, textLayoutParams(topMargin = 12))
        card.addView(aiDownloadProgressBar, textLayoutParams(topMargin = 8))
        card.addView(aiDownloadProgressView, textLayoutParams(topMargin = 4))
        aiModelActionButton = actionButton("Download AI model", primary = false, iconRes = R.drawable.ic_download).apply {
            setOnClickListener { handleModelAction() }
        }
        card.addView(aiModelActionButton, textLayoutParams(topMargin = 16))
        return card
    }

    private fun buildAiGenerationCard(): LinearLayout {
        val card = card()
        card.addView(sectionTextView("Generation output"))
        card.addView(
            infoTextView().apply {
                text = "Create validated Traditional Chinese entries for fresh cover-screen practice."
            },
            textLayoutParams(topMargin = 10),
        )
        val chipColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        chipColumn.addView(buildFeaturePill("Tone-marked pinyin"), textLayoutParams())
        chipColumn.addView(buildFeaturePill("Traditional Hanzi"), textLayoutParams(topMargin = 8))
        chipColumn.addView(buildFeaturePill("Compact English glosses"), textLayoutParams(topMargin = 8))
        card.addView(chipColumn)
        aiGenerationProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                indeterminateTintList = ColorStateList.valueOf(APP_AMBER)
                progressBackgroundTintList = ColorStateList.valueOf(APP_SURFACE_CONTAINER_HIGH)
            }
            minHeight = dp(8)
        }
        aiGenerationStatusView = infoTextView()
        aiGeneratedStatusView = infoTextView()
        aiGenerationCountButton = selectorButton(
            "Words",
            aiLabPreferences.generationTargetCount.toString(),
            R.drawable.ic_sparkle,
        ).apply {
            setOnClickListener { showGenerationTargetDialog() }
        }
        card.addView(aiGenerationCountButton, textLayoutParams(topMargin = 16))
        aiHskButton = selectorButton("Level", aiLabPreferences.hskLevel.label, R.drawable.ic_level).apply {
            setOnClickListener { showHskLevelDialog() }
        }
        card.addView(aiHskButton, textLayoutParams(topMargin = 8))
        card.addView(aiGenerationProgressBar, textLayoutParams(topMargin = 14))
        card.addView(aiGenerationStatusView, textLayoutParams(topMargin = 6))
        card.addView(aiGeneratedStatusView, textLayoutParams(topMargin = 8))
        aiGenerateButton = actionButton("Generate now", primary = true, iconRes = R.drawable.ic_sparkle).apply {
            setOnClickListener {
                maybeRequestNotificationPermission()
                aiLabPreferences.generatedStatus = "${AiLabPreferences.GENERATED_RUNNING}: queued"
                AiVocabularyGenerationWorker.enqueue(this@MainActivity)
                render()
            }
        }
        card.addView(aiGenerateButton, textLayoutParams(topMargin = 14))
        return card
    }

    private fun buildAiSourceCard(): LinearLayout {
        val card = card()
        card.addView(sectionTextView("Source logic"))
        aiSourceStatusView = infoTextView()
        card.addView(aiSourceStatusView, textLayoutParams(topMargin = 10))
        aiSourceButton = selectorButton("Source", aiLabPreferences.sourceMode.label, R.drawable.ic_source).apply {
            setOnClickListener { showSourceModeDialog() }
        }
        card.addView(aiSourceButton, textLayoutParams(topMargin = 14))
        return card
    }

    private fun buildAiAutomationCard(): LinearLayout {
        val card = card()
        card.addView(sectionTextView("Automation"))
        aiScheduleStatusView = infoTextView()
        aiTimeButton = actionButton("Daily time: ${formatGenerationTime()}", primary = false, iconRes = R.drawable.ic_clock).apply {
            setOnClickListener { showGenerationTimePicker() }
        }
        card.addView(aiTimeButton, textLayoutParams(topMargin = 14))

        val dailyRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBackground(APP_MUTED_SURFACE, radius = 18)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            attachPressFeedback(this)
        }
        dailyRow.addView(
            TextView(this).apply {
                text = "Generate daily"
                setTextColor(APP_TEXT_PRIMARY)
                textSize = 15f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                setButtonIcon(this, R.drawable.ic_repeat, APP_SECONDARY_TEXT)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        aiDailySwitch = Switch(this).apply {
            isChecked = aiLabPreferences.dailyGenerationEnabled
            setOnCheckedChangeListener { _, isChecked ->
                if (!updatingUi) handleDailyToggle(isChecked)
            }
        }
        dailyRow.addView(aiDailySwitch)
        card.addView(dailyRow, textLayoutParams(topMargin = 8))

        aiAlarmPermissionButton = actionButton("Alarm permission", primary = false, iconRes = R.drawable.ic_permission).apply {
            setOnClickListener {
                startActivity(AiGenerationScheduler.permissionIntent(this@MainActivity))
            }
        }
        card.addView(aiAlarmPermissionButton, textLayoutParams(topMargin = 8))

        card.addView(aiScheduleStatusView, textLayoutParams(topMargin = 8))
        aiDeleteModelButton = actionButton("Delete AI model", primary = false, iconRes = R.drawable.ic_delete).apply {
            setOnClickListener { handleModelAction() }
        }
        card.addView(aiDeleteModelButton, textLayoutParams(topMargin = 18))

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
        card.addView(
            actionButton("Copy diagnostics", primary = false, iconRes = R.drawable.ic_copy).apply {
                setOnClickListener { copyDeviceDiagnostics() }
            },
            textLayoutParams(topMargin = 14),
        )
        return card
    }

    private fun render() {
        val info = repository.rotationInfo()
        val word = info.currentWord
        val pinyin = PinyinToneFormatter.format(word)
        val previousHanzi = lastRenderedHanzi
        hanziView.text = word.hanzi
        pinyinView.text = "[$pinyin]"
        englishView.text = word.english
        if (previousHanzi != null && previousHanzi != word.hanzi) {
            animateWordChange()
        }
        lastRenderedHanzi = word.hanzi
        previewHanziView.text = word.hanzi
        previewPinyinView.text = "[$pinyin]"
        previewEnglishView.text = word.english
        previewHanziView.textSize = overlayPreferences.hanziSizeSp.toFloat()
        previewPinyinView.textSize = overlayPreferences.pinyinSizeSp.toFloat()
        previewEnglishView.textSize = overlayPreferences.englishSizeSp.toFloat()
        previewHanziView.setTextColor(overlayPreferences.hanziColor)
        previewPinyinView.setTextColor(overlayPreferences.pinyinColor)
        previewEnglishView.setTextColor(overlayPreferences.englishColor)
        nextWordMetricView.text =
            "Next ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(info.nextRotationMillis))}"
        bucketMetricView.text = "${repository.activeWords().size} words"
        frequencyMetricView.text = formatInterval(info.intervalMillis / 1000L)
        renderOverlayActions()
        updateIntervalControls()
        updateAutoHideControls()
        permissionStatusView.text = overlayPermissionText()
        rotationStatusView.text =
            "Interval: ${formatInterval(info.intervalMillis / 1000L)}\nNext rotation: ${
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(info.nextRotationMillis))
            }"
        displayStatusView.text = displayDebugText()
        renderAiLab()
    }

    private fun renderOverlayActions() {
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        val overlayEnabled = overlayPreferences.overlayEnabled
        val buttonText = if (overlayEnabled) getString(R.string.overlay_stop) else getString(R.string.overlay_start)
        val icon = if (overlayEnabled) R.drawable.ic_stop else R.drawable.ic_play
        val background = if (overlayEnabled) APP_ERROR else APP_PRIMARY_CONTAINER

        configureActionButton(
            button = overlayToggleButton,
            text = buttonText,
            primary = true,
            iconRes = icon,
            backgroundColor = background,
        )
        overlayStatusView.text = when {
            overlayEnabled -> "Overlay running on cover display when available."
            hasOverlayPermission -> "Overlay ready. Tap Start to show it on the cover display."
            else -> "Overlay permission needed before starting."
        }
        overlayPermissionContainer.visibility = if (hasOverlayPermission) View.GONE else View.VISIBLE
    }

    private fun renderAiLab() {
        if (!::aiModelStatusView.isInitialized) return

        updatingUi = true
        val modelStatus = aiModelManager.refreshStatus()
        val downloadProgress = aiModelManager.downloadProgress()
        val generating = isAiGenerating()
        val modelReady = modelStatus == AiLabPreferences.MODEL_READY
        val modelFailed = modelStatus == AiLabPreferences.MODEL_FAILED
        val generatedFailed = aiLabPreferences.generatedStatus.startsWith(AiLabPreferences.GENERATED_FAILED)
        val hasGeneratedPack = aiLabPreferences.generatedCount > 0
        val targetCount = aiLabPreferences.generationTargetCount

        configureStatusPill(
            pill = aiModelPillView,
            text = MODEL_DISPLAY_NAME,
            iconRes = R.drawable.ic_status_dot,
            iconTint = when {
                downloadProgress.isDownloading -> APP_AMBER
                modelReady -> APP_SUCCESS
                modelFailed -> APP_ERROR
                else -> APP_BORDER
            },
            backgroundColor = when {
                downloadProgress.isDownloading -> APP_AMBER_SURFACE
                modelReady -> APP_SUCCESS_SURFACE
                modelFailed -> APP_ERROR_CONTAINER
                else -> APP_MUTED_SURFACE
            },
        )
        configureStatusPill(
            pill = aiPackPillView,
            text = when {
                generating -> "Generating"
                hasGeneratedPack -> "${aiLabPreferences.generatedCount} entries"
                else -> "Built-in list"
            },
            iconRes = when {
                generating -> R.drawable.ic_sparkle
                hasGeneratedPack -> R.drawable.ic_check
                else -> R.drawable.ic_learn
            },
            iconTint = when {
                generating -> APP_AMBER
                hasGeneratedPack -> APP_SUCCESS
                else -> APP_SECONDARY_TEXT
            },
            backgroundColor = when {
                generating -> APP_AMBER_SURFACE
                hasGeneratedPack -> APP_SUCCESS_SURFACE
                else -> APP_MUTED_SURFACE
            },
        )
        configureInfoLine(
            view = aiModelStatusView,
            text = when {
                downloadProgress.isDownloading -> "Downloading $MODEL_DISPLAY_NAME · ${downloadProgress.percent}%"
                modelReady -> "$MODEL_DISPLAY_NAME is available on this device."
                modelFailed -> "$MODEL_DISPLAY_NAME download needs attention."
                else -> "$MODEL_DISPLAY_NAME powers local vocabulary generation."
            },
            iconRes = when {
                modelFailed -> R.drawable.ic_warning
                modelReady -> R.drawable.ic_check
                else -> R.drawable.ic_sparkle
            },
            iconTint = when {
                modelFailed -> APP_ERROR
                modelReady -> APP_SUCCESS
                else -> APP_SECONDARY_TEXT
            },
        )
        aiDownloadProgressBar.progress = downloadProgress.percent
        aiDownloadProgressBar.visibility = if (downloadProgress.isDownloading) View.VISIBLE else View.GONE
        aiDownloadProgressView.text =
            "${downloadProgress.percent}% · ${formatBytes(downloadProgress.downloadedBytes)} of ${formatBytes(downloadProgress.totalBytes)}"
        aiDownloadProgressView.visibility = aiDownloadProgressBar.visibility
        aiGenerationProgressBar.visibility = if (generating) View.VISIBLE else View.GONE
        aiGenerationStatusView.visibility = if (generating) View.VISIBLE else View.GONE
        configureInfoLine(
            view = aiGenerationStatusView,
            text = "Building vocabulary${animatedDots()} ${generationStepLabel(aiLabPreferences.generatedStatus)}",
            iconRes = R.drawable.ic_sparkle,
            iconTint = APP_AMBER,
        )
        configureInfoLine(
            view = aiGeneratedStatusView,
            text = when {
                generatedFailed -> "Last generation failed. Recovery log appears when you reopen the app."
                hasGeneratedPack -> "Last updated ${formatOptionalTime(aiLabPreferences.lastGenerationMillis)}"
                else -> "Generate a local $targetCount-word pack when you want fresh practice."
            },
            iconRes = when {
                generatedFailed -> R.drawable.ic_warning
                hasGeneratedPack -> R.drawable.ic_check
                else -> R.drawable.ic_next
            },
            iconTint = when {
                generatedFailed -> APP_ERROR
                hasGeneratedPack -> APP_SUCCESS
                else -> APP_SECONDARY_TEXT
            },
        )
        configureInfoLine(
            view = aiSourceStatusView,
            text = "Active vocabulary: ${repository.activeWords().size} words",
            iconRes = R.drawable.ic_source,
            iconTint = APP_SECONDARY_TEXT,
        )
        configureSelectorButton(aiSourceButton, "Source", aiLabPreferences.sourceMode.label, R.drawable.ic_source)
        configureSelectorButton(aiHskButton, "Level", aiLabPreferences.hskLevel.label, R.drawable.ic_level)
        configureSelectorButton(aiGenerationCountButton, "Words", targetCount.toString(), R.drawable.ic_sparkle)
        configureActionButton(
            button = aiModelActionButton,
            text = when {
                downloadProgress.isDownloading -> "Downloading ${downloadProgress.percent}%"
                modelFailed -> "Retry download"
                else -> "Download AI model"
            },
            primary = false,
            iconRes = R.drawable.ic_download,
        )
        aiModelActionButton.visibility = if (modelReady) View.GONE else View.VISIBLE
        aiModelActionButton.isEnabled = !downloadProgress.isDownloading && !generating
        aiModelActionButton.alpha = if (aiModelActionButton.isEnabled) 1f else 0.55f
        configureActionButton(
            button = aiGenerateButton,
            text = when {
                generating -> "Generating..."
                modelReady -> "Generate $targetCount words"
                else -> "Download model first"
            },
            primary = true,
            iconRes = R.drawable.ic_sparkle,
        )
        aiGenerateButton.isEnabled = modelReady && !generating
        aiGenerateButton.alpha = if (aiGenerateButton.isEnabled) 1f else 0.55f
        configureActionButton(
            button = aiTimeButton,
            text = "Daily ${formatGenerationTime()}",
            primary = false,
            iconRes = R.drawable.ic_clock,
        )
        aiSourceButton.isEnabled = !generating
        aiSourceButton.alpha = if (generating) 0.55f else 1f
        aiHskButton.isEnabled = !generating
        aiHskButton.alpha = if (generating) 0.55f else 1f
        aiGenerationCountButton.isEnabled = !generating
        aiGenerationCountButton.alpha = if (generating) 0.55f else 1f

        val exactStatus = if (AiGenerationScheduler.canScheduleExact(this)) "granted" else "blocked"
        val enabled = if (aiLabPreferences.dailyGenerationEnabled) "enabled" else "off"
        aiDailySwitch.setOnCheckedChangeListener(null)
        aiDailySwitch.isChecked = aiLabPreferences.dailyGenerationEnabled
        aiDailySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!updatingUi) handleDailyToggle(isChecked)
        }
        aiAlarmPermissionButton.visibility = if (exactStatus == "granted") View.GONE else View.VISIBLE
        configureInfoLine(
            view = aiScheduleStatusView,
            text = "Daily $enabled · ${formatGenerationTime()} · next ${formatOptionalTime(aiLabPreferences.lastScheduledGenerationMillis)}",
            iconRes = R.drawable.ic_repeat,
            iconTint = APP_SECONDARY_TEXT,
        )
        configureActionButton(
            button = aiDeleteModelButton,
            text = "Delete AI model",
            primary = false,
            iconRes = R.drawable.ic_delete,
            backgroundColor = APP_ERROR_CONTAINER,
            textColor = APP_ON_ERROR_CONTAINER,
        )
        aiDeleteModelButton.visibility = if (modelReady) View.VISIBLE else View.GONE
        aiDeleteModelButton.isEnabled = !generating
        aiDeleteModelButton.alpha = if (aiDeleteModelButton.isEnabled) 1f else 0.55f
        setSubtlePulse(aiModelPillView, modelReady)
        setSubtlePulse(aiPackPillView, generating)
        updatingUi = false
        updateActivityLoop(downloadProgress.isDownloading || generating)
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
        return "${overlayPreferences.overlayStatus}\nMain display fallback disabled.\nOverlay scope: cover display-wide; Samsung does not expose the active cover page.\nDetected displays:\n$displays"
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                progressTintList = ColorStateList.valueOf(APP_AMBER)
                progressBackgroundTintList = ColorStateList.valueOf(APP_SURFACE_CONTAINER_HIGH)
                thumbTintList = ColorStateList.valueOf(APP_AMBER)
            }
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
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
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
        intervalStatusView = infoTextView()
        root.addView(intervalStatusView, textLayoutParams(topMargin = 14))
        intervalChangeButton = actionButton("Change frequency", primary = false, iconRes = R.drawable.ic_clock).apply {
            setOnClickListener { showIntervalOptionsDialog() }
        }
        root.addView(intervalChangeButton, textLayoutParams(topMargin = 12))
        updateIntervalControls()
    }

    private fun showGenerationTimePicker() {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                aiLabPreferences.dailyGenerationHour = hour
                aiLabPreferences.dailyGenerationMinute = minute
                if (aiLabPreferences.dailyGenerationEnabled) {
                    AiGenerationScheduler.scheduleNext(this)
                }
                render()
            },
            aiLabPreferences.dailyGenerationHour,
            aiLabPreferences.dailyGenerationMinute,
            true,
        ).show()
    }

    private fun showSourceModeDialog() {
        val modes = AiVocabularySourceMode.values()
        val labels = modes.map { it.label }.toTypedArray()
        val selected = modes.indexOf(aiLabPreferences.sourceMode)

        AlertDialog.Builder(this)
            .setTitle("Generated source")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                dialog.dismiss()
                aiLabPreferences.sourceMode = modes[which]
                repository.pinWord(repository.currentWord())
                refreshOverlay()
                render()
            }
            .show()
    }

    private fun showHskLevelDialog() {
        val levels = AiHskLevel.entries
        val labels = levels.map { it.label }.toTypedArray()
        val selected = levels.indexOf(aiLabPreferences.hskLevel)

        AlertDialog.Builder(this)
            .setTitle("HSK level")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                dialog.dismiss()
                aiLabPreferences.hskLevel = levels[which]
                render()
            }
            .show()
    }

    private fun showGenerationTargetDialog() {
        val presets = listOf(25, 50, 75, 100, 150)
        val labels = presets.map { "$it words" } + "Custom..."
        val currentIndex = presets.indexOf(aiLabPreferences.generationTargetCount)

        AlertDialog.Builder(this)
            .setTitle("Words per pack")
            .setSingleChoiceItems(labels.toTypedArray(), currentIndex) { dialog, which ->
                dialog.dismiss()
                if (which < presets.size) {
                    aiLabPreferences.generationTargetCount = presets[which]
                    render()
                } else {
                    showCustomGenerationTargetDialog()
                }
            }
            .show()
    }

    private fun showCustomGenerationTargetDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(aiLabPreferences.generationTargetCount.toString())
            selectAll()
            setPadding(dp(12), 0, dp(12), 0)
            background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 16, strokeColor = APP_OUTLINE_VARIANT)
            minHeight = dp(52)
        }

        AlertDialog.Builder(this)
            .setTitle("Custom pack size")
            .setMessage(
                "Choose ${AiLabPreferences.MIN_GENERATION_TARGET_COUNT}-${AiLabPreferences.MAX_GENERATION_TARGET_COUNT} words.",
            )
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Done") { _, _ ->
                val value = input.text.toString().toIntOrNull() ?: return@setPositiveButton
                val normalized = value.coerceIn(
                    AiLabPreferences.MIN_GENERATION_TARGET_COUNT,
                    AiLabPreferences.MAX_GENERATION_TARGET_COUNT,
                )
                aiLabPreferences.generationTargetCount = normalized
                if (normalized != value) {
                    Toast.makeText(
                        this,
                        "Pack size adjusted to $normalized words.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                render()
            }
            .show()
    }

    private fun updateIntervalControls() {
        if (!::intervalStatusView.isInitialized) return

        val current = overlayPreferences.rotationIntervalSeconds
        intervalStatusView.text = "Changes every ${formatInterval(current.toLong())}"
        if (::intervalChangeButton.isInitialized) {
            configureActionButton(
                button = intervalChangeButton,
                text = "Change frequency",
                primary = false,
                iconRes = R.drawable.ic_clock,
            )
        }
    }

    private fun addAutoHideControl(root: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBackground(APP_MUTED_SURFACE, radius = 18)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            attachPressFeedback(this)
        }
        row.addView(
            TextView(this).apply {
                text = "Auto-hide overlay"
                setTextColor(APP_TEXT_PRIMARY)
                textSize = 15f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                setButtonIcon(this, R.drawable.ic_clock, APP_SECONDARY_TEXT)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        autoHideSwitch = Switch(this).apply {
            isChecked = overlayPreferences.autoHideEnabled
            setOnCheckedChangeListener { _, isChecked ->
                if (!updatingUi) handleAutoHideToggle(isChecked)
            }
        }
        row.addView(autoHideSwitch)
        root.addView(row, textLayoutParams(topMargin = 18))

        autoHideStatusView = infoTextView()
        root.addView(autoHideStatusView, textLayoutParams(topMargin = 8))

        autoHideDurationButton = actionButton("Visible duration", primary = false, iconRes = R.drawable.ic_clock).apply {
            setOnClickListener { showAutoHideOptionsDialog() }
        }
        root.addView(autoHideDurationButton, textLayoutParams(topMargin = 12))
        updateAutoHideControls()
    }

    private fun updateAutoHideControls() {
        if (!::autoHideStatusView.isInitialized) return

        val enabled = overlayPreferences.autoHideEnabled
        val duration = formatInterval(overlayPreferences.autoHideSeconds.toLong())
        updatingUi = true
        autoHideSwitch.setOnCheckedChangeListener(null)
        autoHideSwitch.isChecked = enabled
        autoHideSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!updatingUi) handleAutoHideToggle(isChecked)
        }
        updatingUi = false

        configureInfoLine(
            view = autoHideStatusView,
            text = if (enabled) "Floating text hides after $duration; the notification stays active." else "Floating text stays visible while the overlay is running.",
            iconRes = if (enabled) R.drawable.ic_check else R.drawable.ic_repeat,
            iconTint = if (enabled) APP_SUCCESS else APP_SECONDARY_TEXT,
        )
        configureActionButton(
            button = autoHideDurationButton,
            text = "Visible for $duration",
            primary = false,
            iconRes = R.drawable.ic_clock,
        )
        autoHideDurationButton.isEnabled = enabled
        autoHideDurationButton.alpha = if (enabled) 1f else 0.55f
    }

    private fun showAutoHideOptionsDialog() {
        val presets = listOf(
            IntervalPreset("3 sec", 3),
            IntervalPreset("5 sec", 5),
            IntervalPreset("10 sec", 10),
            IntervalPreset("20 sec", 20),
            IntervalPreset("30 sec", 30),
            IntervalPreset("60 sec", 60),
        )
        val labels = presets.map { it.label }
        val currentIndex = presets.indexOfFirst { it.seconds == overlayPreferences.autoHideSeconds }

        AlertDialog.Builder(this)
            .setTitle("Hide floating text after")
            .setSingleChoiceItems(labels.toTypedArray(), currentIndex) { dialog, which ->
                dialog.dismiss()
                setAutoHideSeconds(presets[which].seconds)
            }
            .show()
    }

    private fun showIntervalOptionsDialog() {
        val presets = listOf(
            IntervalPreset("5 sec", 5),
            IntervalPreset("1 min", 60),
            IntervalPreset("15 min", 15 * 60),
            IntervalPreset("30 min", 30 * 60),
            IntervalPreset("60 min", 60 * 60),
            IntervalPreset("90 min", 90 * 60),
        )
        val labels = presets.map { it.label } + "Custom..."
        val currentIndex = presets.indexOfFirst { it.seconds == overlayPreferences.rotationIntervalSeconds }

        AlertDialog.Builder(this)
            .setTitle("Change word every")
            .setSingleChoiceItems(labels.toTypedArray(), currentIndex) { dialog, which ->
                dialog.dismiss()
                if (which < presets.size) {
                    setRotationInterval(presets[which].seconds)
                } else {
                    showCustomIntervalDialog()
                }
            }
            .show()
    }

    private fun showCustomIntervalDialog() {
        val units = listOf("seconds", "minutes", "hours")
        val currentSeconds = overlayPreferences.rotationIntervalSeconds
        val defaultUnitIndex = when {
            currentSeconds % 3600 == 0 && currentSeconds >= 3600 -> 2
            currentSeconds % 60 == 0 && currentSeconds >= 60 -> 1
            else -> 0
        }
        val defaultValue = when (defaultUnitIndex) {
            2 -> currentSeconds / 3600
            1 -> currentSeconds / 60
            else -> currentSeconds
        }
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(defaultValue.toString())
            selectAll()
            setPadding(dp(12), 0, dp(12), 0)
            background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 16, strokeColor = APP_OUTLINE_VARIANT)
            minHeight = dp(52)
        }
        val unitSpinner = spinner(
            labels = units,
            selectedIndex = defaultUnitIndex,
            onSelected = {},
        )
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
            addView(
                input,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
            addView(
                unitSpinner,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(10)
                },
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Change word every")
            .setView(row)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Done") { _, _ ->
                val value = input.text.toString().toIntOrNull() ?: return@setPositiveButton
                val multiplier = when (unitSpinner.selectedItemPosition) {
                    2 -> 3600
                    1 -> 60
                    else -> 1
                }
                val totalSeconds = (value.toLong() * multiplier.toLong())
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
                setRotationInterval(totalSeconds)
            }
            .show()
    }

    private fun maybePromptForAiFailureLog() {
        val failureId = aiLabPreferences.lastFailureLogId
        val shouldPrompt = failureId > 0L &&
            failureId != aiLabPreferences.promptedFailureLogId &&
            aiLabPreferences.generatedStatus.startsWith(AiLabPreferences.GENERATED_FAILED) &&
            aiLabPreferences.pendingDebugLog.isNotBlank()

        if (!shouldPrompt || aiFailurePromptShowing) return

        aiGeneratedStatusView.post {
            if (!isFinishing && !isDestroyed) {
                showSaveAiLogPrompt(failureId)
            }
        }
    }

    private fun showSaveAiLogPrompt(failureId: Long) {
        if (aiFailurePromptShowing) return

        aiFailurePromptShowing = true
        AlertDialog.Builder(this)
            .setTitle("Save AI debug log?")
            .setMessage("AI vocabulary generation failed. Save a text log so it can be shared for debugging?")
            .setPositiveButton("Save log") { _, _ ->
                aiLabPreferences.promptedFailureLogId = failureId
                saveAiDebugLog()
                aiFailurePromptShowing = false
            }
            .setNegativeButton("Not now") { _, _ ->
                aiLabPreferences.promptedFailureLogId = failureId
                aiFailurePromptShowing = false
            }
            .setOnCancelListener {
                aiLabPreferences.promptedFailureLogId = failureId
                aiFailurePromptShowing = false
            }
            .show()
    }

    private fun saveAiDebugLog() {
        val file = AiDebugLogDumper.savePendingLog(this)
        if (file == null) {
            Toast.makeText(this, "No AI failure log available yet.", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(
            this,
            "AI debug log saved: ${file.absolutePath}",
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun formatGenerationTime(): String =
        String.format(Locale.US, "%02d:%02d", aiLabPreferences.dailyGenerationHour, aiLabPreferences.dailyGenerationMinute)

    private fun formatOptionalTime(millis: Long): String =
        if (millis <= 0L) {
            "not set"
        } else {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))
        }

    private fun updateActivityLoop(shouldRefresh: Boolean) {
        handler.removeCallbacks(downloadProgressRunnable)
        if (shouldRefresh) {
            handler.postDelayed(downloadProgressRunnable, DOWNLOAD_PROGRESS_REFRESH_MILLIS)
        }
    }

    private fun handleModelAction() {
        if (aiModelManager.refreshStatus() == AiLabPreferences.MODEL_READY) {
            val deleted = aiModelManager.deleteModel()
            Toast.makeText(
                this,
                if (deleted) "AI model deleted." else "Could not delete AI model.",
                Toast.LENGTH_SHORT,
            ).show()
        } else {
            aiModelManager.startDownload()
        }
        render()
    }

    private fun handleOverlayToggle() {
        if (overlayPreferences.overlayEnabled) {
            overlayPreferences.overlayEnabled = false
            startService(
                Intent(this, CoverOverlayService::class.java)
                    .setAction(CoverOverlayActions.ACTION_STOP),
            )
            Toast.makeText(this, "Overlay stopped.", Toast.LENGTH_SHORT).show()
        } else {
            maybeRequestNotificationPermission()
            if (Settings.canDrawOverlays(this)) {
                overlayPreferences.overlayEnabled = true
                startForegroundService(
                    Intent(this, CoverOverlayService::class.java)
                        .setAction(CoverOverlayActions.ACTION_START),
                )
                Toast.makeText(this, "Overlay starting.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(CoverOverlayService.permissionSettingsIntent(this))
            }
        }
        render()
    }

    private fun copyDeviceDiagnostics() {
        val text = listOf(
            permissionStatusView.text,
            rotationStatusView.text,
            displayStatusView.text,
        ).joinToString(separator = "\n\n")
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("FlipWords diagnostics", text))
        Toast.makeText(this, "Diagnostics copied.", Toast.LENGTH_SHORT).show()
    }

    private fun handleDailyToggle(enabled: Boolean) {
        if (enabled) {
            if (AiGenerationScheduler.canScheduleExact(this)) {
                aiLabPreferences.dailyGenerationEnabled = true
                AiGenerationScheduler.scheduleNext(this)
            } else {
                aiLabPreferences.dailyGenerationEnabled = false
                startActivity(AiGenerationScheduler.permissionIntent(this))
            }
        } else {
            AiGenerationScheduler.cancel(this)
        }
        render()
    }

    private fun handleAutoHideToggle(enabled: Boolean) {
        overlayPreferences.autoHideEnabled = enabled
        refreshOverlay()
        render()
    }

    private fun animateWordChange() {
        listOf(hanziView, pinyinView, englishView).forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = dp(8).toFloat()
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 45L)
                .setDuration(180L)
                .start()
        }
    }

    private fun setRotationInterval(seconds: Int) {
        val min = OverlayPreferences.MIN_ROTATION_INTERVAL_SECONDS
        val max = OverlayPreferences.MAX_ROTATION_INTERVAL_SECONDS
        val normalized = seconds.coerceIn(min, max)
        val currentWord = repository.currentWord()
        overlayPreferences.rotationIntervalSeconds = normalized
        repository.pinWord(currentWord)
        WordUpdateScheduler.schedule(this)
        refreshOverlay()
        if (normalized != seconds) {
            Toast.makeText(
                this,
                "Frequency adjusted to ${formatInterval(normalized.toLong())}.",
                Toast.LENGTH_SHORT,
            ).show()
        }
        render()
    }

    private fun setAutoHideSeconds(seconds: Int) {
        val min = OverlayPreferences.MIN_AUTO_HIDE_SECONDS
        val max = OverlayPreferences.MAX_AUTO_HIDE_SECONDS
        val normalized = seconds.coerceIn(min, max)
        overlayPreferences.autoHideSeconds = normalized
        refreshOverlay()
        if (normalized != seconds) {
            Toast.makeText(
                this,
                "Auto-hide adjusted to ${formatInterval(normalized.toLong())}.",
                Toast.LENGTH_SHORT,
            ).show()
        }
        render()
    }

    private fun isAiGenerating(): Boolean =
        aiLabPreferences.generatedStatus.startsWith(AiLabPreferences.GENERATED_RUNNING)

    private fun animatedDots(): String =
        ".".repeat(((System.currentTimeMillis() / DOWNLOAD_PROGRESS_REFRESH_MILLIS) % 4).toInt())

    private fun generationStepLabel(status: String): String =
        when {
            status.contains("loading model", ignoreCase = true) -> "Loading model"
            status.contains("validating", ignoreCase = true) -> "Validating entries"
            status.contains("queued", ignoreCase = true) -> "Queued"
            status.contains("checking model", ignoreCase = true) -> "Checking model"
            else -> "Preparing"
        }

    private fun formLabel(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(APP_TEXT_SECONDARY)
            textSize = 13f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = false
        }

    private fun spinner(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit): Spinner =
        Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                labels,
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 18, strokeColor = APP_OUTLINE_VARIANT)
            setPadding(dp(12), 0, dp(12), 0)
            minimumHeight = dp(52)
            setSelection(selectedIndex.coerceIn(labels.indices), false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    onSelected(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }

    private fun buildTopAppBar(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(
                        TextView(this@MainActivity).apply {
                            text = getString(R.string.app_name)
                            setTextColor(APP_TEXT_PRIMARY)
                            textSize = 28f
                            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                            includeFontPadding = false
                        },
                    )
                    addView(
                        TextView(this@MainActivity).apply {
                            text = "Cover-screen Chinese practice"
                            setTextColor(APP_TEXT_SECONDARY)
                            textSize = 13f
                            includeFontPadding = false
                            setPadding(0, dp(6), 0, 0)
                        },
                    )
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
            addView(
                ImageView(this@MainActivity).apply {
                    setImageResource(R.drawable.flipwords_logo)
                    contentDescription = getString(R.string.app_name)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    background = roundedBackground(APP_SURFACE, radius = 18)
                    setPadding(dp(6), dp(6), dp(6), dp(6))
                },
                LinearLayout.LayoutParams(dp(48), dp(48)),
            )
        }

    private fun buildBottomNavigation(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            background = roundedStrokeBackground(APP_SURFACE, radius = 32, strokeColor = APP_OUTLINE_VARIANT)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            applySoftElevation(this, NAV_ELEVATION_DP)
            AppTab.values().forEach { tab ->
                addView(
                    tabButton(tab),
                    LinearLayout.LayoutParams(
                        0,
                        dp(56),
                        1f,
                    ).apply {
                        marginEnd = if (tab == AppTab.values().last()) 0 else dp(4)
                    },
                )
            }
        }

    private fun tabButton(tab: AppTab): TextView =
        TextView(this).apply {
            text = tab.title
            contentDescription = tab.title
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            includeFontPadding = false
            textSize = 11f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            setPadding(dp(6), dp(5), dp(6), dp(4))
            attachPressFeedback(this)
            setOnClickListener {
                if (activeTab == tab) return@setOnClickListener
                activeTab = tab
                updateVisibleSection(animate = true)
                render()
            }
            tabButtons[tab] = this
        }

    private fun updateVisibleSection(animate: Boolean = false) {
        if (!::learnSection.isInitialized) return

        val activeSection = when (activeTab) {
            AppTab.LEARN -> learnSection
            AppTab.STYLE -> styleSection
            AppTab.AI_LAB -> aiLabSection
            AppTab.DEVICE -> deviceSection
        }
        listOf(learnSection, styleSection, aiLabSection, deviceSection).forEach { section ->
            section.visibility = if (section == activeSection) View.VISIBLE else View.GONE
        }
        if (animate) {
            activeSection.alpha = 0f
            activeSection.translationY = dp(8).toFloat()
            activeSection.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180L)
                .start()
        }
        updateTabStyles()
    }

    private fun updateTabStyles() {
        tabButtons.forEach { (tab, button) ->
            val selected = tab == activeTab
            val tint = if (selected) APP_PRIMARY else APP_TEXT_SECONDARY
            button.setTextColor(tint)
            button.alpha = if (selected) 1f else 0.74f
            button.typeface = Typeface.create(SANS_FAMILY, if (selected) Typeface.BOLD else Typeface.NORMAL)
            button.background = roundedBackground(if (selected) APP_SUCCESS_SURFACE else Color.TRANSPARENT, radius = 26)
            setTopButtonIcon(button, tab.iconRes, tint)
            button.animate()
                .scaleX(if (selected) 1f else 0.96f)
                .scaleY(if (selected) 1f else 0.96f)
                .translationZ(if (selected) dp(1).toFloat() else 0f)
                .setDuration(160L)
                .start()
        }
    }

    private fun formatBytes(bytes: Long): String {
        val gib = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format(Locale.US, "%.2f GB", gib)
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
            background = roundedStrokeBackground(APP_SURFACE, radius = 30, strokeColor = APP_OUTLINE_VARIANT)
            applySoftElevation(this, CARD_ELEVATION_DP)
            setPadding(dp(22), dp(22), dp(22), dp(22))
        }

    private fun actionButton(text: String, primary: Boolean, iconRes: Int? = null): TextView =
        TextView(this).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            textSize = 14f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = false
            setPadding(dp(18), dp(10), dp(18), dp(10))
            minHeight = dp(56)
            minWidth = 0
            configureActionButton(this, text, primary, iconRes)
            attachPressFeedback(this)
        }

    private fun selectorButton(label: String, value: String, iconRes: Int): TextView =
        TextView(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            textSize = 15f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = false
            setPadding(dp(16), 0, dp(16), 0)
            minHeight = dp(56)
            configureSelectorButton(this, label, value, iconRes)
            attachPressFeedback(this)
        }

    private fun configureSelectorButton(button: TextView, label: String, value: String, iconRes: Int) {
        button.text = "$label: $value"
        button.setTextColor(APP_TEXT_PRIMARY)
        button.background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 20, strokeColor = APP_OUTLINE_VARIANT)
        setButtonIcon(button, iconRes, APP_SECONDARY_TEXT)
    }

    private fun configureActionButton(
        button: TextView,
        text: String,
        primary: Boolean,
        iconRes: Int? = null,
        backgroundColor: Int = if (primary) APP_PRIMARY_CONTAINER else APP_SECONDARY_CONTAINER,
        textColor: Int = if (primary) APP_ON_PRIMARY else APP_SECONDARY_TEXT,
    ) {
        button.text = text
        button.setTextColor(textColor)
        button.background = roundedBackground(backgroundColor, radius = 28)
        setButtonIcon(button, iconRes, textColor)
    }

    private fun setButtonIcon(button: TextView, iconRes: Int?, color: Int) {
        val icon = iconRes?.let { getDrawable(it)?.mutate() }
        icon?.setTint(color)
        button.compoundDrawablePadding = if (icon == null) 0 else dp(8)
        button.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
    }

    private fun setTopButtonIcon(button: TextView, iconRes: Int, color: Int) {
        val icon = getDrawable(iconRes)?.mutate()
        icon?.setTint(color)
        button.compoundDrawablePadding = dp(2)
        button.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null)
    }

    private fun infoTextView(): TextView =
        TextView(this).apply {
            setTextColor(APP_TEXT_SECONDARY)
            textSize = 14f
            includeFontPadding = true
            typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
        }

    private fun sectionTextView(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(APP_TEXT_PRIMARY)
            textSize = 22f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = true
        }

    private fun metricPill(): TextView =
        TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(APP_SECONDARY_TEXT)
            textSize = 12f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = false
            minHeight = dp(36)
            background = roundedBackground(APP_MUTED_SURFACE, radius = 18)
            setPadding(dp(8), 0, dp(8), 0)
        }

    private fun statusPill(): TextView =
        TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(APP_PRIMARY)
            textSize = 12f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = false
            minHeight = dp(38)
            background = roundedBackground(APP_SUCCESS_SURFACE, radius = 19)
            setPadding(dp(8), 0, dp(8), 0)
        }

    private fun buildFeaturePill(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(APP_TEXT_PRIMARY)
            textSize = 12f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = false
            minHeight = dp(40)
            gravity = Gravity.CENTER_VERTICAL
            background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 20, strokeColor = APP_OUTLINE_VARIANT)
            setPadding(dp(14), 0, dp(14), 0)
            setButtonIcon(this, R.drawable.ic_check, APP_PRIMARY)
        }

    private fun configureStatusPill(
        pill: TextView,
        text: String,
        iconRes: Int,
        iconTint: Int,
        backgroundColor: Int,
    ) {
        pill.text = text
        pill.setTextColor(APP_TEXT_PRIMARY)
        pill.background = roundedBackground(backgroundColor, radius = 19)
        setButtonIcon(pill, iconRes, iconTint)
        pill.elevation = if (backgroundColor == APP_SUCCESS_SURFACE) dp(1).toFloat() else 0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && backgroundColor == APP_SUCCESS_SURFACE) {
            pill.outlineAmbientShadowColor = APP_SUCCESS_GLOW
            pill.outlineSpotShadowColor = APP_SUCCESS_GLOW
        }
    }

    private fun configureInfoLine(view: TextView, text: String, iconRes: Int, iconTint: Int) {
        view.text = text
        view.setTextColor(if (iconTint == APP_ERROR) APP_ON_ERROR_CONTAINER else APP_TEXT_SECONDARY)
        setButtonIcon(view, iconRes, iconTint)
    }

    private fun swatchView(color: Int, sizeDp: Int): TextView =
        TextView(this).apply {
            background = swatchBackground(color, selected = true)
            minWidth = dp(sizeDp)
            minHeight = dp(sizeDp)
            attachPressFeedback(this)
        }

    private fun swatchBackground(color: Int, selected: Boolean): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dp(if (selected) 3 else 1), if (selected) APP_AMBER else APP_BORDER)
        }

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radius).toFloat()
            setColor(color)
        }

    private fun roundedStrokeBackground(color: Int, radius: Int, strokeColor: Int): GradientDrawable =
        roundedBackground(color, radius).apply {
            setStroke(dp(1), strokeColor)
        }

    private fun applySoftElevation(view: View, elevationDp: Int) {
        view.elevation = dp(elevationDp).toFloat()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.outlineAmbientShadowColor = APP_SHADOW
            view.outlineSpotShadowColor = APP_SHADOW
        }
    }

    private fun sectionContainer(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }

    private fun attachPressFeedback(view: View) {
        view.setOnTouchListener { touchedView, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchedView.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .alpha(0.88f)
                        .translationZ(dp(2).toFloat())
                        .setDuration(90L)
                        .start()
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    touchedView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .translationZ(0f)
                        .setDuration(140L)
                        .start()
                }
            }
            false
        }
    }

    private fun setSubtlePulse(view: View, active: Boolean) {
        if (active) {
            if (view.animation == null) {
                val pulse = AlphaAnimation(0.72f, 1f).apply {
                    duration = 900L
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                }
                view.startAnimation(pulse)
            }
        } else {
            view.clearAnimation()
            view.alpha = 1f
        }
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

    private enum class AppTab(val title: String, val iconRes: Int) {
        LEARN("Learn", R.drawable.ic_learn),
        STYLE("Style", R.drawable.ic_style),
        AI_LAB("AI Lab", R.drawable.ic_sparkle),
        DEVICE("Device", R.drawable.ic_device),
    }

    private data class IntervalPreset(val label: String, val seconds: Int)

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 42
        private const val DOWNLOAD_PROGRESS_REFRESH_MILLIS = 1_000L
        private const val CARD_ELEVATION_DP = 2
        private const val NAV_ELEVATION_DP = 8

        private const val SANS_FAMILY = "sans-serif"
        private const val SERIF_FAMILY = "serif"
        private const val MODEL_DISPLAY_NAME = "Gemma 4 E2B"

        private val APP_BACKGROUND = Color.parseColor("#F7FAF7")
        private val APP_SURFACE = Color.parseColor("#FFFFFF")
        private val APP_MUTED_SURFACE = Color.parseColor("#F1F4F1")
        private val APP_SURFACE_CONTAINER_HIGH = Color.parseColor("#E5E9E6")
        private val PREVIEW_BACKGROUND = Color.parseColor("#2D3130")
        private val APP_TEXT_PRIMARY = Color.parseColor("#181C1B")
        private val APP_TEXT_SECONDARY = Color.parseColor("#3E4945")
        private val APP_INVERSE_TEXT = Color.parseColor("#EEF2EE")
        private val APP_OUTLINE_VARIANT = Color.parseColor("#BEC9C4")
        private val APP_BORDER = Color.parseColor("#6E7975")
        private val APP_PRIMARY = Color.parseColor("#005344")
        private val APP_PRIMARY_CONTAINER = Color.parseColor("#006D5B")
        private val APP_ON_PRIMARY = Color.parseColor("#FFFFFF")
        private val APP_SECONDARY_CONTAINER = Color.parseColor("#CFE3EE")
        private val APP_SECONDARY_TEXT = Color.parseColor("#374953")
        private val APP_AMBER = Color.parseColor("#FFBA38")
        private val APP_AMBER_SURFACE = Color.parseColor("#FFF3D6")
        private val APP_ERROR = Color.parseColor("#BA1A1A")
        private val APP_ERROR_CONTAINER = Color.parseColor("#FFDAD6")
        private val APP_ON_ERROR_CONTAINER = Color.parseColor("#93000A")
        private val APP_SUCCESS = Color.parseColor("#0E9F6E")
        private val APP_SUCCESS_SURFACE = Color.parseColor("#E2F7EF")
        private val APP_SHADOW = Color.parseColor("#24006B59")
        private val APP_SUCCESS_GLOW = Color.parseColor("#550E9F6E")

        private val COLOR_PALETTE = intArrayOf(
            OverlayPreferences.DEFAULT_HANZI_COLOR,
            OverlayPreferences.DEFAULT_PINYIN_COLOR,
            OverlayPreferences.DEFAULT_ENGLISH_COLOR,
            APP_ON_PRIMARY,
            APP_TEXT_PRIMARY,
            APP_PRIMARY_CONTAINER,
            Color.parseColor("#4F616B"),
            Color.parseColor("#0E9F6E"),
            APP_AMBER,
            Color.parseColor("#BA1A1A"),
            Color.parseColor("#805800"),
        )
    }
}
