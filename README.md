# LexyClient 2.0 — Fabric 1.20.1

Minecraft client mod for desktop/PojavLauncher with responsive interface, a curated draggable HUD, custom UI sounds, configurable performance presets and Spotify OAuth PKCE controls.

- 4 original OGG UI chimes; adjustable volume and on/off.
- Optimization/performance values are **menu-only**; in-game HUD strictly allowlists useful metrics and caps visible widget count.
- Spotify Android ACTION_VIEW attempt, copy-login-URL fallback and secure manual callback paste to complete PKCE when loopback callbacks fail. Requires user's registered Spotify Developer application and exact redirect `http://127.0.0.1:43871/callback`.

Run Gradle with Java 17 and Fabric Loom: `gradle build`. Dependencies: Fabric API 0.92.3+1.20.1.

Full instructions: LEEME_ES.md.