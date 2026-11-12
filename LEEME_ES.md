# LexyClient 2.1.0 — Fabric 1.20.1

Requiere **Minecraft 1.20.1**, **Fabric Loader**, **Fabric API** y **Java 17** (en PojavLauncher usa el entorno Java compatible con 1.20.1).

1. Elimina las versiones anteriores de LexyClient de `mods`.
2. Copia `LexyClient-2.1.0.jar` a la carpeta `mods` de la instancia.
3. Abre el juego y entra a **Cheats > AutoTotem > CONFIGURE**.
4. Selecciona `MODE`: DIRECT o INVENTORY. `RANDOM TOTEM SLOT` cambia qué ranura se usa cuando hay más de un tótem disponible.
5. Ajusta `MIN DELAY`, `MAX DELAY`, `INVENTORY STEP` y `AFTER-SWAP COOLDOWN` en milisegundos. Usa flechas UP/DOWN para recorrer las opciones en Pojav.
6. Activa AutoTotem explícitamente; queda apagado por defecto.

**DIRECT** realiza el intercambio usando el control normal de offhand. **INVENTORY** abre el inventario, muestra un puntero virtual y utiliza la secuencia coger/poner/devolver. Si el intercambio no es aceptado por el servidor no se garantiza que termine correctamente; el programa evitará operar durante un cooldown.

Los intervalos en milisegundos se redondean efectivamente al siguiente tick del cliente (normalmente 50 ms). El cursor de pantalla dibujado no es un cursor físico de Android. Al cancelar en medio de una transferencia, si queda un objeto en el cursor, vuelve a colocarlo manualmente en una ranura vacía.

**Compatibilidad:** la compilación en GitHub Actions comprueba Java/Fabric, pero no sustituye una prueba dentro de Minecraft o PojavLauncher.