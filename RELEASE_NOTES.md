# LexyClient 2.4.0 — Spotify Free + TriggerBot

## Spotify
- Checked the user-provided SpotiCraft 1.20.1 mod. Its bytecode explicitly checks the account's `premium` status and renders a `noPremium` page. Thus SpotiCraft itself does not offer unrestricted free playback control.
- Added a genuine **Free Spotify** browser mode which does not need a Spotify Developer Client ID or Spotify API authorization. Search in the official Spotify app/browser, open the Spotify app or library; Spotify itself handles free playback and advertisements.
- Keeps the separate PKCE / OAuth Spotify API mode with stored refresh token for eligible users. Does not circumvent official API rules.
- Official Spotify's development API now requires the app owner to have Premium, and transport commands require Premium.

## Cheats — TriggerBot
- Separate TriggerBot tab inside Cheats; default OFF.
- Automatic melee click for visible living targets under the crosshair, using vanilla interaction manager.
- Filters: players/mobs/both; maximum conservative range 2.0–3.0 meters; optional sword/axe requirement; option to ignore teammates and to require sneaking.
- Configurable CPS 1–12 and charged attack minimum percentage. Options saved in config/lexy-client.json.
- Does not target entities through walls or extend vanilla melee range.

## Compatibility
- Retains existing AutoTotem, movable clean HUD, 11 original UI sounds and scalable UI for Minecraft 1.20.1 Fabric, PC and Pojav.
- No runtime Minecraft or real Spotify account test was performed; GitHub Actions compilation is used for Java/Fabric compatibility.