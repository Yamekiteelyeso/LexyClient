package com.lexy.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Secondary access and diagnostics when a keybind is swallowed by another mod. */
public final class LexyCommands {
    private LexyCommands() {}
    public static void register(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,registryAccess)->{
            dispatcher.register(ClientCommandManager.literal("lexy")
                .executes(ctx->{open(ctx.getSource().getClient());return 1;})
                .then(ClientCommandManager.literal("open").executes(ctx->{open(ctx.getSource().getClient());return 1;}))
                .then(ClientCommandManager.literal("status").executes(ctx->{
                    var mc=ctx.getSource().getClient();
                    ctx.getSource().sendFeedback(Text.literal("§bLexyClient 4.0.0 §aACTIVE §7| Fabric 1.20.1 | "+mc.getCurrentFps()+" FPS"));
                    ctx.getSource().sendFeedback(Text.literal("§7Open: Right Shift / F8 / /lexy | Render: "+mc.getWindow().getScaledWidth()+"x"+mc.getWindow().getScaledHeight()));
                    return 1;
                }))
                .then(ClientCommandManager.literal("coords").executes(ctx->{LexyTools.copyCoords(ctx.getSource().getClient());return 1;}))
                .then(ClientCommandManager.literal("resetui").executes(ctx->{LexyTools.resetUI();ctx.getSource().sendFeedback(Text.literal("§aLexy UI scale and HUD restored"));return 1;}))
                .then(ClientCommandManager.literal("sprint").executes(ctx->{LexyTools.toggleSprint();ctx.getSource().sendFeedback(Text.literal("§bAutoSprint: "+LexyTools.autoSprint));return 1;}))
                .then(ClientCommandManager.literal("bright").executes(ctx->{LexyTools.toggleBright();ctx.getSource().sendFeedback(Text.literal("§bFullbright: "+LexyTools.fullbright));return 1;}))
                .then(ClientCommandManager.literal("watermark").executes(ctx->{LexyTools.toggleHud();ctx.getSource().sendFeedback(Text.literal("§bWatermark: "+LexyTools.watermark));return 1;}))
            );
        });
    }
    public static void open(MinecraftClient mc){
        if(mc!=null)mc.execute(()->mc.setScreen(new LexyModernScreen(null)));
    }
}