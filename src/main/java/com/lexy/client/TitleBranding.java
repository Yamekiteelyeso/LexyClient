package com.lexy.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Safe title replacement with a vanilla fallback; never prevents the game booting. */
public final class TitleBranding {
    private TitleBranding() {}
    private static boolean skipOnce;
    public static void openVanilla(MinecraftClient mc){skipOnce=true;mc.setScreen(new TitleScreen());}
    public static void register() {
        ScreenEvents.AFTER_INIT.register((mc,screen,w,h)->{
            if(screen instanceof TitleScreen && LexyTheme.customTitle && !skipOnce){
                // Schedule outside the initialization callback to avoid recursive screen initialization.
                mc.execute(()->{if(mc.currentScreen==screen)mc.setScreen(new LexyHomeScreen(screen));});
                return;
            }
            if(screen instanceof TitleScreen)skipOnce=false;
            if(screen instanceof TitleScreen || screen instanceof GameMenuScreen) {
                ScreenKeyboardEvents.afterKeyPress(screen).register((s,key,scan,mods)->{
                    if(key==GLFW.GLFW_KEY_RIGHT_SHIFT || key==GLFW.GLFW_KEY_F8){
                        if(LexyClient.allowMenuToggle())mc.execute(()->mc.setScreen(new LexyModernScreen(s)));
                    }
                });
                ScreenKeyboardEvents.afterKeyRelease(screen).register((s,key,scan,mods)->{
                    if(key==GLFW.GLFW_KEY_RIGHT_SHIFT || key==GLFW.GLFW_KEY_F8)LexyClient.menuShiftReleased();
                });
            }
            if (screen instanceof TitleScreen) {
                int bw=Math.min(170,Math.max(105,w/3));
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("§7LEXY 4.2  §7MODULES"),button->mc.setScreen(new LexyModernScreen(screen))).dimensions(8,Math.max(10,h-36),bw,22).build());
                ScreenEvents.afterRender(screen).register((s,ctx,mouseX,mouseY,delta)->draw(ctx,w,h));
                System.out.println("[LexyClient 4.2.0] Title screen fallback button initialized "+w+"x"+h);
            } else if (screen instanceof GameMenuScreen) {
                int bw=Math.min(160,Math.max(95,w/3));
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("§7LEXY 4.2  §7MODULES"),button->mc.setScreen(new LexyModernScreen(screen))).dimensions(Math.max(6,w-bw-8),Math.max(5,h-32),bw,22).build());
            }
        });
    }
    private static void draw(DrawContext ctx,int w,int h) {
        int panelWidth=Math.min(246,Math.max(90,w-16));
        ctx.fill(8,7,8+panelWidth,66,0xE9161616);
        ctx.fill(8,7,11,66,LexyTheme.palette().accent());
        ctx.drawTexture(LexyClient.LOGO,17,13,45,45,0,0,256,256,256,256);
        var font=LexyClient.mc.textRenderer;
        ctx.drawTextWithShadow(font,Text.literal("LexyClient 4.2"),70,21,0xFFBBBBBB);
        if(panelWidth>200)ctx.drawTextWithShadow(font,"RSHIFT / F8  |  /lexy",70,39,0xFFD4D4D4);
    }
}