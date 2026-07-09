package com.example.samsungzh

import android.animation.ValueAnimator
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
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
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
    private lateinit var schedulerPreferences: SchedulerPreferences
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
    private lateinit var schedulerSpacingButton: TextView
    private lateinit var intervalChangeButton: TextView
    private lateinit var hskFilterButton: TextView
    private lateinit var statsSeenView: TextView
    private lateinit var statsFamiliarView: TextView
    private lateinit var statsMasteredView: TextView
    private lateinit var statsDueSoonView: TextView
    private lateinit var progressChartView: VocabularyProgressChartView
    private lateinit var recentProgressView: TextView
    private lateinit var wordOptionsButton: ImageView
    private lateinit var autoHideStatusView: TextView
    private lateinit var autoHideDurationButton: TextView
    private lateinit var autoHideSwitch: Switch
    private lateinit var permissionStatusView: TextView
    private lateinit var rotationStatusView: TextView
    private lateinit var displayStatusView: TextView
    private lateinit var aiModelStatusView: TextView
    private lateinit var aiModelWarningView: TextView
    private lateinit var aiGeneratedStatusView: TextView
    private lateinit var aiScheduleStatusView: TextView
    private lateinit var aiSourceStatusView: TextView
    private lateinit var aiDownloadProgressBar: ProgressBar
    private lateinit var aiDownloadPanelView: LinearLayout
    private lateinit var aiDownloadProgressView: TextView
    private lateinit var aiGenerationProgressBar: ProgressBar
    private lateinit var aiGenerationStatusView: TextView
    private lateinit var aiGenerateRequirementView: TextView
    private lateinit var aiModelActionButton: TextView
    private lateinit var aiDeleteModelButton: TextView
    private lateinit var aiGenerateButton: TextView
    private lateinit var aiTimeButton: TextView
    private lateinit var aiAlarmPermissionButton: TextView
    private lateinit var aiDailySwitch: Switch
    private lateinit var aiSourceButton: TextView
    private lateinit var aiSourceMixButton: TextView
    private lateinit var aiHskButton: TextView
    private lateinit var aiGenerationCountButton: TextView
    private lateinit var aiModelPillView: TextView
    private lateinit var aiPackPillView: TextView
    private lateinit var learnSection: LinearLayout
    private lateinit var styleSection: LinearLayout
    private lateinit var schedulerSection: LinearLayout
    private lateinit var aiLabSection: LinearLayout
    private lateinit var deviceSection: LinearLayout
    private lateinit var mainScrollView: ScrollView
    private lateinit var topTitleView: TextView
    private val tabButtons = mutableMapOf<AppTab, ImageView>()
    private var activeTab = AppTab.LEARN
    private var selectedHskFilter: Int? = null
    private var updatingUi = false
    private var aiFailurePromptShowing = false
    private var lastRenderedHanzi: String? = null
    private var lastRenderedModelReady: Boolean? = null
    private var lastRenderedGeneratedCount: Int? = null

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
        schedulerPreferences = SchedulerPreferences(this)
        aiLabPreferences = AiLabPreferences(this)
        aiModelManager = AiModelManager(this)
        repository.recordFullAppOpen()
        WordUpdateScheduler.schedule(this)
        configureSystemBars()
        buildLayout()
        render()
        maybePromptForAiFailureLog()
    }

    override fun onResume() {
        super.onResume()
        repository.recordFullAppOpen()
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
        mainScrollView = ScrollView(this).apply {
            setBackgroundColor(APP_BACKGROUND)
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            addView(contentRoot)
        }

        learnSection = sectionContainer().apply {
            addView(buildHeroCard(), textLayoutParams())
            addView(buildStatsCard(), textLayoutParams(topMargin = 16))
        }
        styleSection = sectionContainer().apply {
            addView(buildPreviewCard(), textLayoutParams())
            addView(buildStyleCard(), textLayoutParams(topMargin = 16))
        }
        schedulerSection = sectionContainer().apply {
            addView(buildAdaptiveSchedulerCard(), textLayoutParams())
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

        contentRoot.addView(buildTopAppBar(), textLayoutParams())
        contentRoot.addView(learnSection, textLayoutParams(topMargin = 18))
        contentRoot.addView(styleSection, textLayoutParams(topMargin = 18))
        contentRoot.addView(schedulerSection, textLayoutParams(topMargin = 18))
        contentRoot.addView(aiLabSection, textLayoutParams(topMargin = 18))
        contentRoot.addView(deviceSection, textLayoutParams(topMargin = 18))
        contentShell.addView(
            mainScrollView,
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
        val bottomNavigation = buildBottomNavigation()
        root.addView(
            bottomNavigation,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ),
        )
        applySystemBarPadding(contentShell, contentRoot, bottomNavigation)
        setContentView(root)
        root.requestApplyInsets()
        updateVisibleSection()
    }

    private fun buildHeroCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedGradientBackground(
                startColor = PREVIEW_BACKGROUND,
                endColor = APP_LEARN_HERO_DEEP,
                radius = 32,
            )
            applySoftElevation(this, AI_HERO_ELEVATION_DP)
            setPadding(dp(24), dp(22), dp(24), dp(24))
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleRow.addView(
            TextView(this).apply {
                text = "Current word"
                setTextColor(APP_AI_HERO_TEXT)
                textSize = 14f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                includeFontPadding = false
                setButtonIcon(this, R.drawable.ic_learn, APP_AMBER)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        wordOptionsButton = iconBadge(
            iconRes = R.drawable.ic_settings,
            iconTint = APP_AI_HERO_TEXT,
            backgroundColor = Color.TRANSPARENT,
            sizeDp = 44,
        ).apply {
            contentDescription = "Advanced word options"
            isClickable = true
            isFocusable = true
            attachPressFeedback(this)
            setOnClickListener {
                performSelectionHaptic()
                showWordOptionsDialog()
            }
        }
        titleRow.addView(wordOptionsButton)
        card.addView(titleRow)

        hanziView = TextView(this).apply {
            setTextColor(APP_ON_PRIMARY)
            textSize = 60f
            gravity = Gravity.CENTER
            typeface = Typeface.create(SERIF_FAMILY, Typeface.BOLD)
            includeFontPadding = false
            setPadding(0, dp(34), 0, 0)
        }
        pinyinView = TextView(this).apply {
            setTextColor(APP_AI_HERO_TEXT)
            textSize = 22f
            gravity = Gravity.CENTER
            typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
            includeFontPadding = false
            setPadding(0, dp(10), 0, 0)
        }
        englishView = TextView(this).apply {
            setTextColor(APP_AMBER_SURFACE)
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
            setTextColor(APP_AI_HERO_TEXT)
            setPadding(0, dp(18), 0, 0)
        }
        card.addView(overlayStatusView)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(30), 0, 0)
        }
        overlayToggleButton = actionButton(getString(R.string.overlay_start), primary = true, iconRes = R.drawable.ic_play).apply {
            setOnClickListener {
                performActionHaptic()
                handleOverlayToggle()
            }
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
            performSelectionHaptic()
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
                performSelectionHaptic()
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
            background = ShimmerCardDrawable(
                startColor = PREVIEW_BACKGROUND,
                endColor = APP_PREVIEW_DEEP,
                shimmerColor = PREVIEW_SHIMMER,
                radiusPx = dp(28).toFloat(),
            )
            applySoftElevation(this, CARD_ELEVATION_DP)
            setPadding(dp(22), dp(22), dp(22), dp(24))
        }
        card.addView(
            TextView(this).apply {
                text = "Cover preview"
                setTextColor(APP_INVERSE_TEXT)
                textSize = 17f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                includeFontPadding = false
                setButtonIcon(this, R.drawable.ic_device, APP_AMBER)
            },
        )

        previewHanziView = TextView(this).apply {
            includeFontPadding = false
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
            typeface = Typeface.create(SERIF_FAMILY, Typeface.BOLD)
        }
        previewPinyinView = TextView(this).apply {
            includeFontPadding = false
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
            typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
        }
        previewEnglishView = TextView(this).apply {
            includeFontPadding = false
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
        }
        card.addView(previewHanziView)
        card.addView(previewPinyinView)
        card.addView(previewEnglishView)
        return card
    }

    private fun buildStyleCard(): LinearLayout {
        val card = appCard()
        card.addView(appSectionHeader("Text style", "Typography", R.drawable.ic_style))
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

    private fun buildAdaptiveSchedulerCard(): LinearLayout {
        val card = appCard()
        card.addView(appSectionHeader("Adaptive scheduler", "Context-aware learning", R.drawable.ic_clock))
        addAdaptiveSchedulerInfo(card)
        addAutoHideControl(card)
        return card
    }

    private fun buildStatsCard(): LinearLayout {
        val card = appCard().apply {
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        card.addView(appSectionHeader("Vocabulary Progress", "Estimated progress", R.drawable.ic_learn))
        hskFilterButton = actionButton("All", primary = false, iconRes = R.drawable.ic_level).apply {
            minHeight = dp(42)
            textSize = 13f
            setPadding(dp(14), 0, dp(14), 0)
            background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 21, strokeColor = APP_AI_SUBTLE_STROKE)
            setTextColor(APP_PRIMARY)
            setOnClickListener {
                performSelectionHaptic()
                showLearnHskFilterDialog()
            }
        }
        card.addView(
            hskFilterButton,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)).apply {
                topMargin = dp(12)
            },
        )

        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(14), 0, 0)
        }
        val rowOne = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val rowTwo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        statsSeenView = metricTile("Seen")
        statsFamiliarView = metricTile("Familiar")
        statsMasteredView = metricTile("Mastered")
        statsDueSoonView = metricTile("Due Soon")
        rowOne.addView(statsSeenView, weightedButtonLayoutParams())
        rowOne.addView(statsFamiliarView, weightedButtonLayoutParams(startMargin = 8))
        rowTwo.addView(statsMasteredView, weightedButtonLayoutParams())
        rowTwo.addView(statsDueSoonView, weightedButtonLayoutParams(startMargin = 8))
        grid.addView(rowOne)
        grid.addView(rowTwo, textLayoutParams(topMargin = 8))
        card.addView(grid)

        progressChartView = VocabularyProgressChartView(this).apply {
            minimumHeight = dp(118)
            setPadding(0, dp(2), 0, 0)
        }
        card.addView(progressChartView, textLayoutParams(topMargin = 12))

        recentProgressView = infoTextView().apply { textSize = 13f }
        card.addView(compactStatusPanel(recentProgressView, R.drawable.ic_check), textLayoutParams(topMargin = 10))

        val definitions = infoTextView().apply {
            textSize = 13f
            text = "New: barely seen. Learning: being reinforced. Familiar: seen across sessions. Stable: appears less often. Mastered: occasional maintenance."
        }
        card.addView(compactStatusPanel(definitions, R.drawable.ic_status_dot), textLayoutParams(topMargin = 8))
        return card
    }

    private fun buildAiLabHeroCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
            aiPackPillView = statusPill().apply {
                visibility = View.GONE
            }
            addView(
                TextView(this@MainActivity).apply {
                    text = "Optional local vocabulary generation."
                    setTextColor(APP_TEXT_SECONDARY)
                    textSize = 19f
                    typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
                    includeFontPadding = false
                },
                textLayoutParams(),
            )
            aiModelWarningView = TextView(this@MainActivity).apply {
                setTextColor(APP_ON_ERROR_CONTAINER)
                textSize = 16f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                includeFontPadding = true
                minHeight = dp(86)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(12), dp(18), dp(12))
                background = roundedStrokeBackground(AI_WARNING_SURFACE, radius = 18, strokeColor = APP_ERROR_CONTAINER)
                setButtonIcon(this, R.drawable.ic_warning, APP_ON_ERROR_CONTAINER)
            }
            addView(aiModelWarningView, textLayoutParams(topMargin = 34))
        }
    }

    private fun buildAiModelCard(): LinearLayout {
        val card = aiCard().apply {
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        aiModelPillView = TextView(this).apply {
            text = "GEMMA-4-E2B-IT.LITERTLM"
            setTextColor(APP_PRIMARY)
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(34)
            background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 8, strokeColor = APP_AI_SUBTLE_STROKE)
            setPadding(dp(12), 0, dp(12), 0)
            setButtonIcon(this, R.drawable.ic_device, APP_PRIMARY)
        }
        card.addView(aiModelPillView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        aiModelStatusView = TextView(this).apply {
            text = "Large local model required for autonomous on-device curriculum generation."
            setTextColor(APP_TEXT_SECONDARY)
            textSize = 17f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = true
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        card.addView(aiModelStatusView, textLayoutParams(topMargin = 18))
        aiDownloadProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                progressTintList = ColorStateList.valueOf(APP_AMBER)
                progressBackgroundTintList = ColorStateList.valueOf(APP_SURFACE_CONTAINER_HIGH)
            }
            minimumHeight = dp(8)
        }
        aiDownloadProgressView = TextView(this).apply {
            setTextColor(APP_TEXT_SECONDARY)
            textSize = 13f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            includeFontPadding = false
            gravity = Gravity.END
        }
        aiDownloadPanelView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBackground(APP_MUTED_SURFACE, radius = 14)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                addView(
                    LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        addView(
                            TextView(this@MainActivity).apply {
                                text = "Downloading..."
                                setTextColor(APP_PRIMARY)
                                textSize = 15f
                                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                                includeFontPadding = false
                                setButtonIcon(this, R.drawable.ic_download, APP_PRIMARY)
                            },
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                        )
                        addView(aiDownloadProgressView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    },
                    textLayoutParams(),
                )
                addView(aiDownloadProgressBar, textLayoutParams(topMargin = 14))
            }
        card.addView(
            aiDownloadPanelView,
            textLayoutParams(topMargin = 28),
        )
        aiModelActionButton = actionButton("Download model", primary = false, iconRes = R.drawable.ic_download).apply {
            setOnClickListener {
                performActionHaptic()
                handleModelAction()
            }
        }
        card.addView(aiModelActionButton, textLayoutParams(topMargin = 26))
        return card
    }

    private fun buildAiGenerationCard(): LinearLayout {
        val card = aiCard().apply {
            setPadding(dp(24), dp(28), dp(24), dp(24))
        }
        card.addView(
            TextView(this).apply {
                text = "Generation Output"
                setTextColor(APP_TEXT_PRIMARY)
                textSize = 24f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                includeFontPadding = false
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        aiGeneratedStatusView = TextView(this).apply {
            setTextColor(APP_TEXT_SECONDARY)
            textSize = 16f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = true
            gravity = Gravity.CENTER_HORIZONTAL
        }
        card.addView(aiGeneratedStatusView, textLayoutParams(topMargin = 14))
        card.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.START
                addView(buildFeaturePill("Tone-marked pinyin"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(54)))
                addView(buildFeaturePill("Traditional Hanzi"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(54)).apply {
                    topMargin = dp(10)
                })
                addView(buildFeaturePill("Semantic context"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(54)).apply {
                    topMargin = dp(10)
                })
            },
            textLayoutParams(topMargin = 24),
        )
        aiGenerationProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                indeterminateTintList = ColorStateList.valueOf(APP_AMBER)
                progressBackgroundTintList = ColorStateList.valueOf(APP_SURFACE_CONTAINER_HIGH)
            }
            minimumHeight = dp(8)
        }
        aiGenerationStatusView = infoTextView()
        aiGenerationCountButton = selectorButton(
            "Words",
            aiLabPreferences.generationTargetCount.toString(),
            R.drawable.ic_sparkle,
        ).apply {
            setOnClickListener {
                performSelectionHaptic()
                showGenerationTargetDialog()
            }
        }
        aiHskButton = selectorButton("Level", aiLabPreferences.hskLevel.label, R.drawable.ic_level).apply {
            setOnClickListener {
                performSelectionHaptic()
                showHskLevelDialog()
            }
        }
        aiGenerationCountButton.visibility = View.GONE
        aiHskButton.visibility = View.GONE
        card.addView(aiGenerationCountButton)
        card.addView(aiHskButton)
        card.addView(aiGenerationProgressBar, textLayoutParams(topMargin = 14))
        card.addView(aiGenerationStatusView, textLayoutParams(topMargin = 6))
        aiGenerateButton = actionButton("Generate now", primary = true, iconRes = R.drawable.ic_sparkle).apply {
            setOnClickListener {
                performActionHaptic()
                maybeRequestNotificationPermission()
                aiLabPreferences.generatedStatus = "${AiLabPreferences.GENERATED_RUNNING}: queued"
                AiVocabularyGenerationWorker.enqueue(this@MainActivity)
                render()
            }
        }
        card.addView(aiGenerateButton, textLayoutParams(topMargin = 28))
        aiGenerateRequirementView = TextView(this).apply {
                text = "Requires model download completion"
                setTextColor(APP_TEXT_SECONDARY)
                textSize = 12f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                gravity = Gravity.CENTER
                includeFontPadding = false
                setButtonIcon(this, R.drawable.ic_status_dot, APP_TEXT_SECONDARY)
            }
        card.addView(
            aiGenerateRequirementView,
            textLayoutParams(topMargin = 12),
        )
        return card
    }

    private fun buildAiSourceCard(): LinearLayout {
        val card = aiCard().apply {
            setPadding(dp(24), dp(28), dp(24), dp(24))
        }
        card.addView(
            TextView(this).apply {
                text = "Source Logic"
                setTextColor(APP_TEXT_PRIMARY)
                textSize = 24f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                includeFontPadding = false
            },
        )
        aiSourceStatusView = infoTextView()
        aiSourceStatusView.visibility = View.GONE
        card.addView(aiSourceStatusView)
        aiSourceButton = sourceSegmentButton("Built-in only").apply {
            setOnClickListener {
                performSelectionHaptic()
                aiLabPreferences.sourceMode = AiVocabularySourceMode.BUILT_IN_ONLY
                render()
            }
        }
        aiSourceMixButton = sourceSegmentButton("Mix both").apply {
            setOnClickListener {
                performSelectionHaptic()
                aiLabPreferences.sourceMode = AiVocabularySourceMode.MIX_BOTH
                render()
            }
        }
        card.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBackground(APP_MUTED_SURFACE, radius = 18)
                setPadding(dp(6), dp(6), dp(6), dp(6))
                addView(aiSourceButton, textLayoutParams())
                addView(aiSourceMixButton, textLayoutParams(topMargin = 6))
            },
            textLayoutParams(topMargin = 26),
        )
        return card
    }

    private fun buildAiAutomationCard(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val card = aiCard().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(22), dp(24), dp(22))
        }
        card.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(
                    TextView(this@MainActivity).apply {
                        text = "Daily generation"
                        setTextColor(APP_TEXT_PRIMARY)
                        textSize = 24f
                        typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                        includeFontPadding = false
                    },
                )
                aiTimeButton = TextView(this@MainActivity).apply {
                    text = formatGenerationTime()
                    setTextColor(APP_PRIMARY)
                    textSize = 16f
                    typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                    includeFontPadding = false
                    minHeight = dp(36)
                    gravity = Gravity.CENTER_VERTICAL
                    setButtonIcon(this, R.drawable.ic_clock, APP_PRIMARY)
                    attachPressFeedback(this)
                    setOnClickListener {
                        performSelectionHaptic()
                        showGenerationTimePicker()
                    }
                }
                addView(aiTimeButton, textLayoutParams(topMargin = 8))
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        aiScheduleStatusView = infoTextView()
        aiDailySwitch = Switch(this).apply {
            isChecked = aiLabPreferences.dailyGenerationEnabled
            setOnCheckedChangeListener { _, isChecked ->
                if (!updatingUi) {
                    performSelectionHaptic()
                    handleDailyToggle(isChecked)
                }
            }
        }
        card.addView(aiDailySwitch)
        root.addView(card, textLayoutParams())

        aiAlarmPermissionButton = actionButton("Alarm permission", primary = false, iconRes = R.drawable.ic_permission).apply {
            setOnClickListener {
                performSelectionHaptic()
                startActivity(AiGenerationScheduler.permissionIntent(this@MainActivity))
            }
        }
        root.addView(aiAlarmPermissionButton, textLayoutParams(topMargin = 12))
        aiScheduleStatusView.visibility = View.GONE
        root.addView(aiScheduleStatusView)
        aiDeleteModelButton = actionButton("Delete model", primary = false, iconRes = R.drawable.ic_delete).apply {
            background = roundedBackground(APP_ERROR, radius = 28)
            setTextColor(APP_ON_PRIMARY)
            setOnClickListener {
                performActionHaptic()
                showDeleteModelConfirmation()
            }
        }
        root.addView(aiDeleteModelButton, textLayoutParams(topMargin = 28))

        return root
    }

    private fun buildStatusCard(): LinearLayout {
        val card = appCard()
        card.addView(appSectionHeader("Device status", "Cover display", R.drawable.ic_device))
        permissionStatusView = infoTextView()
        rotationStatusView = infoTextView()
        displayStatusView = infoTextView()
        card.addView(statusPanel(permissionStatusView, R.drawable.ic_permission), textLayoutParams(topMargin = 16))
        card.addView(statusPanel(rotationStatusView, R.drawable.ic_clock), textLayoutParams(topMargin = 10))
        card.addView(statusPanel(displayStatusView, R.drawable.ic_device), textLayoutParams(topMargin = 10))
        card.addView(
            actionButton("Copy diagnostics", primary = false, iconRes = R.drawable.ic_copy).apply {
                setOnClickListener {
                    performConfirmHaptic()
                    copyDeviceDiagnostics()
                    playSuccessPulse(this)
                }
            },
            textLayoutParams(topMargin = 14),
        )
        return card
    }

    private fun render() {
        val info = repository.adaptiveRotationInfo()
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
            "Next ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(info.nextOpportunityMillis))}"
        bucketMetricView.text = "${repository.activeWords().size} words"
        frequencyMetricView.text = info.progress.status.readableLabel()
        renderOverlayActions()
        renderLearnStats()
        updateIntervalControls()
        updateAutoHideControls()
        permissionStatusView.text = overlayPermissionText()
        rotationStatusView.text =
            "Scheduler: adaptive\nNext opportunity: ${
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(info.nextOpportunityMillis))
            }\nContext: ${info.phoneLearningState.readableLabel()}"
        displayStatusView.text = displayDebugText()
        renderAiLab()
    }

    private fun renderLearnStats() {
        if (!::statsSeenView.isInitialized) return

        val words = repository.activeWords()
        val hsk = selectedHskFilter?.takeIf { it > 0 }
        val includeUncategorized = selectedHskFilter == -1
        val filteredWords = if (includeUncategorized) words.filter { it.hskLevel == null } else words.filter {
            hsk == null || it.hskLevel == hsk
        }
        val filteredIds = filteredWords.map { it.stableId() }.toSet()
        val progress = repository.progressById()
        val filteredProgress = filteredIds.map { progress[it] ?: WordProgress(it) }
        val sessions = repository.compactSessions()
        val stats = DailyStatsAggregator.aggregate(
            words = filteredWords,
            progressById = progress,
            sessions = sessions,
            nowMillis = System.currentTimeMillis(),
            hskLevel = hsk,
        )
        val seen = filteredProgress.count { it.timesDisplayed > 0 }
        val familiar = filteredProgress.count {
            it.status == WordStatus.FAMILIAR ||
                it.status == WordStatus.STABLE ||
                it.status == WordStatus.MASTERED
        }
        val mastered = filteredProgress.count { it.status == WordStatus.MASTERED }
        val dueSoon = filteredProgress.count {
            it.timesDisplayed > 0 &&
                it.status != WordStatus.HIDDEN &&
                it.status != WordStatus.MASTERED &&
                it.predictedRecall < SchedulerConfig.TOO_SOON_RECALL_THRESHOLD
        }

        statsSeenView.text = "Seen\n$seen / ${filteredWords.size}"
        statsFamiliarView.text = "Familiar\n$familiar"
        statsMasteredView.text = "Mastered\n$mastered"
        statsDueSoonView.text = "Due Soon\n$dueSoon"
        hskFilterButton.text = learnFilterLabel()
        progressChartView.setStats(
            DailyStatsAggregator.weeklySeries(
                words = filteredWords,
                progressById = progress,
                nowMillis = System.currentTimeMillis(),
                hskLevel = hsk,
            ),
        )
        recentProgressView.text =
            "This week: +${stats.movedToFamiliarToday} Familiar, +${stats.movedToStableToday} Stable, +${stats.movedToMasteredToday} Mastered\nEligible exposure today: ${
                formatInterval((stats.totalEligibleExposureMillisToday / 1000L).coerceAtLeast(0L))
            }"
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
            hasOverlayPermission -> "Overlay ready. Tap Start to show it on the cover display."
            else -> "Overlay permission needed before starting."
        }
        setVisibleAnimated(overlayStatusView, !overlayEnabled, slide = true)
        setVisibleAnimated(overlayPermissionContainer, !hasOverlayPermission, slide = true)
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

        aiModelWarningView.text = when {
            downloadProgress.isDownloading -> "Model downloading\nBuilt-in vocabulary remains available offline."
            modelReady -> "Model ready\nLocal AI generation is available on this device."
            modelFailed -> "Model download failed\nBuilt-in vocabulary remains available offline."
            else -> "Model not downloaded\nBuilt-in vocabulary remains available offline."
        }
        aiModelWarningView.setTextColor(if (modelReady) APP_PRIMARY else APP_ON_ERROR_CONTAINER)
        aiModelWarningView.background = roundedStrokeBackground(
            color = if (modelReady) APP_SUCCESS_SURFACE else AI_WARNING_SURFACE,
            radius = 18,
            strokeColor = if (modelReady) APP_SUCCESS_SURFACE else APP_ERROR_CONTAINER,
        )
        setButtonIcon(aiModelWarningView, if (modelReady) R.drawable.ic_check else R.drawable.ic_warning, if (modelReady) APP_PRIMARY else APP_ON_ERROR_CONTAINER)
        aiModelPillView.text = "GEMMA-4-E2B-IT.LITERTLM"
        aiModelPillView.setTextColor(APP_PRIMARY)
        aiModelPillView.background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 8, strokeColor = APP_AI_SUBTLE_STROKE)
        setButtonIcon(aiModelPillView, R.drawable.ic_device, APP_PRIMARY)
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
            text = "Large local model required for autonomous on-device curriculum generation.",
            iconRes = null,
            iconTint = APP_SECONDARY_TEXT,
        )
        aiDownloadProgressBar.progress = downloadProgress.percent
        aiDownloadProgressView.text =
            "${formatBytes(downloadProgress.downloadedBytes)} / ${formatBytes(downloadProgress.totalBytes)}"
        setVisibleAnimated(aiDownloadPanelView, downloadProgress.isDownloading, slide = true)
        setVisibleAnimated(aiGenerationProgressBar, generating, slide = true)
        setVisibleAnimated(aiGenerationStatusView, generating, slide = true)
        configureInfoLine(
            view = aiGenerationStatusView,
            text = "Building vocabulary${animatedDots()} ${generationStepLabel(aiLabPreferences.generatedStatus)}",
            iconRes = R.drawable.ic_sparkle,
            iconTint = APP_AMBER,
        )
        configureInfoLine(
            view = aiGeneratedStatusView,
            text = when {
                generatedFailed -> "Creates exactly $targetCount valid Traditional Chinese entries per synthesis cycle. Last generation failed."
                hasGeneratedPack -> "Creates exactly $targetCount valid Traditional Chinese entries per synthesis cycle. Last updated ${formatOptionalTime(aiLabPreferences.lastGenerationMillis)}."
                else -> "Creates exactly $targetCount valid Traditional Chinese entries per synthesis cycle."
            },
            iconRes = null,
            iconTint = APP_SECONDARY_TEXT,
        )
        configureInfoLine(
            view = aiSourceStatusView,
            text = "Active vocabulary: ${repository.activeWords().size} words",
            iconRes = R.drawable.ic_source,
            iconTint = APP_SECONDARY_TEXT,
        )
        configureSourceSegment(aiSourceButton, aiLabPreferences.sourceMode == AiVocabularySourceMode.BUILT_IN_ONLY)
        configureSourceSegment(aiSourceMixButton, aiLabPreferences.sourceMode == AiVocabularySourceMode.MIX_BOTH)
        configureSelectorButton(aiHskButton, "Level", aiLabPreferences.hskLevel.label, R.drawable.ic_level)
        configureSelectorButton(aiGenerationCountButton, "Words", targetCount.toString(), R.drawable.ic_sparkle)
        configureActionButton(
            button = aiModelActionButton,
            text = when {
                downloadProgress.isDownloading -> "Pause"
                modelReady -> "Model ready"
                modelFailed -> "Retry download"
                else -> "Download AI model"
            },
            primary = false,
            iconRes = if (downloadProgress.isDownloading) R.drawable.ic_stop else R.drawable.ic_download,
            backgroundColor = APP_SURFACE_CONTAINER_HIGH,
            textColor = APP_TEXT_PRIMARY,
        )
        val showModelAction = !modelReady || downloadProgress.isDownloading
        setVisibleAnimated(aiModelActionButton, showModelAction, slide = true)
        if (showModelAction) {
            setEnabledAnimated(aiModelActionButton, !downloadProgress.isDownloading && !generating && !modelReady)
        } else {
            aiModelActionButton.isEnabled = false
        }
        configureActionButton(
            button = aiGenerateButton,
            text = when {
                generating -> "Generating..."
                modelReady -> "Generate Now"
                else -> "Generate Now"
            },
            primary = true,
            iconRes = R.drawable.ic_sparkle,
            backgroundColor = if (modelReady) APP_PRIMARY_CONTAINER else APP_DISABLED_PRIMARY,
            textColor = APP_ON_PRIMARY,
        )
        setEnabledAnimated(aiGenerateButton, modelReady && !generating)
        aiGenerateRequirementView.text = when {
            generating -> "Generation is running"
            modelReady -> "Ready for on-device generation"
            else -> "Requires model download completion"
        }
        setButtonIcon(
            aiGenerateRequirementView,
            if (modelReady) R.drawable.ic_check else R.drawable.ic_status_dot,
            if (modelReady) APP_PRIMARY else APP_TEXT_SECONDARY,
        )
        aiGenerateRequirementView.setTextColor(if (modelReady) APP_PRIMARY else APP_TEXT_SECONDARY)
        configureActionButton(
            button = aiTimeButton,
            text = formatGenerationTime(),
            primary = false,
            iconRes = R.drawable.ic_clock,
            backgroundColor = Color.TRANSPARENT,
            textColor = APP_PRIMARY,
        )
        setEnabledAnimated(aiSourceButton, !generating)
        setEnabledAnimated(aiHskButton, !generating)
        setEnabledAnimated(aiGenerationCountButton, !generating)

        val exactStatus = if (AiGenerationScheduler.canScheduleExact(this)) "granted" else "blocked"
        val enabled = if (aiLabPreferences.dailyGenerationEnabled) "enabled" else "off"
        aiDailySwitch.setOnCheckedChangeListener(null)
        aiDailySwitch.isChecked = aiLabPreferences.dailyGenerationEnabled
        aiDailySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!updatingUi) {
                performSelectionHaptic()
                handleDailyToggle(isChecked)
            }
        }
        setVisibleAnimated(aiAlarmPermissionButton, exactStatus != "granted", slide = true)
        configureInfoLine(
            view = aiScheduleStatusView,
            text = "Daily $enabled · ${formatGenerationTime()} · next ${formatOptionalTime(aiLabPreferences.lastScheduledGenerationMillis)}",
            iconRes = R.drawable.ic_repeat,
            iconTint = APP_SECONDARY_TEXT,
        )
        configureActionButton(
            button = aiDeleteModelButton,
            text = "Delete model from phone",
            primary = false,
            iconRes = R.drawable.ic_delete,
            backgroundColor = APP_ERROR,
            textColor = APP_ON_PRIMARY,
        )
        setVisibleAnimated(aiDeleteModelButton, modelReady, slide = true)
        setSubtlePulse(aiModelPillView, downloadProgress.isDownloading)
        setSubtlePulse(aiPackPillView, generating)
        maybePlayAiStatusSuccess(modelReady, aiLabPreferences.generatedCount)
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
        val fallbackStatus = "Main display fallback disabled."
        val statusLines = if (overlayPreferences.overlayStatus.contains(fallbackStatus)) {
            overlayPreferences.overlayStatus
        } else {
            "${overlayPreferences.overlayStatus}\n$fallbackStatus"
        }
        return "$statusLines\nOverlay scope: cover display-wide; Samsung does not expose the active cover page.\nDetected displays:\n$displays"
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
        val panel = controlPanel()
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
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
            performSelectionHaptic()
            setVisibleAnimated(palette, palette.visibility != View.VISIBLE, slide = true)
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

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    performSelectionHaptic()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    performSelectionHaptic()
                    playPreviewPulse()
                }
            })
        }

        label.text = "$labelPrefix size: ${current}sp"
        panel.addView(header)
        panel.addView(seekBar, textLayoutParams(topMargin = 4))
        panel.addView(palette, textLayoutParams(topMargin = 8))
        root.addView(panel, textLayoutParams(topMargin = 14))
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
                        performSelectionHaptic()
                        onSelected(color)
                        playSuccessPulse(this)
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

    private fun addAdaptiveSchedulerInfo(root: LinearLayout) {
        intervalStatusView = infoTextView()
        root.addView(statusPanel(intervalStatusView, R.drawable.ic_clock), textLayoutParams(topMargin = 16))
        schedulerSpacingButton = actionButton("Minimum spacing", primary = false, iconRes = R.drawable.ic_clock).apply {
            setOnClickListener {
                performSelectionHaptic()
                showMinimumSpacingDialog()
            }
        }
        root.addView(schedulerSpacingButton, textLayoutParams(topMargin = 10))
        intervalChangeButton = actionButton("Restore hidden words", primary = false, iconRes = R.drawable.ic_repeat).apply {
            setOnClickListener {
                performSelectionHaptic()
                showRestoreHiddenWordsConfirmation()
            }
        }
        root.addView(intervalChangeButton, textLayoutParams(topMargin = 10))
        updateIntervalControls()
    }

    private fun showLearnHskFilterDialog() {
        val labels = arrayOf("All", "HSK 1", "HSK 2", "HSK 3", "HSK 4", "HSK 5", "HSK 6", "Uncategorized")
        val values = arrayOf<Int?>(null, 1, 2, 3, 4, 5, 6, -1)
        val selected = values.indexOf(selectedHskFilter).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("HSK filter")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                dialog.dismiss()
                selectedHskFilter = values[which]
                render()
            }
            .show()
    }

    private fun showWordOptionsDialog() {
        val word = repository.currentWord()
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        val hideButton = actionButton("Hide word", primary = false, iconRes = R.drawable.ic_stop).apply {
            configureActionButton(
                button = this,
                text = "Hide word",
                primary = false,
                iconRes = R.drawable.ic_stop,
                backgroundColor = APP_ERROR_CONTAINER,
                textColor = APP_ON_ERROR_CONTAINER,
            )
        }
        val resetButton = actionButton("Reset progress", primary = false, iconRes = R.drawable.ic_repeat)
        actions.addView(hideButton, textLayoutParams())
        actions.addView(resetButton, textLayoutParams(topMargin = 10))

        lateinit var dialog: AlertDialog
        dialog = AlertDialog.Builder(this)
            .setTitle("Advanced word options")
            .setMessage("${word.hanzi} [${PinyinToneFormatter.format(word)}]\n${word.english}")
            .setView(actions)
            .setNegativeButton("Cancel", null)
            .show()

        hideButton.setOnClickListener {
            performSelectionHaptic()
            dialog.dismiss()
            showHideWordConfirmation(word)
        }
        resetButton.setOnClickListener {
            performSelectionHaptic()
            dialog.dismiss()
            showResetProgressConfirmation(word)
        }
    }

    private fun showHideWordConfirmation(word: WordEntry) {
        showConfirmationDialog(
            title = "Hide word?",
            message = "This hides ${word.hanzi} from normal scheduling until hidden words are restored.",
            positiveText = "Hide word",
            destructive = true,
        ) {
            performConfirmHaptic()
            repository.hideCurrentWord()
            refreshOverlay()
            render()
        }
    }

    private fun showResetProgressConfirmation(word: WordEntry) {
        showConfirmationDialog(
            title = "Reset progress?",
            message = "This clears estimated progress for ${word.hanzi}. The scheduler will treat it like a new word again.",
            positiveText = "Reset progress",
            destructive = true,
        ) {
            performConfirmHaptic()
            repository.resetCurrentWordProgress()
            render()
        }
    }

    private fun showRestoreHiddenWordsConfirmation() {
        showConfirmationDialog(
            title = "Restore hidden words?",
            message = "Hidden words will return to normal scheduling and may appear again in future learning sessions.",
            positiveText = "Restore",
            destructive = false,
        ) {
            performConfirmHaptic()
            repository.restoreHiddenWords()
            Toast.makeText(this, "Hidden words restored.", Toast.LENGTH_SHORT).show()
            render()
        }
    }

    private fun showMinimumSpacingDialog() {
        val presets = listOf(
            IntervalPreset("15m", 15),
            IntervalPreset("30m", 30),
            IntervalPreset("60m", 60),
            IntervalPreset("90m", 90),
            IntervalPreset("120m", 120),
            IntervalPreset("240m", 240),
        )
        val labels = presets.map { it.label }.toMutableList().apply {
            add("Custom...")
        }
        val current = schedulerPreferences.minimumSpacingMinutes.toInt()
        val selected = presets.indexOfFirst { it.seconds == current }

        AlertDialog.Builder(this)
            .setTitle("Minimum word spacing")
            .setSingleChoiceItems(labels.toTypedArray(), selected) { dialog, which ->
                dialog.dismiss()
                performSelectionHaptic()
                if (which < presets.size) {
                    setMinimumSpacingMinutes(presets[which].seconds.toLong())
                } else {
                    showCustomMinimumSpacingDialog()
                }
            }
            .show()
    }

    private fun showCustomMinimumSpacingDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(schedulerPreferences.minimumSpacingMinutes.toString())
            selectAll()
            setPadding(dp(12), 0, dp(12), 0)
            background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 16, strokeColor = APP_OUTLINE_VARIANT)
            minHeight = dp(52)
        }

        AlertDialog.Builder(this)
            .setTitle("Custom spacing")
            .setMessage(
                "Choose ${SchedulerPreferences.MIN_MINIMUM_SPACING_MINUTES}-${SchedulerPreferences.MAX_MINIMUM_SPACING_MINUTES} minutes.",
            )
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Done") { _, _ ->
                val value = input.text.toString().toLongOrNull() ?: return@setPositiveButton
                performSelectionHaptic()
                setMinimumSpacingMinutes(value)
            }
            .show()
    }

    private fun setMinimumSpacingMinutes(minutes: Long) {
        val normalized = SchedulerPreferences.normalizeMinutes(minutes)
        schedulerPreferences.minimumSpacingMinutes = normalized
        if (normalized != minutes) {
            Toast.makeText(
                this,
                "Spacing adjusted to ${formatInterval(normalized * 60L)}.",
                Toast.LENGTH_SHORT,
            ).show()
        }
        refreshOverlay()
        render()
    }

    private fun showConfirmationDialog(
        title: String,
        message: String,
        positiveText: String,
        destructive: Boolean,
        onConfirm: () -> Unit,
    ) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(positiveText) { _, _ -> onConfirm() }
            .show()
        dialog.getButton(Dialog.BUTTON_POSITIVE)?.setTextColor(if (destructive) APP_ERROR else APP_PRIMARY)
    }

    private fun showGenerationTimePicker() {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                performSelectionHaptic()
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
                performSelectionHaptic()
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
                performSelectionHaptic()
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
                performSelectionHaptic()
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
                performSelectionHaptic()
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

        val info = repository.adaptiveRotationInfo()
        intervalStatusView.text =
            "Words change after a valid exposure opportunity. Minimum spacing target: ${
                formatInterval(info.minimumSpacingMillis / 1000L)
            }.\nCurrent context: ${info.phoneLearningState.readableLabel()}."
        if (::intervalChangeButton.isInitialized) {
            val hasHiddenWords = repository.progressById().values.any { it.status == WordStatus.HIDDEN || it.isHidden }
            configureActionButton(
                button = schedulerSpacingButton,
                text = "Minimum spacing: ${formatInterval(schedulerPreferences.minimumSpacingMinutes * 60L)}",
                primary = false,
                iconRes = R.drawable.ic_clock,
            )
            configureActionButton(
                button = intervalChangeButton,
                text = "Restore hidden words",
                primary = false,
                iconRes = R.drawable.ic_repeat,
            )
            setEnabledAnimated(intervalChangeButton, hasHiddenWords)
        }
    }

    private fun addAutoHideControl(root: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 22, strokeColor = APP_AI_SUBTLE_STROKE)
            setPadding(dp(16), dp(10), dp(14), dp(10))
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
                if (!updatingUi) {
                    performSelectionHaptic()
                    handleAutoHideToggle(isChecked)
                }
            }
        }
        row.addView(autoHideSwitch)
        root.addView(row, textLayoutParams(topMargin = 14))

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBackground(APP_MUTED_SURFACE, radius = 20)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            addView(
                iconBadge(
                    iconRes = R.drawable.ic_repeat,
                    iconTint = APP_SECONDARY_TEXT,
                    backgroundColor = APP_SURFACE,
                    sizeDp = 34,
                ),
                LinearLayout.LayoutParams(dp(34), dp(34)),
            )
            autoHideStatusView = infoTextView().apply {
                textSize = 13f
                setPadding(dp(12), 0, dp(10), 0)
            }
            addView(autoHideStatusView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            autoHideDurationButton = TextView(this@MainActivity).apply {
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                textSize = 12f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                includeFontPadding = false
                minHeight = dp(38)
                setPadding(dp(12), 0, dp(12), 0)
                attachPressFeedback(this)
                setOnClickListener {
                    performSelectionHaptic()
                    showAutoHideOptionsDialog()
                }
            }
            addView(autoHideDurationButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)))
        }
        root.addView(statusRow, textLayoutParams(topMargin = 10))
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

        autoHideStatusView.text = if (enabled) {
            "Floating text hides after $duration. Notification stays active."
        } else {
            "Floating text stays visible while the overlay is running."
        }
        autoHideStatusView.setTextColor(APP_TEXT_SECONDARY)
        setButtonIcon(autoHideStatusView, null, APP_TEXT_SECONDARY)
        autoHideDurationButton.text = "Visible for $duration"
        autoHideDurationButton.setTextColor(if (enabled) APP_PRIMARY else APP_TEXT_SECONDARY)
        autoHideDurationButton.background = roundedStrokeBackground(
            color = if (enabled) APP_SURFACE else APP_SURFACE_CONTAINER_HIGH,
            radius = 19,
            strokeColor = APP_AI_SUBTLE_STROKE,
        )
        setButtonIcon(autoHideDurationButton, R.drawable.ic_clock, if (enabled) APP_PRIMARY else APP_TEXT_SECONDARY)
        setEnabledAnimated(autoHideDurationButton, enabled)
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
                performSelectionHaptic()
                setAutoHideSeconds(presets[which].seconds)
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
        val dialog = Dialog(this).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCanceledOnTouchOutside(true)
        }
        val diagnostics = aiFailureDiagnostics()
        val root = ScrollView(this).apply {
            setPadding(dp(18), dp(24), dp(18), dp(24))
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
        }
        val rootContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(APP_SURFACE, radius = 34)
            clipToOutline = true
            applySoftElevation(this, NAV_ELEVATION_DP)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = DottedWarningDrawable(
                backgroundColor = AI_FAILURE_HEADER,
                dotColor = AI_FAILURE_DOT,
                radiusPx = dp(34).toFloat(),
                density = resources.displayMetrics.density,
            )
            setPadding(dp(24), dp(26), dp(24), dp(24))
            addView(
                iconBadge(
                    iconRes = R.drawable.ic_warning,
                    iconTint = APP_ON_PRIMARY,
                    backgroundColor = APP_ERROR,
                    sizeDp = 60,
                ),
                LinearLayout.LayoutParams(dp(60), dp(60)),
            )
            addView(
                TextView(this@MainActivity).apply {
                    text = "AI generation did not\ncomplete"
                    setTextColor(APP_ON_ERROR_CONTAINER)
                    textSize = 24f
                    gravity = Gravity.CENTER
                    typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
                    includeFontPadding = true
                    setLineSpacing(0f, 1f)
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = dp(14)
                },
            )
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(26), dp(24), dp(26), dp(24))
            addView(
                TextView(this@MainActivity).apply {
                    text = "Your built-in vocabulary is still active.\nYou can save a debug log to help us identify the issue, or dismiss this message to continue learning."
                    setTextColor(APP_TEXT_PRIMARY)
                    textSize = 16f
                    gravity = Gravity.CENTER
                    includeFontPadding = true
                    setLineSpacing(dp(3).toFloat(), 1f)
                },
            )
            addView(
                buildFailureDiagnosticPanel(diagnostics),
                textLayoutParams(topMargin = 22),
            )
            val saveButton = actionButton("Save debug log", primary = true, iconRes = R.drawable.ic_copy).apply {
                textSize = 16f
                minHeight = dp(58)
                configureActionButton(
                    button = this,
                    text = "Save debug log",
                    primary = true,
                    iconRes = R.drawable.ic_copy,
                    backgroundColor = APP_PRIMARY_CONTAINER,
                    textColor = APP_ON_PRIMARY,
                )
                setOnClickListener {
                    performActionHaptic()
                    aiLabPreferences.promptedFailureLogId = failureId
                    saveAiDebugLog()
                    dialog.dismiss()
                }
            }
            val dismissButton = actionButton("Dismiss", primary = false).apply {
                textSize = 16f
                minHeight = dp(58)
                background = roundedStrokeBackground(APP_SURFACE, radius = 29, strokeColor = APP_BORDER)
                setTextColor(APP_PRIMARY)
                setOnClickListener {
                    performSelectionHaptic()
                    aiLabPreferences.promptedFailureLogId = failureId
                    dialog.dismiss()
                }
            }
            addView(saveButton, textLayoutParams(topMargin = 24))
            addView(dismissButton, textLayoutParams(topMargin = 14))
        }
        card.addView(header, textLayoutParams())
        card.addView(body, textLayoutParams())
        rootContent.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        root.addView(
            rootContent,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        dialog.setContentView(root)
        dialog.setOnCancelListener {
            aiLabPreferences.promptedFailureLogId = failureId
        }
        dialog.setOnDismissListener {
            if (aiFailurePromptShowing) {
                aiLabPreferences.promptedFailureLogId = failureId
                aiFailurePromptShowing = false
            }
        }
        dialog.window?.setDimAmount(0.42f)
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun buildFailureDiagnosticPanel(diagnostics: AiFailureDiagnostics): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 14, strokeColor = APP_AI_SUBTLE_STROKE)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        TextView(this@MainActivity).apply {
                            text = "DIAGNOSTIC DETAILS"
                            setTextColor(APP_TEXT_PRIMARY)
                            textSize = 12f
                            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                            includeFontPadding = false
                        },
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                    )
                    addView(
                        ImageView(this@MainActivity).apply {
                            setImageResource(R.drawable.ic_device)
                            imageTintList = ColorStateList.valueOf(APP_TEXT_SECONDARY)
                            contentDescription = null
                        },
                        LinearLayout.LayoutParams(dp(24), dp(24)),
                    )
                },
            )
            addView(View(this@MainActivity).apply { setBackgroundColor(APP_AI_SUBTLE_STROKE) }, textLayoutParams(topMargin = 14).apply {
                height = dp(1)
            })
            addDiagnosticRow(
                label = "Accepted entries:",
                value = diagnostics.acceptedEntries,
                valueColor = APP_TEXT_PRIMARY,
                pill = true,
                topMargin = 16,
            )
            addDiagnosticRow(
                label = "Backend:",
                value = diagnostics.backend,
                valueColor = APP_ON_ERROR_CONTAINER,
                icon = "⊗",
                topMargin = 14,
            )
            addDiagnosticRow(
                label = "Model file:",
                value = diagnostics.modelFile,
                valueColor = APP_PRIMARY,
                icon = if (diagnostics.modelPresent) "⊙" else "⊗",
                topMargin = 14,
            )
        }

    private fun LinearLayout.addDiagnosticRow(
        label: String,
        value: String,
        valueColor: Int,
        icon: String? = null,
        pill: Boolean = false,
        topMargin: Int,
    ) {
        addView(
            LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    TextView(this@MainActivity).apply {
                        text = label
                        setTextColor(APP_TEXT_PRIMARY)
                        textSize = 15f
                        includeFontPadding = false
                        isSingleLine = true
                    },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, if (pill) 1f else 0.46f),
                )
                addView(
                    TextView(this@MainActivity).apply {
                        text = if (icon == null) value else "$icon $value"
                        setTextColor(valueColor)
                        textSize = 15f
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                        includeFontPadding = false
                        gravity = Gravity.CENTER_VERTICAL or Gravity.END
                        isSingleLine = true
                        if (pill) {
                            background = roundedBackground(APP_SURFACE_CONTAINER_HIGH, radius = 6)
                            setPadding(dp(12), dp(6), dp(12), dp(6))
                        }
                    },
                    if (pill) {
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    } else {
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.54f)
                    },
                )
            },
            textLayoutParams(topMargin = topMargin),
        )
    }

    private fun aiFailureDiagnostics(): AiFailureDiagnostics {
        val pendingLog = aiLabPreferences.pendingDebugLog
        val accepted = Regex("""Valid entries accepted:\s*([^\n]+)""")
            .find(pendingLog)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeUnless { it == "not available" }
            ?: aiLabPreferences.generatedCount.takeIf { it > 0 }?.toString()
            ?: "0"
        val backendReason = aiLabPreferences.generatedStatus
            .substringAfter("${AiLabPreferences.GENERATED_FAILED}:", missingDelimiterValue = "failed")
            .trim()
            .ifBlank { "failed" }
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        val modelPresent = aiModelManager.modelFile().exists()
        return AiFailureDiagnostics(
            acceptedEntries = "$accepted / ${aiLabPreferences.generationTargetCount}",
            backend = backendReason.ellipsizeEnd(13),
            modelFile = if (modelPresent) "Present" else "Missing",
            modelPresent = modelPresent,
        )
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
        if (aiModelManager.refreshStatus() != AiLabPreferences.MODEL_READY) {
            aiModelManager.startDownload()
        }
        render()
    }

    private fun showDeleteModelConfirmation() {
        showConfirmationDialog(
            title = "Delete AI model?",
            message = "This removes the downloaded model from this phone. Built-in vocabulary will keep working, and you can download the model again later.",
            positiveText = "Delete model",
            destructive = true,
        ) {
            performConfirmHaptic()
            val deleted = aiModelManager.deleteModel()
            Toast.makeText(
                this,
                if (deleted) "AI model deleted." else "Could not delete AI model.",
                Toast.LENGTH_SHORT,
            ).show()
            render()
        }
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
            animateContentIn(view, startDelay = index * WORD_STAGGER_MS)
        }
        listOf(nextWordMetricView, bucketMetricView, frequencyMetricView, overlayStatusView).forEachIndexed { index, view ->
            animateStatusRefresh(view, startDelay = 90L + index * 28L)
        }
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
            setPadding(0, dp(2), 0, dp(2))
            addView(
                TextView(this@MainActivity).apply {
                    topTitleView = this
                    text = activeTab.title
                    setTextColor(APP_TEXT_PRIMARY)
                    textSize = 28f
                    typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                    includeFontPadding = false
                    gravity = Gravity.CENTER_VERTICAL
                    minHeight = dp(48)
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
                    applySoftElevation(this, AI_CARD_ELEVATION_DP)
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
            background = roundedStrokeBackground(APP_SURFACE, radius = 0, strokeColor = APP_AI_SUBTLE_STROKE)
            setPadding(dp(18), dp(8), dp(18), dp(8))
            applySoftElevation(this, NAV_ELEVATION_DP)
            AppTab.values().forEach { tab ->
                addView(
                    tabButton(tab),
                    LinearLayout.LayoutParams(
                        0,
                        dp(64),
                        1f,
                    ).apply {
                        marginEnd = if (tab == AppTab.values().last()) 0 else dp(8)
                    },
                )
            }
        }

    private fun tabButton(tab: AppTab): ImageView =
        ImageView(this).apply {
            contentDescription = tab.title
            setImageResource(tab.iconRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
            isClickable = true
            isFocusable = true
            minimumHeight = dp(48)
            minimumWidth = dp(48)
            setPadding(0, dp(18), 0, dp(18))
            attachPressFeedback(this)
            setOnClickListener {
                if (activeTab == tab) return@setOnClickListener
                performSelectionHaptic()
                activeTab = tab
                updateVisibleSection(animate = true)
                scrollMainContentToTop(animate = true)
                render()
            }
            tabButtons[tab] = this
        }

    private fun updateVisibleSection(animate: Boolean = false) {
        if (!::learnSection.isInitialized) return

        val activeSection = when (activeTab) {
            AppTab.LEARN -> learnSection
            AppTab.STYLE -> styleSection
            AppTab.SCHEDULE -> schedulerSection
            AppTab.AI_LAB -> aiLabSection
            AppTab.DEVICE -> deviceSection
        }
        listOf(learnSection, styleSection, schedulerSection, aiLabSection, deviceSection).forEach { section ->
            section.visibility = if (section == activeSection) View.VISIBLE else View.GONE
        }
        if (animate) {
            animateContentIn(activeSection)
        }
        if (::topTitleView.isInitialized) {
            topTitleView.text = activeTab.title
        }
        updateTabStyles()
    }

    private fun scrollMainContentToTop(animate: Boolean) {
        if (!::mainScrollView.isInitialized) return

        mainScrollView.post {
            if (animate && animationsEnabled()) {
                mainScrollView.smoothScrollTo(0, 0)
            } else {
                mainScrollView.scrollTo(0, 0)
            }
        }
    }

    private fun updateTabStyles() {
        tabButtons.forEach { (tab, button) ->
            val selected = tab == activeTab
            val tint = if (selected) APP_PRIMARY else APP_TEXT_SECONDARY
            button.alpha = if (selected) 1f else 0.74f
            button.background = roundedBackground(if (selected) APP_SUCCESS_SURFACE else Color.TRANSPARENT, radius = 28)
            setNavigationTabIcon(button, tab.iconRes, tint)
            animateTabState(button, selected)
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

    private fun appCard(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedStrokeBackground(APP_SURFACE, radius = 28, strokeColor = APP_AI_SUBTLE_STROKE)
            applySoftElevation(this, AI_CARD_ELEVATION_DP)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

    private fun aiCard(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedStrokeBackground(APP_SURFACE, radius = 28, strokeColor = APP_AI_SUBTLE_STROKE)
            applySoftElevation(this, AI_CARD_ELEVATION_DP)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

    private fun appSectionHeader(title: String, subtitle: String, iconRes: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                iconBadge(
                    iconRes = iconRes,
                    iconTint = APP_PRIMARY,
                    backgroundColor = APP_SUCCESS_SURFACE,
                    sizeDp = 44,
                ),
                LinearLayout.LayoutParams(dp(44), dp(44)),
            )
            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), 0, 0, 0)
                    addView(
                        TextView(this@MainActivity).apply {
                            text = title
                            setTextColor(APP_TEXT_PRIMARY)
                            textSize = 22f
                            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                            includeFontPadding = false
                        },
                    )
                    addView(
                        TextView(this@MainActivity).apply {
                            text = subtitle
                            setTextColor(APP_TEXT_SECONDARY)
                            textSize = 13f
                            typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
                            includeFontPadding = true
                            setPadding(0, dp(5), 0, 0)
                        },
                    )
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
        }

    private fun aiSectionHeader(title: String, subtitle: String, iconRes: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                iconBadge(
                    iconRes = iconRes,
                    iconTint = APP_PRIMARY,
                    backgroundColor = APP_SUCCESS_SURFACE,
                    sizeDp = 44,
                ),
                LinearLayout.LayoutParams(dp(44), dp(44)),
            )
            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), 0, 0, 0)
                    addView(
                        TextView(this@MainActivity).apply {
                            text = title
                            setTextColor(APP_TEXT_PRIMARY)
                            textSize = 22f
                            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                            includeFontPadding = false
                        },
                    )
                    addView(
                        TextView(this@MainActivity).apply {
                            text = subtitle
                            setTextColor(APP_TEXT_SECONDARY)
                            textSize = 13f
                            typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
                            includeFontPadding = true
                            setPadding(0, dp(5), 0, 0)
                        },
                    )
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
        }

    private fun controlPanel(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 22, strokeColor = APP_AI_SUBTLE_STROKE)
            setPadding(dp(16), dp(14), dp(16), dp(12))
        }

    private fun statusPanel(textView: TextView, iconRes: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            background = roundedBackground(APP_MUTED_SURFACE, radius = 20)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            addView(
                iconBadge(
                    iconRes = iconRes,
                    iconTint = APP_SECONDARY_TEXT,
                    backgroundColor = APP_SURFACE,
                    sizeDp = 36,
                ),
                LinearLayout.LayoutParams(dp(36), dp(36)),
            )
            textView.setPadding(dp(12), 0, 0, 0)
            addView(textView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

    private fun compactStatusPanel(textView: TextView, iconRes: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            background = roundedBackground(APP_MUTED_SURFACE, radius = 16)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addView(
                iconBadge(
                    iconRes = iconRes,
                    iconTint = APP_SECONDARY_TEXT,
                    backgroundColor = APP_SURFACE,
                    sizeDp = 30,
                ),
                LinearLayout.LayoutParams(dp(30), dp(30)),
            )
            textView.setPadding(dp(10), 0, 0, 0)
            addView(textView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

    private fun aiMetaRow(label: String, value: String, iconRes: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBackground(APP_MUTED_SURFACE, radius = 20)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            addView(
                iconBadge(
                    iconRes = iconRes,
                    iconTint = APP_SECONDARY_TEXT,
                    backgroundColor = APP_SURFACE,
                    sizeDp = 36,
                ),
                LinearLayout.LayoutParams(dp(36), dp(36)),
            )
            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), 0, 0, 0)
                    addView(
                        TextView(this@MainActivity).apply {
                            text = label.uppercase(Locale.US)
                            setTextColor(APP_TEXT_SECONDARY)
                            textSize = 11f
                            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                            includeFontPadding = false
                        },
                    )
                    addView(
                        TextView(this@MainActivity).apply {
                            text = value
                            setTextColor(APP_PRIMARY)
                            textSize = 12f
                            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                            includeFontPadding = false
                            setPadding(0, dp(5), 0, 0)
                        },
                    )
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
        }

    private fun iconBadge(iconRes: Int, iconTint: Int, backgroundColor: Int, sizeDp: Int): ImageView =
        ImageView(this).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(iconTint)
            contentDescription = null
            background = roundedBackground(backgroundColor, radius = sizeDp / 2)
            setPadding(dp(10), dp(10), dp(10), dp(10))
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
        button.background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 18, strokeColor = APP_AI_SUBTLE_STROKE)
        setButtonIcon(button, iconRes, APP_PRIMARY)
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

    private fun setNavigationTabIcon(button: ImageView, iconRes: Int, color: Int) {
        button.setImageResource(iconRes)
        button.imageTintList = ColorStateList.valueOf(color)
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

    private fun metricTile(label: String): TextView =
        TextView(this).apply {
            text = "$label\n0"
            gravity = Gravity.CENTER
            setTextColor(APP_TEXT_PRIMARY)
            textSize = 13f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = true
            minHeight = dp(62)
            background = roundedStrokeBackground(APP_MUTED_SURFACE, radius = 14, strokeColor = APP_AI_SUBTLE_STROKE)
            setPadding(dp(8), dp(8), dp(8), dp(8))
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
            setTextColor(APP_TEXT_SECONDARY)
            textSize = 13f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = false
            minHeight = dp(54)
            gravity = Gravity.CENTER_VERTICAL
            background = roundedStrokeBackground(APP_BACKGROUND, radius = 27, strokeColor = APP_AI_SUBTLE_STROKE)
            setPadding(dp(18), 0, dp(22), 0)
            setButtonIcon(this, R.drawable.ic_check, APP_PRIMARY)
        }

    private fun sourceSegmentButton(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(APP_TEXT_SECONDARY)
            textSize = 16f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = false
            gravity = Gravity.CENTER
            minHeight = dp(58)
            isClickable = true
            isFocusable = true
            attachPressFeedback(this)
        }

    private fun configureSourceSegment(button: TextView, selected: Boolean) {
        button.setTextColor(if (selected) APP_PRIMARY else APP_TEXT_SECONDARY)
        button.background = if (selected) {
            roundedStrokeBackground(APP_SURFACE, radius = 16, strokeColor = APP_AI_SUBTLE_STROKE)
        } else {
            roundedBackground(Color.TRANSPARENT, radius = 16)
        }
        button.elevation = if (selected) dp(1).toFloat() else 0f
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

    private fun configureInfoLine(view: TextView, text: String, iconRes: Int?, iconTint: Int) {
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

    private fun roundedGradientBackground(startColor: Int, endColor: Int, radius: Int): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(startColor, endColor),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radius).toFloat()
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
                    if (animationsEnabled()) {
                        touchedView.animate()
                            .scaleX(0.97f)
                            .scaleY(0.97f)
                            .alpha(0.88f)
                            .translationZ(dp(2).toFloat())
                            .setDuration(PRESS_DURATION_MS)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (animationsEnabled()) {
                        touchedView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .translationZ(0f)
                            .setDuration(RELEASE_DURATION_MS)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    } else {
                        touchedView.scaleX = 1f
                        touchedView.scaleY = 1f
                        touchedView.alpha = 1f
                        touchedView.translationZ = 0f
                    }
                }
            }
            false
        }
    }

    private fun animateContentIn(view: View, startDelay: Long = 0L) {
        if (!animationsEnabled()) {
            view.alpha = 1f
            view.translationY = 0f
            return
        }

        view.animate().cancel()
        view.alpha = 0f
        view.translationY = dp(8).toFloat()
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(startDelay)
            .setDuration(CONTENT_ENTER_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateStatusRefresh(view: View, startDelay: Long = 0L) {
        if (!animationsEnabled()) return

        view.animate().cancel()
        view.alpha = 0.72f
        view.translationY = dp(3).toFloat()
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(startDelay)
            .setDuration(STATE_CHANGE_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateTabState(view: View, selected: Boolean) {
        if (!animationsEnabled()) {
            view.scaleX = if (selected) 1f else 0.96f
            view.scaleY = if (selected) 1f else 0.96f
            view.translationZ = if (selected) dp(1).toFloat() else 0f
            return
        }

        view.animate()
            .scaleX(if (selected) 1f else 0.96f)
            .scaleY(if (selected) 1f else 0.96f)
            .translationZ(if (selected) dp(1).toFloat() else 0f)
            .setDuration(STATE_CHANGE_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun setVisibleAnimated(view: View, visible: Boolean, slide: Boolean = false) {
        val targetVisibility = if (visible) View.VISIBLE else View.GONE

        view.animate().cancel()
        if (view.visibility == targetVisibility) {
            if (visible) {
                view.alpha = 1f
                view.translationY = 0f
            }
            return
        }

        if (!animationsEnabled()) {
            view.visibility = targetVisibility
            view.alpha = 1f
            view.translationY = 0f
            return
        }

        if (visible) {
            view.visibility = View.VISIBLE
            view.alpha = 0f
            if (slide) view.translationY = dp(6).toFloat()
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(STATE_CHANGE_DURATION_MS)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            view.animate()
                .alpha(0f)
                .translationY(if (slide) dp(4).toFloat() else 0f)
                .setDuration(STATE_CHANGE_DURATION_MS)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    view.visibility = View.GONE
                    view.alpha = 1f
                    view.translationY = 0f
                }
                .start()
        }
    }

    private fun setEnabledAnimated(view: View, enabled: Boolean, disabledAlpha: Float = 0.55f) {
        view.isEnabled = enabled
        val targetAlpha = if (enabled) 1f else disabledAlpha
        view.animate().cancel()
        if (!animationsEnabled()) {
            view.alpha = targetAlpha
            return
        }

        if (view.alpha == targetAlpha) {
            view.alpha = targetAlpha
            return
        }
        view.animate()
            .alpha(targetAlpha)
            .setDuration(STATE_CHANGE_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun playPreviewPulse() {
        listOf(previewHanziView, previewPinyinView, previewEnglishView).forEachIndexed { index, view ->
            playSuccessPulse(view, startDelay = index * 24L)
        }
    }

    private fun playSuccessPulse(view: View, startDelay: Long = 0L) {
        if (!animationsEnabled()) return

        view.animate().cancel()
        view.animate()
            .scaleX(1.035f)
            .scaleY(1.035f)
            .translationZ(dp(2).toFloat())
            .setStartDelay(startDelay)
            .setDuration(PULSE_DURATION_MS)
            .setInterpolator(OvershootInterpolator(1.4f))
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0f)
                    .setDuration(RELEASE_DURATION_MS)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun maybePlayAiStatusSuccess(modelReady: Boolean, generatedCount: Int) {
        val previousModelReady = lastRenderedModelReady
        if (previousModelReady == false && modelReady) {
            performConfirmHaptic()
            playSuccessPulse(aiModelPillView)
        }
        lastRenderedModelReady = modelReady

        val previousGeneratedCount = lastRenderedGeneratedCount
        if (previousGeneratedCount != null && previousGeneratedCount <= 0 && generatedCount > 0) {
            performConfirmHaptic()
            playSuccessPulse(aiPackPillView)
        }
        lastRenderedGeneratedCount = generatedCount
    }

    private fun performSelectionHaptic() {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun performActionHaptic() {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun performConfirmHaptic() {
        val feedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        window.decorView.performHapticFeedback(feedback)
    }

    private fun animationsEnabled(): Boolean =
        ValueAnimator.areAnimatorsEnabled()

    private fun setSubtlePulse(view: View, active: Boolean) {
        if (active && animationsEnabled()) {
            if (view.animation == null) {
                val pulse = AlphaAnimation(0.82f, 1f).apply {
                    duration = RUNNING_PULSE_DURATION_MS
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

    private fun String.ellipsizeEnd(maxChars: Int): String {
        if (length <= maxChars) return this
        return "${take((maxChars - 3).coerceAtLeast(1))}..."
    }

    private fun learnFilterLabel(): String =
        when (selectedHskFilter) {
            null -> "All"
            -1 -> "Uncategorized"
            else -> "HSK $selectedHskFilter"
        }

    private fun WordStatus.readableLabel(): String =
        when (this) {
            WordStatus.NEW -> "New"
            WordStatus.LEARNING -> "Learning"
            WordStatus.FAMILIAR -> "Familiar"
            WordStatus.STABLE -> "Stable"
            WordStatus.MASTERED -> "Mastered"
            WordStatus.RETIRED -> "Retired"
            WordStatus.HIDDEN -> "Hidden"
        }

    private fun PhoneLearningState.readableLabel(): String =
        when (this) {
            PhoneLearningState.ASLEEP_OR_INACTIVE -> "inactive pause"
            PhoneLearningState.LOCKED_IDLE -> "locked idle"
            PhoneLearningState.GLANCE_OPPORTUNITY -> "glance opportunity"
            PhoneLearningState.ACTIVE_PHONE_USE -> "active phone use"
            PhoneLearningState.FULL_APP_LEARNING -> "full app learning"
            PhoneLearningState.MOVING_OR_WORKOUT -> "moving"
            PhoneLearningState.UNKNOWN -> "unknown"
        }

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
    private fun applySystemBarPadding(contentShell: View, contentRoot: View, bottomNavigation: View) {
        val baseLeft = dp(18)
        val baseTop = dp(18)
        val baseRight = dp(18)
        val contentTop = dp(18)
        val contentBottom = dp(112)
        val navTop = dp(8)
        val navBottom = dp(8)
        contentShell.setPadding(baseLeft, baseTop, baseRight, 0)
        contentRoot.setPadding(0, contentTop, 0, contentBottom)
        bottomNavigation.setPadding(baseLeft, navTop, baseRight, navBottom)
        contentShell.setOnApplyWindowInsetsListener { _, insets ->
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
            contentShell.setPadding(
                baseLeft,
                baseTop + topInset,
                baseRight,
                0,
            )
            contentRoot.setPadding(0, contentTop, 0, contentBottom + bottomInset)
            bottomNavigation.setPadding(baseLeft, navTop, baseRight, navBottom + bottomInset)
            insets
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class AppTab(val title: String, val iconRes: Int) {
        LEARN("Learn", R.drawable.ic_learn),
        STYLE("Style", R.drawable.ic_style),
        SCHEDULE("Schedule", R.drawable.ic_clock),
        AI_LAB("AI Lab", R.drawable.ic_sparkle),
        DEVICE("Device", R.drawable.ic_device),
    }

    private data class IntervalPreset(val label: String, val seconds: Int)

    private data class AiFailureDiagnostics(
        val acceptedEntries: String,
        val backend: String,
        val modelFile: String,
        val modelPresent: Boolean,
    )

    private class VocabularyProgressChartView(context: android.content.Context) : View(context) {
        private var stats: List<DailyLearningStats> = emptyList()
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3E4945")
            textSize = 10.5f * resources.displayMetrics.scaledDensity
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        private val trackPath = Path()
        private val trackRect = RectF()
        private val dotRadius = 3.5f * resources.displayMetrics.density

        fun setStats(value: List<DailyLearningStats>) {
            stats = value
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val latest = stats.lastOrNull() ?: return
            val left = paddingLeft + 6f
            val right = width - paddingRight - 6f
            if (right <= left) return

            val counts = intArrayOf(
                latest.newCount,
                latest.learningCount,
                latest.familiarCount,
                latest.stableCount,
                latest.masteredCount,
            )
            val labels = arrayOf("New", "Learning", "Familiar", "Stable", "Mastered")
            val colors = intArrayOf(
                Color.parseColor("#DCE8E2"),
                Color.parseColor("#BFD7CF"),
                Color.parseColor("#8FC5B5"),
                Color.parseColor("#43A78D"),
                Color.parseColor("#006D5B"),
            )

            val barTop = paddingTop + 20f
            val barHeight = 18f * resources.displayMetrics.density
            trackRect.set(left, barTop, right, barTop + barHeight)
            val radius = barHeight / 2f
            fillPaint.shader = null
            fillPaint.color = Color.parseColor("#E8EFEB")
            canvas.drawRoundRect(trackRect, radius, radius, fillPaint)

            val total = counts.sum().coerceAtLeast(1)
            var segmentLeft = left
            trackPath.reset()
            trackPath.addRoundRect(trackRect, radius, radius, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(trackPath)
            counts.forEachIndexed { index, count ->
                if (count <= 0) return@forEachIndexed
                val segmentRight = if (index == counts.lastIndex) {
                    right
                } else {
                    segmentLeft + ((right - left) * count.toFloat() / total.toFloat())
                }
                fillPaint.color = colors[index]
                canvas.drawRect(segmentLeft, trackRect.top, segmentRight, trackRect.bottom, fillPaint)
                segmentLeft = segmentRight
            }
            canvas.restore()

            drawLegend(canvas, labels, colors, left, trackRect.bottom + 28f)
        }

        private fun drawLegend(canvas: Canvas, labels: Array<String>, colors: IntArray, left: Float, top: Float) {
            val rowGap = 22f * resources.displayMetrics.density
            val columnWidth = (width - paddingLeft - paddingRight - 12f) / 3f
            labels.forEachIndexed { index, label ->
                val row = if (index < 3) 0 else 1
                val column = if (index < 3) index else index - 3
                val x = left + column * columnWidth
                val y = top + row * rowGap
                fillPaint.shader = null
                fillPaint.color = colors[index]
                canvas.drawCircle(x + dotRadius, y - dotRadius, dotRadius, fillPaint)
                canvas.drawText(label, x + dotRadius * 3f, y, labelPaint)
            }
        }
    }

    private class ShimmerCardDrawable(
        private val startColor: Int,
        private val endColor: Int,
        private val shimmerColor: Int,
        private val radiusPx: Float,
    ) : Drawable() {
        private val rect = RectF()
        private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var progress = 0f
        private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3_200L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidateSelf()
            }
            start()
        }

        override fun draw(canvas: Canvas) {
            rect.set(bounds)
            if (rect.isEmpty) return

            basePaint.shader = LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                startColor,
                endColor,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRoundRect(rect, radiusPx, radiusPx, basePaint)

            val width = rect.width()
            val shimmerCenter = rect.left - width * 0.45f + width * 1.9f * progress
            shimmerPaint.shader = LinearGradient(
                shimmerCenter - width * 0.24f,
                rect.top,
                shimmerCenter + width * 0.24f,
                rect.bottom,
                intArrayOf(Color.TRANSPARENT, shimmerColor, Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRoundRect(rect, radiusPx, radiusPx, shimmerPaint)
        }

        override fun setAlpha(alpha: Int) {
            basePaint.alpha = alpha
            shimmerPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            basePaint.colorFilter = colorFilter
            shimmerPaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    private class DottedWarningDrawable(
        private val backgroundColor: Int,
        private val dotColor: Int,
        private val radiusPx: Float,
        density: Float,
    ) : Drawable() {
        private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }
        private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dotColor
            style = Paint.Style.FILL
        }
        private val path = Path()
        private val rect = RectF()
        private val dotRadius = 1.2f * density
        private val dotGap = 12f * density

        override fun draw(canvas: Canvas) {
            rect.set(bounds)
            path.reset()
            path.addRoundRect(
                rect,
                floatArrayOf(radiusPx, radiusPx, radiusPx, radiusPx, 0f, 0f, 0f, 0f),
                Path.Direction.CW,
            )
            canvas.drawPath(path, backgroundPaint)
            var y = rect.top + dotGap
            while (y < rect.bottom) {
                var x = rect.left + dotGap
                while (x < rect.right) {
                    canvas.drawCircle(x, y, dotRadius, dotPaint)
                    x += dotGap
                }
                y += dotGap
            }
        }

        override fun setAlpha(alpha: Int) {
            backgroundPaint.alpha = alpha
            dotPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            backgroundPaint.colorFilter = colorFilter
            dotPaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 42
        private const val DOWNLOAD_PROGRESS_REFRESH_MILLIS = 1_000L
        private const val PRESS_DURATION_MS = 90L
        private const val RELEASE_DURATION_MS = 140L
        private const val STATE_CHANGE_DURATION_MS = 160L
        private const val CONTENT_ENTER_DURATION_MS = 210L
        private const val PULSE_DURATION_MS = 120L
        private const val RUNNING_PULSE_DURATION_MS = 1_200L
        private const val WORD_STAGGER_MS = 45L
        private const val CARD_ELEVATION_DP = 2
        private const val AI_CARD_ELEVATION_DP = 1
        private const val AI_HERO_ELEVATION_DP = 4
        private const val NAV_ELEVATION_DP = 8

        private const val SANS_FAMILY = "sans-serif"
        private const val SERIF_FAMILY = "serif"
        private const val MODEL_DISPLAY_NAME = "Gemma 4 E2B"

        private val APP_BACKGROUND = Color.parseColor("#F7FAF7")
        private val APP_SURFACE = Color.parseColor("#FFFFFF")
        private val APP_MUTED_SURFACE = Color.parseColor("#F1F4F1")
        private val APP_SURFACE_CONTAINER_HIGH = Color.parseColor("#E5E9E6")
        private val PREVIEW_BACKGROUND = Color.parseColor("#2D3130")
        private val APP_PREVIEW_DEEP = Color.parseColor("#141918")
        private val PREVIEW_SHIMMER = Color.parseColor("#26FFFFFF")
        private val APP_TEXT_PRIMARY = Color.parseColor("#181C1B")
        private val APP_TEXT_SECONDARY = Color.parseColor("#3E4945")
        private val APP_INVERSE_TEXT = Color.parseColor("#EEF2EE")
        private val APP_OUTLINE_VARIANT = Color.parseColor("#BEC9C4")
        private val APP_BORDER = Color.parseColor("#6E7975")
        private val APP_PRIMARY = Color.parseColor("#005344")
        private val APP_PRIMARY_CONTAINER = Color.parseColor("#006D5B")
        private val APP_LEARN_HERO_DEEP = Color.parseColor("#012A24")
        private val APP_AI_HERO_DEEP = Color.parseColor("#00382F")
        private val APP_AI_HERO_TEXT = Color.parseColor("#D8F4EA")
        private val APP_AI_SUBTLE_STROKE = Color.parseColor("#D7E2DD")
        private val APP_ON_PRIMARY = Color.parseColor("#FFFFFF")
        private val APP_SECONDARY_CONTAINER = Color.parseColor("#CFE3EE")
        private val APP_SECONDARY_TEXT = Color.parseColor("#374953")
        private val APP_AMBER = Color.parseColor("#FFBA38")
        private val APP_AMBER_SURFACE = Color.parseColor("#FFF3D6")
        private val APP_ERROR = Color.parseColor("#BA1A1A")
        private val APP_ERROR_CONTAINER = Color.parseColor("#FFDAD6")
        private val APP_ON_ERROR_CONTAINER = Color.parseColor("#93000A")
        private val AI_FAILURE_HEADER = Color.parseColor("#FFD2D0")
        private val AI_FAILURE_DOT = Color.parseColor("#EFB3B0")
        private val AI_WARNING_SURFACE = Color.parseColor("#FFF4F1")
        private val APP_DISABLED_PRIMARY = Color.parseColor("#84BDB2")
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
