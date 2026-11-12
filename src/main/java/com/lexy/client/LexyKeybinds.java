package com.lexy.client;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.nio.file.*;
import java.util.*;

/** Real rebindable Minecraft key bindings and four single-fire chat/command macros. */
public final class LexyKeybinds {
    private LexyKeybinds() {}
    public static KeyBinding totemToggle, triggerToggle, triggerHold, zoom, aimToggle, aimHold, panic, clickerToggle, clickerHold;
    private static final KeyBinding[] macroKeys = new KeyBinding[4];
    private static final String[] macroTexts = new String[4];
    private static final long[] lastMacro = new long[4];
    private static final List<KeyBinding> ALL = new ArrayList<>();
    private static final String CAT = "key.categories.lexyclient";
    private static final Path MACRO_FILE=FabricLoader.getInstance().getConfigDir().resolve("lexy-macros.json");

    private static KeyBinding add(String name, int key) {
        KeyBinding binding=KeyBindingHelper.registerKeyBinding(new KeyBinding(name,InputUtil.Type.KEYSYM,key,CAT));
        ALL.add(binding);
        return binding;
    }
    public static void init() {
        totemToggle=add("key.lexyclient.totem",GLFW.GLFW_KEY_UNKNOWN);
        triggerToggle=add("key.lexyclient.trigger",GLFW.GLFW_KEY_UNKNOWN);
        triggerHold=add("key.lexyclient.hold_trigger",GLFW.GLFW_KEY_UNKNOWN);
        zoom=add("key.lexyclient.zoom",GLFW.GLFW_KEY_C);
        aimToggle=add("key.lexyclient.aim_toggle",GLFW.GLFW_KEY_UNKNOWN);
        aimHold=add("key.lexyclient.aim_hold",GLFW.GLFW_KEY_UNKNOWN);
        panic=add("key.lexyclient.panic",GLFW.GLFW_KEY_UNKNOWN);
        clickerToggle=add("key.lexyclient.autoclicker",GLFW.GLFW_KEY_UNKNOWN);
        clickerHold=add("key.lexyclient.hold_autoclicker",GLFW.GLFW_KEY_UNKNOWN);
        for(int i=0;i<4;i++) {
            macroTexts[i]="";
            macroKeys[i]=add("key.lexyclient.macro"+(i+1),GLFW.GLFW_KEY_UNKNOWN);
        }
        load();
    }
    public static List<KeyBinding> all() {return Collections.unmodifiableList(ALL);}
    public static String label(int i) {
        return switch(i) {case 0->"AutoTotem";case 1->"TriggerBot";case 2->"Hold TriggerBot";case 3->"Zoom";
            case 4->"Aim Assist Toggle";case 5->"Aim Assist Hold";case 6->"Panic Disable";case 7->"AutoClicker Toggle";case 8->"AutoClicker Hold";default->"Macro "+(i-8);};
    }
    public static KeyBinding at(int index) {return ALL.get(index);}
    public static String keyName(int index) {return at(index).getBoundKeyLocalizedText().getString();}
    public static void setKey(int index,int keyCode,int scanCode) {
        if(index<0||index>=ALL.size())return;
        KeyBinding b=ALL.get(index);
        b.setBoundKey(keyCode<0?InputUtil.UNKNOWN_KEY:InputUtil.fromKeyCode(keyCode,scanCode));
        KeyBinding.updateKeysByCode();
        MinecraftClient mc=MinecraftClient.getInstance();
        if(mc!=null)mc.options.write();
    }
    public static String macro(int i) {return i>=0&&i<4?macroTexts[i]:"";}
    public static void setMacro(int i,String line) {if(i>=0&&i<4)macroTexts[i]=line==null?"":line.replace('\n',' ').replace('\r',' ').trim();}
    public static void tick(MinecraftClient mc) {
        if(mc==null)return;
        while(totemToggle.wasPressed()) {
            boolean enabled=!LexyClient.enabled.getOrDefault("AutoTotem",false);
            LexyClient.enabled.put("AutoTotem",enabled);
            if(!enabled)AutoTotem.onDisabled();
            LexyClient.save();
            if(mc.player!=null)mc.player.sendMessage(Text.literal("§9LexyClient §7· AutoTotem "+(enabled?"§aON":"§cOFF")),true);
        }
        while(triggerToggle.wasPressed()) {
            boolean enabled=!LexyClient.enabled.getOrDefault("TriggerBot",false);
            LexyClient.enabled.put("TriggerBot",enabled);
            TriggerBot.reset();LexyClient.save();
            if(mc.player!=null)mc.player.sendMessage(Text.literal("§9LexyClient §7· TriggerBot "+(enabled?"§aON":"§cOFF")),true);
        }
        while(aimToggle.wasPressed()) {
            boolean active=!LexyClient.enabled.getOrDefault("AimAssist",false);
            LexyClient.enabled.put("AimAssist",active);
            if(!active)AimAssist.reset();
            LexyClient.save();
            if(mc.player!=null)mc.player.sendMessage(Text.literal("§9LexyClient §7· Aim Assist "+(active?"§aON":"§cOFF")),true);
        }
        while(clickerToggle.wasPressed()) {
            boolean active=!LexyClient.enabled.getOrDefault("AutoClicker",false);
            LexyClient.enabled.put("AutoClicker",active);
            AutoClicker.reset();LexyClient.save();
            if(mc.player!=null)mc.player.sendMessage(Text.literal("§9LexyClient §7· AutoClicker "+(active?"§aON":"§cOFF")),true);
        }
        while(panic.wasPressed()) {
            LexyClient.enabled.put("AimAssist",false);
            LexyClient.enabled.put("TriggerBot",false);
            LexyClient.enabled.put("AutoTotem",false);
            LexyClient.enabled.put("AutoClicker",false);
            AimAssist.reset();TriggerBot.reset();AutoTotem.onDisabled();AutoClicker.reset();
            LexyClient.save();
            if(mc.player!=null)mc.player.sendMessage(Text.literal("§9LexyClient §c· All combat modules disabled"),true);
        }
        for(int i=0;i<4;i++) {
            while(macroKeys[i].wasPressed()) {
                if(mc.player==null||mc.getNetworkHandler()==null||mc.currentScreen!=null)continue;
                long now=System.nanoTime()/1_000_000L;
                if(now-lastMacro[i]<650)continue;
                String line=macroTexts[i];
                if(line==null||line.isBlank())continue;
                lastMacro[i]=now;
                if(line.startsWith("/")&&line.length()>1)mc.getNetworkHandler().sendChatCommand(line.substring(1));
                else if(!line.startsWith("/"))mc.getNetworkHandler().sendChatMessage(line);
            }
        }
        ZoomController.tick(mc,zoom);
    }
    public static void save() {
        try {Files.createDirectories(MACRO_FILE.getParent());
            JsonObject root=new JsonObject();JsonArray values=new JsonArray();
            for(String m:macroTexts)values.add(m);root.add("macros",values);
            Files.writeString(MACRO_FILE,new GsonBuilder().setPrettyPrinting().create().toJson(root));
        }catch(Exception ex){System.err.println("Lexy macro saving failed: "+ex.getMessage());}
    }
    private static void load() {
        if(!Files.exists(MACRO_FILE))return;
        try {JsonObject root=JsonParser.parseString(Files.readString(MACRO_FILE)).getAsJsonObject();
            JsonArray a=root.getAsJsonArray("macros");if(a==null)return;
            for(int i=0;i<Math.min(4,a.size());i++)setMacro(i,a.get(i).getAsString());
        }catch(Exception ex){System.err.println("Lexy macro loading failed: "+ex.getMessage());}
    }
}