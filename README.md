# Kiroku

Kiroku is a private Android app for notes and daily habits. It has no accounts, ads, analytics, or content synchronization. Notes, attachments, and habits remain on the device, and Android cloud backups are disabled. An internet connection is used only when the user opens Settings and checks GitHub for an update.

## Features

- Create, autosave, edit, pin, color, search, and safely delete notes
- Write Markdown with a compact formatting toolbar and formatted preview
- Import notes from `.txt` and `.md` files
- Attach images, PDFs, and Markdown files; open them in Kiroku and export them through Android
- Track separate daily habits with progress and one-tap completion
- Manage habits with a name, description, color, creation date, and 28 available icons
- Set an optional daily time and local notification under **More options**
- Review current streak, longest streak, and total completed days
- Navigate a monthly calendar and correct past or current entries
- Choose a light, dark, or system theme, with dynamic colors on Android 12 and later
- Check the latest GitHub Release from Settings, download its verified APK, and open Android's installer
- Use an adaptive edge-to-edge interface with persistent tab state

## Technology and structure

The project uses Kotlin 2.4.10, Jetpack Compose and Material 3 (BOM 2026.06.01), Navigation Compose, ViewModel, Coroutines and Flow, Room 3.0.1, DataStore, Android Gradle Plugin 9.2.1, and Gradle 9.4.1. The minimum SDK is 26, the target and compile SDK are 37.0, and Java/Kotlin bytecode targets JDK 17.

```text
app/src/main/java/dev/bugiel/kiroku/
├── data/       Room, DAOs, document access, and local repositories
├── di/         Small manual AppContainer
├── domain/     Models, search, streaks, and date logic
├── reminder/   Local habit reminder scheduling and delivery
├── update/     GitHub Release checks and verified APK downloads
└── ui/         Compose screens, Markdown, ViewModels, theme, and UI helpers
```

## Day rollover and streaks

Completions use a composite primary key made from the habit ID and local `epochDay`. A day rollover never deletes data. The UI reads the new local calendar day when the app resumes and once per minute while it remains open.

A current streak ends today or, while today is still incomplete, yesterday. If neither today nor yesterday is complete, the current streak is zero. The longest streak is the longest uninterrupted sequence of unique local calendar days. Calendar edits recalculate every statistic immediately.

An optional reminder time does not change daily status or streak calculations. Before showing a notification, Kiroku checks whether the habit is already complete for the current local day. Reminders are rescheduled after device restarts, time-zone changes, and app updates. Android 13 and later require notification permission.

## Updates and data safety

**Check for updates** reads the latest release from `totallyeli/kiroku-android`. Before installation, Kiroku verifies the package name, higher version code, signing certificate, and the APK's SHA-256 digest when GitHub provides one. The user confirms the actual installation in Android's system dialog.

An app update uses the same package name and signing key. The version 2-to-3 database migration only adds the attachment table; it does not replace or delete existing notes, habits, completions, or streaks. Attachments are stored in private app files and also remain intact during a normal update. Uninstalling the app still removes its local app data.

## Installation

Download the current APK from the GitHub **Releases** page and open it on the Android device. Android may ask for permission to install from that source during the first manual installation. An installed version can subsequently update through **Settings → Updates**.

## Build and test

Requirements: JDK 17 or later, Android SDK Platform 37.0, and Build Tools 36.0.0. The local SDK path belongs only in the ignored `local.properties` file.

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

On Windows, run the same tasks with `gradlew.bat`. The generated APK is located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project language

Repository documentation and development communication use English. The shipped app interface remains German until multilingual support is implemented as a dedicated feature. See [CONTRIBUTING.md](CONTRIBUTING.md) for the policy that applies to future changes.

## Known limitations

- Every habit currently repeats daily; custom weekly schedules are not available yet.
- Android power-saving behavior may delay reminders slightly.
- There are intentionally no accounts or cloud synchronization. Attachments can be exported individually, but a complete app backup is not implemented.
- Automated logic tests run locally. Visual device testing requires an emulator or an Android device connected through ADB.
