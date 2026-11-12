# LexyClient 1.6 — Guía rápida

Versión para **Minecraft 1.20.1 / Fabric / Java 17**.

**Teclas:** SHIFT DERECHO abre las opciones, SHIFT IZQUIERDO las cierra. CTRL DERECHO abre directamente el editor HUD.

**Interfaz adaptable:** la GUI se reduce proporcionalmente con la resolución de pantalla. En Apariencia cambia **MENU SIZE** y **HUD SIZE** por separado.

**Mover FPS, ping y demás:** entra al juego, pulsa SHIFT DERECHO y después **MOVE HUD**, o CTRL DERECHO. Verás los módulos de HUD sobre el juego real. Arrástralos con el ratón. Usa los botones FPS y PING para mostrarlos u ocultarlos. Botones +/- para tamaño; RESET para restaurar. SHIFT IZQUIERDO o ESC guarda y cierra. El tamaño y las posiciones se mantienen al reiniciar y se adaptan a cambios de resolución.

**Spotify:** en su pestaña está el botón azul **LOGIN WITH SPOTIFY**. Necesitas introducir una vez tu Client ID público de una app creada en https://developer.spotify.com/dashboard. Registra la URL de retorno exacta `http://127.0.0.1:43871/callback`. El botón abre un enlace oficial de autorización Spotify en tu navegador para iniciar sesión sin introducir la contraseña en Minecraft. Necesitas Spotify Desktop abierto en este ordenador para escuchar por el mismo PC. No se descarga ni se reproduce directamente el catálogo de Spotify dentro del motor de sonido de Minecraft. El reproductor de WAV/AIFF local sí reproduce sonido dentro de Minecraft.

**Instalar:** coloca solo `lexy-client-1.6.0.jar` en `.minecraft/mods`, instala Fabric API 1.20.1, y elimina versiones anteriores de LexyClient. No abras el .jar directamente.

**Compilación:** JDK 17 y Gradle 8.11.1: `gradle build`. El workflow GitHub Actions compila y publica el .jar como artifact. Verifica dentro del juego el funcionamiento final.