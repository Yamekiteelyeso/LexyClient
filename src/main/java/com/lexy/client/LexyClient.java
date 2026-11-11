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
 public static boolean hudBackground = true, hudShadow = true, compactHud = false, spotifyHud = true;
 public static String spotifyClientId = "";
 public static int localMusicVolume=65; public static boolean localMusicLoop=false;
 public static final int[] ACCENTS = {0xFF3B82F6,0xFF60A5FA,0xFF2563EB,0xFF22D3EE};
 public static int accent(){return ACCENTS[Math.floorMod(accentChoice,ACCENTS.length)];}
 public static int moduleCount(){return MODULES.size();}

 private static KeyBinding guiKey, hudKey, spotifyKey;
 private static long ticks, combatTicks, lastHitTick, sessionStart;
 private static int hits, swings, kills, deaths, longestCombo, combo, damageEvents, lastHealth = -1;
 private static String lastTarget = "None";
 private static java.util.UUID lastAttacked; private static boolean wasDead;
 private static final int WHITE = 0xFFF2F7FF, BLUE = 0xFF409AFF;
 private static final Path CONFIG = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("lexy-client.json");
 public record Module(String id, String category, Supplier<String> value, boolean defaultOn) {}
 private static void add(String category, String id, Supplier<String> value, boolean on) { MODULES.add(new Module(id, category, value, on)); enabled.put(id,on); }
 private static String fmt(double n) {return String.format(Locale.ROOT,"%.1f",n);}
 private static PlayerEntity p(){return mc == null ? null : mc.player;}
 private static String inventory(net.minecraft.item.Item item) {PlayerEntity p=p();if(p==null)return "0";int n=0;for(int i=0;i<p.getInventory().size();i++){ItemStack s=p.getInventory().getStack(i);if(s.isOf(item))n+=s.getCount();}return Integer.toString(n);}
 private static String armor(int slot){PlayerEntity p=p();if(p==null)return "-";ItemStack s=p.getInventory().getArmorStack(slot);if(s.isEmpty())return "Empty";return s.getName().getString()+" "+(s.isDamageable()?Math.round(100.0*(s.getMaxDamage()-s.getDamage())/s.getMaxDamage())+"%":"∞");}
 private static String effect(int index){PlayerEntity p=p();if(p==null)return "-";List<StatusEffectInstance> es=new ArrayList<>(p.getStatusEffects());if(index>=es.size())return "-";StatusEffectInstance e=es.get(index);return Text.translatable(e.getEffectType().getTranslationKey()).getString()+" "+(e.getDuration()/20)+"s";}
 private static String target(){if(mc.crosshairTarget instanceof EntityHitResult hit){Entity e=hit.getEntity();return e.getName().getString();}return "None";}
 private static String targetHealth(){if(mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity e)return fmt(e.getHealth())+" / "+fmt(e.getMaxHealth());return "-";}
 private static String targetDistance(){if(mc.crosshairTarget instanceof EntityHitResult hit && p()!=null)return fmt(Math.sqrt(p().squaredDistanceTo(hit.getEntity())))+"m";return "-";}
 private static String status(){return p()==null?"-":fmt(p().getHealth())+" / "+fmt(p().getMaxHealth());}
 private static String ping(){if(mc.getNetworkHandler()==null||p()==null)return "-";var entry=mc.getNetworkHandler().getPlayerListEntry(p().getUuid());return entry==null?"-":entry.getLatency()+"ms";}
 private static String worldTime(){return mc.world==null?"-":Long.toString(mc.world.getTimeOfDay()%24000);}
 private static String xyz(int i){return p()==null?"-":Integer.toString(i==0?p().getBlockX():i==1?p().getBlockY():p().getBlockZ());}
 private static String speed(){return p()==null?"-":fmt(Math.sqrt(Math.pow(p().getX()-p().prevX,2)+Math.pow(p().getZ()-p().prevZ,2))*20)+" b/s";}
 private static String durability(){if(p()==null)return "-";ItemStack s=p().getMainHandStack();return s.isDamageable()?(s.getMaxDamage()-s.getDamage())+" / "+s.getMaxDamage():"-";}
 private static void register(){
  add("Combat","Target Name",LexyClient::target,true);
  add("Combat","Target Health",LexyClient::targetHealth,true);
  add("Combat","Target Distance",LexyClient::targetDistance,true);
  add("Combat","Attack Chain",()->Integer.toString(combo),true);
  add("Combat","Longest Attack Chain",()->Integer.toString(longestCombo),false);
  add("Combat","Attack Attempts",()->Integer.toString(hits),false);
  add("Combat","Targeted Swings",()->Integer.toString(swings),false);
  add("Combat","Attack Attempts/min",()->Long.toString(Math.round(hits*60000.0/Math.max(60000,System.currentTimeMillis()-sessionStart))),false);
  add("Combat","Combat Timer",()->combatTicks==0?"Idle":(combatTicks/20)+"s",false);
  add("Combat","Last Attacked",()->lastTarget,false);
  add("HUD","Health",LexyClient::status,true);
  add("HUD","Absorption",()->p()==null?"-":fmt(p().getAbsorptionAmount()),true);
  add("HUD","Food",()->p()==null?"-":Integer.toString(p().getHungerManager().getFoodLevel()),false);
  add("HUD","Saturation",()->p()==null?"-":fmt(p().getHungerManager().getSaturationLevel()),false);
  add("HUD","Helmet",()->armor(3),true);
  add("HUD","Chestplate",()->armor(2),true);
  add("HUD","Leggings",()->armor(1),true);
  add("HUD","Boots",()->armor(0),true);
  add("HUD","Mainhand Durability",LexyClient::durability,true);
  add("HUD","Totems",()->inventory(Items.TOTEM_OF_UNDYING),true);
  add("HUD","Ender Pearls",()->inventory(Items.ENDER_PEARL),true);
  add("HUD","Golden Apples",()->inventory(Items.GOLDEN_APPLE),true);
  add("HUD","Enchanted Apples",()->inventory(Items.ENCHANTED_GOLDEN_APPLE),false);
  add("HUD","Experience Level",()->p()==null?"-":Integer.toString(p().experienceLevel),false);
  add("HUD","Effect 1",()->effect(0),true);
  add("HUD","Effect 2",()->effect(1),false);
  add("HUD","Effect 3",()->effect(2),false);
  add("HUD","Effect 4",()->effect(3),false);
  add("HUD","FPS",()->mc.getCurrentFps()+" FPS",true);
  add("HUD","Ping",LexyClient::ping,true);
  add("HUD","XYZ X",()->xyz(0),false);
  add("HUD","XYZ Y",()->xyz(1),false);
  add("HUD","XYZ Z",()->xyz(2),false);
  add("HUD","Speed",LexyClient::speed,false);
  add("HUD","Facing",()->p()==null?"-":p().getHorizontalFacing().asString(),false);
  add("HUD","Biome",()->p()==null||mc.world==null?"-":mc.world.getBiome(p().getBlockPos()).getKey().map(k->k.getValue().toString()).orElse("unknown"),false);
  add("HUD","World Time",LexyClient::worldTime,false);
  add("Statistics","Session Time",()->(System.currentTimeMillis()-sessionStart)/1000+"s",true);
  add("Statistics","Possible Kills",()->Integer.toString(kills),true);
  add("Statistics","Session Deaths",()->Integer.toString(deaths),true);
  add("Statistics","Possible K/D",()->fmt((double)kills/Math.max(1,deaths)),false);
  add("Statistics","Health Drop Events",()->Integer.toString(damageEvents),false);
  add("Utilities","Low Health Alert",()->p()!=null&&p().getHealth()<=8?"LOW HEALTH!":"OK",true);
  add("Utilities","Low Helmet Alert",()->armorAlert(3),true);
  add("Utilities","Low Chest Alert",()->armorAlert(2),true);
  add("Utilities","Low Legs Alert",()->armorAlert(1),true);
  add("Utilities","Low Boots Alert",()->armorAlert(0),true);
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
  add("Performance","Render Distance",()->mc.options.getViewDistance().getValue()+" chunks",false);
  add("Performance","FPS Limit",()->mc.options.getMaxFps().getValue()+" FPS",false);
  add("Performance","Particles Mode",()->mc.options.getParticles().getValue().toString(),false);
  add("Performance","Graphics Quality",()->mc.options.getGraphicsMode().getValue().toString(),false);
  add("Performance","Entity Render Scale",()->Math.round(mc.options.getEntityDistanceScaling().getValue()*100)+"%",false);
  add("Music","Spotify Now Playing",()->SpotifyController.INSTANCE.playing(),false);
  add("Music","Spotify Connection",()->SpotifyController.INSTANCE.status(),false);
  add("Music","Local Track",()->LocalMusicPlayer.INSTANCE.current(),false);
  add("Music","Music Volume",()->LocalMusicPlayer.INSTANCE.volume()+"%",false);

 }
 private static String armorAlert(int slot){if(p()==null)return "-";ItemStack s=p().getInventory().getArmorStack(slot);if(s.isEmpty())return "MISSING";if(!s.isDamageable())return "OK";return (100*(s.getMaxDamage()-s.getDamage())/s.getMaxDamage())<=15?"REPAIR!":"OK";}
 @Override public void onInitializeClient(){mc=MinecraftClient.getInstance();sessionStart=System.currentTimeMillis();register();load();SpotifyController.INSTANCE.setClientId(spotifyClientId);LocalMusicPlayer.INSTANCE.setVolume(localMusicVolume);if(localMusicLoop)LocalMusicPlayer.INSTANCE.toggleLoop();guiKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("Lexy: Modules",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,"Lexy Client"));hudKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("Lexy: HUD Editor",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_CONTROL,"Lexy Client"));spotifyKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("Lexy: Spotify",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,"Lexy Client"));
 ClientTickEvents.END_CLIENT_TICK.register(client->{ticks++;while(guiKey.wasPressed())client.setScreen(new LexyScreen(false));while(hudKey.wasPressed())client.setScreen(new LexyScreen(true));while(spotifyKey.wasPressed())client.setScreen(new LexyScreen(false,7));if(client.player==null){lastHealth=-1;return;}if(lastHealth>=0&&client.player.getHealth()<lastHealth)damageEvents++;lastHealth=(int)client.player.getHealth();if(client.player.isDead()&&!wasDead)deaths++;wasDead=client.player.isDead();if(lastAttacked!=null&&ticks-lastHitTick>100)lastAttacked=null;if(lastAttacked!=null&&client.world!=null){for(Entity entity:client.world.getEntities()){if(entity.getUuid().equals(lastAttacked)&&entity instanceof LivingEntity living&&living.isDead()){kills++;lastAttacked=null;break;}}}if(combatTicks>0)combatTicks++;if(ticks-lastHitTick>60)combo=0;});
 net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player,world,hand,entity,hitResult)->{if(world.isClient&&player==mc.player){swings++;hits++;combo++;longestCombo=Math.max(combo,longestCombo);lastHitTick=ticks;combatTicks=1;lastTarget=entity.getName().getString();lastAttacked=entity.getUuid();}return net.minecraft.util.ActionResult.PASS;});
 HudRenderCallback.EVENT.register((context,delta)->{
  if(mc.player==null||mc.options.hudHidden||mc.currentScreen instanceof LexyScreen)return;
  int defaultY=8;
  int sw=mc.getWindow().getScaledWidth(), sh=mc.getWindow().getScaledHeight();
  for(Module m:MODULES){
   if(!enabled.getOrDefault(m.id(),false)||(!spotifyHud&&m.category().equals("Music")))continue;
   String value;
   try{value=m.value().get();}catch(RuntimeException e){value="Unavailable";}
   String label=m.id();
   String output=label+"  "+value;
   int w=Math.max(90,mc.textRenderer.getWidth(output)+21);
   int x=Math.max(0,Math.min(sw-w,positions.getOrDefault(m.id(),8)));
   int yy=Math.max(0,Math.min(sh-20,positions.getOrDefault(m.id()+".y",defaultY)));
   if(hudBackground)context.fill(x,yy,x+w,yy+(compactHud?15:18),(hudOpacity<<24)|0x151C29);
   context.fill(x,yy,x+3,yy+(compactHud?15:18),accent());
   if(hudBackground)context.fill(x,yy+(compactHud?14:17),x+w,yy+(compactHud?15:18),0xFF34465B);
   int labelColor=m.category().equals("Utilities")&&(value.contains("!")||value.equals("REPAIR!")||value.equals("MISSING"))?0xFFFF707B:WHITE;
   if(hudShadow)context.drawTextWithShadow(mc.textRenderer,output,x+10,yy+(compactHud?3:5),labelColor);
   else context.drawText(mc.textRenderer,output,x+10,yy+(compactHud?3:5),labelColor,false);
   defaultY+=hudSpacing;
  }
 });
 net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client->{save();SpotifyController.INSTANCE.shutdown();LocalMusicPlayer.INSTANCE.shutdown();});
 }
 public static void save(){try{Files.createDirectories(CONFIG.getParent());JsonObject root=new JsonObject();JsonObject mods=new JsonObject();enabled.forEach(mods::addProperty);root.add("modules",mods);JsonObject coords=new JsonObject();positions.forEach(coords::addProperty);root.add("positions",coords);
 JsonObject style=new JsonObject();style.addProperty("hudOpacity",hudOpacity);style.addProperty("hudSpacing",hudSpacing);
 style.addProperty("accentChoice",accentChoice);style.addProperty("hudBackground",hudBackground);
 style.addProperty("hudShadow",hudShadow);style.addProperty("compactHud",compactHud);style.addProperty("spotifyHud",spotifyHud);
 root.add("style",style);root.addProperty("spotifyClientId",spotifyClientId);
 root.addProperty("localMusicVolume",LocalMusicPlayer.INSTANCE.volume());root.addProperty("localMusicLoop",LocalMusicPlayer.INSTANCE.looping());
 Files.writeString(CONFIG,new GsonBuilder().setPrettyPrinting().create().toJson(root));}catch(IOException e){System.err.println("Lexy config save failed: "+e);}}
 public static void load(){if(!Files.exists(CONFIG))return;try{JsonObject root=JsonParser.parseString(Files.readString(CONFIG)).getAsJsonObject();if(root.has("modules"))for(var e:root.getAsJsonObject("modules").entrySet())if(enabled.containsKey(e.getKey()))enabled.put(e.getKey(),e.getValue().getAsBoolean());if(root.has("positions"))for(var e:root.getAsJsonObject("positions").entrySet())positions.put(e.getKey(),e.getValue().getAsInt());
 if(root.has("style")){JsonObject style=root.getAsJsonObject("style");
 if(style.has("hudOpacity"))hudOpacity=Math.max(50,Math.min(245,style.get("hudOpacity").getAsInt()));
 if(style.has("hudSpacing"))hudSpacing=Math.max(16,Math.min(36,style.get("hudSpacing").getAsInt()));
 if(style.has("accentChoice"))accentChoice=style.get("accentChoice").getAsInt();
 if(style.has("hudBackground"))hudBackground=style.get("hudBackground").getAsBoolean();
 if(style.has("hudShadow"))hudShadow=style.get("hudShadow").getAsBoolean();
 if(style.has("compactHud"))compactHud=style.get("compactHud").getAsBoolean();
 if(style.has("spotifyHud"))spotifyHud=style.get("spotifyHud").getAsBoolean();
 }
 if(root.has("spotifyClientId"))spotifyClientId=root.get("spotifyClientId").getAsString();
 if(root.has("localMusicVolume"))localMusicVolume=Math.max(0,Math.min(100,root.get("localMusicVolume").getAsInt()));
 if(root.has("localMusicLoop"))localMusicLoop=root.get("localMusicLoop").getAsBoolean();
 }catch(Exception e){System.err.println("Lexy config load failed: "+e);}}
}