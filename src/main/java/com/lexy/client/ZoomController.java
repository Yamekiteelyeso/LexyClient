package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

/** FOV zoom that restores the user's original setting when released; no permanent options writes. */
public final class ZoomController {
    private ZoomController() {}
    public static int zoomPercent=35;
    public static boolean toggleMode=false;
    private static boolean toggled=false,active=false;
    private static int originalFov=-1,appliedFov=-1;

    public static void tick(MinecraftClient mc,KeyBinding key) {
        if(mc==null||key==null)return;
        if(toggleMode)while(key.wasPressed())toggled=!toggled;
        boolean want=(toggleMode?toggled:key.isPressed())&&mc.player!=null&&mc.currentScreen==null;
        if(want) {
            int current=mc.options.getFov().getValue();
            if(!active) {active=true;originalFov=current;appliedFov=-1;}
            int target=Math.max(10,Math.min(70,originalFov*Math.max(15,Math.min(70,zoomPercent))/100));
            if(current!=target)mc.options.getFov().setValue(target);
            appliedFov=target;
        } else if(active) {reset(mc);}
    }
    public static void reset(MinecraftClient mc) {
        if(active&&mc!=null&&originalFov>0&&mc.options.getFov().getValue()==appliedFov)
            mc.options.getFov().setValue(originalFov);
        active=false;originalFov=-1;appliedFov=-1;toggled=false;
    }
    public static boolean isActive(){return active;}
}