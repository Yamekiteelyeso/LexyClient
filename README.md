# LexyClient 4.0 — Ultimate Edition

Minecraft 1.20.1 / Fabric / Java 17. A client-side mod with an original Lunar/Feather-inspired layout.

## Install
Put `lexy-client-4.0.0.jar` in the **active instance** `mods/` directory alongside Fabric API. Remove older LexyClient JAR files. Use Fabric 1.20.1, not Forge.

**Windows:** `%APPDATA%\.minecraft\mods` is only the default path. Launchers may use different instance folders.

**Amethyst (Android):** identify the active `.minecraft` directory in the launcher settings. One example observed in the user's previous log was `/storage/emulated/0/Android/data/git.artdeell.mjlaunch/files/.minecraft/mods/`. Android permissions and profiles may change this; use the actual game directory.

If the `latest.log` `Loading ... mods:` section does not list `lexy-client 4.0.0`, Fabric **did not discover the file**. Changing keybinds or GUIs cannot fix that.

## Controls
- Right Shift: open / close modules
- F8: backup menu
- `/lexy`: open in-world (and `/lexy status` to check mod load)
- Title screen and pause menu: Lexy button
- Menu: 12 categories; HUD editor; Settings; Profiles; search and favorites
- `ESC`: return to previous screen
- On custom title screen, `ESC` opens vanilla title for recovery.

## Functionality
Four combat modules from Lexy 3.0, configurable macros, full-size HUD, waypoints, AutoSprint, AutoWalk, AutoTool, AutoRespawn, ToggleSneak, alerts, zoom, native video presets, adaptive frame-rate target, 7 themes, profile snapshots and optional local audio/Spotify API.

Profile snapshots live in `.minecraft/config/lexy-profiles/` and UI settings in `.minecraft/config/lexy-ui-v4.json`. Existing 3.0 settings remain in `lexy-client.json`.

Spotify's official API restricts in-game streaming and remote playback; local MP3 playback requires no premium account.

## Verification
Build with `gradle build` using the GitHub CI workflow. Headless startup smoke testing is supplemental; it cannot confirm mouse interactions, Pojav rendering or server gameplay. Do not mistake a successful compile for a complete functional test.