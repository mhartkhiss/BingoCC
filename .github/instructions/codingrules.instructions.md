---
description: Project-wide coding rules for Android Kotlin development
applyTo: '**'
---

# Goal
Keep coding style strictly modular, maintainable, and aligned with modern Android and Kotlin development standards.

## General Architecture Patterns
- Docs when uncertain: consult the `context7` MCP server for the latest guidance before writing code for new APIs or unfamiliar libraries.
- DRY and cleanliness: avoid duplicate code and keep logic clear with a robust folder hierarchy.
- Separation of concerns: do not mix UI, business logic, and data access. Use suitable patterns like MVVM or MVI.

## Android Kotlin Rules
- Kotlin file limits: keep any single file (including Compose UIs and Services) under 500 lines.
- Resource constraints: no hardcoded strings, colors, or dimens in Kotlin files. Use `strings.xml`, `colors.xml`, `dimens.xml`, and related resources. Keep shared constants in `Constants.kt`.
- Self-correction and memory: when build errors are caused by missing imports, add imports and retain context across retries.

## Build Verification
- Assemble debug check: verify changes by running `./gradlew assembleDebug` and ensure it succeeds before concluding work.