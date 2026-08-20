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
```
