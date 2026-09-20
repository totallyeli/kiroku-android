# Changelog

## 1.4.1 — 2026-09-20

- Fixed to-do snoozes silently disappearing after a reboot or timezone change; snoozed reminders are now re-armed from a persisted store
- Fixed reminders firing up to 15 minutes late by requesting exact alarms when the system allows it
- Fixed an app crash at startup or boot when a habit had corrupted or unschedulable reminder data
- Fixed workout history under-reporting peak weight when the same exercise was logged twice in one session
- Fixed a race that could error instead of resuming when tapping into an already-started workout day twice at once
- Fixed note editors reverting typed text or losing newly added attachments after an autosave
- Fixed deleting a note racing an in-flight autosave so the deleted note could reappear
- Fixed double-tapping Save creating duplicate habits or to-dos
- Fixed the completed section header doing nothing when the Completed filter was active
- Fixed the caret jumping to the end when a new note received its id at first autosave
- Fixed ordered Markdown lists starting a numbered list from a custom start number always rendering 1
- Fixed an infinite spinner in the attachment viewer for images that fail to decode
- Started sweeping orphaned attachment files and stale temporary files at app start
- Re-enabled lint checks for release builds and documented the local-only release signing setup

## 1.4.0 — 2026-09-17

- Added a Workouts tab for custom routines, exercises, and dated workout days
- Added multiple sets per exercise with kilogram weights and repetition counts
- Added peak weights on exercise cards and editable exercise history
- Added exercise archiving and restoration while preserving historical sessions
- Added validation, unsaved-edit protection, and confirmation before deleting plans or workout days
- Added English and German translations and the additive version 6 database migration

## 1.3.0 — 2026-08-26

- Added a dedicated To-dos tab with search and Open, Completed, and All filters
- Added smart Overdue, Today, Tomorrow, Upcoming, and collapsible Completed sections
- Added to-do deadlines, priorities, colors, icons, optional reminders, editing, deletion, completion, reopening, and undo
- Added notification actions to complete a to-do or snooze it for 10 minutes
- Added full English and German localization for the new feature
- Added the additive version 5 database migration without changing existing notes, attachments, habits, or completion history

## 1.2.1 — 2026-08-23

- Removed previous-day habit cards from today's habit list
- Added a dedicated habit-history calendar for choosing a past date
- Added past-date controls for marking or unmarking each scheduled habit without affecting today's view

## 1.2.0 — 2026-08-20

- Added full English and German interfaces with an in-app language selector and system-language support
- Added habit schedules for selected weekdays or configurable intervals such as every two days
- Updated streaks, calendar availability, progress, and reminders to follow each habit's schedule
- Added a **Missed yesterday** action so a scheduled habit can be completed after midnight while retaining yesterday's date
- Added the version 4 database migration with daily defaults and no deletion of existing notes, attachments, habits, or completions

## 1.1.0 — 2026-08-20

- Added update checks in Settings with GitHub Release comparison, verified APK downloads, and Android installer handoff
- Added a Markdown editor with a formatting toolbar and formatted preview
- Added local image, PDF, and Markdown note attachments with integrated viewing and export
- Added `.txt` and `.md` imports that create new notes
- Added 28 habit icons behind **Show more icons**
- Added the version 3 database migration while preserving existing notes, habits, and completions

## 1.0.0 — 2026-08-19

- Added local notes with search, colors, and pinning
- Added daily habits with a calendar, streaks, and statistics
- Added an optional reminder time with local notifications
- Added light, dark, system-controlled, and dynamic color themes
