# LexyClient 3.0.0 — Visible, usable, verifiable

Fabric client-only mod for Minecraft **1.20.1**, Java 17, Fabric API.

## When the mod appears to do nothing
1. Check **Mods** (if Mod Menu is installed). Look for `LexyClient`, version 3.0.0. Mod Menu is optional.
2. On the title screen find **LEXY 3.0 OPEN MENU**; it also appears on Pause (ESC).
3. Press **Right Shift** or **F8**. Both are registered key bindings. On Title/Pause the screen keyboard handler also opens the GUI.
4. In-game, type **`/lexy`** (client-side, works even when the server has no Lexy plugin).
5. Type **`/lexy status`**. If unknown, Fabric did not load LexyClient: check launcher profile, game version, `mods` folder and Fabric API.
6. In `%APPDATA%\.minecraft\logs\latest.log`, search `[LexyClient 3.0.0]`. For crashes, send the log.

## New practical features
- Hand-drawn icon sprite atlas for the 12-section graphite-and-blue UI.
- Utility tools: copy XYZ, auto sprint, reversible fullbright, loaded watermark, HUD editor, max FPS profile, reset UI, save settings.
- `/lexy coords`, `/lexy status`, `/lexy resetui`, `/lexy sprint`, `/lexy bright`, `/lexy watermark`.
- In-world visual indicator and action bar so you can verify the mod is really active.
- 5 more useful HUD metrics: biome, server, day time, Java memory, empty inventory slots.
- 4 additional real video options: brightness, GUI scale, chat opacity and chat scale.
- Existing AutoTotem, TriggerBot, Aim Assist, AutoClicker, Zoom, macros, Spotify Connect, local MP3 playback and optimization retained.

All options are real UI controls, not fake modules. Server protections and Spotify account restrictions still apply.

## Build
GitHub Actions builds `gradle build` on Java 17 with Fabric 1.20.1 mappings. The build proves Java compatibility and bundling. A separate GitHub startup smoke test launches Minecraft in Xvfb and checks the mod entrypoint plus resource reload; it still does not test actual gameplay or mouse/key interactions. Install only the remapped `.jar` alongside Fabric API, removing older LexyClient jars.
