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
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.MotionEvent
import android.view.Display
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val customVocabularyScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: WordRepository
    private lateinit var overlayPreferences: OverlayPreferences
    private lateinit var schedulerPreferences: SchedulerPreferences
    private lateinit var aiLabPreferences: AiLabPreferences
    private lateinit var generatedVocabularyStore: GeneratedVocabularyStore
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
    private lateinit var customVocabularyLauncher: LinearLayout
    private lateinit var customVocabularyCountView: TextView
    private lateinit var wordOptionsButton: ImageView
    private lateinit var autoHideStatusView: TextView
    private lateinit var autoHideDurationButton: TextView
    private lateinit var autoHideSwitch: Switch
    private lateinit var permissionStatusView: TextView
    private lateinit var rotationStatusView: TextView
    private lateinit var displayStatusView: TextView
    private lateinit var aiModelNameView: TextView
    private lateinit var aiModelStatusView: TextView
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
    private var customVocabularyResolveJob: Job? = null
    private var customVocabularySheet: CustomVocabularySheetController? = null
    private var customVocabularyDraft = ""
    private var lastRenderedHanzi: String? = null
    private var lastRenderedModelReady: Boolean? = null

    private val downloadProgressRunnable = object : Runnable {
        override fun run() {
            if (::aiDownloadProgressBar.isInitialized) {
                render()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        customVocabularyDraft = savedInstanceState
            ?.getString(STATE_CUSTOM_VOCABULARY_DRAFT)
            .orEmpty()
        val restoreCustomVocabularySheet =
            savedInstanceState?.getBoolean(STATE_CUSTOM_VOCABULARY_SHEET_OPEN, false) == true
        repository = WordRepository(this)
        overlayPreferences = OverlayPreferences(this)
        schedulerPreferences = SchedulerPreferences(this)
        aiLabPreferences = AiLabPreferences(this)
        generatedVocabularyStore = GeneratedVocabularyStore(this)
        aiModelManager = AiModelManager(this)
        repository.recordFullAppOpen()
        WordUpdateScheduler.schedule(this)
        configureSystemBars()
        buildLayout()
        render()
        maybePromptForAiFailureLog()
        if (restoreCustomVocabularySheet) {
            mainScrollView.post { showCustomVocabularyDialog() }
        }
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
        customVocabularyResolveJob?.cancel()
        customVocabularyScope.cancel()
        customVocabularySheet?.dismiss()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CUSTOM_VOCABULARY_DRAFT, customVocabularyDraft)
        outState.putBoolean(
            STATE_CUSTOM_VOCABULARY_SHEET_OPEN,
            customVocabularySheet != null,
        )
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
            addView(buildCustomVocabularyLauncher(), textLayoutParams(topMargin = 12))
            addView(buildStatsCard(), textLayoutParams(topMargin = 16))
        }
        styleSection = sectionContainer().apply {
            addView(buildPreviewCard(), textLayoutParams())
            addView(buildStyleCard(), textLayoutParams(topMargin = 16))
        }
        schedulerSection = sectionContainer().apply {
            addView(buildAdaptiveSchedulerCard(), textLayoutParams())
            addView(buildSchedulerNote(), textLayoutParams(topMargin = 10))
        }
        aiLabSection = sectionContainer().apply {
            addView(buildAiWorkspaceCard(), textLayoutParams())
            addView(buildAiSourceCard(), textLayoutParams(topMargin = 16))
            addView(buildAiAutomationCard(), textLayoutParams(topMargin = 16))
        }
        deviceSection = sectionContainer().apply {
            addView(buildStatusCard(), textLayoutParams())
            addView(buildPromptReviewCard(), textLayoutParams(topMargin = 16))
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
            background = ShimmerCardDrawable(
                startColor = APP_HERO_BACKGROUND,
                endColor = APP_HERO_DEEP,
                shimmerColor = PREVIEW_SHIMMER,
                radiusPx = dp(32).toFloat(),
            )
            applySoftElevation(this, AI_HERO_ELEVATION_DP)
            setPadding(dp(24), dp(22), dp(24), dp(24))
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleRow.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    ImageView(this@MainActivity).apply {
                        setImageResource(R.drawable.ic_learn)
                        imageTintList = ColorStateList.valueOf(APP_AMBER)
                        contentDescription = null
                    },
                    LinearLayout.LayoutParams(dp(22), dp(22)),
                )
                addView(
                    TextView(this@MainActivity).apply {
                        text = "Current word"
                        setTextColor(APP_AI_HERO_TEXT)
                        textSize = 14f
                        typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                        includeFontPadding = false
                        setPadding(dp(8), 0, 0, 0)
                    },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                )
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

    private fun buildCustomVocabularyLauncher(): LinearLayout =
        LinearLayout(this).apply {
            customVocabularyLauncher = this
            id = R.id.custom_vocabulary_launcher
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            minimumHeight = dp(80)
            background = roundedStrokeBackground(
                color = APP_SURFACE,
                radius = 24,
                strokeColor = APP_AI_SUBTLE_STROKE,
            )
            applySoftElevation(this, AI_CARD_ELEVATION_DP)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            addView(
                iconBadge(
                    iconRes = R.drawable.ic_add,
                    iconTint = APP_PRIMARY,
                    backgroundColor = APP_SUCCESS_SURFACE,
                    sizeDp = 48,
                ),
                LinearLayout.LayoutParams(dp(48), dp(48)),
            )
            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), 0, dp(8), 0)
                    addView(
                        TextView(this@MainActivity).apply {
                            text = getString(R.string.custom_vocabulary_launcher_title)
                            setTextColor(APP_TEXT_PRIMARY)
                            textSize = 17f
                            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                            includeFontPadding = false
                        },
                    )
                    customVocabularyCountView = TextView(this@MainActivity).apply {
                        setTextColor(APP_TEXT_SECONDARY)
                        textSize = 13f
                        typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
                        includeFontPadding = true
                        setPadding(0, dp(4), 0, 0)
                    }
                    addView(customVocabularyCountView)
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
            addView(
                ImageView(this@MainActivity).apply {
                    setImageResource(R.drawable.ic_next)
                    imageTintList = ColorStateList.valueOf(APP_PRIMARY)
                    contentDescription = null
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    setPadding(dp(9), dp(9), dp(9), dp(9))
                },
                LinearLayout.LayoutParams(dp(48), dp(48)),
            )
            attachPressFeedback(this)
            setOnClickListener {
                performSelectionHaptic()
                showCustomVocabularyDialog()
            }
        }

    private fun buildPreviewCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ShimmerCardDrawable(
                startColor = APP_HERO_BACKGROUND,
                endColor = APP_HERO_DEEP,
                shimmerColor = PREVIEW_SHIMMER,
                radiusPx = dp(28).toFloat(),
            )
            applySoftElevation(this, CARD_ELEVATION_DP)
            setPadding(dp(22), dp(22), dp(22), dp(24))
        }
        card.addView(darkHeroTitleRow("Cover preview", R.drawable.ic_device))

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
        val card = shimmerHeroCard()
        card.addView(darkHeroHeader("Adaptive scheduler", "Context-aware learning", R.drawable.ic_clock))
        addAdaptiveSchedulerInfo(card)
        addAutoHideControl(card)
        return card
    }

    private fun buildSchedulerNote(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
            intervalStatusView = infoTextView().apply {
                textSize = 13f
                setTextColor(APP_TEXT_SECONDARY)
                setLineSpacing(dp(2).toFloat(), 1f)
            }
            addView(intervalStatusView)
            addView(
                infoTextView().apply {
                    textSize = 13f
                    setTextColor(APP_TEXT_SECONDARY)
                    setLineSpacing(dp(2).toFloat(), 1f)
                    text = "Notes\n  - Words update after useful exposure, not just elapsed time.\n  - The scheduler weighs valid display time, unlocks, app opens, days seen, and taps to estimate when a word should return."
                },
                textLayoutParams(topMargin = 8),
            )
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

    private fun buildAiWorkspaceCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ShimmerCardDrawable(
                startColor = APP_HERO_BACKGROUND,
                endColor = APP_HERO_DEEP,
                shimmerColor = PREVIEW_SHIMMER,
                radiusPx = dp(28).toFloat(),
            )
            applySoftElevation(this, AI_HERO_ELEVATION_DP)
            setPadding(dp(24), dp(28), dp(24), dp(24))
        }
        card.addView(darkHeroTitleRow("AI Workspace", R.drawable.ic_sparkle))
        aiModelNameView = TextView(this).apply {
            text = "Gemma-4-E2B-it"
            setTextColor(APP_AI_HERO_TEXT)
            textSize = 15f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = true
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        card.addView(aiModelNameView, textLayoutParams(topMargin = 24))
        aiModelStatusView = TextView(this).apply {
            setTextColor(APP_ON_PRIMARY)
            textSize = 30f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = true
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        card.addView(aiModelStatusView, textLayoutParams(topMargin = 2))
        aiGeneratedStatusView = TextView(this).apply {
            setTextColor(APP_AI_HERO_TEXT)
            textSize = 16f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = true
            setLineSpacing(dp(3).toFloat(), 1f)
        }
        card.addView(aiGeneratedStatusView, textLayoutParams(topMargin = 8))

        aiDownloadProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                progressTintList = ColorStateList.valueOf(APP_AMBER)
                progressBackgroundTintList = ColorStateList.valueOf(APP_WORKSPACE_PANEL_STROKE)
            }
            minimumHeight = dp(8)
        }
        aiDownloadProgressView = TextView(this).apply {
            setTextColor(APP_AI_HERO_TEXT)
            textSize = 13f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            includeFontPadding = false
            gravity = Gravity.END
        }
        aiDownloadPanelView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                background = roundedStrokeBackground(APP_WORKSPACE_PANEL, radius = 16, strokeColor = APP_WORKSPACE_PANEL_STROKE)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                addView(
                    LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        addView(
                            TextView(this@MainActivity).apply {
                                text = "Downloading..."
                                setTextColor(APP_ON_PRIMARY)
                                textSize = 15f
                                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                                includeFontPadding = false
                                setButtonIcon(this, R.drawable.ic_download, APP_AMBER)
                            },
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                        )
                        addView(aiDownloadProgressView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    },
                    textLayoutParams(),
                )
                addView(aiDownloadProgressBar, textLayoutParams(topMargin = 14))
            }
        card.addView(aiDownloadPanelView, textLayoutParams(topMargin = 22))

        aiGenerationProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                indeterminateTintList = ColorStateList.valueOf(APP_AMBER)
                progressBackgroundTintList = ColorStateList.valueOf(APP_WORKSPACE_PANEL_STROKE)
            }
            minimumHeight = dp(8)
        }
        aiGenerationStatusView = TextView(this).apply {
            setTextColor(APP_AI_HERO_TEXT)
            textSize = 14f
            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
            includeFontPadding = true
            visibility = View.GONE
        }
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
        card.addView(aiGenerationProgressBar, textLayoutParams(topMargin = 22))
        card.addView(aiGenerationStatusView, textLayoutParams(topMargin = 8))

        aiModelActionButton = actionButton("Download model", primary = false, iconRes = R.drawable.ic_download).apply {
            setOnClickListener {
                performActionHaptic()
                handleModelAction()
            }
        }
        card.addView(aiModelActionButton, textLayoutParams(topMargin = 26))

        aiGenerateButton = actionButton("Generate now", primary = true, iconRes = R.drawable.ic_sparkle).apply {
            visibility = View.GONE
            setOnClickListener {
                performActionHaptic()
                maybeRequestNotificationPermission()
                aiLabPreferences.generatedStatus = "${AiLabPreferences.GENERATED_RUNNING}: queued"
                AiVocabularyGenerationWorker.enqueue(this@MainActivity)
                render()
            }
        }
        card.addView(aiGenerateButton, textLayoutParams(topMargin = 12))
        aiGenerateRequirementView = TextView(this).apply {
                text = "Requires model download completion"
                setTextColor(APP_AI_HERO_TEXT)
                textSize = 12f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                gravity = Gravity.CENTER
                includeFontPadding = false
                setButtonIcon(this, R.drawable.ic_status_dot, APP_AMBER)
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
        val card = shimmerHeroCard()
        card.addView(darkHeroHeader("Device status", "Cover display", R.drawable.ic_device))
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

    private fun buildPromptReviewCard(): LinearLayout {
        val card = appCard()
        card.addView(
            appSectionHeader(
                title = "AI prompt review",
                subtitle = "Read-only generation details",
                iconRes = R.drawable.ic_sparkle,
            ),
        )
        card.addView(
            TextView(this).apply {
                text = "Review the prompts sent to the on-device model. Your current phrase-pack settings are reflected automatically."
                setTextColor(APP_TEXT_SECONDARY)
                textSize = 14f
                typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
                includeFontPadding = true
                setLineSpacing(dp(2).toFloat(), 1f)
            },
            textLayoutParams(topMargin = 16),
        )
        card.addView(
            actionButton("Review AI prompts", primary = false, iconRes = R.drawable.ic_sparkle).apply {
                setOnClickListener {
                    performSelectionHaptic()
                    showPromptReviewDialog()
                }
            },
            textLayoutParams(topMargin = 16),
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
        renderCustomVocabularyLauncher()
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

    private fun renderCustomVocabularyLauncher() {
        if (!::customVocabularyCountView.isInitialized) return

        val count = repository.customVocabularyCount()
        val baseDescription = getString(R.string.custom_vocabulary_launcher_subtitle)
        customVocabularyCountView.text = if (count == 0) {
            baseDescription
        } else {
            getString(
                R.string.custom_vocabulary_launcher_subtitle_with_count,
                baseDescription,
                resources.getQuantityString(R.plurals.custom_vocabulary_count, count, count),
            )
        }
        customVocabularyLauncher.contentDescription = getString(
            R.string.custom_vocabulary_launcher_accessibility,
            customVocabularyCountView.text,
        )
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
        val preserveGeneratedStatus = generating ||
            aiLabPreferences.generatedStatus.startsWith(AiLabPreferences.GENERATED_FAILED)
        val generatedMetadata = generatedVocabularyStore.syncMetadata(preserveStatus = preserveGeneratedStatus)
        val modelReady = modelStatus == AiLabPreferences.MODEL_READY
        val modelFailed = modelStatus == AiLabPreferences.MODEL_FAILED
        val generatedFailed = aiLabPreferences.generatedStatus.startsWith(AiLabPreferences.GENERATED_FAILED)
        val hasGeneratedPack = generatedMetadata.hasEntries
        val targetCount = aiLabPreferences.generationTargetCount

        aiModelStatusView.text = when {
            generating -> "Generation running"
            downloadProgress.isDownloading -> "Downloading"
            generatedFailed -> "Generation failed"
            modelReady -> "Ready for Generation"
            else -> "Model needed"
        }
        val workspaceStatusColor = if (generatedFailed || modelFailed) APP_ERROR_CONTAINER else APP_ON_PRIMARY
        aiModelStatusView.setTextColor(workspaceStatusColor)
        setButtonIcon(
            aiModelStatusView,
            when {
                generatedFailed || modelFailed -> R.drawable.ic_warning
                generating -> R.drawable.ic_sparkle
                downloadProgress.isDownloading -> R.drawable.ic_download
                modelReady -> R.drawable.ic_check
                else -> R.drawable.ic_device
            },
            if (generatedFailed || modelFailed) APP_ERROR_CONTAINER else APP_AMBER,
        )
        aiDownloadProgressBar.progress = downloadProgress.percent
        aiDownloadProgressView.text =
            "${formatBytes(downloadProgress.downloadedBytes)} / ${formatBytes(downloadProgress.totalBytes)}"
        setVisibleAnimated(aiDownloadPanelView, downloadProgress.isDownloading, slide = true)
        setVisibleAnimated(aiGenerationProgressBar, generating, slide = true)
        setVisibleAnimated(aiGenerationStatusView, generating, slide = true)
        aiGenerationStatusView.text = "Building vocabulary${animatedDots()} ${generationStepLabel(aiLabPreferences.generatedStatus)}"
        aiGenerationStatusView.setTextColor(APP_AI_HERO_TEXT)
        setButtonIcon(aiGenerationStatusView, R.drawable.ic_sparkle, APP_AMBER)
        aiGeneratedStatusView.text = when {
            downloadProgress.isDownloading -> "Built-in vocabulary stays available while the local model downloads."
            generatedFailed -> "Last run did not complete. Built-in vocabulary remains active."
            hasGeneratedPack -> "Updated ${formatGeneratedUpdateTime(generatedMetadata.lastGenerationMillis)}"
            modelReady -> "Creates $targetCount Traditional Chinese entries per run."
            else -> "Download the local model to create personalized Traditional Chinese vocabulary."
        }
        aiGeneratedStatusView.setTextColor(if (generatedFailed || modelFailed) AI_FAILURE_HEADER else APP_AI_HERO_TEXT)
        setButtonIcon(aiGeneratedStatusView, null, APP_AI_HERO_TEXT)
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
                downloadProgress.isDownloading -> "Downloading..."
                modelReady -> "Ready"
                modelFailed -> "Retry download"
                else -> "Download AI model"
            },
            primary = false,
            iconRes = if (downloadProgress.isDownloading) R.drawable.ic_download else R.drawable.ic_download,
            backgroundColor = if (modelFailed) APP_ERROR_CONTAINER else APP_AMBER_SURFACE,
            textColor = if (modelFailed) APP_ON_ERROR_CONTAINER else APP_PRIMARY,
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
        setVisibleAnimated(aiGenerateButton, modelReady || generating, slide = true)
        setEnabledAnimated(aiGenerateButton, modelReady && !generating)
        aiGenerateRequirementView.text = when {
            generating -> "Generation is running"
            downloadProgress.isDownloading -> "Download in progress"
            generatedFailed -> "Last generation failed"
            modelReady -> "Ready for Generation"
            else -> "Requires model download completion"
        }
        setButtonIcon(
            aiGenerateRequirementView,
            if (modelReady) R.drawable.ic_check else R.drawable.ic_status_dot,
            if (modelReady) APP_AMBER else APP_AI_HERO_TEXT,
        )
        aiGenerateRequirementView.setTextColor(if (generatedFailed) AI_FAILURE_HEADER else APP_AI_HERO_TEXT)
        setVisibleAnimated(aiGenerateRequirementView, !modelReady || generating || generatedFailed, slide = true)
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
        maybePlayAiStatusSuccess(modelReady)
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
        schedulerSpacingButton = actionButton("Minimum spacing", primary = false, iconRes = R.drawable.ic_clock).apply {
            setOnClickListener {
                performSelectionHaptic()
                showMinimumSpacingDialog()
            }
        }
        root.addView(schedulerSpacingButton, textLayoutParams(topMargin = 18))
        intervalChangeButton = actionButton("Restore hidden words", primary = false, iconRes = R.drawable.ic_repeat).apply {
            setOnClickListener {
                performSelectionHaptic()
                showRestoreHiddenWordsConfirmation()
            }
        }
        root.addView(intervalChangeButton, textLayoutParams(topMargin = 10))
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

    private fun showCustomVocabularyDialog() {
        customVocabularySheet?.dismiss()
        customVocabularySheet = CustomVocabularySheetController().also { it.show() }
    }

    private inner class CustomVocabularySheetController {
        private val dialog = Dialog(this@MainActivity)
        private lateinit var capturePanel: LinearLayout
        private lateinit var loadingPanel: LinearLayout
        private lateinit var reviewPanel: LinearLayout
        private lateinit var messagePanel: LinearLayout
        private lateinit var rawInput: EditText
        private lateinit var rawCounter: TextView
        private lateinit var captureError: TextView
        private lateinit var loadingHeading: TextView
        private lateinit var loadingBody: TextView
        private lateinit var loadingCancelButton: TextView
        private lateinit var reviewHeading: TextView
        private lateinit var reviewHelp: TextView
        private lateinit var reviewPreview: LinearLayout
        private lateinit var reviewHanziPreview: TextView
        private lateinit var reviewPinyinPreview: TextView
        private lateinit var reviewEnglishPreview: TextView
        private lateinit var hanziInput: EditText
        private lateinit var pinyinInput: EditText
        private lateinit var englishInput: EditText
        private lateinit var reviewError: TextView
        private lateinit var reviewPrimaryButton: TextView
        private lateinit var reviewLaterButton: TextView
        private lateinit var messageHeading: TextView
        private lateinit var messageBody: TextView
        private lateinit var messagePrimaryButton: TextView
        private lateinit var messageSecondaryButton: TextView
        private lateinit var messageTertiaryButton: TextView
        private lateinit var closeButton: ImageView
        private var preserveDraftAfterDismiss = false
        private var updatingReviewFields = false
        private var manualReview = false
        private var dismissAllowed = true

        init {
            dialog.setCanceledOnTouchOutside(false)
            dialog.setOnKeyListener { _, keyCode, event ->
                keyCode == KeyEvent.KEYCODE_BACK &&
                    event.action == KeyEvent.ACTION_UP &&
                    !dismissAllowed
            }
            dialog.setContentView(buildSheetContent())
            dialog.setOnDismissListener {
                customVocabularyResolveJob?.cancel()
                customVocabularyResolveJob = null
                if (!preserveDraftAfterDismiss) customVocabularyDraft = ""
                if (customVocabularySheet === this) customVocabularySheet = null
            }
        }

        fun show() {
            dialog.show()
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                attributes = attributes.apply { dimAmount = 0.42f }
                setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE,
                )
            }
            showCapture(requestFocus = true)
        }

        fun dismiss() {
            if (dialog.isShowing) dialog.dismiss()
        }

        private fun buildSheetContent(): View =
            FrameLayout(this@MainActivity).apply {
                id = R.id.custom_vocabulary_sheet
                setPadding(dp(12), dp(20), dp(12), dp(12))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    accessibilityPaneTitle = getString(R.string.custom_vocabulary_sheet_title)
                }
                addView(
                    ScrollView(this@MainActivity).apply {
                        isFillViewport = false
                        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                        clipToPadding = false
                        addView(
                            LinearLayout(this@MainActivity).apply {
                                orientation = LinearLayout.VERTICAL
                                background = roundedBackground(APP_SURFACE, radius = 28)
                                applySoftElevation(this, NAV_ELEVATION_DP)
                                setPadding(dp(20), dp(12), dp(20), dp(22))
                                addView(buildSheetHeader())
                                capturePanel = buildCapturePanel()
                                loadingPanel = buildLoadingPanel()
                                reviewPanel = buildReviewPanel()
                                messagePanel = buildMessagePanel()
                                addView(capturePanel, textLayoutParams(topMargin = 20))
                                addView(loadingPanel, textLayoutParams(topMargin = 20))
                                addView(reviewPanel, textLayoutParams(topMargin = 20))
                                addView(messagePanel, textLayoutParams(topMargin = 20))
                            },
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            ),
                        )
                    },
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM,
                    ),
                )
            }

        private fun buildSheetHeader(): LinearLayout =
            LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(
                    View(this@MainActivity).apply {
                        background = roundedBackground(APP_OUTLINE_VARIANT, radius = 2)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    },
                    LinearLayout.LayoutParams(dp(40), dp(4)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    },
                )
                addView(
                    LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        addView(
                            LinearLayout(this@MainActivity).apply {
                                orientation = LinearLayout.VERTICAL
                                addView(
                                    TextView(this@MainActivity).apply {
                                        text = getString(R.string.custom_vocabulary_sheet_title)
                                        setTextColor(APP_TEXT_PRIMARY)
                                        textSize = 23f
                                        typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                                        includeFontPadding = false
                                    },
                                )
                                addView(
                                    TextView(this@MainActivity).apply {
                                        text = getString(R.string.custom_vocabulary_sheet_subtitle)
                                        setTextColor(APP_TEXT_SECONDARY)
                                        textSize = 14f
                                        includeFontPadding = true
                                        setLineSpacing(dp(2).toFloat(), 1f)
                                        setPadding(0, dp(5), 0, 0)
                                    },
                                )
                            },
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                        )
                        addView(
                            iconBadge(
                                iconRes = R.drawable.ic_close,
                                iconTint = APP_TEXT_PRIMARY,
                                backgroundColor = APP_MUTED_SURFACE,
                                sizeDp = 48,
                            ).apply {
                                closeButton = this
                                contentDescription = getString(R.string.custom_vocabulary_close)
                                isClickable = true
                                isFocusable = true
                                attachPressFeedback(this)
                                setOnClickListener {
                                    if (dismissAllowed) {
                                        performSelectionHaptic()
                                        dialog.cancel()
                                    }
                                }
                            },
                            LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                                marginStart = dp(12)
                            },
                        )
                    },
                    textLayoutParams(topMargin = 16),
                )
            }

        private fun buildCapturePanel(): LinearLayout =
            statePanel().apply {
                addView(sheetHeading(getString(R.string.custom_vocabulary_capture_heading)))
                addView(
                    sheetBody(getString(R.string.custom_vocabulary_capture_help)),
                    textLayoutParams(topMargin = 6),
                )
                rawInput = sheetInput(
                    id = R.id.custom_vocabulary_raw_input,
                    hint = getString(R.string.custom_vocabulary_raw_hint),
                    maxLength = CUSTOM_VOCABULARY_INPUT_MAX_CHARS,
                    multiline = true,
                )
                addView(
                    sheetLabel(getString(R.string.custom_vocabulary_raw_label), rawInput.id),
                    textLayoutParams(topMargin = 18),
                )
                addView(rawInput, textLayoutParams(topMargin = 6))
                rawCounter = TextView(this@MainActivity).apply {
                    setTextColor(APP_TEXT_SECONDARY)
                    textSize = 12f
                    gravity = Gravity.END
                    includeFontPadding = true
                }
                addView(rawCounter, textLayoutParams(topMargin = 4))
                captureError = sheetStatusText(error = true)
                addView(captureError, textLayoutParams(topMargin = 8))
                addView(
                    actionButton(
                        getString(R.string.custom_vocabulary_generate),
                        primary = true,
                        iconRes = R.drawable.ic_sparkle,
                    ).apply {
                        setOnClickListener {
                            performActionHaptic()
                            generatePreview()
                        }
                    },
                    textLayoutParams(topMargin = 14),
                )
                rawInput.doAfterTextChanged { editable ->
                    customVocabularyDraft = editable?.toString().orEmpty()
                    updateRawCounter()
                    clearCaptureError()
                }
                rawInput.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        generatePreview()
                        true
                    } else {
                        false
                    }
                }
            }

        private fun buildLoadingPanel(): LinearLayout =
            statePanel(centered = true).apply {
                addView(
                    ProgressBar(this@MainActivity).apply {
                        isIndeterminate = true
                        contentDescription = getString(R.string.custom_vocabulary_generating_body)
                    },
                    LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    },
                )
                loadingHeading = sheetHeading(getString(R.string.custom_vocabulary_generating_heading)).apply {
                    gravity = Gravity.CENTER
                    isFocusable = true
                }
                addView(loadingHeading, textLayoutParams(topMargin = 16))
                loadingBody = sheetBody(getString(R.string.custom_vocabulary_generating_body)).apply {
                    gravity = Gravity.CENTER
                    accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                }
                addView(loadingBody, textLayoutParams(topMargin = 8))
                loadingCancelButton = actionButton(
                    getString(R.string.custom_vocabulary_cancel_generation),
                    primary = false,
                ).apply {
                    setOnClickListener {
                        customVocabularyResolveJob?.cancel()
                        showCapture(requestFocus = true)
                    }
                }
                addView(loadingCancelButton, textLayoutParams(topMargin = 18))
            }

        private fun buildReviewPanel(): LinearLayout =
            statePanel().apply {
                reviewHeading = sheetHeading(getString(R.string.custom_vocabulary_review_heading)).apply {
                    isFocusable = true
                }
                addView(reviewHeading)
                reviewHelp = sheetBody(getString(R.string.custom_vocabulary_review_help))
                addView(reviewHelp, textLayoutParams(topMargin = 6))
                reviewPreview = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = ShimmerCardDrawable(
                        startColor = APP_HERO_BACKGROUND,
                        endColor = APP_HERO_DEEP,
                        shimmerColor = PREVIEW_SHIMMER,
                        radiusPx = dp(24).toFloat(),
                    )
                    setPadding(dp(18), dp(18), dp(18), dp(18))
                    reviewHanziPreview = TextView(this@MainActivity).apply {
                        setTextColor(APP_ON_PRIMARY)
                        textSize = 40f
                        gravity = Gravity.CENTER
                        typeface = Typeface.create(SERIF_FAMILY, Typeface.BOLD)
                        includeFontPadding = false
                    }
                    reviewPinyinPreview = TextView(this@MainActivity).apply {
                        setTextColor(APP_AI_HERO_TEXT)
                        textSize = 18f
                        gravity = Gravity.CENTER
                        includeFontPadding = true
                        setPadding(0, dp(8), 0, 0)
                    }
                    reviewEnglishPreview = TextView(this@MainActivity).apply {
                        setTextColor(APP_AMBER_SURFACE)
                        textSize = 18f
                        gravity = Gravity.CENTER
                        typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                        includeFontPadding = true
                        setPadding(0, dp(8), 0, 0)
                    }
                    addView(reviewHanziPreview)
                    addView(reviewPinyinPreview)
                    addView(reviewEnglishPreview)
                }
                addView(reviewPreview, textLayoutParams(topMargin = 16))
                hanziInput = sheetInput(
                    id = R.id.custom_vocabulary_hanzi_input,
                    hint = getString(R.string.custom_vocabulary_hanzi_hint),
                    maxLength = GeneratedVocabularyValidator.MAX_HANZI_CHARS,
                )
                pinyinInput = sheetInput(
                    id = R.id.custom_vocabulary_pinyin_input,
                    hint = getString(R.string.custom_vocabulary_pinyin_hint),
                    maxLength = GeneratedVocabularyValidator.MAX_PINYIN_CHARS,
                )
                englishInput = sheetInput(
                    id = R.id.custom_vocabulary_english_input,
                    hint = getString(R.string.custom_vocabulary_english_hint),
                    maxLength = GeneratedVocabularyValidator.MAX_ENGLISH_CHARS,
                )
                addView(
                    sheetLabel(getString(R.string.custom_vocabulary_hanzi_label), hanziInput.id),
                    textLayoutParams(topMargin = 18),
                )
                addView(hanziInput, textLayoutParams(topMargin = 6))
                addView(
                    sheetLabel(getString(R.string.custom_vocabulary_pinyin_label), pinyinInput.id),
                    textLayoutParams(topMargin = 14),
                )
                addView(pinyinInput, textLayoutParams(topMargin = 6))
                addView(
                    sheetLabel(getString(R.string.custom_vocabulary_english_label), englishInput.id),
                    textLayoutParams(topMargin = 14),
                )
                addView(englishInput, textLayoutParams(topMargin = 6))
                reviewError = sheetStatusText(error = true)
                addView(reviewError, textLayoutParams(topMargin = 10))
                reviewPrimaryButton = actionButton(
                    getString(R.string.custom_vocabulary_add_now),
                    primary = true,
                    iconRes = R.drawable.ic_learn,
                ).apply {
                    setOnClickListener { submitCandidate(activateNow = true) }
                }
                addView(reviewPrimaryButton, textLayoutParams(topMargin = 16))
                reviewLaterButton = actionButton(
                    getString(R.string.custom_vocabulary_add_later),
                    primary = false,
                    iconRes = R.drawable.ic_clock,
                ).apply {
                    setOnClickListener { submitCandidate(activateNow = false) }
                }
                addView(reviewLaterButton, textLayoutParams(topMargin = 10))
                addView(
                    actionButton(getString(R.string.custom_vocabulary_change_input), primary = false).apply {
                        setOnClickListener { showCapture(requestFocus = true) }
                    },
                    textLayoutParams(topMargin = 10),
                )
                listOf(hanziInput, pinyinInput, englishInput).forEach { input ->
                    input.doAfterTextChanged {
                        if (!updatingReviewFields) {
                            clearReviewError()
                            refreshReviewPreview()
                        }
                    }
                }
            }

        private fun buildMessagePanel(): LinearLayout =
            statePanel(centered = true).apply {
                messageHeading = sheetHeading("").apply {
                    gravity = Gravity.CENTER
                    isFocusable = true
                }
                addView(messageHeading)
                messageBody = sheetBody("").apply {
                    gravity = Gravity.CENTER
                    accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                }
                addView(messageBody, textLayoutParams(topMargin = 8))
                messagePrimaryButton = actionButton("", primary = true)
                messageSecondaryButton = actionButton("", primary = false)
                messageTertiaryButton = actionButton("", primary = false)
                addView(messagePrimaryButton, textLayoutParams(topMargin = 20))
                addView(messageSecondaryButton, textLayoutParams(topMargin = 10))
                addView(messageTertiaryButton, textLayoutParams(topMargin = 10))
            }

        private fun statePanel(centered: Boolean = false): LinearLayout =
            LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = if (centered) Gravity.CENTER_HORIZONTAL else Gravity.NO_GRAVITY
                visibility = View.GONE
            }

        private fun sheetHeading(value: String): TextView =
            TextView(this@MainActivity).apply {
                text = value
                setTextColor(APP_TEXT_PRIMARY)
                textSize = 20f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                includeFontPadding = false
            }

        private fun sheetBody(value: String): TextView =
            TextView(this@MainActivity).apply {
                text = value
                setTextColor(APP_TEXT_SECONDARY)
                textSize = 15f
                typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
                includeFontPadding = true
                setLineSpacing(dp(3).toFloat(), 1f)
            }

        private fun sheetLabel(value: String, inputId: Int): TextView =
            TextView(this@MainActivity).apply {
                text = value
                labelFor = inputId
                setTextColor(APP_TEXT_PRIMARY)
                textSize = 14f
                typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                includeFontPadding = false
            }

        private fun sheetInput(
            id: Int,
            hint: String,
            maxLength: Int,
            multiline: Boolean = false,
        ): EditText =
            EditText(this@MainActivity).apply {
                this.id = id
                this.hint = hint
                setTextColor(APP_TEXT_PRIMARY)
                setHintTextColor(APP_TEXT_SECONDARY)
                textSize = 16f
                typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
                filters = arrayOf(InputFilter.LengthFilter(maxLength))
                background = sheetInputBackground(error = false)
                minimumHeight = dp(if (multiline) 88 else 56)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                gravity = if (multiline) Gravity.TOP or Gravity.START else Gravity.CENTER_VERTICAL
                inputType = if (multiline) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                } else {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                }
                imeOptions = if (multiline) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
                if (multiline) {
                    minLines = 2
                    maxLines = 4
                    isSingleLine = false
                } else {
                    maxLines = 2
                    isSingleLine = false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                }
            }

        private fun sheetStatusText(error: Boolean): TextView =
            sheetBody("").apply {
                setTextColor(if (error) APP_ON_ERROR_CONTAINER else APP_TEXT_SECONDARY)
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                visibility = View.GONE
                if (error) setButtonIcon(this, R.drawable.ic_warning, APP_ERROR)
            }

        private fun sheetInputBackground(error: Boolean): Drawable =
            roundedStrokeBackground(
                color = APP_MUTED_SURFACE,
                radius = 16,
                strokeColor = if (error) APP_ERROR else APP_OUTLINE_VARIANT,
            )

        private fun setActivePanel(panel: View) {
            listOf(capturePanel, loadingPanel, reviewPanel, messagePanel).forEach {
                it.visibility = if (it === panel) View.VISIBLE else View.GONE
            }
        }

        private fun showCapture(requestFocus: Boolean) {
            customVocabularyResolveJob?.cancel()
            customVocabularyResolveJob = null
            setDismissEnabled(true)
            setActivePanel(capturePanel)
            captureError.visibility = View.GONE
            rawInput.background = sheetInputBackground(error = false)
            if (rawInput.text.toString() != customVocabularyDraft) {
                rawInput.setText(customVocabularyDraft)
                rawInput.setSelection(rawInput.text.length)
            }
            updateRawCounter()
            if (requestFocus) {
                dialog.window?.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE,
                )
                rawInput.post {
                    rawInput.requestFocus()
                    getSystemService(InputMethodManager::class.java)
                        ?.showSoftInput(rawInput, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }

        private fun updateRawCounter() {
            rawCounter.text = getString(
                R.string.custom_vocabulary_character_count,
                rawInput.text?.length ?: 0,
            )
        }

        private fun clearCaptureError() {
            if (!::captureError.isInitialized) return
            captureError.visibility = View.GONE
            rawInput.background = sheetInputBackground(error = false)
        }

        private fun showCaptureError(message: String) {
            captureError.text = message
            captureError.visibility = View.VISIBLE
            rawInput.background = sheetInputBackground(error = true)
            rawInput.requestFocus()
            captureError.post { captureError.announceForAccessibility(message) }
        }

        private fun generatePreview() {
            val raw = rawInput.text.toString().trim()
            if (raw.isEmpty() || raw.length > CUSTOM_VOCABULARY_INPUT_MAX_CHARS) {
                showCaptureError(getString(R.string.custom_vocabulary_input_required))
                return
            }
            customVocabularyDraft = raw
            hideKeyboard()
            repository.resolveKnownVocabulary(raw)?.let { candidate ->
                showReview(candidate, isManual = false)
                return
            }
            if (aiModelManager.refreshStatus() != AiLabPreferences.MODEL_READY) {
                showMissingModel()
                return
            }

            showLoading(
                heading = getString(R.string.custom_vocabulary_generating_heading),
                body = getString(R.string.custom_vocabulary_generating_body),
                canCancel = true,
            )
            customVocabularyResolveJob?.cancel()
            customVocabularyResolveJob = customVocabularyScope.launch {
                try {
                    val candidate = withContext(Dispatchers.Default) {
                        LiteRtVocabularyResolver(aiModelManager.modelFile()).resolve(raw)
                    }
                    CustomVocabularyValidator.validationError(candidate)?.let { error(it) }
                    if (dialog.isShowing && customVocabularySheet === this@CustomVocabularySheetController) {
                        showReview(candidate, isManual = false)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    if (dialog.isShowing && customVocabularySheet === this@CustomVocabularySheetController) {
                        showResolutionError()
                    }
                }
            }
        }

        private fun showLoading(heading: String, body: String, canCancel: Boolean) {
            hideKeyboard()
            setDismissEnabled(canCancel)
            setActivePanel(loadingPanel)
            loadingHeading.text = heading
            loadingBody.text = body
            loadingCancelButton.visibility = if (canCancel) View.VISIBLE else View.GONE
            loadingHeading.post {
                loadingHeading.requestFocus()
                loadingBody.announceForAccessibility(body)
            }
        }

        private fun showReview(candidate: ResolvedVocabularyCandidate, isManual: Boolean) {
            hideKeyboard()
            setDismissEnabled(true)
            manualReview = isManual
            updatingReviewFields = true
            hanziInput.setText(candidate.hanzi)
            pinyinInput.setText(candidate.pinyin)
            englishInput.setText(candidate.english)
            updatingReviewFields = false
            clearReviewError()
            refreshReviewPreview()
            setActivePanel(reviewPanel)
            reviewHeading.post { reviewHeading.requestFocus() }
        }

        private fun showManualReview() {
            val raw = customVocabularyDraft.trim()
            val looksLikeHanzi = raw.isNotEmpty() &&
                raw.length <= GeneratedVocabularyValidator.MAX_HANZI_CHARS &&
                raw.any { it.code in 0x3400..0x9FFF }
            showReview(
                ResolvedVocabularyCandidate(
                    hanzi = if (looksLikeHanzi) raw else "",
                    pinyin = "",
                    english = "",
                ),
                isManual = true,
            )
        }

        private fun refreshReviewPreview() {
            val candidate = reviewedCandidate()
            val existing = candidate.hanzi.takeIf { it.isNotBlank() }?.let(repository::findWordByHanzi)
            val previewHanzi = existing?.hanzi ?: candidate.hanzi
            val previewPinyin = existing?.let(PinyinToneFormatter::format) ?: candidate.pinyin
            val previewEnglish = existing?.english ?: candidate.english
            reviewHanziPreview.text = previewHanzi.ifBlank { "—" }
            reviewPinyinPreview.text = previewPinyin.ifBlank { "—" }.let { "[$it]" }
            reviewEnglishPreview.text = previewEnglish.ifBlank { "—" }
            reviewPreview.contentDescription = getString(
                R.string.custom_vocabulary_preview_accessibility,
                previewHanzi.ifBlank { "blank Traditional Chinese" },
                previewPinyin.ifBlank { "blank pinyin" },
                previewEnglish.ifBlank { "blank English meaning" },
            )
            reviewHelp.text = when {
                existing != null -> getString(R.string.custom_vocabulary_duplicate_help)
                manualReview -> getString(
                    R.string.custom_vocabulary_manual_help,
                    customVocabularyDraft,
                )
                else -> getString(R.string.custom_vocabulary_review_help)
            }
            configureActionButton(
                button = reviewPrimaryButton,
                text = getString(
                    if (existing == null) {
                        R.string.custom_vocabulary_add_now
                    } else {
                        R.string.custom_vocabulary_learn_existing
                    },
                ),
                primary = true,
                iconRes = R.drawable.ic_learn,
            )
            reviewLaterButton.visibility = if (existing == null) View.VISIBLE else View.GONE
        }

        private fun reviewedCandidate(): ResolvedVocabularyCandidate =
            ResolvedVocabularyCandidate(
                hanzi = hanziInput.text.toString().trim(),
                pinyin = pinyinInput.text.toString().trim(),
                english = englishInput.text.toString().trim(),
            )

        private fun clearReviewError() {
            if (!::reviewError.isInitialized) return
            reviewError.visibility = View.GONE
            listOf(hanziInput, pinyinInput, englishInput).forEach {
                it.background = sheetInputBackground(error = false)
            }
        }

        private fun showReviewError(message: String, candidate: ResolvedVocabularyCandidate) {
            reviewError.text = message
            reviewError.visibility = View.VISIBLE
            val focusTarget = when {
                message.contains("Chinese", ignoreCase = true) ||
                    message.contains("Hanzi", ignoreCase = true) -> hanziInput
                message.contains("Pinyin", ignoreCase = true) -> pinyinInput
                else -> englishInput
            }
            focusTarget.background = sheetInputBackground(error = true)
            focusTarget.requestFocus()
            reviewError.post { reviewError.announceForAccessibility(message) }
        }

        private fun submitCandidate(activateNow: Boolean) {
            performActionHaptic()
            val reviewed = reviewedCandidate()
            val existing = reviewed.hanzi
                .takeIf(String::isNotBlank)
                ?.let(repository::findWordByHanzi)
            val candidate = existing?.let { word ->
                ResolvedVocabularyCandidate(
                    hanzi = word.hanzi,
                    pinyin = PinyinToneFormatter.format(word),
                    english = word.english,
                )
            } ?: reviewed
            val validationError = CustomVocabularyValidator.validationError(candidate)
            if (validationError != null) {
                showReviewError(validationError, candidate)
                return
            }

            showLoading(
                heading = getString(R.string.custom_vocabulary_saving_heading),
                body = getString(R.string.custom_vocabulary_saving_body),
                canCancel = false,
            )
            customVocabularyResolveJob?.cancel()
            customVocabularyResolveJob = customVocabularyScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        repository.addCustomVocabulary(candidate, activateNow)
                    }
                    if (!dialog.isShowing || customVocabularySheet !== this@CustomVocabularySheetController) return@launch

                    customVocabularyDraft = ""
                    refreshOverlay()
                    render()
                    performConfirmHaptic()
                    val wordLabel = "${result.word.hanzi} [${PinyinToneFormatter.format(result.word)}]"
                    val successMessage = when {
                        result.wasExisting && result.activated -> getString(
                            R.string.custom_vocabulary_success_existing,
                            wordLabel,
                        )
                        result.activated -> getString(R.string.custom_vocabulary_success_now, wordLabel)
                        else -> getString(R.string.custom_vocabulary_success_later, wordLabel)
                    }
                    showMessage(
                        title = getString(R.string.custom_vocabulary_success_title),
                        body = successMessage,
                        iconRes = R.drawable.ic_check,
                        iconTint = APP_SUCCESS,
                        primaryText = getString(R.string.custom_vocabulary_done),
                        onPrimary = { dialog.dismiss() },
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    if (dialog.isShowing && customVocabularySheet === this@CustomVocabularySheetController) {
                        showSaveError(candidate, activateNow)
                    }
                }
            }
        }

        private fun showMissingModel() {
            showMessage(
                title = getString(R.string.custom_vocabulary_model_missing_title),
                body = getString(R.string.custom_vocabulary_model_missing_body),
                iconRes = R.drawable.ic_download,
                iconTint = APP_PRIMARY,
                primaryText = getString(R.string.custom_vocabulary_open_ai_lab),
                onPrimary = { openAiLabWithDraft() },
                secondaryText = getString(R.string.custom_vocabulary_enter_manually),
                onSecondary = { showManualReview() },
                tertiaryText = getString(R.string.custom_vocabulary_back_to_input),
                onTertiary = { showCapture(requestFocus = true) },
            )
        }

        private fun showResolutionError() {
            showMessage(
                title = getString(R.string.custom_vocabulary_resolution_failed_title),
                body = getString(R.string.custom_vocabulary_resolution_failed_body),
                iconRes = R.drawable.ic_warning,
                iconTint = APP_ERROR,
                primaryText = getString(R.string.custom_vocabulary_retry),
                onPrimary = {
                    setActivePanel(capturePanel)
                    generatePreview()
                },
                secondaryText = getString(R.string.custom_vocabulary_enter_manually),
                onSecondary = { showManualReview() },
                tertiaryText = getString(R.string.custom_vocabulary_back_to_input),
                onTertiary = { showCapture(requestFocus = true) },
            )
        }

        private fun showSaveError(candidate: ResolvedVocabularyCandidate, activateNow: Boolean) {
            showMessage(
                title = getString(R.string.custom_vocabulary_save_failed_title),
                body = getString(R.string.custom_vocabulary_save_failed_body),
                iconRes = R.drawable.ic_warning,
                iconTint = APP_ERROR,
                primaryText = getString(R.string.custom_vocabulary_retry_save),
                onPrimary = {
                    showReview(candidate, manualReview)
                    submitCandidate(activateNow)
                },
                secondaryText = getString(R.string.custom_vocabulary_back_to_review),
                onSecondary = { showReview(candidate, manualReview) },
            )
        }

        private fun showMessage(
            title: String,
            body: String,
            iconRes: Int,
            iconTint: Int,
            primaryText: String,
            onPrimary: () -> Unit,
            secondaryText: String? = null,
            onSecondary: (() -> Unit)? = null,
            tertiaryText: String? = null,
            onTertiary: (() -> Unit)? = null,
        ) {
            hideKeyboard()
            setDismissEnabled(true)
            setActivePanel(messagePanel)
            messageHeading.text = title
            setButtonIcon(messageHeading, iconRes, iconTint)
            messageBody.text = body
            bindMessageButton(messagePrimaryButton, primaryText, onPrimary, primary = true)
            bindOptionalMessageButton(messageSecondaryButton, secondaryText, onSecondary)
            bindOptionalMessageButton(messageTertiaryButton, tertiaryText, onTertiary)
            messageHeading.post {
                messageHeading.requestFocus()
                messageBody.announceForAccessibility("$title. $body")
            }
        }

        private fun bindMessageButton(
            button: TextView,
            text: String,
            action: () -> Unit,
            primary: Boolean,
        ) {
            button.visibility = View.VISIBLE
            configureActionButton(button, text, primary)
            button.setOnClickListener {
                performSelectionHaptic()
                action()
            }
        }

        private fun bindOptionalMessageButton(
            button: TextView,
            text: String?,
            action: (() -> Unit)?,
        ) {
            if (text == null || action == null) {
                button.visibility = View.GONE
                button.setOnClickListener(null)
            } else {
                bindMessageButton(button, text, action, primary = false)
            }
        }

        private fun openAiLabWithDraft() {
            preserveDraftAfterDismiss = true
            dialog.dismiss()
            activeTab = AppTab.AI_LAB
            updateVisibleSection(animate = true)
            scrollMainContentToTop(animate = true)
            render()
            aiModelActionButton.post {
                aiModelActionButton.requestFocus()
                aiModelActionButton.announceForAccessibility(
                    getString(R.string.custom_vocabulary_ai_lab_focus),
                )
            }
        }

        private fun hideKeyboard() {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            val focused = dialog.currentFocus ?: rawInput
            getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(focused.windowToken, 0)
            focused.clearFocus()
        }

        private fun setDismissEnabled(enabled: Boolean) {
            dismissAllowed = enabled
            if (::closeButton.isInitialized) {
                closeButton.isEnabled = enabled
                closeButton.alpha = if (enabled) 1f else 0.45f
            }
        }
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

    private fun showPromptReviewDialog() {
        val sections = AiPromptReview.sections(
            hskLevel = aiLabPreferences.hskLevel,
            targetCount = aiLabPreferences.generationTargetCount,
        )
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            sections.forEachIndexed { index, section ->
                if (index > 0) {
                    addView(
                        View(this@MainActivity).apply {
                            setBackgroundColor(APP_OUTLINE_VARIANT)
                        },
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                            topMargin = dp(24)
                            bottomMargin = dp(24)
                        },
                    )
                }
                addView(
                    TextView(this@MainActivity).apply {
                        text = section.title
                        setTextColor(APP_TEXT_PRIMARY)
                        textSize = 17f
                        typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                        includeFontPadding = false
                    },
                )
                addView(
                    TextView(this@MainActivity).apply {
                        text = section.description
                        setTextColor(APP_TEXT_SECONDARY)
                        textSize = 13f
                        typeface = Typeface.create(SANS_FAMILY, Typeface.NORMAL)
                        includeFontPadding = true
                    },
                    textLayoutParams(topMargin = 6),
                )
                addView(
                    TextView(this@MainActivity).apply {
                        text = section.prompt
                        setTextColor(APP_TEXT_PRIMARY)
                        textSize = 12f
                        typeface = Typeface.MONOSPACE
                        includeFontPadding = true
                        setTextIsSelectable(true)
                        setLineSpacing(dp(2).toFloat(), 1f)
                        background = roundedStrokeBackground(
                            APP_MUTED_SURFACE,
                            radius = 14,
                            strokeColor = APP_AI_SUBTLE_STROKE,
                        )
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                    },
                    textLayoutParams(topMargin = 10),
                )
            }
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        AlertDialog.Builder(this)
            .setTitle("AI prompts")
            .setMessage("These prompts are read-only. Phrase-pack values match your current AI Lab settings.")
            .setView(scroll, dp(24), dp(4), dp(24), 0)
            .setPositiveButton("Done", null)
            .show()
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
            "Spacing ${formatInterval(info.minimumSpacingMillis / 1000L)} · ${
                info.phoneLearningState.readableLabel()
            }"
        if (::intervalChangeButton.isInitialized) {
            val hasHiddenWords = repository.progressById().values.any { it.status == WordStatus.HIDDEN || it.isHidden }
            configureActionButton(
                button = schedulerSpacingButton,
                text = "Spacing: ${formatInterval(schedulerPreferences.minimumSpacingMinutes * 60L)}",
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
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(APP_MUTED_SURFACE, radius = 20)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
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
                        setPadding(dp(12), 0, 0, 0)
                    }
                    addView(autoHideStatusView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                },
                textLayoutParams(),
            )
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
            addView(autoHideDurationButton, textLayoutParams(topMargin = 10).apply {
                height = dp(42)
            })
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

    private fun formatGeneratedUpdateTime(millis: Long): String =
        if (millis <= 0L) {
            "not set"
        } else {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(millis))
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

    private fun shimmerHeroCard(radius: Int = 28): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ShimmerCardDrawable(
                startColor = APP_HERO_BACKGROUND,
                endColor = APP_HERO_DEEP,
                shimmerColor = PREVIEW_SHIMMER,
                radiusPx = dp(radius).toFloat(),
            )
            applySoftElevation(this, AI_HERO_ELEVATION_DP)
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

    private fun darkHeroTitleRow(title: String, iconRes: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                ImageView(this@MainActivity).apply {
                    setImageResource(iconRes)
                    imageTintList = ColorStateList.valueOf(APP_AMBER)
                    contentDescription = null
                },
                LinearLayout.LayoutParams(dp(28), dp(28)),
            )
            addView(
                TextView(this@MainActivity).apply {
                    text = title
                    setTextColor(APP_ON_PRIMARY)
                    textSize = 24f
                    typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                    includeFontPadding = false
                    setPadding(dp(12), 0, 0, 0)
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }

    private fun darkHeroHeader(title: String, subtitle: String, iconRes: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                ImageView(this@MainActivity).apply {
                    setImageResource(iconRes)
                    imageTintList = ColorStateList.valueOf(APP_AMBER)
                    contentDescription = null
                },
                LinearLayout.LayoutParams(dp(34), dp(34)),
            )
            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), 0, 0, 0)
                    addView(
                        TextView(this@MainActivity).apply {
                            text = title
                            setTextColor(APP_ON_PRIMARY)
                            textSize = 24f
                            typeface = Typeface.create(SANS_FAMILY, Typeface.BOLD)
                            includeFontPadding = false
                        },
                    )
                    addView(
                        TextView(this@MainActivity).apply {
                            text = subtitle
                            setTextColor(APP_AI_HERO_TEXT)
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
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBackground(APP_MUTED_SURFACE, radius = 20)
            minimumHeight = dp(72)
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
            textView.gravity = Gravity.CENTER_VERTICAL
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

    private fun maybePlayAiStatusSuccess(modelReady: Boolean) {
        val previousModelReady = lastRenderedModelReady
        if (previousModelReady == false && modelReady) {
            performConfirmHaptic()
            playSuccessPulse(aiModelStatusView)
        }
        lastRenderedModelReady = modelReady
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
        private val sweepDurationMillis: Long = 6_400L,
        private val restDurationMillis: Long = 2_400L,
    ) : Drawable() {
        private val rect = RectF()
        private val beamRect = RectF()
        private val clipPath = Path()
        private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var progress = 0f
        private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = sweepDurationMillis + restDurationMillis
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

            val sweepFraction = sweepDurationMillis.toFloat() / (sweepDurationMillis + restDurationMillis).toFloat()
            if (progress >= sweepFraction) {
                shimmerPaint.shader = null
                return
            }

            val width = rect.width()
            val height = rect.height()
            val linearSweepProgress = progress / sweepFraction
            val sweepProgress = easeOutCubic(linearSweepProgress)
            val fade = edgeFade(linearSweepProgress)
            val beamWidth = width * 0.34f
            val travelStart = rect.left - height - beamWidth
            val travelEnd = rect.right + height + beamWidth
            val shimmerCenter = travelStart + (travelEnd - travelStart) * sweepProgress

            shimmerPaint.shader = LinearGradient(
                shimmerCenter - beamWidth,
                0f,
                shimmerCenter + beamWidth,
                0f,
                intArrayOf(
                    shimmerColorWithAlpha(0f),
                    shimmerColorWithAlpha(0.35f * fade),
                    shimmerColorWithAlpha(fade),
                    shimmerColorWithAlpha(0.35f * fade),
                    shimmerColorWithAlpha(0f),
                ),
                floatArrayOf(0f, 0.32f, 0.5f, 0.68f, 1f),
                Shader.TileMode.CLAMP,
            )
            beamRect.set(
                shimmerCenter - beamWidth,
                rect.top - height * 1.8f,
                shimmerCenter + beamWidth,
                rect.bottom + height * 1.8f,
            )
            clipPath.reset()
            clipPath.addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW)
            val saveCount = canvas.save()
            canvas.clipPath(clipPath)
            canvas.rotate(-18f, rect.centerX(), rect.centerY())
            canvas.drawRect(beamRect, shimmerPaint)
            canvas.restoreToCount(saveCount)
        }

        private fun edgeFade(value: Float): Float {
            val fadeInWindow = 0.12f
            val fadeOutWindow = 0.36f
            val raw = when {
                value < fadeInWindow -> value / fadeInWindow
                value > 1f - fadeOutWindow -> (1f - value) / fadeOutWindow
                else -> 1f
            }.coerceIn(0f, 1f)
            return raw * raw * (3f - 2f * raw)
        }

        private fun easeOutCubic(value: Float): Float {
            val inverse = 1f - value.coerceIn(0f, 1f)
            return 1f - inverse * inverse * inverse
        }

        private fun shimmerColorWithAlpha(alphaFactor: Float): Int =
            Color.argb(
                (Color.alpha(shimmerColor) * alphaFactor).toInt().coerceIn(0, 255),
                Color.red(shimmerColor),
                Color.green(shimmerColor),
                Color.blue(shimmerColor),
            )

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
        private const val CUSTOM_VOCABULARY_INPUT_MAX_CHARS = 120
        private const val STATE_CUSTOM_VOCABULARY_DRAFT = "custom_vocabulary_draft"
        private const val STATE_CUSTOM_VOCABULARY_SHEET_OPEN = "custom_vocabulary_sheet_open"
        private const val CARD_ELEVATION_DP = 2
        private const val AI_CARD_ELEVATION_DP = 1
        private const val AI_HERO_ELEVATION_DP = 4
        private const val NAV_ELEVATION_DP = 8

        private const val SANS_FAMILY = "sans-serif"
        private const val SERIF_FAMILY = "serif"

        private val APP_BACKGROUND = Color.parseColor("#F7FAF7")
        private val APP_SURFACE = Color.parseColor("#FFFFFF")
        private val APP_MUTED_SURFACE = Color.parseColor("#F1F4F1")
        private val APP_SURFACE_CONTAINER_HIGH = Color.parseColor("#E5E9E6")
        private val APP_HERO_BACKGROUND = Color.parseColor("#2D3130")
        private val APP_HERO_DEEP = Color.parseColor("#012A24")
        private val PREVIEW_BACKGROUND = APP_HERO_BACKGROUND
        private val APP_PREVIEW_DEEP = Color.parseColor("#141918")
        private val PREVIEW_SHIMMER = Color.parseColor("#13FFFFFF")
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
        private val APP_WORKSPACE_PANEL = Color.parseColor("#173F37")
        private val APP_WORKSPACE_PANEL_STROKE = Color.parseColor("#2C6156")
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
