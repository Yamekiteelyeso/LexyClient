package com.lexy.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** A themed entry point and visible LexyClient identity on the actual Minecraft title screen. */
public final class TitleBranding {
    private TitleBranding() {}
    public static void register() {
        ScreenEvents.AFTER_INIT.register((mc,screen,w,h)->{
            if (!(screen instanceof TitleScreen)) return;
            int x=12,y=Math.max(12,h-34);
            Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("§bLexyClient  §7Settings"),button->mc.setScreen(new LexyScreen(false,0,screen))).dimensions(x,y,150,22).build());
            ScreenEvents.afterRender(screen).register((s,ctx,mouseX,mouseY,delta)->draw(ctx,w,h));
        });
    }
    private static void draw(DrawContext ctx,int w,int h) {
        ctx.fill(8,7,238,64,0xD90D1727);
        ctx.fill(8,7,11,64,LexyClient.accent());
        ctx.fill(12,65,238,66,0xFF3B82F6);
        ctx.drawTexture(LexyClient.LOGO,17,13,48,48,0,0,256,256,256,256);
        var font=LexyClient.mc.textRenderer;
        ctx.drawTextWithShadow(font,Text.literal("LexyClient"),71,21,0xFF80C6FF);
        ctx.drawTextWithShadow(font,Text.literal("FABRIC 1.20.1  |  BLUE EDITION"),71,35,0xFFC9D6E7);
        ctx.drawTextWithShadow(font,Text.literal("RSHIFT: modules   LSHIFT: close"),71,49,0xFF90A7C3);
    }
}