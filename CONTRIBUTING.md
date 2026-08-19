# Contributing

## Project language

English is the required language for repository documentation and development communication. Use English for:

- Source-code identifiers and comments
- Documentation and examples
- Test names and developer-facing fixtures
- Commit messages, pull requests, and issues
- Changelog entries and GitHub Release notes

User-facing app text is exempt from this repository-language rule. Kiroku currently ships with a German interface, which must remain unchanged until multilingual support is implemented as a dedicated feature. Localization resources and explicit locale-specific test cases may use their target language; a language-specific test should state why that locale is required.

Before submitting a change, search the maintained files for unintended non-English text and run:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```
