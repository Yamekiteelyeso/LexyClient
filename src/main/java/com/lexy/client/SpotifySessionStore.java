package com.lexy.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;

/** Private, client-ID-bound refresh-token cache. Tokens are never written to logs or the main settings file. */
public final class SpotifySessionStore {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("lexy-spotify-session.json");
    private SpotifySessionStore() {}

    public static synchronized String load(String clientId) {
        if (clientId == null || clientId.isBlank() || !Files.isRegularFile(FILE)) return "";
        try {
            if (Files.size(FILE) > 16_384) return "";
            JsonObject data = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!data.has("clientId") || !clientId.equals(data.get("clientId").getAsString())) return "";
            String token = data.has("refreshToken") ? data.get("refreshToken").getAsString() : "";
            return token.length() > 8 && token.length() <= 8192 ? token : "";
        } catch (Exception ignored) { return ""; }
    }

    public static synchronized void save(String clientId, String refreshToken) {
        if (clientId == null || clientId.isBlank() || refreshToken == null || refreshToken.isBlank()) return;
        Path temp = null;
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject data = new JsonObject();
            data.addProperty("version", 1);
            data.addProperty("clientId", clientId);
            data.addProperty("refreshToken", refreshToken);
            data.addProperty("savedAt", System.currentTimeMillis());
            temp = Files.createTempFile(FILE.getParent(), "lexy-spotify-", ".tmp");
            restrict(temp);
            Files.writeString(temp, data.toString(), StandardCharsets.UTF_8);
            try { Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING); }
            restrict(FILE);
        } catch (IOException ignored) {
            // Persisting credentials is best effort; never leak tokens into error messages.
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) {}
        }
    }

    private static void restrict(Path file) {
        try { Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------")); }
        catch (UnsupportedOperationException | IOException | SecurityException ignored) {
            // Filesystems on Android/Windows can have a different permissions model.
        }
    }

    public static synchronized void delete() {
        try { Files.deleteIfExists(FILE); } catch (IOException ignored) {}
    }
}