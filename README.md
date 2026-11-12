# LexyClient 2.3.0 — Spotify Session Edition

A Fabric Minecraft 1.20.1 Java 17 client mod. GitHub Actions builds a remapped installable JAR.

The Spotify design recreates comparable behavior to the user's reference SpotiCraft 1.20.1 mod **without** copying its proprietary code or client ID (SpotiCraft is All Rights Reserved).

Spotify features: Authorization Code + PKCE with local callback and mobile clipboard fallback; user-owned Spotify Client ID; token refresh session persistence via `SpotifySessionStore`; profile, playback and progress, shuffle/repeat/volume, Spotify Connect device choices, track search and playlists. Tokens are private local config, and not logged.

Spotify is a separate player; neither the app nor LexyClient streams Spotify audio in Minecraft. Spotify Premium/development allowlists and API restrictions still apply.

Also retains the scalable GUI, draggable HUD, original OGG UI audio cues, optimization settings and existing AutoTotem controls.

See [LEEME_ES.md](LEEME_ES.md) for user configuration and installation details.