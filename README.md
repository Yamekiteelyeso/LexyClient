# LexyClient 1.6 — Blue Edition for Fabric 1.20.1

**Working source for Minecraft 1.20.1, Java 17, Fabric.** Build via GitHub Actions or Gradle 8.11.1. Tested by CI compile; full Minecraft UI/gameplay verification still recommended.

## Main controls

- **Right Shift** — open LexyClient ClickGUI. **Left Shift** — close it.
- **MOVE HUD** button (top-right and in the sidebar) — position overlays on the *actual game screen*. It is not a miniature preview.
- **Right Ctrl** — open the HUD layout editor directly.
- Inside HUD editor, drag the FPS, Ping and other enabled readouts; use buttons `FPS` and `PING` to show/hide them, `+`/`-` for size, `RESET` to restore positions. Escape or Left Shift saves and returns.
- **Appearance** — edit Menu Size (70–110%), HUD Size (45–150%), drag snap/grid, text colors, opacity, spacing and alerts. Screen resolution is automatically considered even at 100%.

## Spotify

In **Spotify** tab, enter the Spotify *Client ID* once (not the client secret). Select **LOGIN WITH SPOTIFY**. LexyClient opens the official `https://accounts.spotify.com/authorize` link in the PC browser and uses Authorization Code + PKCE, then receives the callback locally at `http://127.0.0.1:43871/callback`.

1. Create your Spotify application at https://developer.spotify.com/dashboard (GET CLIENT ID button opens it).
2. Under app settings allow the exact redirect URL `http://127.0.0.1:43871/callback`.
3. Copy its public **Client ID** into LexyClient (never use a client secret).
4. Select **LOGIN WITH SPOTIFY** and approve it in the browser.
5. Start Spotify Desktop on the same PC and select `USE COMPUTER` if necessary; use controls inside Minecraft.

Spotify's Web API **does not stream Spotify audio into Minecraft**. Audio plays through the active Spotify Connect device. Local WAV/AIFF playback is provided in the separate **Local Music** tab inside Minecraft itself. Spotify account/app availability and Premium requirements may limit playback controls.

## Building

- Install JDK 17.
- Run `gradle build` using **Gradle 8.11.1** (or download the build artifact from the GitHub Actions workflow).
- Install `build/libs/lexy-client-1.6.0.jar` into `.minecraft/mods`, along with Fabric API for 1.20.1.
- Remove older `lexy-client` jars to avoid duplicate mod IDs.

## Note

AutoTotem is opt-in and may be prohibited on some servers. Combat statistics from the client are approximations, not server-verified facts.