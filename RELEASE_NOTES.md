# LexyClient 2.2.0 — Slate Edition

Compatible with Minecraft 1.20.1 / Fabric Loader / Fabric API / Java 17.

## Reliability
- AutoTotem direct mode now checks both offhand and source inventory slot, waits several ticks for confirmation, and applies bounded backoff when the server rejects an operation. A server may still roll back an inventory change after local prediction; no client can guarantee behavior on every server.
- Inventory mode waits for each expected cursor/offhand state and attempts safe cursor restoration before closing, with ping-aware minimum step delays and timeouts.
- AutoTotem retains configurable delays, health threshold, random source selection and inventory mode. Disabled by default.

## Spotify on PojavLauncher
- Official PKCE; faster loopback callback response, asynchronous token exchange, duplicate authorization protection, five-minute login window, and protected cancellation.
- Better explanation of 400/401/403/404/429 errors, account allowlist/Premium constraints, and Spotify device selection on Android.
- Mobile controls: launch Spotify app, copy authorization link, paste callback from clipboard, finish connection without manually typing an entire URL.
- Reduced API polling and rate-limit backoff. No Spotify audio is downloaded or streamed inside Minecraft.

## UI, sounds, optimization
- Modern slate/graphite palette, restrained blue accents, calmer navigation, clearer enabled vs informational modules, fewer unnecessary toggles.
- Responsive Spotify layout with non-overlapping search, current track, devices and controls.
- 7 new original OGG sound effects in addition to the 4 existing ones (11 total), separate hover-audio toggle and sound volume.
- Cached HUD layouts refreshed on demand and at limited frequency, with direct invalidation when configuration or positions change.
- More informative error states and clean single-purpose HUD widgets.

## Verification
- Java/Fabric build via GitHub Actions required. Functional testing on a real PojavLauncher device and a multiplayer server is still required; a green compilation alone is not proof of runtime correctness.