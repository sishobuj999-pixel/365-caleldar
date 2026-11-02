# SED Calendar — Android app + Home Screen widget

Android Studio project (Kotlin) based on the supplied 2026 calendar.

## Features
- Home-screen widget with live clock/date and countdown to 1 January 2027.
- Tap the widget to open the full app.
- App screen with live clock, date, second-by-second countdown, and navigable monthly calendar.
- Current day highlighted.
- Minimum Android version: Android 8.0 (API 26).

## Build
The GitHub Actions workflow builds a debug APK on every push and can also be started manually. Open the repository's **Actions** tab and download the `sed-calendar-debug-apk` artifact from a successful run.

To install locally, download `app-debug.apk` and open it on your Android phone. You may need to allow installation from your browser or file manager.

## Notes
- Countdown target: 1 January 2027 at 00:00 local time.
- Widget date and clock use Android TextClock. Countdown text is refreshed on Android's widget update schedule; the app countdown updates every second.
