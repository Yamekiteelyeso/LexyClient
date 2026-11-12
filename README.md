# LexyClient 2.4.0 — Fabric 1.20.1

Requires Minecraft Java 1.20.1, Java 17, Fabric Loader and Fabric API.

- PC + Pojav-friendly menu, drag-and-drop HUD, optimized overlays, graphics options and original OGG UI sound pack.
- Spotify Free web mode: opens searches, library and Spotify official browser/app links without OAuth. This does **not** control playback from Minecraft or bypass Spotify plan limits.
- Spotify Web API PKCE mode: premium/allowlist restrictions as defined by Spotify; session persistence and token refresh from Lexy 2.3.
- Separate opt-in TriggerBot under Cheats, configurable targets, attack interval/cooldown, range and weapon/sneak/team conditions, using vanilla melee interactions.
- AutoTotem retained with direct and inventory approaches.

Build: `gradle build` with Gradle 8.11.1 / JDK 17. Install the remapped `build/libs/lexy-client-2.4.0.jar` into `mods`. Remove prior LexyClient JARs.

No guarantee of server acceptance or Pojav runtime compatibility without testing on the target device.