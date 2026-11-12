package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.*;
import java.util.*;
import java.util.function.*;

/** Direct wrappers around Minecraft's actual 1.20.1 video settings. */
public final class OptimizationManager {
    private OptimizationManager() {}
    private record GraphicsSnapshot(int viewDistance,int simulationDistance,GraphicsMode graphics,
            ParticlesMode particles,CloudRenderMode clouds,boolean shadows,boolean ao,int blend,
            double entityScale,boolean vsync) {}
    private static GraphicsSnapshot beforePreset;
    public static boolean hasPreviousPreset(){return beforePreset!=null;}
    /** One-step in-memory undo. The vanilla Options screen remains the source of truth. */
    public static boolean restorePreviousPreset(){
        GraphicsSnapshot s=beforePreset;
        if(s==null)return false;
        GameOptions o=o();
        o.getViewDistance().setValue(s.viewDistance());
        o.getSimulationDistance().setValue(s.simulationDistance());
        o.getGraphicsMode().setValue(s.graphics());
        o.getParticles().setValue(s.particles());
        o.getCloudRenderMode().setValue(s.clouds());
        o.getEntityShadows().setValue(s.shadows());
        o.getAo().setValue(s.ao());
        o.getBiomeBlendRadius().setValue(s.blend());
        o.getEntityDistanceScaling().setValue(s.entityScale());
        o.getEnableVsync().setValue(s.vsync());
        o.write();beforePreset=null;
        return true;
    }
    public static boolean adaptiveEnabled = false;
    public static int targetFps = 60, minimumDistance = 4, maximumDistance = 12;
    private static int measurements, fpsSum, secondsLow, secondsHigh;
    private static long nextTune;
    public static void tick(MinecraftClient mc) {
        if(!adaptiveEnabled||mc==null||mc.player==null||mc.currentScreen!=null) return;
        // Read a one-second average; change graphics options at most once every 25 seconds.
        fpsSum+=Math.max(1,mc.getCurrentFps());
        if(++measurements<20)return;
        int avg=fpsSum/measurements;fpsSum=0;measurements=0;
        int target=Math.max(30,Math.min(144,targetFps));
        if(avg<Math.round(target*.78f)){secondsLow++;secondsHigh=0;}
        else if(avg>Math.round(target*1.25f)){secondsHigh++;secondsLow=0;}
        else{secondsLow=0;secondsHigh=0;}
        long now=System.nanoTime()/1_000_000L;
        if(now<nextTune)return;
        if(secondsLow>=8&&o().getViewDistance().getValue()>Math.min(minimumDistance,maximumDistance)) {
            o().getViewDistance().setValue(Math.max(Math.min(minimumDistance,maximumDistance),o().getViewDistance().getValue()-2));
            o().getParticles().setValue(ParticlesMode.MINIMAL);
            o().write();secondsLow=0;nextTune=now+25000;
        } else if(secondsHigh>=25&&o().getViewDistance().getValue()<Math.max(minimumDistance,maximumDistance)) {
            o().getViewDistance().setValue(Math.min(Math.max(minimumDistance,maximumDistance),o().getViewDistance().getValue()+1));
            o().write();secondsHigh=0;nextTune=now+30000;
        }
    }
    public record Entry(String name, String help, Supplier<String> value, IntConsumer adjust) {}
    public static final List<Entry> OPTIONS = List.of(
        new Entry("Adaptive FPS Target", "Desired FPS for auto graphics tuning", ()->targetFps+" FPS",
            d->targetFps=clamp(targetFps+d*10,30,144)),
        new Entry("Adaptive Minimum Chunks", "Do not reduce render distance below this", ()->minimumDistance+" chunks",
            d->minimumDistance=clamp(minimumDistance+d,2,maximumDistance)),
        new Entry("Adaptive Maximum Chunks", "Do not raise render distance above this", ()->maximumDistance+" chunks",
            d->maximumDistance=clamp(maximumDistance+d,minimumDistance,20)),
        new Entry("Simulation Distance", "Server-side simulation radius in singleplayer", ()->o().getSimulationDistance().getValue()+" chunks",
            d->o().getSimulationDistance().setValue(clamp(o().getSimulationDistance().getValue()+d*2,4,16))),
        new Entry("Render Distance", "Chunks rendered by your client", ()-> String.valueOf(o().getViewDistance().getValue())+" chunks",
            d -> o().getViewDistance().setValue(clamp(o().getViewDistance().getValue()+d*2,2,16))),
        new Entry("FPS Limit", "Frame-rate cap; higher is not always smoother", ()-> o().getMaxFps().getValue()+" FPS",
            d -> o().getMaxFps().setValue(clamp(o().getMaxFps().getValue()+d*30,30,260))),
        new Entry("Graphics", "Fast mode reduces expensive effects", ()-> o().getGraphicsMode().getValue().toString(),
            d -> o().getGraphicsMode().setValue(o().getGraphicsMode().getValue()==GraphicsMode.FAST?GraphicsMode.FANCY:GraphicsMode.FAST)),
        new Entry("Particles", "Reduces game particle density", ()-> o().getParticles().getValue().toString(),
            d -> {ParticlesMode[] a={ParticlesMode.ALL,ParticlesMode.DECREASED,ParticlesMode.MINIMAL}; int i=Arrays.asList(a).indexOf(o().getParticles().getValue());o().getParticles().setValue(a[Math.floorMod(i+d,a.length)]);}),
        new Entry("Clouds", "Turn off clouds for less rendering", ()-> o().getCloudRenderMode().getValue().toString(),
            d -> {CloudRenderMode[] a={CloudRenderMode.OFF,CloudRenderMode.FAST,CloudRenderMode.FANCY};int i=Arrays.asList(a).indexOf(o().getCloudRenderMode().getValue());o().getCloudRenderMode().setValue(a[Math.floorMod(i+d,a.length)]);}),
        new Entry("Entity Shadows", "Disable entity shadow rendering", ()-> bool(o().getEntityShadows().getValue()),
            d -> o().getEntityShadows().setValue(!o().getEntityShadows().getValue())),
        new Entry("Smooth Lighting", "Ambient occlusion (AO)", ()-> bool(o().getAo().getValue()),
            d -> o().getAo().setValue(!o().getAo().getValue())),
        new Entry("Biome Blend", "Less color blending means less work", ()-> String.valueOf(o().getBiomeBlendRadius().getValue()),
            d -> o().getBiomeBlendRadius().setValue(clamp(o().getBiomeBlendRadius().getValue()+d,0,7))),
        new Entry("Entity Distance", "Client render distance for entities", ()-> Math.round(o().getEntityDistanceScaling().getValue()*100)+"%",
            d -> o().getEntityDistanceScaling().setValue(clamp(o().getEntityDistanceScaling().getValue()+d*0.25,0.5,2.0))),
        new Entry("VSync", "Synchronize frames with monitor refresh", ()-> bool(o().getEnableVsync().getValue()),
            d -> o().getEnableVsync().setValue(!o().getEnableVsync().getValue())),
        new Entry("Mipmap Levels", "Lowering levels may reduce GPU usage", ()-> String.valueOf(o().getMipmapLevels().getValue()),
            d -> o().getMipmapLevels().setValue(clamp(o().getMipmapLevels().getValue()+d,0,4))),
        new Entry("Distortion FX", "Screen distortion intensity (visual comfort)", ()-> Math.round(o().getDistortionEffectScale().getValue()*100)+"%",
            d -> o().getDistortionEffectScale().setValue(clamp(o().getDistortionEffectScale().getValue()+d*0.25,0,1))),
        new Entry("Brightness", "Vanilla gamma adjustment", ()->Math.round(o().getGamma().getValue()*100)+"%",
            d->o().getGamma().setValue(clamp(o().getGamma().getValue()+d*0.1,0,1))),
        new Entry("GUI Scale", "Adjust Minecraft GUI scale (may require refresh)", ()->String.valueOf(o().getGuiScale().getValue()),
            d->o().getGuiScale().setValue(clamp(o().getGuiScale().getValue()+d,0,4))),
        new Entry("Chat Opacity", "Adjust chat transparency", ()->Math.round(o().getChatOpacity().getValue()*100)+"%",
            d->o().getChatOpacity().setValue(clamp(o().getChatOpacity().getValue()+d*0.1,0,1))),
        new Entry("Chat Scale", "Chat text size", ()->Math.round(o().getChatScale().getValue()*100)+"%",
            d->o().getChatScale().setValue(clamp(o().getChatScale().getValue()+d*0.1,0,1))),
        new Entry("FOV Effects", "Speed-change field-of-view intensity", ()-> Math.round(o().getFovEffectScale().getValue()*100)+"%",
            d -> o().getFovEffectScale().setValue(clamp(o().getFovEffectScale().getValue()+d*0.25,0,1)))
    );
    private static GameOptions o(){ return MinecraftClient.getInstance().options; }
    private static String bool(boolean v){return v?"ON":"OFF";}
    private static int clamp(int v,int a,int b){return Math.max(a,Math.min(b,v));}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
    public static void adjust(int index,int direction){
        if(index<0||index>=OPTIONS.size())return;
        OPTIONS.get(index).adjust().accept(direction);
        o().write();
    }
    public static void apply(String mode){
        if(!Set.of("POJAV","MAX FPS","BALANCED","QUALITY").contains(mode))return;
        GameOptions x=o();
        beforePreset=new GraphicsSnapshot(x.getViewDistance().getValue(),x.getSimulationDistance().getValue(),
                x.getGraphicsMode().getValue(),x.getParticles().getValue(),x.getCloudRenderMode().getValue(),
                x.getEntityShadows().getValue(),x.getAo().getValue(),x.getBiomeBlendRadius().getValue(),
                x.getEntityDistanceScaling().getValue(),x.getEnableVsync().getValue());
        switch(mode){
            case "POJAV" -> {
                x.getViewDistance().setValue(4);x.getSimulationDistance().setValue(4);
                x.getGraphicsMode().setValue(GraphicsMode.FAST);
                x.getParticles().setValue(ParticlesMode.MINIMAL);x.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                x.getEntityShadows().setValue(false);x.getAo().setValue(false);
                x.getBiomeBlendRadius().setValue(0);x.getEntityDistanceScaling().setValue(0.5);x.getEnableVsync().setValue(false);
            }
            case "MAX FPS" -> {
                x.getViewDistance().setValue(6); x.getGraphicsMode().setValue(GraphicsMode.FAST);
                x.getParticles().setValue(ParticlesMode.MINIMAL); x.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                x.getEntityShadows().setValue(false); x.getAo().setValue(false); x.getBiomeBlendRadius().setValue(0);
                x.getEntityDistanceScaling().setValue(0.5); x.getEnableVsync().setValue(false);
            }
            case "BALANCED" -> {
                x.getViewDistance().setValue(10); x.getGraphicsMode().setValue(GraphicsMode.FAST);
                x.getParticles().setValue(ParticlesMode.DECREASED); x.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                x.getEntityShadows().setValue(false); x.getAo().setValue(true); x.getBiomeBlendRadius().setValue(2);
                x.getEntityDistanceScaling().setValue(0.75);
            }
            case "QUALITY" -> {
                x.getViewDistance().setValue(16); x.getGraphicsMode().setValue(GraphicsMode.FANCY);
                x.getParticles().setValue(ParticlesMode.ALL); x.getCloudRenderMode().setValue(CloudRenderMode.FANCY);
                x.getEntityShadows().setValue(true); x.getAo().setValue(true); x.getBiomeBlendRadius().setValue(5);
                x.getEntityDistanceScaling().setValue(1.0);
            }
            default -> {return;}
        }
        x.write();
    }
}