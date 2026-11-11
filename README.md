# Lexy Client 1.4.0 — Blue Edition

Client-only Fabric mod for **Minecraft 1.20.1**, built with **Java 17**. Designed for legitimate BoxPvP gameplay, with graphite/blue menus, HUD customization, real video settings, Spotify Connect controls and a **real in-process music player for your own files**.

## Controls

- **Right Shift** — open Lexy GUI (registered with `GLFW_KEY_RIGHT_SHIFT`).
- **Right Ctrl** — edit the HUD positions by dragging them.
- Optional **Lexy: Spotify** keybinding in Minecraft's Controls menu opens the Spotify tab.
- **Esc** — close the GUI.

## UI

The custom Screen has a graphite background, blue accent colors, module switches, live module search, resizable viewport, scrollable sidebar navigation, optimization sliders / buttons, Spotify tab and Local Music tab. Four blue accent shades, HUD opacity and spacing settings are persisted to `config/lexy-client.json`.

## Spotify on the same PC — no phone required

Lexy includes an official Spotify Web API OAuth2 PKCE implementation. Connect your account, search tracks, queue songs, play/pause, skip, select a device, use **USE COMPUTER** (selects an available Spotify Connect device of type `Computer`, preferring an active one), and **REFRESH DEVICES**.

**Crucial limitation:** Spotify's Web API does not let a Java mod decode or download Spotify music. Spotify audio comes from the official Spotify desktop application / official supported Spotify Connect player **running on this same computer**. A separate phone or speaker is *not* required, but Spotify itself must be running somewhere on the same computer. The `USE COMPUTER` control cannot identify which of several devices is physically local; if multiple computers are listed, select the intended computer from the device list. Spotify Premium, a Spotify developer Client ID, and Spotify app authorization are required for Spotify Web API playback functions. Spotify developer access policies apply.

This mod **does not pretend to have Spotify music streaming built into the Java game engine**, and it does not bypass Spotify DRM.

### Configuring Spotify

1. Create a Spotify app in https://developer.spotify.com/dashboard using a Premium account, subject to Spotify's developer access rules.
2. Register the exact redirect URI `http://127.0.0.1:43871/callback` in your Spotify app.
3. Launch Lexy -> Spotify. Enter your **Client ID** only (never a Client Secret), then press **CONNECT**. A browser window will be used once for authorization.
4. Start Spotify on your computer and choose **USE COMPUTER** in Lexy, or choose a computer from DEVICES. Search/play tracks directly from Lexy's GUI.

The OAuth tokens remain in memory during each Minecraft session; exit and reconnect on the next session.

## Local music actually playing inside Minecraft (no other player)

Lexy 1.4 adds `LocalMusicPlayer.java`, a Java Sound PCM streaming player running on a dedicated background thread **inside the Minecraft process**. Audio is sent to your PC's output sound device. No Spotify or second app is needed for your local files.

1. Open `Right Shift > Local Music` and click **OPEN FOLDER**.
2. Put your legally owned `.wav`, `.aiff`, `.aif` or `.au` music files in `.minecraft/config/lexy-music/`.
3. Click **REFRESH**, click a track, and use pause/resume, previous/next, stop, volume and loop.
4. WAV should be standard PCM if possible. Support for a particular file depends on codecs and audio devices available in your Java runtime. MP3 and streaming from Spotify are not included in the local player.

The local audio volume and loop selection persist in the JSON config. Stopping playback or closing Minecraft closes the associated audio line.

## Optimization and BoxPvP

- 13 real client video settings directly bound to Minecraft GameOptions.
- 3 video presets: Max FPS, Balanced and Quality.
- 84 independently toggled **HUD readouts**, not 84 separate game engine optimizers. Includes useful BoxPvP resources, armor durability, session stats, performance, Spotify and local player metadata.
- Conservative combat telemetry uses local observations. Attempted hits and possible kills are estimates rather than spoofed server stats.
- Does not automate combat, bypass server protection, or modify reach, knockback or movement packets.

## Build instructions

Requirements: JDK 17, Gradle 8.x compatible with Fabric Loom 1.9, internet access for downloading the Minecraft/Fabric artifacts.

Run from repository root:

```bash
gradle build
```

The compiled Fabric mod will appear in `build/libs/` (use the remapped version, not the `-sources` or `-dev` variants, if any). Install Fabric Loader + Fabric API for Minecraft 1.20.1 and put the mod in `.minecraft/mods/`.

The project includes `.github/workflows/build.yml` to compile and upload the JAR artifact via GitHub Actions after you push the source to a GitHub repo.

**Verification limitation:** This sandbox lacks Gradle, cached Fabric/Minecraft dependencies, and network access to download them, so an actual Fabric runtime JAR cannot be compiled or Minecraft-tested here. The standalone local player was compiled with `javac` using a FabricLoader stub and basic audio tests; this is not the same as testing in Minecraft. Do not label this source package as a compiled / tested game mod.

## Source structure

```
src/main/java/com/lexy/client/
  LexyClient.java         — keybinds, HUD readouts, events, saving
  LexyScreen.java         — graphite-blue GUI, module search, music controls
  SpotifyController.java  — OAuth2 PKCE, Spotify Web API, PC selection
  LocalMusicPlayer.java   — independent local PCM audio playback
  OptimizationManager.java — Minecraft graphics options + FPS presets
```

## Official Spotify docs

- https://developer.spotify.com/documentation/web-api/reference/get-a-users-available-devices
- https://developer.spotify.com/documentation/web-playback-sdk
- https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow