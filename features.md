# FlipWords Features

This document describes the current FlipWords feature set. It is meant to be a product-level reference for what the app does today, including stable features, experimental AI features, and known platform limitations.

## Core Learning Experience

- Shows one Traditional Chinese learning item at a time.
- Each item has three parts:
  - Hanzi, shown as the primary visual anchor.
  - Pinyin, shown in brackets underneath.
  - English meaning, shown underneath the pinyin.
- Uses tone-marked pinyin on visible app surfaces.
  - Built-in vocabulary is stored mostly as plain pinyin internally.
  - `PinyinToneFormatter` converts supported built-in entries to tone-marked display text.
  - AI-generated entries are expected to include tone marks directly.
- The same current word is shared between:
  - Main app view.
  - Cover-screen overlay.
  - Foreground notification.
- Tapping `Next word` advances to the next item immediately.
- Tapping the cover overlay also advances to the next item when Android allows touch handling there.

## Vocabulary

- Ships with an offline built-in vocabulary source.
- Built-in target size is 500 beginner/intermediate Traditional Chinese words and compact phrases.
- Vocabulary focuses on Taiwan-useful daily language, such as:
  - MRT, bus, train, HSR, taxi, scooter, and directions.
  - Convenience stores, night markets, cafes, restaurants, and payment.
  - Food and drink ordering, including sugar/ice/spice phrases.
  - Daily schedule, work, school, meetings, travel, health, and social basics.
- Built-in vocabulary is deterministic and available without network access.
- Unit tests assert:
  - Built-in list size is 500.
  - Built-in Hanzi entries are unique.
  - Known Simplified-only characters are not present.

## Word Rotation

- Default word change interval is 90 minutes.
- The app has a test-mode timing slider from 5 seconds up to 90 minutes.
- The current word is calculated from:
  - An anchor timestamp.
  - The configured interval.
  - The active vocabulary bucket size.
- Missed background updates recover naturally because the current word is recomputed from time.
- Manual next-word behavior repins the anchor so the selected next word becomes current.
- WorkManager schedules periodic background refresh work.
  - Android WorkManager has a practical minimum periodic interval of about 15 minutes.
  - Very short test intervals are most reliable while the foreground overlay service is running.
- Word rotation is restored after app restart and phone reboot.

## Main App UI

- Native Android Kotlin app using programmatic views.
- Modern light UI with:
  - App logo in the header.
  - Large current-word learning view.
  - Overlay preview card.
  - Text styling controls.
  - Word timing controls.
  - AI Lab controls.
  - Device/debug status.
- Handles status-bar and navigation-bar padding so content does not sit underneath system bars.
- Uses the app logo from `app/src/main/res/drawable/flipwords_logo.png`.
- App label and launcher identity are `FlipWords`.

## Cover Overlay

- Experimental foreground-service overlay for personal sideloaded use.
- Uses Android `SYSTEM_ALERT_WINDOW` / "Appear on top" permission.
- Starts from the `Start overlay` button.
- Stops from the `Stop overlay` button or notification action.
- Targets the Samsung cover display first:
  - Prefers display id `1`.
  - Falls back only to another non-default display.
  - Never attaches to `Display.DEFAULT_DISPLAY`, so it should not appear on the main inner screen.
- Registers a display listener while running.
  - Rechecks displays when the phone opens/closes or displays change.
  - Removes and retries overlay attachment when display availability changes.
- Overlay is transparent.
  - No solid background behind the text.
  - Small padding only for placement.
- Overlay displays:
  - Hanzi.
  - Tone-marked pinyin in brackets.
  - English meaning.
- Overlay text can be tapped to advance to the next word when touch is permitted.

## Cover Overlay Limitations

- Samsung/Android overlays attach to an entire display, not to a specific Samsung cover-screen page.
- Normal Android overlay APIs do not expose whether the user is on:
  - The first cover clock page.
  - A swiped cover widget page.
  - Another Samsung cover surface.
- Because of that, the current overlay cannot reliably appear only on the first cover clock page while hiding on other cover pages.
- The app documents this in its device/debug status as: cover display-wide overlay scope.
- If Samsung blocks cover-display overlay attachment, the app keeps the foreground notification fallback.

## Persistent Notification

- Required because the overlay runs as a foreground service.
- Shows the current word, tone-marked pinyin, English meaning, and overlay attachment display label.
- Provides actions for:
  - Next word.
  - Stop overlay.
- Acts as a fallback visible surface if Samsung hides or blocks the overlay.

## Text Size Controls

- The app provides separate sliders for:
  - Hanzi size.
  - Pinyin size.
  - English size.
- Defaults:
  - Hanzi: 30sp.
  - Pinyin: 13sp.
  - English: 14sp.
- Ranges:
  - Hanzi: 18-56sp.
  - Pinyin: 10-28sp.
  - English: 10-32sp.
- Size changes update:
  - In-app overlay preview.
  - Running cover overlay after refresh.
- Values persist in `SharedPreferences`.

## Text Color Controls

- The app provides separate color controls for:
  - Hanzi.
  - Pinyin.
  - English.
- Uses a saved-color swatch palette.
- Defaults are chosen for visibility on dark cover-screen backgrounds.
- Color changes update:
  - In-app overlay preview.
  - Running cover overlay after refresh.
- Values persist in `SharedPreferences`.

## AI Lab

- Experimental opt-in local AI vocabulary generation area.
- Keeps the built-in 500-entry vocabulary as a stable fallback.
- Uses Google LiteRT-LM as the intended on-device local model runtime.
- Current model target:
  - Repository: `litert-community/gemma-4-E2B-it-litert-lm`
  - File: `gemma-4-E2B-it.litertlm`
  - Runtime dependency: `com.google.ai.edge.litertlm:litertlm-android:0.13.1`
- The model is not bundled in the APK.
- The model is downloaded only when the user taps `Download AI model`.
- The app stores the downloaded model in app external downloads storage.
- The app validates that the model file exists and is at least 2 GB before marking it ready.
- LiteRT-LM is loaded by reflection at runtime.
  - This avoids compile-time Kotlin metadata conflicts because the app currently compiles with Kotlin 2.0.20 while LiteRT-LM is built with newer Kotlin metadata.

## AI Model Download Progress

- AI Lab shows model download status.
- Displays a horizontal progress bar while downloading.
- Displays downloaded and total size in GB.
- Refreshes progress every second while the download is active.
- Uses Android `DownloadManager`.
- Download is configured to avoid metered and roaming networks.
- Android's own download notification also remains available.

## AI Vocabulary Generation

- Generation is started manually with `Generate now` or by an exact daily alarm.
- Generation runs in a WorkManager `CoroutineWorker`.
- The worker runs as foreground work with a notification.
- The worker initializes LiteRT-LM only inside the background generation task.
- Backend strategy:
  - Try GPU first.
  - If GPU initialization/generation fails, retry with CPU.
- Prompt asks for 80 candidate entries so the validator can accept the first 50 valid unique entries.
- Generated pack size requirement is exactly 50 valid entries.
- AI-generated content target:
  - Beginner/intermediate Traditional Taiwanese Mandarin.
  - Words and short phrases only.
  - No full sentences.
  - Compact enough for the cover screen.
- Expected generated entry fields:
  - `hanzi`
  - `pinyin`
  - `english`
  - plus app-added metadata: `source`, `createdAtMillis`

## AI Vocabulary Validation

- Generated vocabulary is not activated unless validation accepts 50 entries.
- Validation rejects:
  - Empty Hanzi, pinyin, or English fields.
  - Hanzi longer than 8 characters.
  - Pinyin longer than 48 characters.
  - English meaning longer than 36 characters.
  - Duplicate Hanzi entries.
  - Known Simplified-only characters.
  - Chinese sentence punctuation.
  - Pinyin without tone marks.
  - Entries with no CJK Hanzi.
- Failed generation does not replace the active generated pack.
- If generation fails, the built-in vocabulary remains available.

## Generated Vocabulary Storage

- Generated entries are stored in app-private JSON.
- Storage file name is `generated_vocabulary.json`.
- Generated packs are separate from `Vocabulary.kt`.
- The built-in list remains unchanged when AI generation succeeds or fails.
- Saved generated entries include:
  - Hanzi.
  - Pinyin.
  - English.
  - Source label.
  - Creation timestamp.

## Active Vocabulary Source Modes

- AI Lab lets the user cycle through active source modes:
  - `Built-in only`
  - `Generated first`
  - `Generated only`
  - `Mix both`
- Default mode is `Generated first`.
- If no generated pack exists, every mode falls back to the built-in list.
- `Generated first` uses generated entries before the built-in fallback list.
- `Generated only` uses only generated entries once available.
- `Mix both` interleaves generated and built-in entries.
- Source mode affects the same `WordRepository` used by the main app, overlay, and notification.

## AI Daily Scheduling

- AI Lab provides a time picker for daily generation.
- Default scheduled time is 02:00.
- The user can enable or disable daily generation.
- Exact daily scheduling uses `AlarmManager.setExactAndAllowWhileIdle()`.
- Requires Android Alarms & reminders permission on supported Android versions.
- If exact alarm permission is blocked:
  - The app opens the relevant permission screen.
  - Manual generation remains available.
- Boot receiver reschedules daily generation after reboot if daily generation is enabled.
- Alarm receiver enqueues the AI generation worker and schedules the next daily alarm.

## AI Failure Diagnostics

- AI generation failures record a pending diagnostic log.
- The app asks whether to save the log the next time it is opened after a failure.
- The AI Lab also includes a manual `Save AI debug log` button.
- Saved logs are `.txt` files.
- Saved logs are written under the app external documents folder in `FlipWordsLogs`.
- A Toast displays the exact file path after saving.
- The log includes:
  - Failure reason.
  - Accepted valid entry count if available.
  - Generated status and source mode.
  - Daily generation settings.
  - Model repository, file name, path, status, existence, size, and download id.
  - Device manufacturer/model, SDK, and ABI list.
  - Raw model output preview, truncated to 12,000 characters.
  - Exception stack trace when available.

## Permissions

- `SYSTEM_ALERT_WINDOW`
  - Needed for the experimental cover overlay.
- `FOREGROUND_SERVICE`
  - Needed for foreground overlay/service behavior.
- `FOREGROUND_SERVICE_SPECIAL_USE`
  - Used by the cover overlay foreground service.
- `FOREGROUND_SERVICE_DATA_SYNC`
  - Used by WorkManager foreground AI generation.
- `POST_NOTIFICATIONS`
  - Needed on Android 13+ for notification visibility.
- `RECEIVE_BOOT_COMPLETED`
  - Used to restore word update scheduling and AI daily scheduling after reboot.
- `INTERNET`
  - Needed to download the optional AI model.
- `SCHEDULE_EXACT_ALARM`
  - Needed for exact daily AI generation scheduling.

## Background Work And Services

- `ChineseWordApplication`
  - Schedules word update work on app startup.
- `WordUpdateScheduler`
  - Schedules periodic WorkManager refresh work for word rotation.
- `WordUpdateWorker`
  - Touches the current word so background state stays warm.
- `CoverOverlayService`
  - Foreground service for the experimental cover overlay.
- `AiVocabularyGenerationWorker`
  - Foreground WorkManager worker for AI vocabulary generation.
- `AiGenerationAlarmReceiver`
  - Receives daily generation alarms and starts AI generation work.
- `BootReceiver`
  - Reschedules word refresh and AI daily alarm after reboot.

## Tests

- Word rotation tests cover:
  - Same word within the 90-minute window.
  - Advance after 90 minutes.
  - Custom short intervals.
  - Wrapping at the end of vocabulary.
  - Times before anchor.
- Vocabulary tests cover:
  - 500 built-in entries.
  - No duplicate built-in Hanzi.
  - No known Simplified-only characters.
- Active vocabulary selector tests cover:
  - Built-in only.
  - Generated first.
  - Generated only.
  - Mixed mode.
  - Fallback to built-in when no generated pack exists.
- Generated vocabulary validator tests cover:
  - Valid JSON parsing.
  - Simplified-character rejection.
  - Sentence-punctuation rejection.
  - Overlong field and duplicate rejection.
  - Pinyin-without-tone-marks rejection.
- Pinyin formatter tests cover:
  - Adding tone marks for supported built-in pinyin.
  - Preserving already tone-marked pinyin.

## Build And Deployment

- Native Android Kotlin app.
- Android Gradle Plugin version is managed in the root Gradle build.
- App compiles with Kotlin 2.0.20.
- LiteRT-LM is runtime-only to avoid compile-time Kotlin metadata incompatibilities.
- Debug APK can be installed with Android Studio or `adb install`.
- Intended v1 distribution is personal sideloading on a Samsung Galaxy Z Flip 6.

## Current Known Limitations

- Cover overlay is experimental and Samsung may block or hide it in some cover states.
- Overlay scope is cover-display-wide, not first-cover-page-only.
- Third-party cover-screen behavior depends on Samsung firmware and Android overlay policy.
- The AI Lab is experimental and may require iteration after testing on device.
- The LiteRT-LM model is multi-GB and may take a long time to download.
- AI generation can be slow and may use significant battery/thermal resources.
- Exact daily AI scheduling depends on Android exact-alarm permission.
- Built-in pinyin tone conversion is mapping-based, not a full Chinese pronunciation engine.
- Multi-pronunciation characters are handled only for known current vocabulary contexts.
