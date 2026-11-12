# LexyClient 2.7 — Slate Blue

## Novedades
- Aim Assist suave con selección de jugadores o mobs, FOV, distancia, suavidad, velocidad, altura de puntería, predicción y fijación de objetivo. **Desactivado por defecto**.
- AutoTotem: intercambio directo de emergencia y prioridad para la mano secundaria vacía.
- TriggerBot: comprobación de objetivo estable, intervalos variables, filtro de invisibles y golpes críticos opcionales.
- Nuevas teclas: activar Aim Assist, mantener Aim Assist y desactivar de golpe todos los módulos de combate.
- Cheats ahora tiene tres pestañas con configuración real.
- Optimización: objetivo FPS y límites de distancia adaptativa configurables; nuevos acentos azules y grises.

## Uso
1. Instala Fabric Loader y Fabric API para Minecraft 1.20.1 con Java 17.
2. Elimina todos los JAR antiguos de LexyClient de `mods`.
3. Copia `lexy-client-2.7.0.jar` a `mods`.
4. En Minecraft abre **Shift derecho → Cheats**: AutoTotem, TriggerBot y Aim Assist.
5. En **Keybinds** asigna teclas para los tres módulos, zoom y cuatro macros. También aparecen en Controles de Minecraft.
6. En **Optimization** ajusta Auto FPS, objetivo y límites de distancia. Para Pojav usa el preset POJAV.

**Spotify:** la versión gratuita abre canciones en la aplicación oficial; la reproducción MP3 de archivos propios sí sucede dentro de Minecraft. El catálogo Spotify no se puede transmitir libremente dentro del juego con la cuenta Free.

Esta versión se compila en CI, pero es necesario probar el comportamiento en cada teléfono y servidor. Algunas funciones automáticas están prohibidas en servidores multijugador.

## AutoClicker 2.7

En Cheats > CLICKER > CONFIGURE, personaliza clic izquierdo/derecho, CPS minimo y maximo, variacion temporal, modo de activacion, armas, alcance, objetivo y exclusiones, uso de objetos y bloqueos de inventario. Mantener mouse, mantener tecla o activado (TOGGLE). Las teclas se asignan desde Keybinds o Controles de Minecraft. Boton derecho sobre una fila de ajustes recorre hacia atras. Desactivado inicialmente.

No realiza clics sobre interfaces ni modifica el alcance. Los CPS se procesan por tick y pueden fluctuar con los TPS/FPS del cliente. No se garantiza compatibilidad con las reglas de cada servidor.