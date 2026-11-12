# LexyClient 4.0.0 — Ultimate UI & Reliability

## Implemented
- New original 12-category module interface with responsive 1/2/3-column cards, icon atlas, favorite filter, search, module-specific settings and themes.
- New title UI with Singleplayer, Multiplayer, Realms (vanilla API), Mods, Lexy Settings, Profiles, Minecraft Options and Quit; vanilla fallback remains accessible.
- RShift, F8, `/lexy` and pause/title controls access the same modern menu.
- Seven styles, UI sound and animation controls, local profile save/load with JSON validation and atomic disk writes.
- AutoTool, AutoWalk, ToggleSneak, AutoRespawn, alerts, waypoints, native AutoJump, four existing combat modules, Zoom, local music integration and real vanilla graphics settings.
- Expanded Optimization with over 20 directly adjustable vanilla performance options, four presets and conservative adaptive view-distance controls.
- Preserves the existing Combat advanced settings, Keybinds/Macros, HUD editor and Spotify integration through reachable legacy detail screens.
- Displays module version at startup in logs and in-game for troubleshooting.

## Not implemented / limitations
- No extended-reach packet spoofing, fly bypass, server-forced velocity, or unlocked Spotify catalog playback without Premium.
- No claims of measured FPS increase until tested against a baseline on the same hardware.
- Full GUI clickthrough requires real-device testing. If Fabric's mod listing omits `lexy-client`, installation path must be fixed; code cannot load a JAR that the launcher did not discover.