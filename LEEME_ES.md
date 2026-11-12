# LexyClient 3.0.0 — instalación y diagnóstico

**Requisitos:** Minecraft Java 1.20.1, Fabric Loader, Fabric API, Java 17. No instales este JAR en Forge, NeoForge, Bedrock ni como plugin de servidor.

1. Cierra Minecraft. Elimina todas las versiones anteriores de LexyClient de `mods`.
2. Coloca **lexy-client-3.0.0.jar** en la carpeta `mods` del perfil de Minecraft que usas.
3. Abre el perfil **Fabric 1.20.1**. En el menú de título se debe mostrar **LEXY 3.0 OPEN MENU**.
4. Pulsa **Shift derecho** o **F8**. También puedes abrir desde Pausa (ESC).
5. Entra a un mundo y escribe **`/lexy`** o **`/lexy status`**. El comando es del cliente, no requiere permisos en un servidor.
6. Si un ajuste de escala oculta el menú: **`/lexy resetui`**. Puedes usar `/lexy coords`, `/lexy sprint`, `/lexy bright` y `/lexy watermark`.
7. Si Minecraft no muestra absolutamente nada, adjunta `%APPDATA%\.minecraft\logs\latest.log` y la pantalla del perfil del lanzador.

**Lo nuevo:** nueva barra de navegación con iconos reales, ocho accesos útiles, marca de carga visible, HUD con métricas extra, opciones de FPS/chat/brillo/escala, acceso de emergencia F8 y comandos de diagnóstico.

La compilación automática verifica el bytecode Java y los recursos. No sustituye una prueba manual real con Minecraft ni demuestra el origen de un fallo en tu PC.
