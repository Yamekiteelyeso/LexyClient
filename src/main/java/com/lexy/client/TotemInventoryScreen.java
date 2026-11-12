package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

/** Visible vanilla inventory transfer, with a virtual pointer that works on Pojav too. */
public final class TotemInventoryScreen extends InventoryScreen {
    private static final int OFFHAND = 45;
    private final int sourceSlot;
    private int phase = 0;
    private long dueAt;
    private long phaseStart;
    private boolean done = false;
    private boolean cancel = false;
    private boolean pickedUp = false;

    public TotemInventoryScreen(PlayerEntity player,int sourceSlot){
        super(player);
        this.sourceSlot=sourceSlot;
        this.phaseStart=now();
        this.dueAt=phaseStart+AutoTotem.stepDelay();
    }
    private static long now(){return System.nanoTime()/1_000_000L;}
    private void schedule(){phaseStart=now();dueAt=phaseStart+AutoTotem.stepDelay();}
    private boolean contextReady(){
        return client!=null && client.player!=null && client.currentScreen==this
            && client.player.currentScreenHandler==client.player.playerScreenHandler
            && client.interactionManager!=null && handler.slots.size()>OFFHAND;
    }
    private void click(int slot){
        client.interactionManager.clickSlot(handler.syncId,slot,0,SlotActionType.PICKUP,client.player);
    }
    private void finish(boolean success){
        if(done)return;
        done=true;
        if(client!=null){
            AutoTotem.completed(client,sourceSlot,success);
            // Do not close while the cursor holds items; allow the player to recover them.
            if(handler.getCursorStack().isEmpty() && client.currentScreen==this)client.setScreen(null);
            else if(client.player!=null)client.player.sendMessage(net.minecraft.text.Text.literal("§cLexyClient: item on cursor - place it into an empty slot manually."),true);
        }
    }
    private void tryReturnCursor(){
        if(handler.getCursorStack().isEmpty())return;
        if(sourceSlot>=0 && sourceSlot<handler.slots.size() && handler.getSlot(sourceSlot).getStack().isEmpty()){
            click(sourceSlot);
        }
    }
    @Override public void handledScreenTick(){
        super.handledScreenTick();
        if(done)return;
        if(!contextReady())return;
        if(cancel || !LexyClient.enabled.getOrDefault("AutoTotem",false)){
            if(pickedUp)tryReturnCursor();
            finish(false);return;
        }
        if(now()<dueAt)return;
        try{
            switch(phase){
                case 0 -> {
                    if(!AutoTotem.mayContinue(client) || !handler.getCursorStack().isEmpty() ||
                       !handler.getSlot(sourceSlot).getStack().isOf(Items.TOTEM_OF_UNDYING)){
                        finish(false);return;
                    }
                    click(sourceSlot);
                    pickedUp=true;
                    phase=1;schedule();
                }
                case 1 -> {
                    if(!handler.getCursorStack().isOf(Items.TOTEM_OF_UNDYING)){
                        tryReturnCursor();finish(false);return;
                    }
                    click(OFFHAND);
                    phase=2;schedule();
                }
                case 2 -> {
                    // PICKUP leaves the previous offhand stack on the cursor. Put it back.
                    if(!handler.getCursorStack().isEmpty()){
                        if(handler.getSlot(sourceSlot).getStack().isEmpty())click(sourceSlot);
                        else {finish(false);return;}
                    }
                    phase=3;schedule();
                }
                default -> finish(handler.getSlot(OFFHAND).getStack().isOf(Items.TOTEM_OF_UNDYING) && handler.getCursorStack().isEmpty());
            }
        }catch(RuntimeException ex){
            try{tryReturnCursor();}catch(RuntimeException ignored){}
            finish(false);
        }
    }
    @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
        super.render(c,mouseX,mouseY,delta);
        if(done)return;
        if(handler.slots.size()<=OFFHAND || sourceSlot<0 || sourceSlot>=handler.slots.size())return;
        Slot src=handler.getSlot(sourceSlot),target=handler.getSlot(OFFHAND);
        int sx=x+src.x+8,sy=y+src.y+8,tx=x+target.x+8,ty=y+target.y+8;
        int active=phase==0?sourceSlot:OFFHAND;
        Slot sl=handler.getSlot(active);
        c.drawBorder(x+sl.x-2,y+sl.y-2,20,20,0xFF64B5FF);
        float p=(float)Math.max(0,Math.min(1,(now()-phaseStart)/(double)AutoTotem.stepDelay()));
        int px=phase<=1?sx:Math.round(sx+(tx-sx)*p);
        int py=phase<=1?sy:Math.round(sy+(ty-sy)*p);
        // On Android there may be no OS cursor to move, so draw a visible virtual pointer.
        c.fill(px-4,py-4,px+4,py+4,0xFF102236);
        c.drawBorder(px-4,py-4,9,9,0xFF6DD6FF);
        int infoW=Math.min(width-16,234), left=(width-infoW)/2;
        c.fill(left,5,left+infoW,26,0xEC0D1A2A);
        c.fill(left,5,left+3,26,LexyClient.accent());
        String status=phase==0?"Locating totem...":phase==1?"Moving to offhand...":phase==2?"Restoring item...":"Confirming inventory...";
        c.drawCenteredTextWithShadow(textRenderer,"LEXY | "+status,width/2,12,0xFFE3F4FF);
    }
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        if(done)return super.keyPressed(keyCode,scanCode,modifiers);
        if(keyCode==GLFW.GLFW_KEY_ESCAPE || keyCode==GLFW.GLFW_KEY_LEFT_SHIFT){
            cancel=true;
            if(handler.getCursorStack().isEmpty() && phase==0){finish(false);return true;}
            return true;
        }
        return true; // Do not allow user actions to disturb a transfer in progress.
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(done)return super.mouseClicked(mx,my,button);
        cancel=true;
        return true;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){return !done || super.mouseDragged(mx,my,button,dx,dy);}
    @Override public boolean mouseReleased(double mx,double my,int button){return !done || super.mouseReleased(mx,my,button);}
    @Override public boolean shouldPause(){return false;}
}