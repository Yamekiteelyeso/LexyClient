# LexyClient 2.3.0 · Spotify mejorado

Esta actualización reproduce las **funciones** útiles de SpotiCraft mediante código propio. SpotiCraft tiene licencia **All Rights Reserved**, así que no se copian su código, gráficos, bibliotecas empacadas ni Client ID.

## Inicio de sesión y uso

1. Crea tu aplicación en https://developer.spotify.com/dashboard y activa Web API.
2. Añade a las redirecciones `http://127.0.0.1:43871/callback` exactamente como está escrito (no uses `localhost`).
3. Introduce el Client ID de **tu aplicación** en LexyClient → Spotify; no pegues nunca el Client Secret.
4. Pulsa CONNECT ACCOUNT y acepta el inicio de sesión en el navegador. En PojavLauncher, si la URL de retorno no abre Minecraft, usa el botón de pegar y finalizar login con el enlace completo de retorno.
5. Reproduce una canción en tu app Spotify de PC o móvil para que aparezca como dispositivo activo. Puedes controlar reproducción, shuffle, repetir, volumen, elegir dispositivo, buscar canciones y abrir listas de reproducción desde el cliente.
6. La próxima vez que abras Minecraft **no necesitas iniciar sesión de nuevo**, si Spotify no ha revocado la autorización: LexyClient guarda únicamente el refresh token en `config/lexy-spotify-session.json` para renovarlo automáticamente.
7. Para desvincular tu cuenta y borrar el token local, usa **LOG OUT** en la pestaña Spotify.

## Seguridad y límites

- El token está guardado localmente (no cifrado); se aplican permisos de propietario cuando el sistema de archivos los admite. No compartas la carpeta de configuración de Minecraft si hay sesiones de Spotify vinculadas.
- No se imprime ningún token en registros ni se reutiliza el Client ID de SpotiCraft.
- Los límites de Spotify siguen vigentes: quien crea la app en modo desarrollo debe tener Premium y cada usuario necesita estar autorizado; para controlar reproducción también necesitas un dispositivo Spotify disponible.
- El audio de Spotify se reproduce en Spotify (teléfono, PC, web), no dentro de Minecraft.
- Entorno: Minecraft Java 1.20.1, Fabric Loader, Fabric API, Java 17. Funciones táctiles pensadas para PojavLauncher, pero todavía no probadas sobre un dispositivo real.

## Compilación

Usa Gradle 8.11.1 y Java 17: `gradle build`. Copia el `.jar` de `build/libs/` en la carpeta `mods` y quita versiones anteriores de LexyClient.