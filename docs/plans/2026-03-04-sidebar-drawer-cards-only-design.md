---
description: Sidebar drawer + cards-only main UI
---

# Goal

Add a modal sidebar navigation drawer with menu destinations (Actions, Win Patterns, etc.) and simplify the main UI so it only displays bingo cards, with a single add (`+`) entry point for creating/importing cards.

# Current Context

- UI is Jetpack Compose + Navigation Compose.
- Entry point: `MainActivity` -> `BingoApp()`.
- Navigation is centralized in `ui/App.kt` with `NavHost`.
- The current Cards screen (`CardListScreen`) includes an "Actions" panel plus the cards list.

# UX Requirements

- Use a **modal navigation drawer** (hamburger) rather than a permanent sidebar.
- The main Cards screen should be **cards-only**.
- Add a **`+` button** on the Cards screen that lets the user choose:
  - New Random
  - Scan/Import
- Move the following controls off the Cards screen into an **Actions** destination:
  - Called number input + Mark
  - Win pattern chips (toggle active patterns)

# Information Architecture

## Drawer Menu Items

- Cards
- Actions
- Win Patterns
- Scan/Import (optional shortcut; Scan is also available via `+`)
- Settings (optional placeholder, not required for this change)

## Routes

Existing routes:

- `cards`
- `card/{cardId}`
- `scan/import`
- `scan/camera`
- `scan/review`
- `patterns`

New route:

- `actions`

# Screen Designs

## Cards (cards-only)

- Top app bar
  - Title: "Bingo Cards"
  - Navigation icon: hamburger (opens drawer)
- Content
  - Cards list (existing)
- Floating action button
  - `+` opens a small chooser UI
    - New Random (calls existing VM action)
    - Scan/Import (navigates to Scan flow)

## Actions

- Top app bar
  - Title: "Actions"
  - Navigation icon: hamburger (opens drawer)
- Content
  - Called number text field (1–75)
  - Mark button
  - Win pattern chips (toggle active patterns)

## Win Patterns

- Existing `PatternsScreen`.

# State & Data Flow

- `CardListViewModel` remains the source of truth for:
  - `calledNumberText`
  - `onCalledNumberTextChanged`
  - `callNumber(...)`
  - patterns list + toggle
- `ActionsScreen` and `CardListScreen` should share the same `CardListViewModel` instance (via navigation back stack scoping) so state changes are reflected immediately.

# Implementation Notes

- Place `ModalNavigationDrawer` at the app shell level in `BingoApp()` so it wraps the `NavHost`.
- Use `DrawerState` + coroutine scope to open/close.
- The `+` chooser can be implemented as a Material 3 modal bottom sheet or a simple dialog.

# Non-goals

- Full Settings implementation.
- Major redesign of card detail or scan flow beyond adding navigation entry points.
