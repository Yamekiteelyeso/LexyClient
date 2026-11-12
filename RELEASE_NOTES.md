# LexyClient 2.1.0 — Inventario y estabilidad

## AutoTotem
- Dos modos de intercambio: DIRECT (SWAP vanilla) e INVENTORY (abre el inventario y utiliza PICKUP desde una ranura de totem hacia offhand).
- El modo INVENTORY muestra el recorrido de un cursor virtual y los pasos de transferencia. No modifica el cursor físico del dispositivo.
- Retrasos configurables en milisegundos: mínimo, máximo, intervalo por paso y espera tras el intercambio. La ejecución se comprueba en cada tick del cliente (precisión aproximada de 50ms a 20 TPS).
- Selección aleatoria entre ranuras con tótems y evita repetir la ranura anterior cuando hay alternativas.
- Control de inventario: nunca cambia elementos mientras hay otra pantalla abierta; aborta ante estado inesperado e intenta devolver el cursor a la ranura de origen.
- Confirmación de offhand antes de incrementar el contador. Si una operación falla, aplica cooldown y no entra en un bucle rápido.

## Interfaz, datos y rendimiento
- Las pestañas Performance y Optimization muestran ajustes de vídeo reales; los datos informativos del resto de pestañas no aparentan ser switches funcionales.
- El panel Cheats → CONFIGURE incluye todos los parámetros de AutoTotem y controles UP/DOWN táctiles.
- Conservar configuración, logo, botones, HUD y Spotify de la versión anterior.
- Reducida la frecuencia de la búsqueda de objetivos muertos de cada tick a cada diez ticks.

## Limitaciones
- El inventario visual emplea un cursor virtual y clics normales del inventario, no mueve el cursor físico del sistema operativo.
- Ninguna temporización garantiza que un servidor autorice el intercambio o que cumpla las reglas del servidor. No se ha probado el mod ejecutándose en PojavLauncher en un teléfono real.