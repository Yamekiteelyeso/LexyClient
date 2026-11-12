# LexyClient 1.9

A Fabric **Minecraft Java 1.20.1** client-side mod focused on mobile/desktop UI ergonomics. PojavLauncher support is a design goal, not a device-tested guarantee.

Features: compact draggable HUD, FPS / server-reported ping badges, customizable HUD presets, responsive ClickGUI, accessible on-screen navigation and pause-menu entry point, dedicated Cheats > AutoTotem configuration, Spotify PKCE remote control, local WAV/AIFF music and graphics presets.

## Build
Java 17 and Gradle 8.11.1:

```sh
gradle build
```

The distributable mod is `build/libs/lexy-client-1.9.0.jar` (not the sources jar). Requires Fabric Loader and Fabric API.

## Controls
- Right Shift: ClickGUI.
- Left Shift or Escape: Close.
- Right Control: in-world HUD editor.
- On a touch screen, use the LexyClient button on the **pause menu**, then tap the navigation arrows, **MOVE HUD**, **X** and HUD **DONE**.

Spotify authorization: public Client ID + PKCE, `http://127.0.0.1:43871/callback` registered in your developer application. Browser availability and loopback callback behavior vary on Android.

See `LEEME_ES.md` for a Spanish installation guide and `RELEASE_NOTES.md` for change details.