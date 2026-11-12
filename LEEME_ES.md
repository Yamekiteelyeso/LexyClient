# LexyClient 2.0 — PC y PojavLauncher

## Instalación
- Minecraft 1.20.1 + Fabric Loader + Fabric API; Java 17.
- Coloca `lexy-client-2.0.0.jar` en `mods`, quitando otros LexyClient antiguos.
- Menú de pausa > LexyClient, o SHIFT derecho en teclado. RCTRL: editor de HUD.

## HUD y optimización
- Rendimiento y optimización se muestran **únicamente en el menú**, nunca como indicadores flotantes.
- El HUD permite solo métricas relevantes (FPS, ping, salud, comida, tótems, efectos, alertas, coordenadas...), con límite predeterminado de 7 widgets.
- Oculta alertas cuando están OK y opciones apagadas. Puedes activar/desactivar cada widget y moverlo.
- Tres perfiles: Minimal, Normal y Extended. En Appearance cambia Max HUD Widgets.
- Se incluyen cuatro sonidos originales, ajustables desde Appearance > UI Sounds / UI Sound Volume.

## Spotify en PojavLauncher / móvil
1. Desde https://developer.spotify.com/dashboard crea una aplicación Spotify.
2. Registra EXACTAMENTE la URI de retorno: `http://127.0.0.1:43871/callback` (HTTP 127.0.0.1 está permitido como loopback según Spotify).
3. Introduce el **Client ID público** en la pestaña Spotify (NO el Client Secret).
4. Pulsa LOGIN WITH SPOTIFY. Se intenta abrir el navegador Android mediante ACTION_VIEW; si no se abre, pulsa COPY LOGIN URL y pégalo en el navegador.
5. Autoriza tu cuenta en Spotify. Si se vuelve al mod, la conexión será automática.
6. Si el navegador da fallo al llegar a 127.0.0.1, **copia la URL completa** de la barra de direcciones (`http://127.0.0.1:43871/callback?code=...&state=...`), regresa al juego, pega la URL en el campo de retorno y pulsa FINISH LOGIN FROM CALLBACK URL.
7. La cuenta debe tener permisos para esa aplicación en Spotify Developer, y algunos controles de reproducción requieren Spotify Premium y una sesión de Spotify abierta en el teléfono u otro dispositivo.

El mod usa PKCE y verifica el parámetro `state`. Nunca introduces tu contraseña de Spotify en LexyClient. Los tokens se mantienen en memoria hasta cerrar Minecraft.

**Limitación importante:** el mod no reproduce canciones de Spotify en sí mismo; controla Spotify mediante su Web API. No hay garantía sin probar PojavLauncher en un teléfono real. Si el login falla, consulta el estado que aparece en la pestaña Spotify.