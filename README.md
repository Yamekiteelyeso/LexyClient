# LexyClient 2.7 — Slate Blue Combat Update

Fabric 1.20.1 client mod for PC and PojavLauncher, built against Java 17.

## What's new
- **Aim Assist:** configurable smooth rotation and target acquisition with distance/FOV/visibility/weapon/team filters. Disabled by default.
- **AutoTotem:** emergency direct swap, quick offhand refill; mouse-safe inventory swap.
- **TriggerBot:** stable targets, configurable timing variation, invisibility and critical-hit filters.
- **Control keys:** Aim Assist toggle/hold; emergency disable all combat modules; previous Zoom and four macros.
- **Adaptive optimization:** tune FPS target and min/max render chunks in the Performance menu.
- Slate and blue interface with three real combat panels and six accent swatches.
- Existing Spotify Free (open official player), Spotify API Connect and local MP3/WAV/AIFF player remain. Spotify content is not streamed from Spotify into Minecraft.

## Build and install
JDK 17, Fabric Loader + Fabric API for 1.20.1, Gradle 8.11.1. Run `gradle build` and install the remapped `build/libs/lexy-client-2.7.0.jar` in `mods`. Remove prior versions.

Auto combat modules may violate multiplayer server rules; they default to OFF. A successful compilation does not replace real device/server testing.

## AutoClicker (new in 2.7)

Cheats > CLICKER > CONFIGURE: left/right/both, mouse hold/key hold/toggle, independent CPS intervals (bounded by 20 ticks/s), jitter, attack charge, range (vanilla max 3 blocks), teams/invisible/weapon filters, optional air swing, block/item interaction, sneak and pause guards. Disabled by default. Assign toggle and hold keys in Keybinds or Minecraft Controls. No GUI clicks, no packet/reach manipulation. TriggerBot suppresses its own attacks when AutoClicker handles left.