# Contributing

## Project language

English is the required language for repository documentation and development communication. Use English for:

- Source-code identifiers and comments
- Documentation and examples
- Test names and developer-facing fixtures
- Commit messages, pull requests, and issues
- Changelog entries and GitHub Release notes

User-facing localized text is exempt from this repository-language rule. English is the default locale and new user-facing text must always be added to the English resource catalog. Every supported locale must contain the same resource keys; Kiroku currently supports English and German. Explicit locale-specific tests and localization resources may use their target language.

Before submitting a change, search the maintained files for unintended non-English text and run:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
python3 scripts/verify_workout_storage.py
python3 scripts/verify_exercise_library.py
```

Run the SQLite checks after compilation so that generated Room schemas and migration code are available. Exercise-library changes must preserve recorded session snapshots, sets, and exercise identities when plan assignments change. Replacing a day's exercise must never attribute the old exercise's sets to its replacement.

## Publishing

Use the authenticated GitHub account `totallyeli` and commit identity `Elias Bugiel <totallyeli@users.noreply.github.com>`. Verify authentication with `gh api user` before pushing or creating a release. Do not rewrite published history or tags as part of a normal release.

Release APKs must use the existing signing certificate and a higher version code. Keep signing material outside version control and use the local ignored signing properties described in the README. Run tests, release lint, migration checks, and APK signature verification before publication.
