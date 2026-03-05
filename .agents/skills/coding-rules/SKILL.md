---
name: coding-rules
description: Unified coding guidelines Android (Kotlin) focusing on cleanliness, modularity, and rules.
---

# Goal
Keep coding style strictly modular, maintainable, and aligned with modern Android and Kotlin development standards.

# General / Architecture Patterns (All Platforms)
- **Docs when uncertain**: Always consult the `context7` MCP server for the latest guidelines before writing code for new APIs or unfamiliar libraries.
- **DRY & Cleanliness**: Avoid duplicate code. Structure logic clearly with a robust folder hierarchy.
- **Separation of Concerns**: UI, business logic, and database access must not blur into each other. Use appropriate design patterns (like MVVM / MVI).

# Android (Kotlin) Rules
- **Kotlin File Limits**: Limit any single file (including Compose UIs or Services) to strictly under 500 lines.
- **Resource Constraints**: No hardcoded strings, colors, or dimens in Kotlin files. Use `strings.xml`, `colors.xml`, `dimens.xml`, etc. Keep shared constants in `Constants.kt`.
- **Self-Correction & Memory**: Ensure build errors from missing imports are corrected by adding imports and retaining context across attempts.

# Build Verification
- **Assemble Debug Check**: Always verify changes by running a debug build. Execute `./gradlew assembleDebug` and ensure it completes successfully without errors before concluding the task.
