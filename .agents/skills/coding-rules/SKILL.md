---
name: coding-rules
description: Unified coding guidelines Android (Kotlin) focusing on cleanliness, modularity, and rules.
---

# Goal
Keep coding style strictly modular, maintainable, and aligned with user preferences for both Android and React Desktop environments.

# General / Architecture Patterns (All Platforms)
- **Docs when uncertain**: Always consult the `context7` MCP server for the latest guidelines before writing code for new APIs or unfamiliar libraries.
- **DRY & Cleanliness**: Avoid duplicate code. Structure logic clearly with a robust folder hierarchy.
- **Separation of Concerns**: UI, business logic, and database access must not blur into each other.

# Android (Kotlin) Rules
- **Kotlin File Limits**: Limit any single file (including Compose UIs or Services) to strictly under 500 lines.
- **Resource Constraints**: No hardcoded strings, colors, or dimens in Kotlin files. Use `strings.xml`, `colors.xml`, `dimens.xml`, etc. Keep shared constants in `Constants.kt`.
- **Self-Correction & Memory**: Ensure build errors from missing imports are corrected by adding imports and retaining context across attempts.
- **Verification**: Run `.\gradlew app:assembleDebug --no-daemon 2>&1` from the project root to verify the build. Output streams directly to the terminal.
