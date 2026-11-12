# LexyClient 2.1.0

Client-side mod for **Minecraft 1.20.1 + Fabric**. The source project includes a responsive graphite/blue menu, customizable HUD widgets, audio UI feedback, Spotify OAuth PKCE controls and optional AutoTotem inventory helpers.

## Build

Java 17 and Gradle 8.x: `gradle build`. Use the build artifact from CI in `.minecraft/mods` with Fabric API.

## AutoTotem 2.1

Switch between `DIRECT` slot SWAP and an `INVENTORY` sequence that opens the vanilla inventory, renders a virtual pointer, and performs checked vanilla PICKUP actions. Configure randomized totem source-slot choice and millisecond delays in `Cheats > CONFIGURE`.

This is not a server-side plugin and cannot override server inventory rules. Millisecond values are sampled on 20 Hz client ticks. The system cursor is not moved, which keeps touch launchers supported. Please follow server rules.