# Workout tracking — Kiroku 1.4.0

This document describes the workout feature and its validation for Kiroku 1.4.0, dated 2026-09-17.

## Delivered behavior

- Fourth bottom-navigation tab: Workouts / Training.
- User-created plans with named exercises; plans and exercises can be renamed.
- One workout day per plan and local date; reopening today resumes its saved entries.
- Multiple sets per exercise, with weight in kilograms and positive integer reps.
- Decimal point/comma input, bodyweight entries at 0 kg, validation, and explicit Save.
- Peak weights on exercise cards; dated history with editable past sets.
- Exercise archiving/restoration preserves history. Plan and workout-day deletion require confirmation.
- ViewModels preserve editor drafts through configuration changes; leaving unsaved sets prompts for confirmation.
- English/German localization, existing themes, and locale-aware display.

## Persistence

Room schema 6 adds workout plans, exercises, sessions, session-exercise snapshots, and sets. Migration 5→6 creates only these tables and their indexes. Existing entities and records remain unchanged. Session creation and set replacement are transactional. Exercise name/order snapshots preserve old workout days when the routine changes.

## Verification

- `test`: 60 JVM tests passed, including 13 new workout tests.
- `lint`: passed with 0 errors; remaining warnings concern existing resources and available dependency upgrades.
- `assembleDebug` and `assembleRelease`: passed.
- `python scripts/verify_workout_storage.py`: six in-memory SQLite checks passed, covering migration/data preservation, uniqueness, foreign keys, rollback, archiving, and deletion boundaries.
- English/German resource keys match; `git diff --check` passes.
- Debug APK certificate matches the previously published v1.3.0 certificate: `f003cf5eaeadcabf19ef7ec1e0eb095376677e7318860cbff541c315559d3899`.
- No Android device/emulator was connected. Interactive UI, keyboard, rotation, and physical upgrade smoke tests remain manual.

## Release packaging

Version 1.4.0 uses version code 6 and retains package ID `dev.bugiel.kiroku`. The release variant builds unsigned; the established distribution workflow uses the existing debug certificate for update compatibility. Release assets are `kiroku-v1.4.0.apk` and `kiroku-v1.4.0.apk.sha256`.

Install updates over the existing application. Uninstalling first removes Android application data.
