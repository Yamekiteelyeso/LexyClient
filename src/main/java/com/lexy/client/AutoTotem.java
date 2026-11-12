package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** A client-tick state machine for vanilla offhand inventory transfers. */
public final class AutoTotem {
    private AutoTotem() {}
    public enum Mode { DIRECT, INVENTORY }
    public static Mode mode = Mode.DIRECT;
    public static boolean onlyWhenEmpty = true;
    public static boolean lowHealthOnly = false;
    public static boolean randomSource = true;
    public static int healthThreshold = 12;
    public static int minDelayMs = 200;
    public static int maxDelayMs = 400;
    public static int inventoryStepMs = 230;
    public static int cooldownMs = 750;
    public static boolean notify = false;

    private static int equipped, lastSource = -1, scheduledSource = -1, verifySource = -1;
    private static int failures, confirmedTicks;
    private static long nextAction, verificationStart, verificationDeadline;
    private static TotemInventoryScreen running;
    private static UUID playerId;
    private static String state = "Ready";

    private static long now() { return System.nanoTime() / 1_000_000L; }
    public static int swaps() { return equipped; }
    public static String status() {
        return LexyClient.enabled.getOrDefault("AutoTotem", false)
                ? (running != null ? "Inventory mode" : state) + " | " + equipped + " swaps" : "Disabled";
    }
    private static int delay() {
        int low = Math.max(50, Math.min(1500, minDelayMs));
        int high = Math.max(low, Math.min(2500, maxDelayMs));
        return low == high ? low : ThreadLocalRandom.current().nextInt(low, high + 1);
    }
    private static int source(PlayerInventory inventory) {
        List<Integer> choices = new ArrayList<>(12);
        for (int i = 0; i < 36; i++) {
            if (inventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) choices.add(i < 9 ? i + 36 : i);
        }
        if (choices.isEmpty()) return -1;
        if (!randomSource) return choices.get(0);
        if (choices.size() > 1) choices.remove(Integer.valueOf(lastSource));
        return choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
    }
    private static boolean canEquip(MinecraftClient mc) {
        if (!LexyClient.enabled.getOrDefault("AutoTotem", false) || mc == null || mc.player == null
                || mc.interactionManager == null || mc.player.isDead() || mc.player.isSpectator()
                || mc.player.isCreative() || mc.player.currentScreenHandler != mc.player.playerScreenHandler) return false;
        ItemStack hand = mc.player.getOffHandStack();
        return !hand.isOf(Items.TOTEM_OF_UNDYING) && (!onlyWhenEmpty || hand.isEmpty())
                && (!lowHealthOnly || mc.player.getHealth() <= healthThreshold);
    }
    public static void tick(MinecraftClient mc) {
        if (mc == null || mc.player == null) { resetRuntime(); return; }
        if (!mc.player.getUuid().equals(playerId)) { resetRuntime(); playerId = mc.player.getUuid(); }
        long time = now();
        if (verifySource >= 0) {
            // Client-side inventory prediction is optimistic. Wait for a server update window.
            if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                if (time >= verificationDeadline) failure("Inventory changed");
                return;
            }
            boolean offhand = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
            boolean sourceMoved = !mc.player.playerScreenHandler.getSlot(verifySource).getStack().isOf(Items.TOTEM_OF_UNDYING);
            if (offhand && sourceMoved) confirmedTicks++; else confirmedTicks = 0;
            if (time >= verificationStart && confirmedTicks >= 3) {
                int src = verifySource;
                verifySource = -1;
                successes(mc, src);
            } else if (time >= verificationDeadline) failure("Server did not confirm swap");
            return;
        }
        if (running != null) {
            if (mc.currentScreen != running) {
                running = null;
                failure("Inventory interrupted");
            }
            return;
        }
        if (!canEquip(mc) || mc.currentScreen != null) {
            scheduledSource = -1;
            return;
        }
        if (time < nextAction) return;
        if (scheduledSource < 0) {
            scheduledSource = source(mc.player.getInventory());
            if (scheduledSource < 0) { state = "No totems available"; nextAction = time + 1200; return; }
            state = "Ready to equip";
            nextAction = time + delay();
            return;
        }
        int src = scheduledSource;
        scheduledSource = -1;
        if (src < 0 || src >= mc.player.playerScreenHandler.slots.size()
                || !mc.player.playerScreenHandler.getSlot(src).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
            nextAction = time + 180;
            return;
        }
        if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
            failure("Move the item from your cursor first");
            return;
        }
        try {
            if (mode == Mode.INVENTORY) {
                running = new TotemInventoryScreen(mc.player, src);
                state = "Opening inventory";
                mc.setScreen(running);
                return;
            }
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, src, 40,
                    SlotActionType.SWAP, mc.player);
            beginVerify(src);
        } catch (RuntimeException ex) { failure("Inventory operation failed"); }
    }
    private static void beginVerify(int src) {
        verifySource = src;
        confirmedTicks = 0;
        long time = now();
        verificationStart = time + Math.max(450, stepDelay() + 200);
        verificationDeadline = time + Math.max(2200, stepDelay() * 4L + 700);
        state = "Checking offhand";
    }
    /** The screen has finished its three inventory clicks; confirm it separately after closing. */
    static void inventoryFinished(MinecraftClient mc, int src, boolean success) {
        running = null;
        if (success) beginVerify(src);
        else failure("Could not complete inventory transfer");
    }
    private static void successes(MinecraftClient mc, int src) {
        lastSource = src;
        failures = 0;
        nextAction = now() + Math.max(300, cooldownMs);
        equipped++;
        state = "Equipped";
        if (notify && mc.player != null) mc.player.sendMessage(Text.literal("§bLexyClient §7| §fTotem equipped"), true);
        LexySounds.success();
    }
    private static void failure(String message) {
        scheduledSource = -1;
        verifySource = -1;
        confirmedTicks = 0;
        failures = Math.min(failures + 1, 5);
        nextAction = now() + Math.max(500, cooldownMs) + Math.min(9000, 600L * (1 << failures));
        state = message + " (retry later)";
    }
    static int stepDelay() {
        // Steps cannot be faster than the server's tick and need a little ACK margin.
        return Math.max(150, Math.min(1400, inventoryStepMs));
    }
    static int stepDelay(MinecraftClient mc) {
        int configured = stepDelay();
        if (mc == null || mc.player == null || mc.getNetworkHandler() == null) return configured;
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? configured : Math.max(configured, Math.min(900, 90 + entry.getLatency() / 2));
    }
    static boolean mayContinue(MinecraftClient mc) { return canEquip(mc); }
    public static void resetRuntime() {
        running = null;
        scheduledSource = -1;
        verifySource = -1;
        confirmedTicks = 0;
        nextAction = 0;
        failures = 0;
        lastSource = -1;
        playerId = null;
        state = "Ready";
    }
    public static void onDisabled() {
        scheduledSource = -1;
        verifySource = -1;
        nextAction = 0;
        state = "Disabled";
        // An active screen finishes cleaning the cursor before closing.
    }
}