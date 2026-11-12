package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import java.util.*;
import java.util.function.*;

/** Module cards are backed by real game or existing mod operations; no dead switches. */
public final class LexyModuleCatalog {
    private LexyModuleCatalog() {}
    public record Entry(String name,String category,String detail,String glyph,BooleanSupplier enabled,Runnable toggle,int legacyTab) {}
    public record Setting(String name,String help,Supplier<String> value,IntConsumer adjust) {}
    private static final List<Entry> ALL=new ArrayList<>();
    private static void add(String name,String category,String detail,String glyph,BooleanSupplier get,Runnable action,int legacy){
        ALL.add(new Entry(name,category,detail,glyph,get,action,legacy));
    }
    private static void combat(String id,String detail,String glyph){
        add(id,"Combat",detail,glyph,()->LexyClient.enabled.getOrDefault(id,false),()->{
            boolean newState=!LexyClient.enabled.getOrDefault(id,false);LexyClient.enabled.put(id,newState);
            if(id.equals("AutoTotem")&&!newState)AutoTotem.onDisabled();
            if(id.equals("TriggerBot"))TriggerBot.reset();
            if(id.equals("AimAssist")&&!newState)AimAssist.reset();
            if(id.equals("AutoClicker"))AutoClicker.reset();
            LexyClient.save();
        },10);
    }
    private static void hud(String id,String desc){
        add(id,"HUD",desc,"▣",()->LexyClient.enabled.getOrDefault(id,false),()->{
            LexyClient.enabled.put(id,!LexyClient.enabled.getOrDefault(id,false));
            HudRenderer.invalidate();LexyClient.save();
        },3);
    }
    public static List<Entry> entries(){
        if(!ALL.isEmpty())return Collections.unmodifiableList(ALL);
        combat("AutoTotem","Verified offhand swap with combat-safe modes","✦");
        combat("TriggerBot","Hits valid targets under your crosshair","✚");
        combat("AimAssist","Smooth local-camera target assistance","◎");
        combat("AutoClicker","Custom left/right click intervals and filters","◉");
        add("AutoSprint","Movement","Sprint while moving forward","➤",()->LexyTools.autoSprint,LexyTools::toggleSprint,-1);
        add("AutoWalk","Movement","Hold forward movement automatically","➤",()->LexyExtras.autoWalk,()->{LexyExtras.autoWalk=!LexyExtras.autoWalk;LexyTheme.save();},-1);
        add("ToggleSneak","Movement","Hold sneak until toggled off","⇩",()->LexyExtras.toggleSneak,()->{LexyExtras.toggleSneak=!LexyExtras.toggleSneak;LexyTheme.save();},-1);
        add("AutoJump","Movement","Use Minecraft's native auto-jump setting","↑",()->MinecraftClient.getInstance().options.getAutoJump().getValue(),()->{
            var opts=MinecraftClient.getInstance().options;opts.getAutoJump().setValue(!opts.getAutoJump().getValue());opts.write();},-1);
        add("Zoom","Movement","Adjust FOV with a configurable key","⌕",()->ZoomController.toggleMode,()->{ZoomController.toggleMode=!ZoomController.toggleMode;LexyClient.save();},11);
        add("AutoTool","Player","Selects the fastest hotbar tool when mining","⚒",()->LexyExtras.autoTool,()->{LexyExtras.autoTool=!LexyExtras.autoTool;LexyTheme.save();},-1);
        add("AutoRespawn","Player","Respawn after death without repeated clicks","↻",()->LexyExtras.autoRespawn,()->{LexyExtras.autoRespawn=!LexyExtras.autoRespawn;LexyTheme.save();},-1);
        add("Durability Alert","Player","Warn before equipped tool breaks","◇",()->LexyExtras.durabilityAlert,()->{LexyExtras.durabilityAlert=!LexyExtras.durabilityAlert;LexyTheme.save();},-1);
        add("Low Health Alert","Player","Warn using the action bar at low HP","♡",()->LexyExtras.lowHealthActionbar,()->{LexyExtras.lowHealthActionbar=!LexyExtras.lowHealthActionbar;LexyTheme.save();},-1);
        add("Waypoint HUD","Player","Show your saved coordinates in game","⌖",()->LexyExtras.waypointsHud,()->{LexyExtras.waypointsHud=!LexyExtras.waypointsHud;LexyTheme.save();},-1);
        add("Fullbright","Render","Increase gamma safely and restore it","☼",()->LexyTools.fullbright,LexyTools::toggleBright,-1);
        add("Client Watermark","Render","Show LexyClient identity during play","L",()->LexyTools.watermark,LexyTools::toggleHud,-1);
        add("Custom Main Menu","Render","Use the Lexy night-sky menu at startup","☾",()->LexyTheme.customTitle,()->{LexyTheme.customTitle=!LexyTheme.customTitle;LexyTheme.save();},-1);
        add("UI Animations","Render","Enable lightweight hover and entrance transitions","◈",()->LexyTheme.animate,()->{LexyTheme.animate=!LexyTheme.animate;LexyTheme.save();},-1);
        for(String m:new String[]{"FPS","Ping","Health","Totems","Speed","Memory","Biome","Server","Mainhand Durability","XYZ X","XYZ Y","XYZ Z","Food","Free Slots"})hud(m,"Draw a real-time "+m+" indicator in the HUD");
        for(int i=0;i<OptimizationManager.OPTIONS.size();i++){
            final int idx=i;var option=OptimizationManager.OPTIONS.get(i);
            add(option.name(),"Optimization",option.help(),"▥",()->false,()->OptimizationManager.adjust(idx,1),-1);
        }
        add("Adaptive FPS","Optimization","Adjust view distance gradually to target FPS","▥",()->OptimizationManager.adaptiveEnabled,()->{OptimizationManager.adaptiveEnabled=!OptimizationManager.adaptiveEnabled;LexyClient.save();},5);
        add("Low Graphics","Optimization","Apply the MAX FPS video options preset","⚡",()->false,()->OptimizationManager.apply("MAX FPS"),5);
        add("Balanced Graphics","Optimization","Apply the BALANCED options preset","◐",()->false,()->OptimizationManager.apply("BALANCED"),5);
        add("Quality Graphics","Optimization","Apply the QUALITY options preset","✧",()->false,()->OptimizationManager.apply("QUALITY"),5);
        add("Android Preset","Optimization","Apply a low-overhead graphics profile","▰",()->false,()->OptimizationManager.apply("POJAV"),5);
        add("Restore Graphics","Optimization","Undo the last preset without resetting Minecraft settings","↺",()->false,()->OptimizationManager.restorePreviousPreset(),5);
        add("Copy Coordinates","Utilities","Copy current XYZ coordinates to clipboard","⌁",()->false,()->LexyTools.copyCoords(MinecraftClient.getInstance()),-1);
        add("Save Waypoint","Utilities","Remember current XYZ in your profile","⌖",()->LexyExtras.hasWaypoint,()->LexyExtras.markWaypoint(MinecraftClient.getInstance()),-1);
        add("HUD Editor","Utilities","Drag HUD widgets and change layouts","▣",()->false,()->{
            MinecraftClient mc=MinecraftClient.getInstance();mc.setScreen(new HudEditorScreen(mc.currentScreen));},-1);
        add("Keys & Macros","Utilities","Bind shortcuts and edit four macros","⌨",()->false,()->{
            MinecraftClient mc=MinecraftClient.getInstance();mc.setScreen(new KeybindSettingsScreen(mc.currentScreen));},-1);
        add("Reset UI Scale","Utilities","Restore the interface layout safely","↺",()->false,LexyTools::resetUI,-1);
        add("Local Music","Music","MP3/WAV/AIFF from your own files","♫",()->LocalMusicPlayer.INSTANCE.playing(),()->{
            MinecraftClient mc=MinecraftClient.getInstance();mc.setScreen(new LexyScreen(false,8,mc.currentScreen));},8);
        add("Spotify Integration","Music","Official app links or optional API controls","♫",()->SpotifyController.INSTANCE.isConnected(),()->{
            MinecraftClient mc=MinecraftClient.getInstance();mc.setScreen(new LexyScreen(false,7,mc.currentScreen));},7);
        add("HUD Opacity","Cosmetics","Change readability of HUD backgrounds","◨",()->false,()->{LexyClient.hudOpacity=clamp(LexyClient.hudOpacity+15,60,245);LexyClient.save();},-1);
        add("UI Sounds","Cosmetics","Enable or disable Lexy interface sounds","♫",()->LexyClient.uiSounds,()->{LexyClient.uiSounds=!LexyClient.uiSounds;LexyClient.save();},-1);
        add("Theme Picker","Cosmetics","Choose from seven coordinated themes","◇",()->false,LexyTheme::next,-1);
        add("Custom HUD Layout","Cosmetics","Open the drag-and-drop editor","▧",()->false,()->{
            MinecraftClient mc=MinecraftClient.getInstance();mc.setScreen(new HudEditorScreen(mc.currentScreen));},-1);
        return Collections.unmodifiableList(ALL);
    }
    private static void option(List<Setting> list,String name,String desc,Supplier<String> val,IntConsumer action){list.add(new Setting(name,desc,val,action));}
    public static List<Setting> settings(String module){
        List<Setting> s=new ArrayList<>();
        for(int i=0;i<OptimizationManager.OPTIONS.size();i++){
            final int idx=i;var item=OptimizationManager.OPTIONS.get(i);
            if(item.name().equals(module)){
                option(s,item.name(),item.help(),item.value(),d->OptimizationManager.adjust(idx,d));
                return s;
            }
        }
        switch(module){
            case "AutoTotem" -> {
                option(s,"Swap mode","DIRECT uses vanilla slot SWAP; INVENTORY requires idle",()->AutoTotem.mode.name(),d->{AutoTotem.mode=AutoTotem.mode==AutoTotem.Mode.DIRECT?AutoTotem.Mode.INVENTORY:AutoTotem.Mode.DIRECT;});
                option(s,"Only empty offhand","Do not overwrite the current offhand item",()->yes(AutoTotem.onlyWhenEmpty),d->AutoTotem.onlyWhenEmpty=!AutoTotem.onlyWhenEmpty);
                option(s,"Emergency HP","Health threshold for direct emergency swap",()->AutoTotem.emergencyHealth+" HP",d->AutoTotem.emergencyHealth=clamp(AutoTotem.emergencyHealth+d,2,20));
                option(s,"Minimum delay","Milliseconds before a swap",()->AutoTotem.minDelayMs+"ms",d->AutoTotem.minDelayMs=clamp(AutoTotem.minDelayMs+d*25,50,1500));
                option(s,"Combat fallback","Never interrupt the mouse during fighting",()->yes(AutoTotem.combatFallbackDirect),d->AutoTotem.combatFallbackDirect=!AutoTotem.combatFallbackDirect);
            }
            case "TriggerBot" -> {
                option(s,"Clicks / sec","Bounded by normal Minecraft tick rate",()->TriggerBot.clicksPerSecond+" CPS",d->TriggerBot.clicksPerSecond=clamp(TriggerBot.clicksPerSecond+d,1,12));
                option(s,"Cooldown threshold","Minimum charged-hit percent",()->TriggerBot.cooldownPercent+"%",d->TriggerBot.cooldownPercent=clamp(TriggerBot.cooldownPercent+d*5,40,100));
                option(s,"Stable aim ticks","Number of ticks an entity must remain targeted",()->TriggerBot.targetStableTicks+" ticks",d->TriggerBot.targetStableTicks=clamp(TriggerBot.targetStableTicks+d,1,12));
                option(s,"Ignore team","Avoid friendly targets",()->yes(TriggerBot.ignoreTeam),d->TriggerBot.ignoreTeam=!TriggerBot.ignoreTeam);
                option(s,"Hold to trigger","Require assigned hold key",()->yes(TriggerBot.requireHold),d->TriggerBot.requireHold=!TriggerBot.requireHold);
            }
            case "AutoClicker" -> {
                option(s,"Minimum CPS","Lower bound for click timing",()->AutoClicker.minCps+" CPS",d->AutoClicker.minCps=clamp(AutoClicker.minCps+d,1,AutoClicker.maxCps));
                option(s,"Maximum CPS","Upper bound for click timing",()->AutoClicker.maxCps+" CPS",d->AutoClicker.maxCps=clamp(AutoClicker.maxCps+d,AutoClicker.minCps,18));
                option(s,"Mouse buttons","Left, right or both",()->AutoClicker.buttons.name(),d->{AutoClicker.Buttons[] a=AutoClicker.Buttons.values();AutoClicker.buttons=a[(AutoClicker.buttons.ordinal()+a.length+d%a.length)%a.length];});
                option(s,"Activation","Click / key hold / toggle",()->AutoClicker.activation.name(),d->{AutoClicker.Activation[] a=AutoClicker.Activation.values();AutoClicker.activation=a[(AutoClicker.activation.ordinal()+a.length+d%a.length)%a.length];});
                option(s,"Pause for AutoTotem","Avoid slot operations during a totem swap",()->yes(AutoClicker.pauseDuringTotem),d->AutoClicker.pauseDuringTotem=!AutoClicker.pauseDuringTotem);
            }
            case "AimAssist" -> {
                option(s,"Smoothing","Higher values turn more gradually",()->AimAssist.smoothness+"%",d->AimAssist.smoothness=clamp(AimAssist.smoothness+d*5,5,95));
                option(s,"FOV cone","Horizontal detection angle",()->AimAssist.fovDegrees+"°",d->AimAssist.fovDegrees=clamp(AimAssist.fovDegrees+d*5,10,150));
                option(s,"Selection range","Target detection only; does not extend reach",()->AimAssist.rangeTenths/10f+" blocks",d->AimAssist.rangeTenths=clamp(AimAssist.rangeTenths+d*2,25,60));
                option(s,"Require hold key","Enable assistance only while held",()->yes(AimAssist.requireHold),d->AimAssist.requireHold=!AimAssist.requireHold);
                option(s,"Ignore invisible","Skip invisible entities",()->yes(AimAssist.ignoreInvisible),d->AimAssist.ignoreInvisible=!AimAssist.ignoreInvisible);
            }
            case "Zoom" -> {
                option(s,"Zoom strength","Percent of original FOV",()->ZoomController.zoomPercent+"%",d->ZoomController.zoomPercent=clamp(ZoomController.zoomPercent+d*5,15,70));
                option(s,"Toggle zoom","Press once instead of holding",()->yes(ZoomController.toggleMode),d->ZoomController.toggleMode=!ZoomController.toggleMode);
            }
            case "Durability Alert" -> option(s,"Warning threshold","Percentage of durability remaining",()->LexyExtras.warnDurability+"%",d->LexyExtras.warnDurability=clamp(LexyExtras.warnDurability+d,2,50));
            case "HUD Opacity" -> option(s,"Opacity","Adjust on-screen HUD backgrounds",()->LexyClient.hudOpacity+"/255",d->LexyClient.hudOpacity=clamp(LexyClient.hudOpacity+d*10,60,245));
            case "Adaptive FPS" -> {
                option(s,"Target FPS","Desired frame rate",()->OptimizationManager.targetFps+" FPS",d->OptimizationManager.targetFps=clamp(OptimizationManager.targetFps+d*10,30,144));
                option(s,"Min render chunks","Adaptive lower bound",()->OptimizationManager.minimumDistance+" chunks",d->OptimizationManager.minimumDistance=clamp(OptimizationManager.minimumDistance+d,2,OptimizationManager.maximumDistance));
                option(s,"Max render chunks","Adaptive upper bound",()->OptimizationManager.maximumDistance+" chunks",d->OptimizationManager.maximumDistance=clamp(OptimizationManager.maximumDistance+d,OptimizationManager.minimumDistance,20));
            }
            case "Custom Main Menu" -> option(s,"Custom title","Changes apply the next time you open the title screen",()->yes(LexyTheme.customTitle),d->LexyTheme.customTitle=!LexyTheme.customTitle);
            default -> {}
        }
        return s;
    }
    /** One specific pictogram per common module; otherwise the category glyph. */
    public static int iconIndex(Entry e){
        return switch(e.name()){
            case "AutoTotem" -> 12;
            case "TriggerBot", "AimAssist" -> 14;
            case "AutoClicker" -> 13;
            case "Zoom" -> 16;
            case "AutoSprint", "AutoWalk", "ToggleSneak", "AutoJump" -> 2;
            case "AutoTool" -> 7;
            case "AutoRespawn" -> 30;
            case "Low Health Alert" -> 25;
            case "Durability Alert" -> 12;
            case "Waypoint HUD", "Save Waypoint" -> 20;
            case "Copy Coordinates" -> 29;
            case "Custom Main Menu" -> 23;
            case "FPS Limit", "Adaptive FPS", "Adaptive FPS Target", "Low Graphics", "Balanced Graphics", "Quality Graphics" -> 6;
            case "Fullbright", "Brightness" -> 25;
            case "HUD Editor", "Custom HUD Layout" -> 5;
            case "Keys & Macros" -> 21;
            case "Reset UI Scale", "Restore Graphics" -> 30;
            case "Local Music", "Spotify Integration", "UI Sounds" -> 8;
            case "Theme Picker" -> 9;
            case "HUD Opacity" -> 4;
            default -> switch(e.category()){
                case "Dashboard" -> 0;
                case "Combat" -> 1;
                case "Movement" -> 2;
                case "Player" -> 3;
                case "Render" -> 4;
                case "HUD" -> 5;
                case "Optimization" -> 6;
                case "Utilities" -> 7;
                case "Music" -> 8;
                case "Cosmetics" -> 9;
                case "Profiles" -> 10;
                default -> 11;
            };
        };
    }
    public static boolean isAction(Entry e){
        String n=e.name();
        return e.category().equals("Optimization")&&!n.equals("Adaptive FPS")
            || n.equals("Copy Coordinates")||n.equals("Save Waypoint")||n.equals("Reset UI Scale")
            || n.equals("HUD Editor")||n.equals("Keys & Macros")||n.equals("Local Music")
            || n.equals("Spotify Integration")||n.equals("Theme Picker")||n.equals("Custom HUD Layout")
            || n.equals("HUD Opacity");
    }
    public static void changed(){LexyClient.save();LexyTheme.save();HudRenderer.invalidate();}
    private static int clamp(int x,int low,int high){return Math.max(low,Math.min(high,x));}
    private static String yes(boolean b){return b?"ON":"OFF";}
}