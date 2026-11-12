package com.lexy.client;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Original seven-theme palette and robust, local-only profile snapshots. */
public final class LexyTheme {
    private LexyTheme() {}
    public record Palette(String name, int accent, int accentSoft, int background, int sidebar, int card, int border) {}
    private static final Palette[] PALETTES = {
        new Palette("Dark Blue",0xFF4A95F5,0xFF1F4066,0xFF0A111D,0xFF0E1928,0xFF172439,0xFF2A3C53),
        new Palette("Midnight",0xFF6095CF,0xFF27405F,0xFF090D18,0xFF101725,0xFF1B2535,0xFF354254),
        new Palette("AMOLED",0xFF65B6FF,0xFF16364B,0xFF030507,0xFF090D12,0xFF141B24,0xFF29313B),
        new Palette("Lunar Inspired",0xFF5AABC8,0xFF255B69,0xFF0C161B,0xFF152221,0xFF20302D,0xFF35534F),
        new Palette("Feather Inspired",0xFFE0525E,0xFF59303D,0xFF101115,0xFF161920,0xFF252A32,0xFF454A53),
        new Palette("Purple Neon",0xFFAE82F8,0xFF483269,0xFF100D1B,0xFF1A152B,0xFF29223B,0xFF4A3B65),
        new Palette("Custom Blue",0xFF55D3E5,0xFF225466,0xFF091822,0xFF102530,0xFF1C3542,0xFF31556B)
    };
    private static final Path ROOT=FabricLoader.getInstance().getConfigDir();
    private static final Path PREFS=ROOT.resolve("lexy-ui-v4.json");
    private static final Path PROFILES=ROOT.resolve("lexy-profiles");
    private static final Path MAIN=ROOT.resolve("lexy-client.json");
    public static int index=0;
    public static boolean customTitle=true, animate=true, dimBackground=true, showTooltips=true;
    public static final LinkedHashSet<String> favorites=new LinkedHashSet<>();
    public static String activeProfile="Default";
    public static Palette palette(){return PALETTES[Math.floorMod(index,PALETTES.length)];}
    public static Palette[] palettes(){return PALETTES.clone();}
    public static void next(){index=(index+1)%PALETTES.length;save();}
    public static void choose(int i){index=Math.floorMod(i,PALETTES.length);save();}
    public static void favorite(String name){if(!favorites.remove(name))favorites.add(name);save();}
    public static boolean isFavorite(String name){return favorites.contains(name);}
    private static void write(Path path,String data)throws IOException{
        Files.createDirectories(path.getParent());
        Path tmp=path.resolveSibling(path.getFileName()+".tmp");
        Files.writeString(tmp,data);
        try{Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException ignored){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
    }
    private static JsonObject object(){
        JsonObject root=new JsonObject();root.addProperty("schema",4);root.addProperty("theme",index);
        root.addProperty("title",customTitle);root.addProperty("animate",animate);
        root.addProperty("dim",dimBackground);root.addProperty("tips",showTooltips);
        root.addProperty("activeProfile",activeProfile);
        JsonArray fav=new JsonArray();favorites.forEach(fav::add);root.add("favorites",fav);
        root.add("extras",LexyExtras.serialize());return root;
    }
    public static void save(){try{write(PREFS,new GsonBuilder().setPrettyPrinting().create().toJson(object()));}
        catch(IOException ex){System.err.println("[Lexy] UI preferences save failed: "+ex.getMessage());}}
    public static void load(){
        if(!Files.isRegularFile(PREFS))return;
        try{JsonObject json=JsonParser.parseString(Files.readString(PREFS)).getAsJsonObject();
            if(json.has("theme"))index=Math.floorMod(json.get("theme").getAsInt(),PALETTES.length);
            if(json.has("title"))customTitle=json.get("title").getAsBoolean();
            if(json.has("animate"))animate=json.get("animate").getAsBoolean();
            if(json.has("dim"))dimBackground=json.get("dim").getAsBoolean();
            if(json.has("tips"))showTooltips=json.get("tips").getAsBoolean();
            if(json.has("activeProfile"))activeProfile=json.get("activeProfile").getAsString();
            favorites.clear();if(json.has("favorites"))for(JsonElement e:json.getAsJsonArray("favorites"))if(e.isJsonPrimitive())favorites.add(e.getAsString());
            if(json.has("extras"))LexyExtras.read(json.getAsJsonObject("extras"));
        }catch(Exception ex){System.err.println("[Lexy] UI settings invalid; safe defaults retained: "+ex.getMessage());}
    }
    private static String safeName(String name){
        String s=name==null?"":name.trim().replaceAll("[^a-zA-Z0-9_-]","_");
        if(s.length()>24)s=s.substring(0,24);return s;
    }
    public static List<String> profiles(){
        try{Files.createDirectories(PROFILES);List<String> found=new ArrayList<>();
            try(var files=Files.list(PROFILES)){files.filter(p->p.getFileName().toString().endsWith(".json"))
                    .map(p->p.getFileName().toString().replaceFirst("\\.json$", ""))
                    .sorted().limit(100).forEach(found::add);}return found;
        }catch(IOException ex){return List.of();}
    }
    /** Saves a full snapshot of the current Lexy config and UI preferences in one profile JSON. */
    public static boolean saveProfile(String name){
        name=safeName(name);if(name.isEmpty())return false;
        try{LexyClient.save();JsonObject data=new JsonObject();data.addProperty("schema",4);
            data.add("ui",object());data.add("client",JsonParser.parseString(Files.readString(MAIN)));
            write(PROFILES.resolve(name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(data));
            activeProfile=name;save();return true;
        }catch(Exception ex){System.err.println("[Lexy] Save profile failed: "+ex);return false;}
    }
    public static boolean loadProfile(String name){
        name=safeName(name);if(name.isEmpty())return false;
        try{Path path=PROFILES.resolve(name+".json");JsonObject data=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            // Validate both trees before overwriting the active config.
            JsonObject target=data.getAsJsonObject("client");JsonObject ui=data.getAsJsonObject("ui");
            if(target==null||ui==null||!target.has("modules"))return false;
            write(MAIN,new GsonBuilder().setPrettyPrinting().create().toJson(target));
            write(PREFS,new GsonBuilder().setPrettyPrinting().create().toJson(ui));
            LexyClient.load();load();activeProfile=name;save();HudRenderer.invalidate();
            return true;
        }catch(Exception ex){System.err.println("[Lexy] Load profile failed: "+ex);return false;}
    }
    public static void resetUi(){index=0;animate=true;dimBackground=true;showTooltips=true;save();}
}