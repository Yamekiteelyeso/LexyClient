package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

/** Inventory-based offhand swap. No packet spoofing, reach or combat modifications. */
public final class AutoTotem {
    private AutoTotem() {}
    public static boolean onlyWhenEmpty = true;
    public static boolean lowHealthOnly = false;
    public static int healthThreshold = 12;
    public static int intervalTicks = 8;
    public static boolean notify = true;
    private static int nextAction = 0;
    private static int equipped = 0;
    public static int swaps() { return equipped; }
    public static String status() { return LexyClient.enabled.getOrDefault("AutoTotem", false) ? "ACTIVE | swaps: " + equipped : "DISABLED"; }
    public static void tick(MinecraftClient mc) {
        if (nextAction > 0) { nextAction--; return; }
        if (!LexyClient.enabled.getOrDefault("AutoTotem", false) || mc == null || mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (mc.player.isSpectator() || mc.player.isDead() || mc.player.currentScreenHandler != mc.player.playerScreenHandler) return;
        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.isOf(Items.TOTEM_OF_UNDYING)) return;
        if (onlyWhenEmpty && !offhand.isEmpty()) return;
        if (lowHealthOnly && mc.player.getHealth() > healthThreshold) return;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                // The normal player inventory maps hotbar [0,8] to handler [36,44].
                // SWAP with button 40 uses the standard offhand slot operation.
                int handlerSlot = i < 9 ? i + 36 : i;
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, handlerSlot, 40, SlotActionType.SWAP, mc.player);
                equipped++;
                nextAction = Math.max(2, intervalTicks);
                if (notify) mc.player.sendMessage(Text.literal("§9LexyClient §7| §bTotem equipped to offhand"), true);
                return;
            }
        }
        nextAction = 10;
    }
}