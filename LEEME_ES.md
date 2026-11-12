# LexyClient 1.9 — Fabric 1.20.1

## Instalación PC
1. Instala **Fabric Loader** para Minecraft Java 1.20.1.
2. Coloca **Fabric API** y el `.jar` de LexyClient 1.9 en `.minecraft/mods`.
3. Retira cualquier versión antigua de LexyClient antes de iniciar.
4. Abre con **Shift derecho** o con el botón en el menú de pausa.

## Instalación PojavLauncher (Android)
1. Crea un perfil Minecraft Java 1.20.1 con **Fabric**; selecciona Java 17.
2. Coloca Fabric API y LexyClient en el directorio `mods` de ese perfil de Pojav.
3. En Minecraft, pausa y toca **LexyClient Menu**; no necesitas teclas físicas.
4. Usa **UP / DOWN** para navegar, **X** para cerrar y **MOVE HUD** para cambiar los indicadores.
5. El editor tiene **DONE** y presets. Las posiciones se guardan proporcionalmente.

*Nota:* El `.jar` es un mod de Minecraft Java, no un APK Android. Que compile en Fabric no garantiza compatibilidad con cada móvil, versión de Pojav o renderer.

## HUD
- **Minimal:** FPS y Ping.
- **Normal:** FPS, Ping, Salud, Tótems y aviso de poca salud solo cuando corresponda.
- **Extended:** añade más métricas seleccionadas.
- Solo se muestran métricas habilitadas y con datos útiles. Puedes arrastrarlas y ajustar tamaño.

## Spotify
1. Crea una aplicación en https://developer.spotify.com/dashboard.
2. Registra exactamente `http://127.0.0.1:43871/callback` en sus redirect URIs.
3. Copia el **Client ID público** (no el Client Secret) a la pestaña Spotify.
4. Pulsa **LOGIN WITH SPOTIFY** y autoriza en tu navegador.
5. En Android, si el navegador no se abre, pulsa **COPY URL** y abre el enlace en un navegador del **mismo dispositivo**; el navegador debe poder acceder a `127.0.0.1:43871` localmente.
6. **CANCEL LOGIN** descarta una autorización pendiente; **DISCONNECT** cierra la sesión local.

Spotify sigue usando su API para control de reproducción: no se transmiten canciones dentro del juego. La reproducción/control depende de la cuenta y disponibilidad de dispositivos y permisos de Spotify.