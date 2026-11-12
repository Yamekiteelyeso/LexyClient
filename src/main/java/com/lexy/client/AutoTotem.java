package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Single-click SWAP operations with an idle-time guard, server-state checks and bounded backoff. */
public final class AutoTotem {
    private AutoTotem() {}
    public enum Mode { DIRECT, INVENTORY }
    public static Mode mode = Mode.DIRECT;
    public static boolean onlyWhenEmpty = true;
    public static boolean lowHealthOnly = false;
    public static boolean randomSource = true;
    public static boolean combatFallbackDirect = true;
    public static int safeIdleMs = 850;
    public static int healthThreshold = 12;
    public static int minDelayMs = 180;
    public static int maxDelayMs = 320;
    public static int inventoryStepMs = 200;
    public static int cooldownMs = 800;
    public static boolean notify = false;
    public static boolean emergencyDirect = true, priorityOnEmpty = true;
    public static int emergencyHealth = 8;

    private static int equipped, lastSource = -1, scheduledSource = -1, verifySource = -1, failures, confirmedTicks;
    private static long nextAction, verificationStart, verificationDeadline, lastInputAt;
    private static TotemInventoryScreen running;
    private static UUID playerId;
    private static String state = "Ready";

    private static long now() { return System.nanoTime() / 1_000_000L; }
    public static int swaps() { return equipped; }
    public static boolean busy(MinecraftClient mc) {
        return mc == null || mc.player == null || mc.options.attackKey.isPressed() || mc.options.useKey.isPressed()
                || mc.player.isUsingItem() || mc.player.handSwinging;
    }
    public static String status() {
        return LexyClient.enabled.getOrDefault("AutoTotem", false)
                ? (running != null ? "Inventory transfer" : state) + " · " + equipped + " equipped" : "Disabled";
    }
    private static int delay() {
        int low = Math.max(50, Math.min(1500, minDelayMs));
        int high = Math.max(low, Math.min(2500, maxDelayMs));
        return low == high ? low : ThreadLocalRandom.current().nextInt(low, high + 1);
    }
    private static int source(PlayerInventory inventory) {
        List<Integer> choices = new ArrayList<>(8);
        for (int i = 0; i < 36; i++)
            if (inventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) choices.add(i < 9 ? i + 36 : i);
        if (choices.isEmpty()) return -1;
        if (!randomSource) return choices.get(0);
        if (choices.size() > 1) choices.remove(Integer.valueOf(lastSource));
        return choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
    }
    private static boolean canEquip(MinecraftClient mc) {
        if (!LexyClient.enabled.getOrDefault("AutoTotem", false) || mc == null || mc.player == null
                || mc.interactionManager == null || !mc.player.isAlive() || mc.player.isSpectator()
                || mc.player.isCreative() || mc.player.currentScreenHandler != mc.player.playerScreenHandler) return false;
        ItemStack hand = mc.player.getOffHandStack();
        return !hand.isOf(Items.TOTEM_OF_UNDYING) && (!onlyWhenEmpty || hand.isEmpty())
                && (!lowHealthOnly || mc.player.getHealth() <= healthThreshold);
    }
    public static void tick(MinecraftClient mc) {
        if (mc == null || mc.player == null) { resetRuntime(); return; }
        if (!mc.player.getUuid().equals(playerId)) { resetRuntime(); playerId = mc.player.getUuid(); }
        long time = now();
        if (busy(mc)) lastInputAt = time;
        if (verifySource >= 0) {
            if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                if (time >= verificationDeadline) failure("Inventory changed");
                return;
            }
            boolean offhand = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
            boolean sourceMoved = !mc.player.playerScreenHandler.getSlot(verifySource).getStack().isOf(Items.TOTEM_OF_UNDYING);
            confirmedTicks = (offhand && sourceMoved) ? confirmedTicks + 1 : 0;
            if (time >= verificationStart && confirmedTicks >= 4) success(mc, verifySource);
            else if (time >= verificationDeadline) failure("Swap not confirmed");
            return;
        }
        if (running != null) {
            if (mc.currentScreen != running) { running = null; failure("Inventory closed"); }
            return;
        }
        if (!canEquip(mc) || mc.currentScreen != null) { scheduledSource = -1; return; }
        if (time < nextAction) return;
        if (scheduledSource < 0) {
            scheduledSource = source(mc.player.getInventory());
            if (scheduledSource < 0) { state = "No totems"; nextAction = time + 1200; return; }
            nextAction = time + (priorityOnEmpty && mc.player.getOffHandStack().isEmpty() ? 50 : delay());
            return;
        }
        int src = scheduledSource;
        if (src >= mc.player.playerScreenHandler.slots.size() || !mc.player.playerScreenHandler.getSlot(src).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
            scheduledSource = -1;
            nextAction = time + 180;
            return;
        }
        if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
            scheduledSource = -1;
            nextAction = time + 700;
            state = "Cursor occupied";
            return;
        }
        boolean active = time - lastInputAt < Math.max(250, safeIdleMs);
        boolean emergency = emergencyDirect && mc.player.getHealth()<=Math.max(2,emergencyHealth);
        if (mode == Mode.INVENTORY && active && !combatFallbackDirect && !emergency) {
            // Keep the player's normal mouse/gameplay control. Try again when idle.
            state = "Waiting for idle mouse";
            return;
        }
        scheduledSource = -1;
        try {
            if (mode == Mode.INVENTORY && !active && !emergency) {
                running = new TotemInventoryScreen(mc.player, src);
                state = "Opening inventory";
                mc.setScreen(running);
            } else {
                // Normal vanilla offhand SWAP. This is deliberately non-GUI during combat.
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, src, 40, SlotActionType.SWAP, mc.player);
                beginVerify(src);
            }
        } catch (RuntimeException ex) { failure("Swap failed"); }
    }
    private static void beginVerify(int src) {
        verifySource = src;
        confirmedTicks = 0;
        long time = now();
        verificationStart = time + Math.max(300, stepDelay() + 100);
        verificationDeadline = time + Math.max(1800, stepDelay() * 5L + 700);
        state = "Checking swap";
    }
    static void inventoryFinished(MinecraftClient mc, int src, boolean completed) {
        running = null;
        if (completed) beginVerify(src); else failure("Inventory transfer cancelled");
    }
    private static void success(MinecraftClient mc, int src) {
        verifySource = -1;
        lastSource = src;
        failures = 0;
        nextAction = now() + Math.max(350, cooldownMs);
        equipped++;
        state = "Equipped";
        if (notify && mc.player != null) mc.player.sendMessage(Text.literal("§9LexyClient §7• §fTotem equipped"), true);
        LexySounds.success();
    }
    private static void failure(String message) {
        scheduledSource = -1;
        verifySource = -1;
        confirmedTicks = 0;
        failures = Math.min(failures + 1, 5);
        nextAction = now() + Math.max(500, cooldownMs) + Math.min(9000, 500L * (1 << failures));
        state = message;
    }
    static int stepDelay() { return Math.max(120, Math.min(1400, inventoryStepMs)); }
    static int stepDelay(MinecraftClient mc) {
        int delay = stepDelay();
        if (mc == null || mc.player == null || mc.getNetworkHandler() == null) return delay;
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? delay : Math.max(delay, Math.min(900, 100 + entry.getLatency()));
    }
    public static void resetRuntime() {
        running = null; scheduledSource = -1; verifySource = -1; confirmedTicks = 0;
        nextAction = 0; failures = 0; lastSource = -1; playerId = null; lastInputAt = now(); state = "Ready";
    }
    public static void onDisabled() {
        scheduledSource = -1; verifySource = -1; nextAction = 0; state = "Disabled";
        if (running != null) running.requestStop();
    }
}