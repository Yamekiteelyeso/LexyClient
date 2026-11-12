package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Original, quiet LexyClient UI sounds. Registered during client initialization. */
public final class LexySounds {
    private LexySounds() {}
    private static SoundEvent tap, toggle, open, close;
    private static long lastPlayed;
    private static SoundEvent register(String name) {
        Identifier id = new Identifier("lexy-client", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
    public static void init() {
        tap=register("tap"); toggle=register("toggle"); open=register("open"); close=register("close");
    }
    private static void play(SoundEvent event) {
        if(!LexyClient.uiSounds || event==null || LexyClient.uiSoundVolume<=0) return;
        long now=System.nanoTime();
        if(now-lastPlayed<65_000_000L) return;
        lastPlayed=now;
        try {
            MinecraftClient mc=MinecraftClient.getInstance();
            if(mc!=null)mc.getSoundManager().play(PositionedSoundInstance.master(event,1.0f,LexyClient.uiSoundVolume/100.0f));
        }catch(RuntimeException ignored){}
    }
    public static void tap(){play(tap);}
    public static void toggle(){play(toggle);}
    public static void open(){play(open);}
    public static void close(){play(close);}
}