package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.*;

/** Compact, monochrome HUD. Layout and editor share exactly the same coordinates. */
public final class HudRenderer {
    private HudRenderer() {}
    private static final Map<String,Long> lastSamples=new HashMap<>();
    private static final Map<String,String> recentValues=new HashMap<>();
    private static final Set<String> MINIMAL=Set.of("FPS","Ping","Health","Totems","Low Health Alert");
    private static final Set<String> GAME_HUD=Set.of(
            "FPS","Ping","Health","Absorption","Food","Totems","Golden Apples","Ender Pearls",
            "Mainhand Durability","Offhand Item","Speed","XYZ X","XYZ Y","XYZ Z","Facing",
            "Low Health Alert","Low Helmet Alert","Low Chest Alert","Low Legs Alert","Low Boots Alert",
            "Totem Stock Warning","Biome","Server","Time","Memory","Free Slots",
            "Effect 1","Effect 2","Effect 3","Effect 4");
    private static List<Widget> cache=List.of();
    private static long cacheAt;
    private static int cacheW,cacheH,cacheState;
    public record Widget(String id,String label,String value,int x,int y,int width,int height) {
        public boolean contains(double mx,double my){return mx>=x&&mx<x+width&&my>=y&&my<y+height;}
    }
    public static void invalidate(){cacheAt=0;cache=List.of();}
    public static boolean isRelevant(LexyClient.Module m){
        return m.drawInHud() && GAME_HUD.contains(m.id()) &&
                (m.category().equals("HUD")||m.category().equals("Utilities"));
    }
    /** Coordinates are Minecraft scaled pixels, so do not scale twice for HiDPI. */
    public static float scale(int sw,int sh){
        float fit=Math.min(1.0f,Math.max(.78f,Math.min(sw/700f,sh/400f)));
        return Math.max(.38f,Math.min(1.5f,fit*LexyClient.hudScalePercent/100f));
    }
    private static String title(String id){return switch(id){
        case "Health"->"HP";case "Ping"->"PING";case "Totems"->"TOTEM";
        case "Mainhand Durability"->"DURA";case "Golden Apples"->"GAPS";
        case "Ender Pearls"->"PEARLS";case "Absorption"->"ABS";
        case "Low Health Alert"->"LOW HP";case "Totem Stock Warning"->"TOTEMS LOW";
        default->id.toUpperCase(Locale.ROOT);
    };}
    private static boolean hidden(String id,String value){
        if(value==null||value.isBlank()||value.equals("-")||value.equalsIgnoreCase("Unavailable"))return true;
        if((id.contains("Alert")||id.contains("Warning"))&&value.equalsIgnoreCase("OK"))return true;
        return id.startsWith("Effect")&&value.equals("-");
    }
    private static String metric(LexyClient.Module m){
        long now=System.currentTimeMillis();String id=m.id();
        long interval=(id.equals("FPS")||id.equals("Ping"))?350:125;
        if(now-lastSamples.getOrDefault(id,0L)<interval&&recentValues.containsKey(id))return recentValues.get(id);
        String value;
        try{value=m.value().get();}catch(RuntimeException e){value="-";}
        recentValues.put(id,value);lastSamples.put(id,now);return value;
    }
    public static List<Widget> layout(){
        MinecraftClient mc=MinecraftClient.getInstance();
        int sw=mc.getWindow().getScaledWidth(),sh=mc.getWindow().getScaledHeight();
        long now=System.nanoTime()/1_000_000L;
        int state=Objects.hash(LexyClient.hudScalePercent,LexyClient.compactHud,LexyClient.hudSpacing,
                LexyClient.maxHudWidgets,LexyClient.hudMinimal,LexyClient.hudStyle,
                LexyClient.enabled.hashCode(),LexyClient.positions.hashCode());
        if(now-cacheAt<125&&cacheW==sw&&cacheH==sh&&cacheState==state)return cache;
        float factor=scale(sw,sh);
        int entryH=Math.max(9,Math.round((LexyClient.compactHud?13:17)*factor+2));
        List<Widget> result=new ArrayList<>(Math.min(LexyClient.maxHudWidgets,10));
        int nextX=6,nextY=6,colW=0;
        for(LexyClient.Module module:LexyClient.MODULES){
            if(!isRelevant(module)||!LexyClient.enabled.getOrDefault(module.id(),false))continue;
            String id=module.id();
            if(LexyClient.hudMinimal&&!LexyClient.positions.containsKey(id+".xp")&&!MINIMAL.contains(id))continue;
            String value=metric(module);if(hidden(id,value))continue;
            String label=title(id);
            // Text uses two tones; calculate the actual width instead of a giant 60px minimum.
            int measured=mc.textRenderer.getWidth(label)+mc.textRenderer.getWidth(value)+9;
            int width=Math.min(sw,Math.max(28,Math.round(measured*factor)+10));
            if(nextY+entryH>sh-5){nextX+=colW+6;nextY=6;colW=0;}
            if(nextX>=sw)break;
            int x=nextX,y=nextY;
            if(LexyClient.positions.containsKey(id+".xp")){
                x=Math.round(Math.max(0,Math.min(10000,LexyClient.positions.get(id+".xp")))*Math.max(0,sw-width)/10000f);
                y=Math.round(Math.max(0,Math.min(10000,LexyClient.positions.getOrDefault(id+".yp",0)))*Math.max(0,sh-entryH)/10000f);
            }else if(LexyClient.positions.containsKey(id)){
                x=Math.round(LexyClient.positions.get(id)*sw/960f);
                y=Math.round(LexyClient.positions.getOrDefault(id+".y",nextY)*sh/540f);
            }
            x=Math.max(0,Math.min(Math.max(0,sw-width),x));
            y=Math.max(0,Math.min(Math.max(0,sh-entryH),y));
            result.add(new Widget(id,label,value,x,y,width,entryH));
            if(result.size()>=LexyClient.maxHudWidgets)break;
            nextY+=Math.max(entryH+2,Math.round(LexyClient.hudSpacing*factor));
            colW=Math.max(colW,width);
        }
        cache=List.copyOf(result);cacheAt=now;cacheW=sw;cacheH=sh;cacheState=state;
        return cache;
    }
    public static void place(Widget w,int x,int y){
        MinecraftClient mc=MinecraftClient.getInstance();
        int sw=mc.getWindow().getScaledWidth(),sh=mc.getWindow().getScaledHeight();
        int maxX=Math.max(0,sw-w.width()),maxY=Math.max(0,sh-w.height());
        if(LexyClient.hudGridSnap){int g=Math.max(2,Math.round(LexyClient.hudGridSize*scale(sw,sh)));
            x=Math.round(x/(float)g)*g;y=Math.round(y/(float)g)*g;}
        x=Math.max(0,Math.min(maxX,x));y=Math.max(0,Math.min(maxY,y));
        LexyClient.positions.remove(w.id());LexyClient.positions.remove(w.id()+".y");
        LexyClient.positions.put(w.id()+".xp",maxX==0?0:Math.round(x*10000f/maxX));
        LexyClient.positions.put(w.id()+".yp",maxY==0?0:Math.round(y*10000f/maxY));
        invalidate();
    }
    public static void reset(){LexyClient.positions.clear();invalidate();LexyClient.save();}
    public static void render(DrawContext c,boolean editor,String selected){
        MinecraftClient mc=MinecraftClient.getInstance();
        float factor=scale(mc.getWindow().getScaledWidth(),mc.getWindow().getScaledHeight());
        for(Widget w:layout()){
            int x=w.x(),y=w.y(),ww=w.width(),hh=w.height();
            boolean background=editor||(LexyClient.hudBackground&&LexyClient.hudStyle!=2);
            if(background){
                int alpha=editor?215:LexyClient.hudOpacity;
                c.fill(x,y,x+ww,y+hh,(alpha<<24)|0x0B0B0C);
                if((LexyClient.hudStyle==1||editor)&&ww>3&&hh>3)
                    c.drawBorder(x,y,ww,hh,w.id().equals(selected)?0xFFE0E0E0:0xFF424242);
            }
            String label=w.label(),value=w.value();
            int labelColor=LexyClient.hudTextColor==2?0xFFB2B2B2:0xFF929292;
            int valueColor=LexyClient.hudTextColor==1?0xFFD2D2D2:LexyClient.hudTextColor==2?0xFFBDBDBD:0xFFF1F1F1;
            if(w.id().contains("Alert")||w.id().contains("Warning"))valueColor=0xFFFFFFFF;
            int pad=Math.max(3,Math.round(5*factor));
            int top=Math.max(0,(hh-Math.round(9*factor))/2);
            c.enableScissor(x,y,x+ww,y+hh);
            c.getMatrices().push();c.getMatrices().translate(x+pad,y+top,0);c.getMatrices().scale(factor,factor,1);
            if(LexyClient.hudShadow)c.drawTextWithShadow(mc.textRenderer,label,0,0,labelColor);
            else c.drawText(mc.textRenderer,label,0,0,labelColor,false);
            int vx=mc.textRenderer.getWidth(label)+7;
            if(LexyClient.hudShadow)c.drawTextWithShadow(mc.textRenderer,value,vx,0,valueColor);
            else c.drawText(mc.textRenderer,value,vx,0,valueColor,false);
            c.getMatrices().pop();c.disableScissor();
        }
    }
}