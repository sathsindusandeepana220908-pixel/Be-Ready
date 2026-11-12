# BeReady — AL Master

Offline-first personal Advanced Level study companion for Android 8.1+.

## App architecture
- Android Java host with a local HTML/CSS/JavaScript interface in app/src/main/assets.
- Minimum Android SDK 27; compile/target SDK 35.
- Local-first persistence through browser local storage; no login or network dependency.
- Native Android bridge for notifications, reminders, vibration and document picking.
- Lightweight Gradle project with no mandatory paid services.

## Included study tools
- Dashboard with study goals, subject totals, tasks and weekly study overview.
- Subject management and manual study-time entries.
- Pomodoro focus timer with pause/resume/reset/skip and native completion notifications.
- Task manager with priorities, categories, dates and search.
- Weekly timetable with recurring weekday events and reminders.
- Notes, PDF/document library, flashcards, syllabus topic tracking and formula notes.
- Study analytics, streaks, calendar, settings, and JSON backup/restore.

## Build a debug APK
The GitHub Actions workflow runs on pushes to main and manual dispatch:
1. Open Actions → Build BeReady APK.
2. Open the latest successful run.
3. Download the BeReady-debug-apk artifact.
4. Extract app-debug.apk and install it on your Android device.

For a local build with JDK 17 and Gradle 8.7:
```bash
gradle assembleDebug --no-daemon
```

APK output: app/build/outputs/apk/debug/app-debug.apk.

## Data and privacy
Study records are stored on the device. Use More → Settings → Export backup before uninstalling or changing devices. Imported documents remain referenced through Android's document provider; deleting the app or removing source files may affect access.

## Notes
This project currently uses a Java Android WebView shell with local web assets. It is not a Jetpack Compose/Kotlin application. The UI and core study tools work offline; OS-level notification permission and Android document-provider access are subject to device settings.