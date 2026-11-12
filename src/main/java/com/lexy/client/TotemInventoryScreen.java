package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

/** One atomic offhand swap with a visual preview; never moves the OS mouse or holds an item on the cursor. */
public final class TotemInventoryScreen extends InventoryScreen {
    private static final int OFFHAND = 45;
    private final int sourceSlot;
    private long openedAt = System.nanoTime() / 1_000_000L;
    private long swapAt;
    private boolean swapped, done, stop;

    public TotemInventoryScreen(PlayerEntity player, int sourceSlot) {
        super(player);
        this.sourceSlot = sourceSlot;
    }
    public void requestStop() { stop = true; }
    private static long now() { return System.nanoTime() / 1_000_000L; }
    private boolean safe() {
        return client != null && client.player != null && client.currentScreen == this
                && client.player.currentScreenHandler == client.player.playerScreenHandler
                && client.interactionManager != null && handler.slots.size() > OFFHAND
                && sourceSlot >= 0 && sourceSlot < handler.slots.size()
                && handler.getCursorStack().isEmpty();
    }
    private void finish(boolean ok) {
        if (done) return;
        done = true;
        MinecraftClient mc = client;
        if (mc != null) {
            AutoTotem.inventoryFinished(mc, sourceSlot, ok);
            if (mc.currentScreen == this) mc.setScreen(null);
        }
    }
    @Override public void handledScreenTick() {
        super.handledScreenTick();
        if (done) return;
        if (stop || !LexyClient.enabled.getOrDefault("AutoTotem", false) || !safe()) { finish(false); return; }
        long elapsed = now() - openedAt;
        if (elapsed > 3000) { finish(false); return; }
        if (!swapped) {
            if (elapsed < AutoTotem.stepDelay(client)) return;
            if (!handler.getSlot(sourceSlot).getStack().isOf(Items.TOTEM_OF_UNDYING)) { finish(false); return; }
            try {
                // One vanilla SWAP handles the offhand atomically without PICKUP/PLACE race conditions.
                client.interactionManager.clickSlot(handler.syncId, sourceSlot, 40, SlotActionType.SWAP, client.player);
                swapped = true;
                swapAt = now();
            } catch (RuntimeException ex) { finish(false); }
            return;
        }
        if (now() - swapAt >= Math.max(180, AutoTotem.stepDelay(client))) finish(true);
    }
    @Override public void render(DrawContext c, int mx, int my, float dt) {
        super.render(c, mx, my, dt);
        if (done || sourceSlot < 0 || sourceSlot >= handler.slots.size() || handler.slots.size() <= OFFHAND) return;
        Slot from = handler.getSlot(sourceSlot), to = handler.getSlot(OFFHAND);
        int x1 = x + from.x + 8, y1 = y + from.y + 8;
        int x2 = x + to.x + 8, y2 = y + to.y + 8;
        float progress = Math.max(0, Math.min(1, (now()-openedAt)/(float)Math.max(1,AutoTotem.stepDelay(client))));
        int mxp = Math.round(x1 + (x2-x1) * progress), myp = Math.round(y1 + (y2-y1) * progress);
        c.drawBorder(x+from.x-2,y+from.y-2,20,20,0xFF60A5FA);
        c.drawBorder(x+to.x-2,y+to.y-2,20,20,0xFF3BC3D6);
        c.fill(mxp-3,myp-3,mxp+4,myp+4,0xFF162032);
        c.drawBorder(mxp-4,myp-4,9,9,0xFF89C8FF);
        c.fill(width/2-115,5,width/2+115,26,0xF0141F2D);
        c.drawCenteredTextWithShadow(textRenderer, swapped?"LEXY  •  Confirming swap":"LEXY  •  AutoTotem inventory",width/2,13,0xFFE2F0FF);
    }
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers) {
        if (keyCode==GLFW.GLFW_KEY_ESCAPE || keyCode==GLFW.GLFW_KEY_LEFT_SHIFT) { finish(false); return true; }
        finish(false);
        return true;
    }
    @Override public boolean mouseClicked(double mx,double my,int button) { finish(swapped); return true; }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) { return true; }
    @Override public boolean mouseReleased(double mx,double my,int button) { return true; }
    @Override public void close() { finish(swapped); }
    @Override public boolean shouldPause() { return false; }
}