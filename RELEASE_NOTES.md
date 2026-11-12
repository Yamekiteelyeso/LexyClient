# LexyClient 2.3.0 — Spotify Session Edition

## Spotify integration inspired by publicly observable SpotiCraft behavior

- Reimplemented the working architecture independently; SpotiCraft 1.20.1 is All Rights Reserved. No SpotiCraft classes, assets, embedded client ID, or source code are bundled.
- Reconnect on next Minecraft launch using a stored refresh token, bound to the user's own Spotify Client ID. Explicit disconnect deletes the token file; closing Minecraft no longer disconnects the account.
- Atomic, restricted-permission storage in `config/lexy-spotify-session.json` (best effort on Android filesystems); secrets are never printed or stored inside the general Lexy configuration.
- Uses official Spotify Authorization Code + PKCE and validates authorization `state`. Uses existing local loopback callback and Pojav browser/clipboard fallback.
- Profile display name, playback progress, repeat/shuffle, volume controls, Spotify Connect devices, playlist browsing/playback, and track search.
- Background token refresh, 429 retry handling, status messages. Compatible with Fabric 1.20.1 and Java 17.

## Limitations

Spotify's API controls a separate Spotify app/player; music isn't streamed into Minecraft. Spotify developer apps require appropriate account permissions, Premium and user allowlists. Playback, device features and playlist availability vary with Spotify API policies. No live Pojav Android session was available for runtime testing.