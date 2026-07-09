# FlipWords

<p align="center">
  <img src="app/src/main/res/drawable/flipwords_logo.png" alt="FlipWords logo" width="160" />
</p>

FlipWords is an adaptive spaced-repetition learning app for passive Chinese vocabulary exposure on the Samsung Z Flip cover screen.

It shows compact Traditional Chinese vocabulary cards and passive retrieval prompts, then estimates familiarity from local exposure signals such as display sessions, screen state, unlocks, full app opens, taps, and spacing across days.

Honesty note: the current version estimates familiarity from passive exposure signals. It does not directly measure recall because the overlay does not require user feedback.

## What It Builds

- A full Learn page with current word, estimated progress cards, HSK-aware filters, and a lightweight stacked area progress chart.
- A non-interactive experimental cover overlay for passive vocabulary exposure.
- A context-aware adaptive scheduler that pauses during sleep-like inactivity and resumes with one appropriate word.
- Passive exposure tracking with word states: New, Learning, Familiar, Stable, Mastered, Retired, and Hidden.
- Passive prompt modes such as meaning and pinyin prompts, plus reveal cards.
- Text-size, color, and auto-hide controls for the experimental overlay.
- Offline vocabulary bundled in Kotlin with 500 beginner/intermediate Traditional Chinese words and phrases.
- An opt-in AI Lab that can download a LiteRT-LM model and generate local vocabulary packs.

## Learning Model

FlipWords no longer presents learning as a fixed-frequency word rotator. The internal scheduler still uses a 90-minute default minimum spacing target, but words change only when the phone context suggests a reasonable exposure opportunity.

The scheduler uses:

- Context-aware scheduling from screen/user-present/app-open signals.
- Passive exposure-based familiarity, not measured recall.
- Effective exposure scoring with caps so long inactive sessions do not dominate.
- An estimated half-life model inspired by spaced-repetition research.
- Review/new word balancing so overdue words and new words both appear.
- Rare maintenance resurfacing for mastered or retired words.

See:

- `docs/SCHEDULER.md`
- `docs/ACTIVITY_BASED_LEARNING.md`
- `docs/LEARN_PAGE_STATS.md`
- `docs/CONFIG.md`

## App Icon

The current launcher icon is `app/src/main/res/drawable/flipwords_logo.png`.

If you want to provide your own logo, the easiest handoff is a square PNG:

- Recommended source size: `1024 x 1024 px`
- Format: PNG, transparent background if you want the icon shape handled by Android
- Place it at: `app/src/main/res/drawable/flipwords_logo.png`

The manifest already points `android:icon` and `android:roundIcon` at `@drawable/flipwords_logo`.

## Local Setup

This workspace was scaffolded with a Gradle wrapper, but a local Java runtime is still required before builds can run.

1. Install Android Studio.
2. Install a JDK through Android Studio or your package manager.
3. Open this folder in Android Studio.
4. Let Android Studio sync Gradle.
5. Build `app` as a debug APK.

From `/Users/karankapur/samsung_zh`, run:

```sh
./gradlew test
./gradlew assembleDebug
```

## Phone Deployment

1. On the Flip 6, enable Developer options.
2. Enable USB debugging or wireless debugging.
3. Install the debug APK from Android Studio or with:

```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

4. Open FlipWords and start the experimental cover overlay.

## Experimental Cover Clock Overlay

Samsung may keep third-party widgets on the swipeable Flex Window widget page instead of the first clock screen, so FlipWords uses an experimental overlay mode for personal sideloaded use:

1. Open FlipWords on the phone.
2. Tap `Grant overlay permission` and enable "Appear on top" for FlipWords.
3. Return to the app and tap `Start overlay`.
4. Close the phone and wake the cover screen.
5. Check whether the word appears at the top-left of the cover clock screen.

The app prefers cover display id `1` when available, then tries any other non-main display. It never falls back to the main inner display, so opening the phone should not show the floating overlay on the big screen.

The overlay uses a transparent background. Use the Hanzi, pinyin, and English sliders and color swatches in the app to adjust text sizes and colors. Auto-hide is enabled by default so the floating text disappears after a short visible window; the foreground notification still shows the current word as a fallback.

## Privacy

FlipWords processes learning-state and phone-state signals locally. The scheduler uses these signals to avoid counting fake exposure during inactive periods.

## Experimental AI Lab

FlipWords includes an opt-in local AI Lab for personal testing. It keeps the built-in 500-entry list as the stable fallback, then lets you download Google's LiteRT-LM Android model and generate a local pack of compact Traditional Taiwanese Mandarin words and short phrases.

- Model: `litert-community/gemma-4-E2B-it-litert-lm`
- File: `gemma-4-E2B-it.litertlm`
- Dependency: `com.google.ai.edge.litertlm:litertlm-android:0.13.1`
- Download behavior: on demand only; the model is not bundled in the APK.

Generated packs are saved only after validation accepts the selected number of entries. If download or generation fails, FlipWords continues using the built-in list.
