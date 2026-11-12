# LexyClient 2.8.1 — Blue Slate Interface

A Fabric **1.20.1** Minecraft client mod for PC and PojavLauncher (Java 17).

## Redesigned interface

- Unified graphite/navy/blue theme with a new two-pane workspace, compact status rail, smoother hover feedback, active module cards, clearly defined action buttons and genuine clickable controls.
- A new keybind/macro screen uses the same visual language as the main GUI. The HUD editor and title branding have also been refreshed.
- Sidebar sections remain scrollable via mouse wheel and dedicated touchscreen-friendly buttons. Scrollable content is clipped to its own pane.
- Dashboard values are cached briefly to avoid recalculating every single data source on each frame. The cache lasts at most ~220 ms and does not affect gameplay automation.
- Module and combat configurations are kept, including AutoTotem, TriggerBot, Aim Assist, AutoClicker, zoom, macros, Spotify and local music.

## Keyboard shortcuts

- **Right Shift**: open *and* close the main LexyClient menu. This is intentionally independent of Left Shift (the in-game sneak key).
- **Escape**: close the current menu; in the key editor or HUD editor, return to the parent menu.
- **Ctrl+F**: focus dashboard module search while in a modules category.
- **Right Ctrl**: HUD editor shortcut while in-game.
- Keybinds and macros remain editable in `Keybinds` and in Minecraft's own Controls where applicable.

## Installation

Install Minecraft 1.20.1, Fabric Loader, Fabric API and Java 17. Use the remapped `lexy-client-2.8.1.jar` from `build/libs` or GitHub Actions, in `mods`. **Remove earlier LexyClient JARs** to avoid conflicting mod IDs.

## Build

Requires JDK 17 and Gradle 8.11.1: `gradle build`. Continuous integration compiles and publishes the JAR artifact in GitHub Actions.

Combat automation may violate multiplayer server rules. Build verification is not the same as an in-game device test. Spotify's streaming and playback APIs still respect account and platform restrictions; Free mode opens the official Spotify app and local audio plays your own files inside Minecraft.
