package com.lexy.client;

import net.fabricmc.loader.api.FabricLoader;

import javax.sound.sampled.*;
import java.io.IOException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Local, in-process audio player. It does not download or decode Spotify tracks. */
public final class LocalMusicPlayer {
    public static final LocalMusicPlayer INSTANCE = new LocalMusicPlayer();
    private static final int BUFFER = 8192;
    private final Path folder = FabricLoader.getInstance().getConfigDir().resolve("lexy-music");
    private final ExecutorService audio = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Lexy-Local-Audio"); t.setDaemon(true); return t;
    });
    private volatile List<Path> tracks = List.of();
    private volatile String status = "Add .wav or .aiff files to config/lexy-music";
    private volatile String current = "Nothing playing";
    private volatile boolean paused = false, loop = false, stopped = true;
    private volatile int volume = 65, selected = -1, generation = 0;
    private volatile Future<?> task;
    private volatile SourceDataLine line;
    private final Object monitor = new Object();

    private LocalMusicPlayer() { refresh(); }
    public Path folder() { return folder; }
    public List<String> tracks() { return tracks.stream().map(p -> p.getFileName().toString()).toList(); }
    public String current() { return current; }
    public String status() { return status; }
    public boolean paused() { return paused; }
    public boolean looping() { return loop; }
    public boolean playing() { return !stopped; }
    public int selected() { return selected; }
    public int volume() { return volume; }

    public synchronized void refresh() {
        try {
            Files.createDirectories(folder);
            try (var files = Files.list(folder)) {
                tracks = files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".wav") || name.endsWith(".aiff") || name.endsWith(".aif") || name.endsWith(".au");
                    }).sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))).limit(300).toList();
            }
            status = tracks.size() + " music files available";
        } catch (IOException ex) { status = "Cannot scan music folder: " + ex.getMessage(); }
    }
    public void openFolder() {
        try {
            Files.createDirectories(folder);
            Util.getOperatingSystem().open(folder.toUri());
            status = "Opening music folder...";
        } catch (Exception ex) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) mc.keyboard.setClipboard(folder.toAbsolutePath().toString());
            status = "Folder path copied: " + folder.toAbsolutePath();
        }
    }
    public synchronized void play(int index) {
        if (index < 0 || index >= tracks.size()) { status = "Select a track from the list"; return; }
        stopInternal();
        selected = index;
        Path path = tracks.get(index);
        current = path.getFileName().toString();
        stopped = false; paused = false;
        int ownGeneration = generation;
        task = audio.submit(() -> run(path, ownGeneration));
    }
    private void run(Path path, int ownGeneration) {
        do {
            try (AudioInputStream original = AudioSystem.getAudioInputStream(path.toFile())) {
                AudioFormat source = original.getFormat();
                AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    source.getSampleRate(), 16, source.getChannels(), source.getChannels() * 2,
                    source.getSampleRate(), false);
                try (AudioInputStream converted = AudioSystem.getAudioInputStream(pcm, original)) {
                    SourceDataLine output = AudioSystem.getSourceDataLine(pcm);
                    output.open(pcm, BUFFER * 2);
                    line = output;
                    output.start();
                    status = "Playing " + path.getFileName();
                    byte[] buffer = new byte[BUFFER];
                    int n;
                    while (ownGeneration == generation && !Thread.currentThread().isInterrupted() && (n = converted.read(buffer)) != -1) {
                        synchronized (monitor) {
                            while (paused && ownGeneration == generation) monitor.wait(200L);
                        }
                        if (ownGeneration != generation || Thread.currentThread().isInterrupted()) break;
                        scaleVolume(buffer, n, volume);
                        int offset = 0;
                        while (offset < n && ownGeneration == generation) {
                            int written = output.write(buffer, offset, n - offset);
                            if (written <= 0) break;
                            offset += written;
                        }
                    }
                    if (ownGeneration == generation && !Thread.currentThread().isInterrupted()) output.drain();
                    output.stop(); output.close(); line = null;
                }
            } catch (UnsupportedAudioFileException ex) {
                status = "Unsupported audio format: " + path.getFileName(); break;
            } catch (LineUnavailableException ex) {
                status = "No audio output available: " + ex.getMessage(); break;
            } catch (Exception ex) {
                if (ownGeneration == generation) status = "Audio error: " + ex.getClass().getSimpleName();
                break;
            }
        } while (loop && ownGeneration == generation && !Thread.currentThread().isInterrupted());
        if (ownGeneration == generation) { stopped = true; paused = false; current = "Nothing playing"; }
    }
    /** Software volume for signed 16-bit little-endian PCM; keeps platform mixer optional. */
    static void scaleVolume(byte[] data, int len, int amount) {
        double gain = Math.max(0, Math.min(100, amount)) / 100.0;
        for (int i = 0; i + 1 < len; i += 2) {
            short sample = (short) ((data[i] & 0xFF) | (data[i + 1] << 8));
            int scaled = (int) Math.round(sample * gain);
            data[i] = (byte) scaled;
            data[i + 1] = (byte) (scaled >> 8);
        }
    }
    public void togglePause() {
        if (stopped) { if (!tracks.isEmpty()) play(Math.max(0, selected)); return; }
        paused = !paused;
        synchronized (monitor) { monitor.notifyAll(); }
        status = paused ? "Paused" : "Resumed";
    }
    public void setVolume(int value) { volume = Math.max(0, Math.min(100, value)); }
    public void toggleLoop() { loop = !loop; }
    public synchronized void next() { if (!tracks.isEmpty()) play((Math.max(-1, selected) + 1) % tracks.size()); }
    public synchronized void previous() { if (!tracks.isEmpty()) play((Math.max(0, selected) + tracks.size() - 1) % tracks.size()); }
    private synchronized void stopInternal() {
        generation++; stopped = true; paused = false;
        synchronized (monitor) { monitor.notifyAll(); }
        Future<?> old = task;
        if (old != null) old.cancel(true);
        SourceDataLine output = line;
        if (output != null) { try { output.stop(); output.flush(); output.close(); } catch (Exception ignored) {} }
        line = null;
    }
    public synchronized void stop() { stopInternal(); current = "Nothing playing"; status = "Stopped"; }
    public void shutdown() { stop(); audio.shutdownNow(); }
}