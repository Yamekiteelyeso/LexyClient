# LexyClient 2.5 — Blue Slate Edition

Fabric 1.20.1 client mod for Java 17, PC and PojavLauncher.

## Changes
- AutoTotem inventory is a single atomic vanilla offhand SWAP and opens only when the player is idle; combat fallback uses DIRECT without opening menus. Configurable safe idle and fallback.
- TriggerBot guards manual mouse clicks, checks crosshair line of sight, distance, cooldown and optional hold-key.
- Real rebindable key controls (Minecraft Settings > Controls) for AutoTotem, TriggerBot, hold-to-trigger, and Zoom, plus four single-press chat/command macros configured through LexyClient > Keybinds.
- Hold or toggle Zoom with safe FOV restoration.
- Pojav low-graphics preset and an optional conservative adaptive-FPS governor. No false FPS boost claims.
- In-game Local Music now accepts standard MP3 files using bundled Java Sound decoder, plus WAV/AIFF; Spotify's own licensed music streaming still requires official Spotify clients or Premium-capable APIs.

Compiled using Gradle 8.11.1 via GitHub Actions. Full Minecraft/Pojav gameplay tests are needed separately.