# LexyClient 4.2 — Studio

A Fabric 1.20.1 client-side mod (Java 17). Lexy 4.2 focuses on a considered module center, responsive title, visual consistency, genuine configuration controls and lower per-frame UI overhead.

## Installation

Use Fabric Loader + Fabric API and place only `lexy-client-4.2.0.jar` in the selected Minecraft installation's `mods` directory. Use RSHIFT, F8, `/lexy` in-game, or the Lexy title/pause entry point. If the UI does not appear, check whether the game log actually lists `lexy-client 4.2.0` as loaded.

## What's new

- The title is an original dark atmospheric backdrop, with actual navigation into Minecraft's world list, multiplayer, Realms (vanilla fallback), options and Lexy config.
- A redesigned, Lunar/Feather-inspired but distinct visual system: large pictogram module cards, searchable categories, favorites, clear action states, advanced settings drawer and accessible compact layouts.
- 32 hand-drawn original vector icons packaged as a single atlas, a rebuilt Lexy mark, seven persisted themes, and a functional profile save/load/delete flow.
- The Dashboard shows FPS, ping, RAM, active modules and session time; Restore Graphics reverses the last video preset applied in this session.
- Cached filtered module lists and settings rows; the GUI does not load images from disk every frame.
- Existing functional AutoTotem, TriggerBot, AutoClicker, Aim Assist, music, HUD, and optimization logic is retained, as is the legacy advanced settings interface.

Read `LEEME_ES.md` and `RELEASE_NOTES.md` for details. This is not a substitute for gameplay testing on a target device and there are no fabricated FPS benchmarks.

## Validation

`gradle build` and `gradle runClient` under Xvfb are checked using GitHub Actions. If Minecraft does not recognize the mod, `latest.log` is the authoritative diagnostic.

## Monochrome HUD / HUD monocromatico
Black and grey only. Compact FPS, Ping, HP and Totems widgets. RCtrl to drag the HUD; -/+ changes size, STYLE cycles flat/outline/text, BG toggles background, SNAP/GRID are optional. Existing widget positions are preserved on upgrade.