package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Checked vanilla inventory operation; never attempts to move the Android OS cursor. */
public final class TotemInventoryScreen extends InventoryScreen {
    private static final int OFFHAND = 45;
    private final int sourceSlot;
    private int phase;
    private long dueAt, stepStarted, settleSince;
    private boolean done, cancel;

    public TotemInventoryScreen(PlayerEntity player, int sourceSlot) {
        super(player);
        this.sourceSlot = sourceSlot;
        this.stepStarted = now();
        this.dueAt = stepStarted + AutoTotem.stepDelay();
    }
    private static long now() { return System.nanoTime() / 1_000_000L; }
    private void next() {
        phase++;
        stepStarted = now();
        dueAt = stepStarted + AutoTotem.stepDelay(client);
    }
    private boolean ready() {
        return client != null && client.player != null && client.currentScreen == this
                && client.player.currentScreenHandler == client.player.playerScreenHandler
                && client.interactionManager != null && sourceSlot >= 0
                && sourceSlot < handler.slots.size() && handler.slots.size() > OFFHAND;
    }
    private void click(int slot) {
        client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
    }
    private void returnCursor() {
        if (handler.getCursorStack().isEmpty()) return;
        if (handler.getSlot(sourceSlot).getStack().isEmpty()) {
            click(sourceSlot);
            return;
        }
        // A changed source slot should not force a dropped item or replace someone else's stack.
        for (int i = 9; i < 45; i++) {
            if (i != sourceSlot && handler.getSlot(i).getStack().isEmpty()) {
                click(i);
                return;
            }
        }
    }
    private void finish(boolean success) {
        if (done) return;
        done = true;
        boolean safe = handler.getCursorStack().isEmpty();
        if (client != null) {
            AutoTotem.inventoryFinished(client, sourceSlot, success && safe);
            if (safe && client.currentScreen == this) client.setScreen(null);
            else if (client.player != null) client.player.sendMessage(
                    Text.literal("§cLexyClient: item on cursor. Place it manually before leaving."), true);
        }
    }
    private void abort() {
        try { if (ready()) returnCursor(); } catch (RuntimeException ignored) {}
        finish(false);
    }
    @Override public void handledScreenTick() {
        super.handledScreenTick();
        if (done || !ready()) return;
        if (cancel || !LexyClient.enabled.getOrDefault("AutoTotem", false)) { abort(); return; }
        long t = now();
        if (t < dueAt) return;
        if (t - stepStarted > Math.max(2500, AutoTotem.stepDelay(client) * 6L)) {
            abort(); return;
        }
        try {
            switch (phase) {
                case 0 -> {
                    if (!AutoTotem.mayContinue(client) || !handler.getCursorStack().isEmpty()
                            || !handler.getSlot(sourceSlot).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        finish(false); return;
                    }
                    click(sourceSlot);
                    next();
                }
                case 1 -> {
                    // Wait until Minecraft has applied the pickup before the next click.
                    if (handler.getCursorStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        click(OFFHAND);
                        next();
                    }
                }
                case 2 -> {
                    if (handler.getSlot(OFFHAND).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        if (!handler.getCursorStack().isEmpty()) returnCursor();
                        next();
                        settleSince = 0;
                    }
                }
                default -> {
                    if (!handler.getCursorStack().isEmpty()) returnCursor();
                    boolean ok = handler.getSlot(OFFHAND).getStack().isOf(Items.TOTEM_OF_UNDYING)
                            && handler.getCursorStack().isEmpty();
                    if (!ok) { settleSince = 0; return; }
                    if (settleSince == 0) settleSince = t;
                    if (t - settleSince >= Math.max(250, AutoTotem.stepDelay(client))) finish(true);
                }
            }
        } catch (RuntimeException ex) { abort(); }
    }
    @Override public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        if (done || sourceSlot < 0 || sourceSlot >= handler.slots.size() || handler.slots.size() <= OFFHAND) return;
        Slot src = handler.getSlot(sourceSlot), target = handler.getSlot(OFFHAND);
        int sx = x + src.x + 8, sy = y + src.y + 8;
        int tx = x + target.x + 8, ty = y + target.y + 8;
        float progress = Math.min(1f, Math.max(0f, (now() - stepStarted) / (float) Math.max(1, AutoTotem.stepDelay(client))));
        int px = phase == 0 ? sx : phase == 1 ? Math.round(sx + (tx - sx) * progress) : tx;
        int py = phase == 0 ? sy : phase == 1 ? Math.round(sy + (ty - sy) * progress) : ty;
        c.drawBorder(x + (phase < 2 ? src.x : target.x) - 2, y + (phase < 2 ? src.y : target.y) - 2, 20, 20, 0xFF5FADFF);
        c.fill(px - 4, py - 4, px + 4, py + 4, 0xFF121B28);
        c.drawBorder(px - 4, py - 4, 9, 9, 0xFF72D6FF);
        int left = (width - Math.min(width - 18, 245)) / 2, bw = Math.min(width - 18, 245);
        c.fill(left, 5, left + bw, 27, 0xEE111924);
        c.fill(left, 5, left + 2, 27, LexyClient.accent());
        String text = phase == 0 ? "Selecting totem" : phase == 1 ? "Moving to offhand"
                : phase == 2 ? "Restoring previous item" : "Confirming server state";
        c.drawCenteredTextWithShadow(textRenderer, "LEXY  /  " + text, width / 2, 12, 0xFFE8F3FF);
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (done) return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) { cancel = true; return true; }
        return true;
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (done) return super.mouseClicked(mx, my, button);
        cancel = true;
        return true;
    }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return !done || super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        return !done || super.mouseReleased(mx, my, button);
    }
    @Override public void close() { if (!done) cancel = true; else super.close(); }
    @Override public boolean shouldPause() { return false; }
}