package com.lexy.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import org.lwjgl.glfw.GLFW;
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
            if (screen instanceof TitleScreen || screen instanceof GameMenuScreen) {
                ScreenKeyboardEvents.afterKeyPress(screen).register((s,key,scan,mods)->{
                    if(key==GLFW.GLFW_KEY_RIGHT_SHIFT || key==GLFW.GLFW_KEY_F8){
                        if(LexyClient.allowMenuToggle())mc.execute(()->mc.setScreen(new LexyScreen(false,0,s)));
                    }
                });
                ScreenKeyboardEvents.afterKeyRelease(screen).register((s,key,scan,mods)->{
                    if(key==GLFW.GLFW_KEY_RIGHT_SHIFT || key==GLFW.GLFW_KEY_F8)LexyClient.menuShiftReleased();
                });
            }
            if (screen instanceof TitleScreen) {
                int bw=Math.min(162,Math.max(100,w/3));
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("§bLEXY 3.0  §7OPEN MENU"),button->mc.setScreen(new LexyScreen(false,0,screen))).dimensions(8,Math.max(10,h-34),bw,22).build());
                ScreenEvents.afterRender(screen).register((s,ctx,mouseX,mouseY,delta)->draw(ctx,w,h));
                System.out.println("[LexyClient 3.0.0] Title button registered. GUI dimensions "+w+"x"+h);
            } else if (screen instanceof GameMenuScreen) {
                int bw=Math.min(152,Math.max(95,w/3));
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("§bLEXY 3.0  §7OPEN MENU"),button->mc.setScreen(new LexyScreen(false,0,screen))).dimensions(Math.max(6,w-bw-8),Math.max(5,h-32),bw,22).build());
            }
        });
    }
    private static void draw(DrawContext ctx,int w,int h) {
        int panelWidth=Math.min(252,Math.max(90,w-16));
        ctx.fill(8,7,8+panelWidth,68,0xEA0D1728);
        ctx.fill(8,7,11,68,LexyClient.accent());
        ctx.fill(12,69,8+panelWidth,70,0xFF3B82F6);
        ctx.drawTexture(LexyClient.LOGO,17,13,50,50,0,0,256,256,256,256);
        var font=LexyClient.mc.textRenderer;
        ctx.drawTextWithShadow(font,Text.literal("LexyClient"),74,22,0xFF80C6FF);
        if(panelWidth>200)ctx.drawTextWithShadow(font,Text.literal("FABRIC 1.20.1  |  CLIENT 3.0.0"),74,36,0xFFC9D6E7);
        if(panelWidth>200)ctx.drawTextWithShadow(font,Text.literal("RSHIFT / F8  |  /lexy in game"),74,50,0xFF90A7C3);
    }
}
