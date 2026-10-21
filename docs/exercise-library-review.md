# Exercise library and flexible workout days: Kiroku 1.5.0

## Delivered behavior

- Workouts links to a searchable global exercise library with creation, rename, archive, restore, peak weights, and history.
- One exercise identity can be assigned to multiple plans. Assignments are optional; plans can start empty workout days.
- A day's settings gear opens management controls to add, replace, or remove exercises for that day only.
- Replacing or removing an entry with saved sets requires confirmation. Replacement deletes the old sets rather than attributing them to a different exercise.
- Template assignments, other workout days, and library identities are not modified by day-only changes.
- Peaks and history include saved sets across all plans, including day-only additions and unassigned or archived exercises.
- Same-named exercise identities remain distinct. Library and picker rows show contextual plan names, peaks, and an ID for duplicate names.
- English and German resources match (356 resource keys).

## Persistence and migration

Schema 7 separates global `workout_exercises` from `workout_plan_exercises` assignments. Existing session-exercise snapshots retain their IDs and exercise foreign keys. The explicit 6-to-7 migration backs up and rebuilds exercise descendants with foreign keys enabled, restores records parent-first, and preserves AUTOINCREMENT high-water marks. It never merges exercise names. Old exercises return to their original plans with their original ordering and archived state.

The migration is registered in AppContainer alongside the existing automatic 1-to-6 migration chain. There is no destructive fallback.

## Verification performed

- `:app:testDebugUnitTest`: 77 JVM tests, zero failures/errors.
- `:app:lintDebug` and `:app:lintRelease`: zero errors, 24 existing resource/dependency warnings; no workout diagnostics.
- `:app:assembleDebug` and signed `:app:assembleRelease`: successful.
- `python3 scripts/verify_exercise_library.py`: 8 SQLite tests passed, executing the real Kotlin migration's SQL and the DAO's global-history query. Includes populated/empty databases, all existing application tables, exact exported schema/index/FK comparison, foreign-key integrity, duplicate exercise identity, archived records, and retained deleted-ID high-water marks.
- `python3 scripts/verify_workout_storage.py`: 6 earlier migration/storage checks passed.
- `git diff --check` and English/German resource parity passed.
- APK metadata verified: `dev.bugiel.kiroku`, version name `1.5.0`, version code `8`, minimum SDK 26, target SDK 37.
- APK signature verified and compared with downloaded v1.4.1: certificate SHA-256 `f003cf5eaeadcabf19ef7ec1e0eb095376677e7318860cbff541c315559d3899`.

## Limitations

No Android device or emulator was connected. Interactive layout, keyboard, rotation, and physical APK-upgrade smoke tests were not performed. These should not be inferred from JVM/SQLite/build success.

## Release

Publish as Elias Bugiel using GitHub account `totallyeli`, tag `v1.5.0`, and assets `kiroku-v1.5.0.apk` and `kiroku-v1.5.0.apk.sha256`. Signing secrets remain ignored/outside the repository. Users should install over the previous app rather than uninstalling and losing local data.
