# Kiroku

Kiroku is a local-first Android app for notes, habits, to-dos, and workouts. It has no accounts, ads, analytics, or content synchronization. All records and attachments remain on the device, and Android cloud backups are disabled. An internet connection is used only when the user opens Settings and checks GitHub for an update.

## Features

- Create, autosave, edit, pin, color, search, and safely delete notes
- Write Markdown with a compact formatting toolbar and formatted preview
- Import notes from `.txt` and `.md` files
- Attach images, PDFs, and Markdown files; open them in Kiroku and export them through Android
- Track habits daily, on selected weekdays, or at a configurable day interval
- Manage habits with a name, description, color, creation date, and 28 available icons
- Set an optional schedule and reminder time under **More options**
- Review and correct past completions from a dedicated habit-history calendar
- Review schedule-aware current streak, longest streak, and total completed days
- Navigate a monthly calendar and correct past or current entries
- Create to-dos with deadlines, priorities, colors, icons, and optional local reminders
- Find work quickly with search, status filters, and smart Overdue, Today, Tomorrow, Upcoming, and Completed sections
- Complete, undo, reopen, edit, or safely delete to-dos, including notification actions to complete or snooze for 10 minutes
- Create workout plans such as Push, Pull, Legs, or Cardio and choose exercises for each routine
- Record multiple sets with weight in kilograms and repetitions in dated workout sessions
- See each exercise’s peak weight before opening it and review or correct its history
- Choose a light, dark, or system theme, with dynamic colors on Android 12 and later
- Use Kiroku in English or German, or follow the Android system language
- Check the latest GitHub Release from Settings, download its verified APK, and open Android's installer
- Use an adaptive edge-to-edge interface with persistent tab state

## Technology and structure

The project uses Kotlin 2.4.10, Jetpack Compose and Material 3 (BOM 2026.06.01), Navigation Compose, ViewModel, Coroutines and Flow, Room 3.0.1, DataStore, Android Gradle Plugin 9.2.1, and Gradle 9.4.1. The minimum SDK is 26, the target and compile SDK are 37.0, and Java/Kotlin bytecode targets JDK 17.

```text
app/src/main/java/dev/bugiel/kiroku/
├── data/       Room, DAOs, document access, and local repositories
├── di/         Small manual AppContainer
├── domain/     Models, search, organization, streaks, and date logic
├── reminder/   Local habit and to-do reminder scheduling and delivery
├── update/     GitHub Release checks and verified APK downloads
└── ui/         Compose screens, Markdown, ViewModels, theme, and UI helpers
```

## Day rollover and streaks

Completions use a composite primary key made from the habit ID and local `epochDay`. A day rollover never deletes data. The UI reads the new local calendar day when the app resumes and once per minute while it remains open.

A streak advances across the habit's scheduled occurrences. Non-scheduled days do not break it. While the current occurrence is still incomplete, the streak from the previous occurrence remains active. Calendar edits recalculate every statistic immediately.

The main habit list only shows habits scheduled for the current day. A separate history calendar lets the user choose an earlier date and mark or unmark the habits scheduled for that date without mixing them into today's view.

An optional reminder time does not change status or streak calculations. Notifications are only scheduled for an occurrence in the selected weekly or interval schedule. Before showing one, Kiroku checks whether the habit is already complete for the current local day.

To-do reminders are one-time alarms derived from the selected deadline and offset. They are canceled when the to-do is completed or deleted and updated when its deadline changes. A notification can complete the to-do without opening Kiroku or snooze it for 10 minutes. Habit and to-do reminder identifiers use separate namespaces, so their alarms and notifications do not replace each other.

Reminders are rescheduled after device restarts, time or time-zone changes, and app updates. Kiroku uses inexact alarms that do not require exact-alarm permission. Android 13 and later require notification permission.

## Updates and data safety

**Check for updates** reads the latest release from `totallyeli/kiroku-android`. Before installation, Kiroku verifies the package name, higher version code, signing certificate, and the APK's SHA-256 digest when GitHub provides one. The user confirms the actual installation in Android's system dialog.

An app update uses the same package name and signing key. The version 2-to-3 migration adds the attachment table. The version 3-to-4 migration adds schedule fields with daily defaults, so existing habits keep their previous behavior. The version 4-to-5 migration only adds the to-do table and its indexes. The version 5-to-6 migration adds five workout tables and their indexes. These migrations do not replace or delete notes, attachments, habits, completions, streaks, to-dos, settings, or existing files. Uninstalling the app still removes its local app data.

## Workout tracking

The **Workouts** tab contains your plans. Create a plan, add exercises, and choose **Start / open today’s workout**. Each plan has one workout day per local date; opening it again resumes the existing day. Select an exercise, enter a weight and rep count for each set, and tap **Save**. Kilograms accept a decimal point or comma, and 0 kg supports bodyweight exercises.

Exercise cards show the highest recorded weight for that exercise in that plan. The history screen lists previous dates and sets. Editing or deleting a recorded set recalculates the peak from the remaining history. No unsaved values contribute to personal bests.

Workout days retain snapshots of their exercise names and order. Adding, renaming, or archiving exercises affects future days without rewriting earlier sessions. Archived exercises retain their history and can be restored. Deleting a day or entire plan requires confirmation.

The feature follows the existing Room → repository → ViewModel → Compose structure. Session creation and set replacement run in transactions, and foreign keys keep deletions within the selected workout records. English and German interfaces use the existing themes and date formatting.

## Installation

Download the current APK from the GitHub **Releases** page and open it on the Android device. Android may ask for permission to install from that source during the first manual installation. An installed version can subsequently update through **Settings → Updates**.

## Build and test

Requirements: JDK 17 or later, Android SDK Platform 37.0, and Build Tools 36.0.0. The local SDK path belongs only in the ignored `local.properties` file.

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
```

After compilation, the optional Python standard-library check exercises the generated version 5-to-6 migration against a populated, in-memory SQLite database. It also verifies constraints, transaction rollback, and deletion boundaries:

```bash
python scripts/verify_workout_storage.py
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

Repository documentation and development communication use English. English is the app's primary locale and German is fully supported. See [CONTRIBUTING.md](CONTRIBUTING.md) for the policy that applies to future changes.

## Known limitations

- Android power-saving behavior may delay reminders slightly.
- There are intentionally no accounts or cloud synchronization. Attachments can be exported individually, but a complete app backup is not implemented.
- Automated logic tests run locally. Visual device testing requires an emulator or an Android device connected through ADB.
