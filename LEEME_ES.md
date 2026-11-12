# LexyClient 1.5.0 — Blue Edition

## Cambios en esta entrega
- `Shift derecho`: abre la GUI desde Minecraft (atajo real registrado con Fabric).
- `Ctrl derecho`: editor de posiciones de los HUD.
- Interfaz actualizada con gris grafito, azul eléctrico, paneles redondeados, búsqueda, menú lateral desplazable, controles y persistencia.
- Spotify Connect: conexión segura PKCE, búsqueda, controles, lista de dispositivos y botones `USE COMPUTER` / `REFRESH DEVICES` para usar un Spotify Desktop abierto en la misma PC.
- Nueva pestaña `Local Music`: reproducción de tus archivos WAV/AIFF/AU dentro del proceso de Minecraft, sin otra aplicación. Incluye lista, reproducir, pausar, siguiente/anterior, detener, volumen, repetición y carpeta configurable.
- Continúan disponibles los 13 ajustes gráficos, los 3 perfiles de FPS y los 84 HUDs/informaciones (no son 84 módulos independientes de optimización).

## Spotify: aclaración importante
Conectar Spotify no significa poder extraer o reproducir el audio protegido de Spotify con código Java dentro de Minecraft. El Web API controla reproductores Spotify oficiales. Para oír Spotify **en el mismo equipo, sin teléfono ni otra máquina**, inicia Spotify Desktop en esa computadora y elige `USE COMPUTER` o el dispositivo correcto de la lista en la GUI. Suena en tu computadora, y puedes manejar las canciones desde el juego.

Si quieres música que realmente se reproduzca **desde el mod**, usa la pestaña `Local Music` con archivos propios.

## Cómo abrir la GUI y reproducir música
1. Instala Minecraft 1.20.1 con Fabric Loader y Fabric API compatibles.
2. Compila con JDK 17 y Gradle 8.x ejecutando `gradle build`.
3. Coloca el `.jar` de salida en `.minecraft/mods`.
4. Dentro de Minecraft pulsa **Shift derecho** y entra a `Local Music`.
5. Pulsa `OPEN FOLDER`, agrega archivos WAV, pulsa `REFRESH` y selecciona una canción.

## Compilación automática
Incluye `.github/workflows/build.yml`: al subir el proyecto a GitHub (rama `main`) la acción intenta compilar y publica el `.jar` como artefacto en la página de Actions.

## Estado de verificación
- La clase de audio local se compiló en Java 21 con destino Java 17 usando un stub de FabricLoader.
- Pruebas aisladas de exploración de archivos, escalado de audio PCM, límites del volumen y repetición: aprobadas.
- El proyecto completo **no se ha podido compilar ni ejecutar en Minecraft** en este entorno porque faltan Gradle y las dependencias de Fabric/Minecraft. No se entrega un `.jar` que se haga pasar por funcional.

Consulta `README.md` para más detalles técnicos.