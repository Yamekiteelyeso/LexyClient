# LexyClient 2.2 — Slate Edition

## Instalación
1. Utiliza Minecraft Java 1.20.1, Fabric Loader y Fabric API, con Java 17.
2. Retira cualquier versión anterior de `lexy-client` de la carpeta `mods`.
3. Coloca el archivo `LexyClient-2.2.0.jar` en `mods` y abre el juego.
4. En PojavLauncher puedes entrar a LexyClient desde el menú de pausa; en PC funciona también Shift derecho.

## Interfaz
La interfaz usa tonos pizarra y un diseño menos recargado. La pestaña **All Modules** agrupa indicadores que realmente pueden mostrarse en el HUD; los apartados Combat/Statistics muestran datos de lectura sin simular interruptores. `Performance` y `Optimization` están reservados para ajustes del menú, no para elementos flotantes durante la partida.

## AutoTotem
Abre `Cheats > CONFIGURE`. `DIRECT` usa el intercambio normal de inventario, `INVENTORY` abre la mochila del jugador y realiza los clics con pausas y verificación del cursor. Puedes cambiar los milisegundos de espera, el cooldown y la selección aleatoria de tótem. La función viene desactivada por defecto y algunos servidores pueden rechazar estos movimientos. Si ves `Server did not confirm swap`, revisa el tipo de servidor y los retrasos: no fuerces intervalos extremadamente rápidos.

## Spotify y PojavLauncher
1. Registra una aplicación propia en https://developer.spotify.com/dashboard y copia el **Client ID público**, nunca el secreto.
2. Añade exactamente `http://127.0.0.1:43871/callback` a las redirect URIs.
3. Para aplicaciones en modo desarrollo, Spotify exige Premium al propietario y que los usuarios autorizados estén en la allowlist correspondiente. Sus restricciones son externas al mod.
4. Ve a `Spotify` en LexyClient, pega el Client ID y pulsa `CONNECT ACCOUNT`.
5. Si el móvil no retorna a Minecraft, utiliza `COPY LOGIN LINK` y termina el login en el navegador. Copia la URL final que comience por `http://127.0.0.1:43871/callback?` (no tu contraseña) y utiliza `PASTE FROM CLIPBOARD` y luego `FINISH LOGIN FROM CALLBACK`.
6. Una vez conectado, abre Spotify en el móvil, reproduce una canción y pulsa `REFRESH DEVICES`/`USE PHONE`. La música suena en Spotify, no en Minecraft.

**Importante:** el Client ID se recuerda, pero las credenciales de acceso no se guardan en texto plano; se requiere autorización nueva al reiniciar. Android puede suspender Pojav al abrir el navegador y los resultados dependen de la versión del launcher, de Spotify y del servidor.

## Sonidos
El cliente incorpora 11 efectos originales discretos. Puedes activar/desactivar todos los sonidos, los sonidos al pasar el cursor y el volumen desde `Appearance`.

## Seguridad y límites
No hay garantías de que un servidor acepte operaciones de inventario automatizadas. Las funciones deben usarse respetando las reglas del servidor. La integración Spotify usa la API oficial con PKCE y nunca requiere pegar la contraseña en LexyClient.