package com.lexy.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** A themed entry point and visible LexyClient identity on the actual Minecraft title screen. */
public final class TitleBranding {
    private TitleBranding() {}
    public static void register() {
        ScreenEvents.AFTER_INIT.register((mc,screen,w,h)->{
            if (screen instanceof TitleScreen) {
                int bw=Math.min(162,Math.max(100,w/3));
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("§bLexyClient  §7Settings"),button->mc.setScreen(new LexyScreen(false,0,screen))).dimensions(8,Math.max(10,h-34),bw,22).build());
                ScreenEvents.afterRender(screen).register((s,ctx,mouseX,mouseY,delta)->draw(ctx,w,h));
            } else if (screen instanceof GameMenuScreen) {
                int bw=Math.min(152,Math.max(95,w/3));
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("§bLexyClient  §7Menu"),button->mc.setScreen(new LexyScreen(false,0,screen))).dimensions(Math.max(6,w-bw-8),Math.max(5,h-32),bw,22).build());
            }
        });
    }
    private static void draw(DrawContext ctx,int w,int h) {
        int panelWidth=Math.min(252,Math.max(90,w-16));
        ctx.fill(8,7,8+panelWidth,68,0xD90D1727);
        ctx.fill(8,7,11,68,LexyClient.accent());
        ctx.fill(12,69,8+panelWidth,70,0xFF3B82F6);
        ctx.drawTexture(LexyClient.LOGO,17,13,50,50,0,0,256,256,256,256);
        var font=LexyClient.mc.textRenderer;
        ctx.drawTextWithShadow(font,Text.literal("LexyClient"),74,22,0xFF80C6FF);
        if(panelWidth>200)ctx.drawTextWithShadow(font,Text.literal("FABRIC 1.20.1  |  CLIENT 2.3"),74,36,0xFFC9D6E7);
        if(panelWidth>200)ctx.drawTextWithShadow(font,Text.literal("RSHIFT: menu  |  MENU: touch"),74,50,0xFF90A7C3);
    }
}