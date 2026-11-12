# LexyClient 4.2 — Studio UI Rework

## Instalación

1. Usa **Minecraft Java 1.20.1** con **Fabric Loader**, **Fabric API** y **Java 17**.
2. Quita versiones anteriores de LexyClient de la carpeta `mods` del perfil que ejecutas.
3. Copia **solo** `lexy-client-4.2.0.jar` en `mods` (no el archivo ZIP).
4. En Amethyst/Pojav debes elegir la carpeta `mods` del lanzador real, que puede ser distinta de la de Windows.
5. Abre el juego y verifica que en `logs/latest.log` aparezca `lexy-client 4.2.0`.

## Accesos

- **Shift derecho** o **F8**: abre/cierra el centro de módulos.
- **/lexy**: menú del cliente dentro de un mundo (sin necesidad de teclas).
- Desde el menú principal personalizado: **LEXY MODULES**, **LEXY SETTINGS** y **PROFILES**.
- **VANILLA**: vuelve a la pantalla original de Minecraft si necesitas recuperar el menú.

## Diseño y controles

La 4.2 usa fondos oscuros azul/gris, iconos originales, módulos en tarjetas, búsqueda, favoritos, temas y panel de configuración lateral. Pulsa **la tarjeta o el engranaje** para ver sus opciones; pulsa el botón **ENABLED/DISABLED** para activarla o desactivarla. En **Optimization** también encontrarás **Restore Graphics**, para deshacer el último perfil gráfico aplicado en esta sesión. En opciones de vídeo (como FPS Limit) el botón se muestra como **OPEN / APPLY**, no como un módulo permanente.

**ESC** primero cierra el panel lateral. En **Profiles** puedes guardar, cargar y borrar (doble confirmación) un perfil local.

## Estado de prueba

La compilación de Java y la carga del inicializador Fabric se verifican mediante GitHub Actions. Las pruebas de clics y escala dentro de tu instalación de Amethyst/PC siguen siendo necesarias. No se han creado cheats de relleno ni se prometen ganancias de FPS sin medir.

## Monochrome HUD / HUD monocromatico
Black and grey only. Compact FPS, Ping, HP and Totems widgets. RCtrl to drag the HUD; -/+ changes size, STYLE cycles flat/outline/text, BG toggles background, SNAP/GRID are optional. Existing widget positions are preserved on upgrade.