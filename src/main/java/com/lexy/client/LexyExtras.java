package com.lexy.client;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;

/** Small, real and reversible QoL features. No packet spoofing or server bypass. */
public final class LexyExtras {
    private LexyExtras() {}
    public static boolean autoWalk=false, autoRespawn=false, durabilityAlert=true,
            lowHealthActionbar=true, waypointsHud=true, toggleSneak=false, autoTool=false;
    public static int warnDurability=12;
    public static int waypointX,waypointY,waypointZ;
    public static String waypointName="Home";
    public static boolean hasWaypoint;
    private static int counter, nextWarning, nextDeathTick;
    private static boolean forcingForward,forcingSneak;
    public static JsonObject serialize(){
        JsonObject j=new JsonObject();j.addProperty("walk",autoWalk);j.addProperty("respawn",autoRespawn);
        j.addProperty("durability",durabilityAlert);j.addProperty("warnPercent",warnDurability);
        j.addProperty("health",lowHealthActionbar);j.addProperty("waypointHud",waypointsHud);
        j.addProperty("sneak",toggleSneak);j.addProperty("autoTool",autoTool);j.addProperty("waypoint",hasWaypoint);
        j.addProperty("x",waypointX);j.addProperty("y",waypointY);j.addProperty("z",waypointZ);
        j.addProperty("name",waypointName);return j;
    }
    public static void read(JsonObject j){
        try{
            if(j.has("walk"))autoWalk=j.get("walk").getAsBoolean();
            if(j.has("respawn"))autoRespawn=j.get("respawn").getAsBoolean();
            if(j.has("durability"))durabilityAlert=j.get("durability").getAsBoolean();
            if(j.has("warnPercent"))warnDurability=Math.max(2,Math.min(50,j.get("warnPercent").getAsInt()));
            if(j.has("health"))lowHealthActionbar=j.get("health").getAsBoolean();
            if(j.has("waypointHud"))waypointsHud=j.get("waypointHud").getAsBoolean();
            if(j.has("sneak"))toggleSneak=j.get("sneak").getAsBoolean();
            if(j.has("autoTool"))autoTool=j.get("autoTool").getAsBoolean();
            if(j.has("waypoint"))hasWaypoint=j.get("waypoint").getAsBoolean();
            if(j.has("x"))waypointX=j.get("x").getAsInt();
            if(j.has("y"))waypointY=j.get("y").getAsInt();
            if(j.has("z"))waypointZ=j.get("z").getAsInt();
            if(j.has("name"))waypointName=j.get("name").getAsString();
        }catch(Exception ex){System.err.println("[Lexy] Invalid extras config: "+ex);}
    }
    public static void markWaypoint(MinecraftClient mc){
        if(mc==null||mc.player==null)return;
        waypointX=mc.player.getBlockX();waypointY=mc.player.getBlockY();waypointZ=mc.player.getBlockZ();
        hasWaypoint=true;LexyTheme.save();mc.player.sendMessage(Text.literal("§bLEXY §7· Waypoint saved: "+waypointX+" "+waypointY+" "+waypointZ),true);
    }
    public static String waypoint(){return hasWaypoint?waypointX+", "+waypointY+", "+waypointZ:"Not set";}
    public static void tick(MinecraftClient mc){
        if(mc==null)return;
        // Do not interfere with other menus or synthetic input while a screen is open.
        boolean active=mc.player!=null&&mc.world!=null&&mc.currentScreen==null&&!mc.player.isSpectator();
        if(forcingForward && (!active||!autoWalk)){mc.options.forwardKey.setPressed(false);forcingForward=false;}
        if(forcingSneak && (!active||!toggleSneak)){mc.options.sneakKey.setPressed(false);forcingSneak=false;}
        if(!active){if(autoRespawn&&mc.currentScreen instanceof DeathScreen&&mc.player!=null&&counter++>=nextDeathTick){
            nextDeathTick=counter+25;mc.player.requestRespawn();}return;}
        if(autoWalk){mc.options.forwardKey.setPressed(true);forcingForward=true;}
        if(toggleSneak){mc.options.sneakKey.setPressed(true);forcingSneak=true;}
        counter++;
        // Only inspect the nine hotbar items while actively breaking a block.
        if(autoTool && counter%4==0 && mc.options.attackKey.isPressed()
                && mc.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult hit
                && mc.interactionManager!=null){
            var state=mc.world.getBlockState(hit.getBlockPos());
            int best=mc.player.getInventory().selectedSlot;
            float score=mc.player.getInventory().getStack(best).getMiningSpeedMultiplier(state);
            for(int i=0;i<9;i++){
                var stack=mc.player.getInventory().getStack(i);
                if(stack.isEmpty())continue;
                float candidate=stack.getMiningSpeedMultiplier(state);
                if(candidate>score+0.2f){score=candidate;best=i;}
            }
            if(best!=mc.player.getInventory().selectedSlot)mc.player.getInventory().selectedSlot=best;
        }
        if(counter%20!=0||mc.player==null)return;
        if(durabilityAlert && counter>=nextWarning){
            var stack=mc.player.getMainHandStack();
            if(stack.isDamageable() && stack.getMaxDamage()>0 && 100*(stack.getMaxDamage()-stack.getDamage())/stack.getMaxDamage()<=warnDurability){
                mc.player.sendMessage(Text.literal("§cLEXY §7· Low durability: §f"+stack.getName().getString()),true);
                nextWarning=counter+220;
            }
        }
        if(lowHealthActionbar && mc.player.getHealth()<=Math.max(2,LexyClient.lowHealthThreshold)){
            if(counter%100==0)mc.player.sendMessage(Text.literal("§cLEXY · Low health!"),true);
        }
    }
    public static void shutdown(MinecraftClient mc){
        if(mc!=null){if(forcingForward)mc.options.forwardKey.setPressed(false);if(forcingSneak)mc.options.sneakKey.setPressed(false);}
        forcingForward=false;forcingSneak=false;
    }
}