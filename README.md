# FlipWords

Native Android app for a Samsung Galaxy Z Flip 6 cover screen. It shows one Chinese word with pinyin and English meaning, rotating on a configurable interval that defaults to 90 minutes.

## What It Builds

- A full app view that shows the same word in a larger top-left layout.
- An experimental overlay mode that tries to place the word on the first cover clock screen.
- Text-size controls for the experimental overlay.
- A test-mode word frequency slider from seconds up to 90 minutes.
- Offline vocabulary bundled in Kotlin.
- WorkManager scheduling for background word-state refreshes.

## App Icon

The current launcher icon is `app/src/main/res/drawable/flipwords_logo.png`.

If you want to provide your own logo, the easiest handoff is a square PNG:

- Recommended source size: `1024 x 1024 px`
- Format: PNG, transparent background if you want the icon shape handled by Android
- Place it at: `app/src/main/res/drawable/flipwords_logo.png`

The manifest already points `android:icon` and `android:roundIcon` at `@drawable/flipwords_logo`. For a production-quality Android launcher icon, use Android Studio's Image Asset tool from that 1024 px source so it generates the proper adaptive icon densities.

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

The app prefers cover display id `1` when available, then tries any other non-main display. It never falls back to the main inner display, so opening the phone should not show the floating overlay on the big screen. The debug text in the app shows which displays Android reports and whether the overlay is attached, waiting, or blocked.

The overlay uses a transparent background. Use the Hanzi, pinyin, and English sliders in the app to adjust its text sizes. If Samsung blocks overlays on the cover clock screen, the foreground notification still shows the current word as a fallback.

Use the word timing slider to test fast rotations. The foreground overlay service refreshes around the next configured rotation time, so short intervals such as `5s` are useful while testing. Android's background WorkManager scheduler has a minimum periodic interval of about 15 minutes, so very short intervals are only reliable while the overlay service is running.
