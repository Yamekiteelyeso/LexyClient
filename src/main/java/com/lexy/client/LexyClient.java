package com.lexy.client;

import com.google.gson.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.util.function.*;

public final class LexyClient implements ClientModInitializer {
 public static final List<Module> MODULES = new ArrayList<>();
 public static final Map<String, Boolean> enabled = new LinkedHashMap<>();
 public static final Map<String, Integer> colors = new LinkedHashMap<>();
 public static final Map<String, Integer> positions = new LinkedHashMap<>();
 public static MinecraftClient mc;
 public static int hudOpacity = 215, hudSpacing = 21, accentChoice = 0;
 public static int menuScalePercent = 100, hudScalePercent = 100, hudGridSize = 12;
 public static boolean hudGridSnap = true, hudShowGrid = true;
 public static int lowHealthThreshold = 8, armorWarnPercent = 15, hudTextColor = 0;
 public static final net.minecraft.util.Identifier LOGO = new net.minecraft.util.Identifier("lexy-client", "textures/gui/logo.png");
 public static boolean hudBackground = true, hudShadow = true, compactHud = false, spotifyHud = true;
 public static String spotifyClientId = "";
 public static boolean uiSounds=true;
 public static boolean uiHoverSounds=true;
 public static int uiSoundVolume=35;
 public static int maxHudWidgets=7;
 public static int localMusicVolume=65; public static boolean localMusicLoop=false;
 public static final int[] ACCENTS = {0xFF3B82F6,0xFF60A5FA,0xFF2563EB,0xFF22D3EE};
 public static int accent(){return ACCENTS[Math.floorMod(accentChoice,ACCENTS.length)];}
 public static int moduleCount(){return MODULES.size();}
 public static boolean hudMinimal = false;
 public static int hudPreset = 1;
 public static void applyHudPreset(int preset){
  hudPreset = Math.max(0, Math.min(2, preset));
  Set<String> keep = switch(preset){
   case 0 -> Set.of("FPS", "Ping");
   case 1 -> Set.of("FPS", "Ping", "Health", "Totems", "Low Health Alert");
   default -> Set.of("FPS", "Ping", "Health", "Totems", "Mainhand Durability", "Low Health Alert", "Speed", "XYZ X", "XYZ Y", "XYZ Z");
  };
  for(Module m:MODULES) if(m.drawInHud() && HudRenderer.isRelevant(m)) enabled.put(m.id(),keep.contains(m.id()));
  compactHud = true;hudMinimal = preset == 0;
  positions.clear();HudRenderer.invalidate();save();
 }

 private static final int CONFIG_VERSION = 220;
 private static void applyCompactDefaults(){
  Set<String> keep = Set.of("Health","Totems","FPS","Ping","Low Health Alert");
  for(Module module : MODULES){
   if(module.drawInHud() && HudRenderer.isRelevant(module)) enabled.put(module.id(), keep.contains(module.id()));
  }
  enabled.put("AutoTotem", enabled.getOrDefault("AutoTotem", false));
  compactHud = true;
  hudPreset = 1;
  hudSpacing = Math.min(hudSpacing, 19);
 }

 private static KeyBinding guiKey, hudKey, spotifyKey;
 private static long ticks, combatTicks, lastHitTick, sessionStart;
 private static int hits, swings, kills, deaths, longestCombo, combo, damageEvents, lastHealth = -1;
 private static String lastTarget = "None";
 private static java.util.UUID lastAttacked; private static boolean wasDead;
 private static final int WHITE = 0xFFF2F7FF, BLUE = 0xFF409AFF;
 private static final Path CONFIG = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("lexy-client.json");
 public record Module(String id, String category, Supplier<String> value, boolean defaultOn, boolean drawInHud) {}
 private static void add(String category, String id, Supplier<String> value, boolean on) { MODULES.add(new Module(id, category, value, on, true)); enabled.put(id,on); }
 private static void addAction(String category, String id, Supplier<String> value, boolean on) { MODULES.add(new Module(id, category, value, on, false)); enabled.put(id,on); }
 private static String fmt(double n) {return String.format(Locale.ROOT,"%.1f",n);}
 private static PlayerEntity p(){return mc == null ? null : mc.player;}
 private static String inventory(net.minecraft.item.Item item) {PlayerEntity p=p();if(p==null)return "0";int n=0;for(int i=0;i<p.getInventory().size();i++){ItemStack s=p.getInventory().getStack(i);if(s.isOf(item))n+=s.getCount();}return Integer.toString(n);}
 private static String armor(int slot){PlayerEntity p=p();if(p==null)return "-";ItemStack s=p.getInventory().getArmorStack(slot);if(s.isEmpty())return "Empty";return s.getName().getString()+" "+(s.isDamageable()?Math.round(100.0*(s.getMaxDamage()-s.getDamage())/s.getMaxDamage())+"%":"∞");}
 private static String effect(int index){PlayerEntity p=p();if(p==null)return "-";List<StatusEffectInstance> es=new ArrayList<>(p.getStatusEffects());if(index>=es.size())return "-";StatusEffectInstance e=es.get(index);return Text.translatable(e.getEffectType().getTranslationKey()).getString()+" "+(e.getDuration()/20)+"s";}
 private static String target(){if(mc.crosshairTarget instanceof EntityHitResult hit){Entity e=hit.getEntity();return e.getName().getString();}return "None";}
 private static String targetHealth(){if(mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity e)return fmt(e.getHealth())+" / "+fmt(e.getMaxHealth());return "-";}
 private static String targetDistance(){if(mc.crosshairTarget instanceof EntityHitResult hit && p()!=null)return fmt(Math.sqrt(p().squaredDistanceTo(hit.getEntity())))+"m";return "-";}
 private static String status(){return p()==null?"-":fmt(p().getHealth())+" / "+fmt(p().getMaxHealth());}
 private static String ping(){if(mc.getNetworkHandler()==null||p()==null)return "-";var entry=mc.getNetworkHandler().getPlayerListEntry(p().getUuid());return entry==null?"-":entry.getLatency()+" ms";}
 private static String worldTime(){return mc.world==null?"-":Long.toString(mc.world.getTimeOfDay()%24000);}
 private static String xyz(int i){return p()==null?"-":Integer.toString(i==0?p().getBlockX():i==1?p().getBlockY():p().getBlockZ());}
 private static String speed(){return p()==null?"-":fmt(Math.sqrt(Math.pow(p().getX()-p().prevX,2)+Math.pow(p().getZ()-p().prevZ,2))*20)+" b/s";}
 private static String durability(){if(p()==null)return "-";ItemStack s=p().getMainHandStack();return s.isDamageable()?(s.getMaxDamage()-s.getDamage())+" / "+s.getMaxDamage():"-";}
 private static void register(){
  addAction("AutoTotem", "AutoTotem", AutoTotem::status, false);
  add("AutoTotem", "AutoTotem Swaps", () -> String.valueOf(AutoTotem.swaps()), false);
  add("Utilities", "Totem Stock Warning", () -> Integer.parseInt(inventory(Items.TOTEM_OF_UNDYING)) <= 2 ? "LOW TOTEMS" : "OK", false);
  add("Combat","Target Name",LexyClient::target,false);
  add("Combat","Target Health",LexyClient::targetHealth,false);
  add("Combat","Target Distance",LexyClient::targetDistance,false);
  add("Combat","Attack Chain",()->Integer.toString(combo),false);
  add("Combat","Longest Attack Chain",()->Integer.toString(longestCombo),false);
  add("Combat","Attack Attempts",()->Integer.toString(hits),false);
  add("Combat","Targeted Swings",()->Integer.toString(swings),false);
  add("Combat","Attack Attempts/min",()->Long.toString(Math.round(hits*60000.0/Math.max(60000,System.currentTimeMillis()-sessionStart))),false);
  add("Combat","Combat Timer",()->combatTicks==0?"Idle":(combatTicks/20)+"s",false);
  add("Combat","Last Attacked",()->lastTarget,false);
  add("HUD","Health",LexyClient::status,true);
  add("HUD","Absorption",()->p()==null?"-":fmt(p().getAbsorptionAmount()),false);
  add("HUD","Food",()->p()==null?"-":Integer.toString(p().getHungerManager().getFoodLevel()),false);
  add("HUD","Saturation",()->p()==null?"-":fmt(p().getHungerManager().getSaturationLevel()),false);
  add("HUD","Helmet",()->armor(3),false);
  add("HUD","Chestplate",()->armor(2),false);
  add("HUD","Leggings",()->armor(1),false);
  add("HUD","Boots",()->armor(0),false);
  add("HUD","Mainhand Durability",LexyClient::durability,true);
  add("HUD","Totems",()->inventory(Items.TOTEM_OF_UNDYING),true);
  add("HUD","Ender Pearls",()->inventory(Items.ENDER_PEARL),false);
  add("HUD","Golden Apples",()->inventory(Items.GOLDEN_APPLE),false);
  add("HUD","Enchanted Apples",()->inventory(Items.ENCHANTED_GOLDEN_APPLE),false);
  add("HUD","Experience Level",()->p()==null?"-":Integer.toString(p().experienceLevel),false);
  add("HUD","Effect 1",()->effect(0),false);
  add("HUD","Effect 2",()->effect(1),false);
  add("HUD","Effect 3",()->effect(2),false);
  add("HUD","Effect 4",()->effect(3),false);
  add("HUD","FPS",()->mc.getCurrentFps()+"",true);
  add("HUD","Ping",LexyClient::ping,true);
  add("HUD","XYZ X",()->xyz(0),false);
  add("HUD","XYZ Y",()->xyz(1),false);
  add("HUD","XYZ Z",()->xyz(2),false);
  add("HUD","Speed",LexyClient::speed,false);
  add("HUD","Facing",()->p()==null?"-":p().getHorizontalFacing().asString(),false);
  add("HUD","Biome",()->p()==null||mc.world==null?"-":mc.world.getBiome(p().getBlockPos()).getKey().map(k->k.getValue().toString()).orElse("unknown"),false);
  add("HUD","World Time",LexyClient::worldTime,false);
  add("Statistics","Session Time",()->(System.currentTimeMillis()-sessionStart)/1000+"s",false);
  add("Statistics","Possible Kills",()->Integer.toString(kills),false);
  add("Statistics","Session Deaths",()->Integer.toString(deaths),false);
  add("Statistics","Possible K/D",()->fmt((double)kills/Math.max(1,deaths)),false);
  add("Statistics","Health Drop Events",()->Integer.toString(damageEvents),false);
  add("Utilities","Low Health Alert",()->p()!=null&&p().getHealth()<=lowHealthThreshold?"LOW HEALTH!":"OK",true);
  add("Utilities","Low Helmet Alert",()->armorAlert(3),false);
  add("Utilities","Low Chest Alert",()->armorAlert(2),false);
  add("Utilities","Low Legs Alert",()->armorAlert(1),false);
  add("Utilities","Low Boots Alert",()->armorAlert(0),false);
  add("Combat","Attack Cooldown",()->p()==null?"-":Math.round(p().getAttackCooldownProgress(0)*100)+"%",false);
  add("Combat","Target Armor Points",()->mc.crosshairTarget instanceof EntityHitResult h && h.getEntity() instanceof LivingEntity e ? String.valueOf(e.getArmor()) : "-",false);
  add("Combat","Target Mainhand",()->mc.crosshairTarget instanceof EntityHitResult h && h.getEntity() instanceof LivingEntity e ? e.getMainHandStack().getName().getString() : "-",false);
  add("HUD","Offhand Item",()->p()==null?"-":p().getOffHandStack().getName().getString(),false);
  add("HUD","Offhand Count",()->p()==null?"-":String.valueOf(p().getOffHandStack().getCount()),false);
  add("HUD","Mainhand Item",()->p()==null?"-":p().getMainHandStack().getName().getString(),false);
  add("HUD","Hotbar Slot",()->p()==null?"-":String.valueOf(p().getInventory().selectedSlot+1),false);
  add("HUD","Obsidian",()->inventory(Items.OBSIDIAN),false);
  add("HUD","End Crystals",()->inventory(Items.END_CRYSTAL),false);
  add("HUD","Respawn Anchors",()->inventory(Items.RESPAWN_ANCHOR),false);
  add("HUD","Glowstone",()->inventory(Items.GLOWSTONE),false);
  add("HUD","Cobwebs",()->inventory(Items.COBWEB),false);
  add("HUD","Potions",()->inventory(Items.POTION),false);
  add("HUD","Splash Potions",()->inventory(Items.SPLASH_POTION),false);
  add("HUD","Lingering Potions",()->inventory(Items.LINGERING_POTION),false);
  add("HUD","Arrows",()->inventory(Items.ARROW),false);
  add("HUD","Spectral Arrows",()->inventory(Items.SPECTRAL_ARROW),false);
  add("HUD","Bows",()->inventory(Items.BOW),false);
  add("HUD","Crossbows",()->inventory(Items.CROSSBOW),false);
  add("HUD","Shields",()->inventory(Items.SHIELD),false);
  add("HUD","Firework Rockets",()->inventory(Items.FIREWORK_ROCKET),false);
  add("HUD","Experience Bottles",()->inventory(Items.EXPERIENCE_BOTTLE),false);
  add("HUD","Chorus Fruit",()->inventory(Items.CHORUS_FRUIT),false);
  add("HUD","Water Buckets",()->inventory(Items.WATER_BUCKET),false);
  add("HUD","Lava Buckets",()->inventory(Items.LAVA_BUCKET),false);
  add("HUD","Ender Chests",()->inventory(Items.ENDER_CHEST),false);
  add("HUD","Netherite Swords",()->inventory(Items.NETHERITE_SWORD),false);
  add("HUD","Netherite Axes",()->inventory(Items.NETHERITE_AXE),false);
  addAction("Performance","Render Distance",()->mc.options.getViewDistance().getValue()+" chunks",false);
  addAction("Performance","FPS Limit",()->mc.options.getMaxFps().getValue()+" FPS",false);
  addAction("Performance","Particles Mode",()->mc.options.getParticles().getValue().toString(),false);
  addAction("Performance","Graphics Quality",()->mc.options.getGraphicsMode().getValue().toString(),false);
  addAction("Performance","Entity Render Scale",()->Math.round(mc.options.getEntityDistanceScaling().getValue()*100)+"%",false);
  add("Music","Spotify Now Playing",()->SpotifyController.INSTANCE.playing(),false);
  add("Music","Spotify Connection",()->SpotifyController.INSTANCE.status(),false);
  add("Music","Local Track",()->LocalMusicPlayer.INSTANCE.current(),false);
  add("Music","Music Volume",()->LocalMusicPlayer.INSTANCE.volume()+"%",false);

 }
 private static String armorAlert(int slot){if(p()==null)return "-";ItemStack s=p().getInventory().getArmorStack(slot);if(s.isEmpty())return "MISSING";if(!s.isDamageable())return "OK";return (100*(s.getMaxDamage()-s.getDamage())/s.getMaxDamage())<=armorWarnPercent?"REPAIR!":"OK";}
 @Override public void onInitializeClient(){LexySounds.init();mc=MinecraftClient.getInstance();sessionStart=System.currentTimeMillis();register();load();SpotifyController.INSTANCE.setClientId(spotifyClientId);LocalMusicPlayer.INSTANCE.setVolume(localMusicVolume);TitleBranding.register();if(localMusicLoop)LocalMusicPlayer.INSTANCE.toggleLoop();guiKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("Lexy: Modules",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,"Lexy Client"));hudKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("Lexy: HUD Editor",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_CONTROL,"Lexy Client"));spotifyKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("Lexy: Spotify",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,"Lexy Client"));
 ClientTickEvents.END_CLIENT_TICK.register(client->{ticks++;while(guiKey.wasPressed())if(client.currentScreen==null)client.setScreen(new LexyScreen(false));while(hudKey.wasPressed())client.setScreen(new HudEditorScreen(client.currentScreen));while(spotifyKey.wasPressed())if(client.currentScreen==null)client.setScreen(new LexyScreen(false,7));AutoTotem.tick(client);if(client.player==null){lastHealth=-1;AutoTotem.resetRuntime();return;}if(lastHealth>=0&&client.player.getHealth()<lastHealth)damageEvents++;
 if(lastHealth>lowHealthThreshold && client.player.getHealth()<=lowHealthThreshold)LexySounds.notifySound();
 lastHealth=(int)client.player.getHealth();if(client.player.isDead()&&!wasDead)deaths++;wasDead=client.player.isDead();if(lastAttacked!=null&&ticks-lastHitTick>100)lastAttacked=null;if(lastAttacked!=null&&client.world!=null&&ticks%10==0){for(Entity entity:client.world.getEntities()){if(entity.getUuid().equals(lastAttacked)&&entity instanceof LivingEntity living&&living.isDead()){kills++;lastAttacked=null;break;}}}if(combatTicks>0)combatTicks++;if(ticks-lastHitTick>60)combo=0;});
 net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player,world,hand,entity,hitResult)->{if(world.isClient&&player==mc.player){swings++;hits++;combo++;longestCombo=Math.max(combo,longestCombo);lastHitTick=ticks;combatTicks=1;lastTarget=entity.getName().getString();lastAttacked=entity.getUuid();}return net.minecraft.util.ActionResult.PASS;});
 HudRenderCallback.EVENT.register((context,delta)->{
  if(mc.player==null||mc.options.hudHidden||mc.currentScreen instanceof LexyScreen||mc.currentScreen instanceof HudEditorScreen)return;
  HudRenderer.render(context,false,null);
 });
 net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client->{save();SpotifyController.INSTANCE.shutdown();LocalMusicPlayer.INSTANCE.shutdown();});
 }
 public static void save(){try{Files.createDirectories(CONFIG.getParent());JsonObject root=new JsonObject();JsonObject mods=new JsonObject();enabled.forEach(mods::addProperty);root.add("modules",mods);JsonObject coords=new JsonObject();positions.forEach(coords::addProperty);root.add("positions",coords);root.addProperty("configVersion",CONFIG_VERSION);
 JsonObject style=new JsonObject();style.addProperty("hudOpacity",hudOpacity);style.addProperty("hudSpacing",hudSpacing);
 style.addProperty("accentChoice",accentChoice);
 style.addProperty("menuScalePercent",menuScalePercent);style.addProperty("hudScalePercent",hudScalePercent);
 style.addProperty("hudGridSize",hudGridSize);style.addProperty("hudGridSnap",hudGridSnap);style.addProperty("hudShowGrid",hudShowGrid);style.addProperty("hudBackground",hudBackground);
 style.addProperty("uiHoverSounds",uiHoverSounds);style.addProperty("uiSounds",uiSounds);style.addProperty("uiSoundVolume",uiSoundVolume);style.addProperty("maxHudWidgets",maxHudWidgets);
 style.addProperty("hudPreset",hudPreset);style.addProperty("hudMinimal",hudMinimal);style.addProperty("hudShadow",hudShadow);style.addProperty("compactHud",compactHud);style.addProperty("spotifyHud",spotifyHud);
 style.addProperty("lowHealthThreshold",lowHealthThreshold);style.addProperty("armorWarnPercent",armorWarnPercent);style.addProperty("hudTextColor",hudTextColor);
 JsonObject totem=new JsonObject();totem.addProperty("onlyWhenEmpty",AutoTotem.onlyWhenEmpty);totem.addProperty("lowHealthOnly",AutoTotem.lowHealthOnly);totem.addProperty("healthThreshold",AutoTotem.healthThreshold);
 totem.addProperty("mode",AutoTotem.mode.name());totem.addProperty("randomSource",AutoTotem.randomSource);
 totem.addProperty("minDelayMs",AutoTotem.minDelayMs);totem.addProperty("maxDelayMs",AutoTotem.maxDelayMs);
 totem.addProperty("inventoryStepMs",AutoTotem.inventoryStepMs);totem.addProperty("cooldownMs",AutoTotem.cooldownMs);
 totem.addProperty("notify",AutoTotem.notify);root.add("autoTotem",totem);
 root.add("style",style);root.addProperty("spotifyClientId",spotifyClientId);
 root.addProperty("localMusicVolume",LocalMusicPlayer.INSTANCE.volume());root.addProperty("localMusicLoop",LocalMusicPlayer.INSTANCE.looping());
 Files.writeString(CONFIG,new GsonBuilder().setPrettyPrinting().create().toJson(root));}catch(IOException e){System.err.println("Lexy config save failed: "+e);}}
 public static void load(){if(!Files.exists(CONFIG)){applyCompactDefaults();return;}try{JsonObject root=JsonParser.parseString(Files.readString(CONFIG)).getAsJsonObject();int configVersion=root.has("configVersion")?root.get("configVersion").getAsInt():0;if(root.has("modules"))for(var e:root.getAsJsonObject("modules").entrySet())if(enabled.containsKey(e.getKey()))enabled.put(e.getKey(),e.getValue().getAsBoolean());if(root.has("positions"))for(var e:root.getAsJsonObject("positions").entrySet())positions.put(e.getKey(),e.getValue().getAsInt());
 if(root.has("style")){JsonObject style=root.getAsJsonObject("style");
 if(style.has("hudOpacity"))hudOpacity=Math.max(50,Math.min(245,style.get("hudOpacity").getAsInt()));
 if(style.has("hudSpacing"))hudSpacing=Math.max(16,Math.min(36,style.get("hudSpacing").getAsInt()));
 if(style.has("accentChoice"))accentChoice=style.get("accentChoice").getAsInt();
 if(style.has("menuScalePercent"))menuScalePercent=Math.max(65,Math.min(110,style.get("menuScalePercent").getAsInt()));
 if(style.has("hudScalePercent"))hudScalePercent=Math.max(45,Math.min(150,style.get("hudScalePercent").getAsInt()));
 if(style.has("hudGridSize"))hudGridSize=Math.max(4,Math.min(28,style.get("hudGridSize").getAsInt()));
 if(style.has("hudGridSnap"))hudGridSnap=style.get("hudGridSnap").getAsBoolean();
 if(style.has("hudShowGrid"))hudShowGrid=style.get("hudShowGrid").getAsBoolean();
 if(style.has("hudBackground"))hudBackground=style.get("hudBackground").getAsBoolean();
 if(style.has("uiHoverSounds"))uiHoverSounds=style.get("uiHoverSounds").getAsBoolean();
 if(style.has("uiSounds"))uiSounds=style.get("uiSounds").getAsBoolean();
 if(style.has("uiSoundVolume"))uiSoundVolume=Math.max(0,Math.min(100,style.get("uiSoundVolume").getAsInt()));
 if(style.has("maxHudWidgets"))maxHudWidgets=Math.max(3,Math.min(10,style.get("maxHudWidgets").getAsInt()));
 if(style.has("hudPreset"))hudPreset=Math.max(0,Math.min(2,style.get("hudPreset").getAsInt()));
 if(style.has("hudMinimal"))hudMinimal=style.get("hudMinimal").getAsBoolean();
 if(style.has("hudShadow"))hudShadow=style.get("hudShadow").getAsBoolean();
 if(style.has("compactHud"))compactHud=style.get("compactHud").getAsBoolean();
 if(style.has("spotifyHud"))spotifyHud=style.get("spotifyHud").getAsBoolean();
 if(style.has("lowHealthThreshold"))lowHealthThreshold=Math.max(2,Math.min(20,style.get("lowHealthThreshold").getAsInt()));
 if(style.has("armorWarnPercent"))armorWarnPercent=Math.max(5,Math.min(70,style.get("armorWarnPercent").getAsInt()));
 if(style.has("hudTextColor"))hudTextColor=Math.floorMod(style.get("hudTextColor").getAsInt(),3);
 }
 if(root.has("autoTotem")){JsonObject t=root.getAsJsonObject("autoTotem");
 if(t.has("onlyWhenEmpty"))AutoTotem.onlyWhenEmpty=t.get("onlyWhenEmpty").getAsBoolean();
 if(t.has("lowHealthOnly"))AutoTotem.lowHealthOnly=t.get("lowHealthOnly").getAsBoolean();
 if(t.has("healthThreshold"))AutoTotem.healthThreshold=Math.max(2,Math.min(20,t.get("healthThreshold").getAsInt()));
 if(t.has("intervalTicks")&&!t.has("cooldownMs"))AutoTotem.cooldownMs=Math.max(200,Math.min(2500,t.get("intervalTicks").getAsInt()*50));
 if(t.has("mode"))try{AutoTotem.mode=AutoTotem.Mode.valueOf(t.get("mode").getAsString());}catch(IllegalArgumentException ignored){}
 if(t.has("randomSource"))AutoTotem.randomSource=t.get("randomSource").getAsBoolean();
 if(t.has("minDelayMs"))AutoTotem.minDelayMs=Math.max(50,Math.min(1500,t.get("minDelayMs").getAsInt()));
 if(t.has("maxDelayMs"))AutoTotem.maxDelayMs=Math.max(AutoTotem.minDelayMs,Math.min(2500,t.get("maxDelayMs").getAsInt()));
 if(t.has("inventoryStepMs"))AutoTotem.inventoryStepMs=Math.max(100,Math.min(1100,t.get("inventoryStepMs").getAsInt()));
 if(t.has("cooldownMs"))AutoTotem.cooldownMs=Math.max(200,Math.min(3000,t.get("cooldownMs").getAsInt()));
 if(t.has("notify"))AutoTotem.notify=t.get("notify").getAsBoolean();}
 if(root.has("spotifyClientId"))spotifyClientId=root.get("spotifyClientId").getAsString();
 if(root.has("localMusicVolume"))localMusicVolume=Math.max(0,Math.min(100,root.get("localMusicVolume").getAsInt()));
 if(root.has("localMusicLoop"))localMusicLoop=root.get("localMusicLoop").getAsBoolean();
 if(configVersion < 170) applyCompactDefaults();
 }catch(Exception e){System.err.println("Lexy config load failed: "+e);applyCompactDefaults();}}
}