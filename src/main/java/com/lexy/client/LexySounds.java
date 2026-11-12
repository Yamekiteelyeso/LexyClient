package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Discreet original interface sound pack, all sounds support a master volume control. */
public final class LexySounds {
    private LexySounds() {}
    private static final Map<String, SoundEvent> EVENTS = new HashMap<>();
    private static final Map<String, Long> LAST = new HashMap<>();
    private static final String[] KEYS = {
            "tap", "toggle", "open", "close", "hover", "navigate", "confirm", "deny", "success", "notify", "save"
    };
    public static void init() {
        for(String name:KEYS){
            Identifier id=new Identifier("lexy-client",name);
            EVENTS.put(name, Registry.register(Registries.SOUND_EVENT,id,SoundEvent.of(id)));
        }
    }
    public static void play(String name){
        if(!LexyClient.uiSounds || LexyClient.uiSoundVolume<=0)return;
        SoundEvent event=EVENTS.get(name);
        if(event==null)return;
        long now=System.nanoTime();
        long minGap="hover".equals(name)?175_000_000L:85_000_000L;
        if(now-LAST.getOrDefault(name,0L)<minGap)return;
        LAST.put(name,now);
        try{
            MinecraftClient mc=MinecraftClient.getInstance();
            if(mc!=null)mc.getSoundManager().play(PositionedSoundInstance.master(event,1.0f,LexyClient.uiSoundVolume/100.0f));
        }catch(RuntimeException ignored){}
    }
    public static void tap(){play("tap");}
    public static void toggle(){play("toggle");}
    public static void open(){play("open");}
    public static void close(){play("close");}
    public static void hover(){play("hover");}
    public static void navigate(){play("navigate");}
    public static void confirm(){play("confirm");}
    public static void deny(){play("deny");}
    public static void success(){play("success");}
    public static void notifySound(){play("notify");}
    public static void save(){play("save");}
}