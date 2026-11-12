package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Automated vanilla inventory actions. Runs on the Minecraft client tick thread. */
public final class AutoTotem {
    private AutoTotem() {}

    public enum Mode { DIRECT, INVENTORY }
    public static Mode mode = Mode.DIRECT;
    public static boolean onlyWhenEmpty = true;
    public static boolean lowHealthOnly = false;
    public static boolean randomSource = true;
    public static int healthThreshold = 12;
    public static int minDelayMs = 160;
    public static int maxDelayMs = 300;
    public static int inventoryStepMs = 180;
    public static int cooldownMs = 650;
    public static boolean notify = false;

    private static int equipped = 0;
    private static int lastSource = -1;
    private static int pendingSource = -1;
    private static int verifyingSource = -1;
    private static long verifyAt = 0L;
    private static long nextActionAt = 0L;
    private static TotemInventoryScreen running;
    private static String state = "Ready";

    public static int swaps() {return equipped;}
    public static String status() {return LexyClient.enabled.getOrDefault("AutoTotem", false)
        ? (running != null ? "Inventory sequence" : state) + " | " + equipped + " swaps" : "Disabled";}

    private static long now() {return System.nanoTime() / 1_000_000L;}
    private static int randomDelay(){
        int low=Math.max(50,Math.min(1500,minDelayMs));
        int high=Math.max(low,Math.min(2500,maxDelayMs));
        return low==high ? low : ThreadLocalRandom.current().nextInt(low,high+1);
    }
    private static int chooseSource(PlayerInventory inv){
        List<Integer> list=new ArrayList<>(12);
        for(int i=0;i<36;i++)if(inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING))list.add(i<9?i+36:i);
        if(list.isEmpty())return -1;
        if(!randomSource)return list.get(0);
        if(list.size()>1)list.remove(Integer.valueOf(lastSource));
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
    private static boolean shouldEquip(MinecraftClient mc){
        if(!LexyClient.enabled.getOrDefault("AutoTotem",false) || mc==null || mc.player==null || mc.interactionManager==null)return false;
        if(mc.player.isDead() || mc.player.isSpectator() || mc.player.isCreative() || mc.player.currentScreenHandler!=mc.player.playerScreenHandler)return false;
        ItemStack offhand=mc.player.getOffHandStack();
        if(offhand.isOf(Items.TOTEM_OF_UNDYING))return false;
        if(onlyWhenEmpty&&!offhand.isEmpty())return false;
        return !lowHealthOnly || mc.player.getHealth()<=healthThreshold;
    }
    public static void tick(MinecraftClient mc){
        if(mc==null || mc.player==null){resetRuntime();return;}
        if(verifyingSource>=0){
            if(now()>=verifyAt){
                int source=verifyingSource;
                verifyingSource=-1;
                boolean success=mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
                if(success){completed(mc,source,true);}
                else{state="Server rejected / no swap";nextActionAt=now()+Math.max(650,cooldownMs);}
            }
            return;
        }
        if(running!=null){
            if(mc.currentScreen!=running){
                running=null;
                state="Inventory interrupted";
                pendingSource=-1;
                nextActionAt=now()+Math.max(500,cooldownMs);
            }
            return;
        }
        if(!shouldEquip(mc) || mc.currentScreen!=null){pendingSource=-1;return;}
        long time=now();
        if(time<nextActionAt)return;
        if(pendingSource<0){
            pendingSource=chooseSource(mc.player.getInventory());
            if(pendingSource<0){state="No totems";nextActionAt=time+800;return;}
            nextActionAt=time+randomDelay();
            state="Scheduled";
            return;
        }
        if(!mc.player.playerScreenHandler.getSlot(pendingSource).getStack().isOf(Items.TOTEM_OF_UNDYING)){
            pendingSource=-1;nextActionAt=time+100;return;
        }
        int source=pendingSource;pendingSource=-1;
        if(mode==Mode.INVENTORY){
            // Open vanilla player inventory, then execute checked PICKUP clicks with visible in-GUI guidance.
            running=new TotemInventoryScreen(mc.player,source);
            state="Opening inventory";
            mc.setScreen(running);
            return;
        }
        if(!mc.player.playerScreenHandler.getCursorStack().isEmpty()){
            state="Cursor occupied";nextActionAt=time+Math.max(500,cooldownMs);return;
        }
        try{
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,source,40,SlotActionType.SWAP,mc.player);
            verifyingSource=source;
            verifyAt=time+Math.max(150,randomDelay());
            state="Checking offhand";
            // Delay confirmation until a later tick; a server may reject the swap.
        }catch(RuntimeException ex){
            state="Swap failed";
            nextActionAt=time+Math.max(600,cooldownMs);
        }
    }
    static void completed(MinecraftClient mc,int source,boolean success){
        running=null;
        lastSource=source;
        nextActionAt=now()+Math.max(200,cooldownMs);
        if(success){equipped++;state="Equipped";
            if(notify&&mc!=null&&mc.player!=null)mc.player.sendMessage(Text.literal("§bLexyClient §7| §fTotem equipped"),true);
        }else state="Could not equip";
    }
    static int stepDelay(){return Math.max(100,Math.min(1100,inventoryStepMs));}
    static boolean mayContinue(MinecraftClient mc){return shouldEquip(mc);}
    public static void resetRuntime(){
        running=null;pendingSource=-1;verifyingSource=-1;verifyAt=0;nextActionAt=0;lastSource=-1;state="Ready";
    }
    public static void onDisabled(){
        pendingSource=-1;verifyingSource=-1;nextActionAt=0;
        state="Disabled";
        // The active inventory screen finishes/restores a cursor stack before closing.
    }
}