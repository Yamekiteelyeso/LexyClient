# LexyClient 4.1 — Studio UI Rework

This release addresses visual quality and navigation rather than claiming a complete new cheat engine.

## Actually implemented

- New original, dark slate interface with a consistent type hierarchy, 32 hand-drawn vector icons, centered module illustrations, and clear enabled/disabled/open states.
- Settings appear in a right-side drawer; the catalog stays visible behind them. Escape closes the drawer before leaving the menu.
- Real category navigation, favorites, search, activation controls, contextual module configuration, keyboard shortcuts and scrolling.
- Redesigned title screen with an original Minecraft-inspired backdrop and direct actions for singleplayer, multiplayer, Realms (or vanilla fallback), Lexy modules, profiles, options and exit.
- Seven persisted theme palettes; saved profiles can now be deleted using a two-click confirmation and existing profiles still load as before.
- Fixed dashboard shortcut hitboxes and skipped clicks on catalog cards outside the clipped viewport.
- Performance: cache filtered module lists, settings arrays, and the profiles list rather than rebuilding during every render frame. Icons are packed into one atlas.
- Compact-screen support with collapsed side rail and touch-friendly paging; vanilla title can always be recovered.
- All existing combat, music, HUD, zoom, macros and performance features remain in the project.

## Limits / tests

- Fabric Gradle compilation and an actual Minecraft client-entrypoint smoke test run through GitHub Actions. These are not equivalents of verifying every mouse/touch interaction.
- The legacy advanced configuration screen remains accessible from relevant modules. It has not been fully redesigned.
- The render backdrop is a precomputed texture; no expensive post-processing blur/shaders are added.
- No claimed FPS percentage. Existing module behavior on multiplayer servers is not guaranteed or bypassing server validation.
- Compatible with Minecraft Java 1.20.1 + Fabric API. Load the correct `mods` folder for the selected launcher/profile.