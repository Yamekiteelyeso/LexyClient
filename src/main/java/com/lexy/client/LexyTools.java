package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;

/** Client-only quality of life actions; all changes are reversible. */
public final class LexyTools {
    private LexyTools() {}
    public static boolean watermark = true;
    public static boolean autoSprint = false;
    public static boolean fullbright = false;
    private static boolean appliedBright;
    private static double previousGamma = 0.5;
    public static void tick(MinecraftClient mc) {
        if(mc==null || mc.player==null || mc.world==null)return;
        if(autoSprint && mc.currentScreen==null && mc.player.input!=null &&
           mc.player.input.movementForward>0.7f && !mc.player.isSneaking() &&
           mc.player.getHungerManager().getFoodLevel()>6 && !mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }
        if(fullbright && !appliedBright){
            previousGamma = mc.options.getGamma().getValue();
            mc.options.getGamma().setValue(1.0);appliedBright=true;
        } else if(!fullbright && appliedBright) {
            mc.options.getGamma().setValue(previousGamma);appliedBright=false;
        }
    }
    public static void shutdown(MinecraftClient mc){if(mc!=null && appliedBright){
        mc.options.getGamma().setValue(previousGamma);appliedBright=false;mc.options.write();
    }}
    public static void resetUI(){
        LexyClient.menuScalePercent=95;
        LexyClient.hudScalePercent=100;
        LexyClient.accentChoice=0;
        LexyClient.applyHudPreset(1);
        LexyClient.save();
    }
    public static void copyCoords(MinecraftClient mc){
        if(mc==null||mc.player==null)return;
        String text=mc.player.getBlockX()+" "+mc.player.getBlockY()+" "+mc.player.getBlockZ();
        mc.keyboard.setClipboard(text);
        mc.player.sendMessage(Text.literal("§bLexy §7· Copied coordinates: §f"+text),true);
    }
    public static void toggleHud(){watermark=!watermark;LexyClient.save();}
    public static void toggleSprint(){autoSprint=!autoSprint;LexyClient.save();}
    public static void toggleBright(){fullbright=!fullbright;LexyClient.save();}
    public static String fps(MinecraftClient mc){return mc==null?"-":String.valueOf(mc.getCurrentFps());}
    public static String server(MinecraftClient mc){
        if(mc==null||mc.world==null)return "Not connected";
        return mc.isInSingleplayer()?"Singleplayer":mc.getCurrentServerEntry()==null?"Multiplayer":mc.getCurrentServerEntry().address;
    }
    public static String biome(MinecraftClient mc){
        if(mc==null||mc.world==null||mc.player==null)return "-";
        try{return mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k->k.getValue().toString()).orElse("Unknown");}
        catch(Exception ex){return "-";}
    }
}
