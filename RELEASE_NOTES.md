# LexyClient 1.9 — Mobile & Desktop usability update

## GUI
- ClickGUI adaptive scaling improves usability on smaller Minecraft windows and PojavLauncher.
- On-screen **X** button closes ClickGUI without keyboard shortcuts.
- On-screen **UP / DOWN** menu navigation avoids requiring a mouse wheel.
- **MOVE HUD** from ClickGUI or right Ctrl, and dedicated **DONE** in HUD editor.
- LexyClient entry points from the title screen and in-game pause screen.
- Performance tab once again lists the Performance readouts, which 1.8 accidentally filtered out.

## HUD
- HUD editor controls moved to bottom so upper-left readouts remain visible and draggable.
- Three selectable presets (Minimal / Normal / Extended), zoom, enable/disable FPS and Ping, reset positions.
- Actual client FPS and server-advertised ping (not a direct network latency test), color-coded by rough ranges.
- Only enabled widgets with meaningful values are rendered, with compact defaults for migrated configurations.
- Percentage-position storage is retained for resolutions that change between PC and Pojav.

## AutoTotem
- Single **Cheats** tab containing **AutoTotem** with separate enable/disable and **CONFIGURE** button.
- Configuration: only when offhand empty, health-only switching, health threshold, action interval, notifications.
- No automatic activation by default.

## Spotify
- Authorization Code PKCE continues to use the official Spotify page.
- Browser-launch fallback for Pojav; manual **COPY URL** action if needed.
- **CANCEL LOGIN** resets the pending callback, and callback page reports success only after a successful token exchange.
- Spotify Developer Client ID and `http://127.0.0.1:43871/callback` registration are required; Android browser loopback behavior depends on the OS/launcher.
- Spotify Web API permissions and Premium/account restrictions may apply. This is a remote player controller, not in-game Spotify streaming.

## Build
- Fabric 1.20.1, Java 17, Fabric API. Compilation can be verified with GitHub Actions.
- PojavLauncher runtime behavior is **not confirmed** without testing on a device.