# LexyClient 2.7.0 — AutoClicker & Combat Quality

- Added client-tick AutoClicker with left/right/both and mouse/key/toggle activation.
- Configurable CPS range, timing variance, attack charge, weapon/team/invisible filters, sneak, vanilla reach and air swings.
- Optional right-block/right-item use; no inventory or GUI click injection.
- Added toggle/hold keybinds, persisted settings, panic-disable support.
- Prevent duplicate auto attacks by coordinating with TriggerBot and AutoTotem.
- New four-way Cheats navigation and right-click reverse setting changes.
- Retains Aim Assist, AutoTotem, Zoom, macros, Spotify, local MP3, optimized HUD.

# LexyClient 2.7 — Slate Blue Combat Update

### New: Aim Assist (OFF by default)
- Smooth yaw/pitch aiming at visible living entities, with a configurable cone and aim range.
- Filters for players/mobs, teams, invisibility, held weapon, and hold-key requirement.
- Adjustable smoothness, rotation cap per tick, target height, prediction and target lock.
- Does not modify attack reach or fabricate packets. Disable where prohibited by server rules.

### Combat stability
- AutoTotem: new emergency direct-swap and fast empty-offhand options; keeps mouse control during attacks.
- TriggerBot: target stability requirement, timing variation, invisibility and critical-only filters.
- Panic key to disable all combat modules immediately. Dedicated Aim Assist toggle and hold keys.
- All configuration persists across sessions and is accessible in Cheats and Controls.

### Interface and performance
- Three Cheats tabs: AutoTotem, TriggerBot and Aim Assist.
- Slate blue / graphite theme refresh with six selectable accent tones.
- Performance: adjustable adaptive FPS target, minimum and maximum render distance.
- Existing Spotify, local audio, MP3 playback, 11 UI sounds, key macros, zoom and Pojav touch navigation retained.

### Verification status
- GitHub Actions compiles a real Fabric 1.20.1 remapped JAR; manual in-game/Android testing still recommended.