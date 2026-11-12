package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import java.util.*;

/** Renders the exact same HUD during gameplay and while arranging widgets. */
public final class HudRenderer {
    private HudRenderer() {}
    private static final Map<String,Long> lastSamples = new HashMap<>();
    private static final Map<String,String> rateLimited = new HashMap<>();
    public record Widget(String id, String label, String value, int x, int y, int width, int height) {
        public boolean contains(double mx, double my) { return mx>=x && my>=y && mx<x+width && my<y+height; }
    }
    public static float scale(int sw,int sh) {
        float auto = Math.min(1.0f,Math.min(sw/960f,sh/540f));
        return Math.max(.65f,auto*LexyClient.hudScalePercent/100f);
    }
    private static boolean noisy(String label, String value) {
        if (value == null || value.isBlank()) return true;
        String v = value.trim();
        if ("-".equals(v) || "Unavailable".equalsIgnoreCase(v)) return true;
        if ((label.contains("Alert") || label.contains("Warning")) && "OK".equalsIgnoreCase(v)) return true;
        if (label.startsWith("Target") && ("None".equalsIgnoreCase(v) || "-".equals(v))) return true;
        if (label.startsWith("Effect") && "-".equals(v)) return true;
        if (label.equals("Low Health Alert") && "OK".equalsIgnoreCase(v)) return true;
        return false;
    }
    public static List<Widget> layout() {
        MinecraftClient mc=MinecraftClient.getInstance();
        int sw=mc.getWindow().getScaledWidth(), sh=mc.getWindow().getScaledHeight();
        float scale=scale(sw,sh);
        int entryH=Math.max(8,Math.round((LexyClient.compactHud?15:18)*scale));
        List<Widget> widgets=new ArrayList<>();
        int fallbackX=8, fallbackY=8, columnW=0;
        for (LexyClient.Module module:LexyClient.MODULES) {
            if (!module.drawInHud() || !LexyClient.enabled.getOrDefault(module.id(), false)) continue;
            if (!LexyClient.spotifyHud && module.category().equals("Music")) continue;
            String value;
            try {
                long now = System.currentTimeMillis();
                boolean metric = "FPS".equals(module.id()) || "Ping".equals(module.id());
                if (metric && now-lastSamples.getOrDefault(module.id(),0L)<350L && rateLimited.containsKey(module.id())) value=rateLimited.get(module.id());
                else { value=module.value().get();if(metric){rateLimited.put(module.id(),value);lastSamples.put(module.id(),now);} }
            } catch(RuntimeException ex) {value="Unavailable";}
            String label=module.id();
            if (noisy(label, value)) continue;
            if(LexyClient.hudMinimal && !LexyClient.positions.containsKey(label+".xp") && !Set.of("FPS", "Ping", "Health", "Totems", "Low Health Alert").contains(label)) continue;
            String line = label.equals("FPS") || label.equals("Ping") ? label+": "+value : label+"  "+value;
            int w=Math.max(Math.round(60*scale),Math.round((mc.textRenderer.getWidth(line)+21)*scale));
            w=Math.min(sw,w);
            if(fallbackY+entryH>sh-6){fallbackX+=columnW+8;fallbackY=8;columnW=0;}
            if(fallbackX>=sw) break;
            int x,y;
            if(LexyClient.positions.containsKey(label+".xp")) {
                x=Math.round(Math.max(0,Math.min(10000,LexyClient.positions.get(label+".xp")))*(Math.max(0,sw-w)/10000f));
                y=Math.round(Math.max(0,Math.min(10000,LexyClient.positions.getOrDefault(label+".yp",0)))*(Math.max(0,sh-entryH)/10000f));
            } else if(LexyClient.positions.containsKey(label)) {
                // Old 1.5 coordinates are interpreted as virtual 960x540 positions.
                x=Math.round(LexyClient.positions.get(label)*sw/960f);
                y=Math.round(LexyClient.positions.getOrDefault(label+".y",fallbackY)*sh/540f);
            } else {
                x=fallbackX;y=fallbackY;
            }
            x=Math.max(0,Math.min(Math.max(0,sw-w),x));
            y=Math.max(0,Math.min(Math.max(0,sh-entryH),y));
            widgets.add(new Widget(label,label,value,x,y,w,entryH));
            fallbackY+=Math.max(entryH+1,Math.round(LexyClient.hudSpacing*scale));
            columnW=Math.max(columnW,w);
        }
        return widgets;
    }
    public static void place(Widget widget,int x,int y){
        MinecraftClient mc=MinecraftClient.getInstance();
        int sw=mc.getWindow().getScaledWidth(),sh=mc.getWindow().getScaledHeight();
        int safeX=Math.max(0,sw-widget.width()),safeY=Math.max(0,sh-widget.height());
        if(LexyClient.hudGridSnap){
            int grid=Math.max(2,Math.round(LexyClient.hudGridSize*scale(sw,sh)));
            x=Math.round(x/(float)grid)*grid; y=Math.round(y/(float)grid)*grid;
        }
        x=Math.max(0,Math.min(safeX,x));y=Math.max(0,Math.min(safeY,y));
        LexyClient.positions.remove(widget.id());LexyClient.positions.remove(widget.id()+".y");
        LexyClient.positions.put(widget.id()+".xp",safeX==0?0:Math.round(x*10000f/safeX));
        LexyClient.positions.put(widget.id()+".yp",safeY==0?0:Math.round(y*10000f/safeY));
    }
    public static void reset(){LexyClient.positions.clear();LexyClient.save();}
    public static void render(DrawContext c,boolean editor,String selected) {
        MinecraftClient mc=MinecraftClient.getInstance();
        float scale=scale(mc.getWindow().getScaledWidth(),mc.getWindow().getScaledHeight());
        for (Widget w:layout()){
            if(LexyClient.hudBackground || editor)c.fill(w.x(),w.y(),w.x()+w.width(),w.y()+w.height(),
                    ((editor?235:LexyClient.hudOpacity)<<24)|0x151C29);
            c.fill(w.x(),w.y(),w.x()+Math.max(1,Math.round(3*scale)),w.y()+w.height(),LexyClient.accent());
            if(editor){int border=w.id().equals(selected)?0xFF5BB5FF:0xFF364A63;
                c.drawBorder(w.x(),w.y(),w.width(),w.height(),border);}
            int color = LexyClient.hudTextColor==1 ? LexyClient.accent() : LexyClient.hudTextColor==2 ? 0xFFD5DFED : 0xFFF2F7FF;
            if ("Low Health Alert".equals(w.label()) && w.value().contains("!")) color=0xFFFF7289;
            else if ("FPS".equals(w.label())) {
                try {int fps=Integer.parseInt(w.value().trim());color=fps>=60?0xFF96F6C2:fps>=30?0xFFFFD381:0xFFFF8395;}
                catch(NumberFormatException ex){color=0xFF94C7FF;}
            } else if ("Ping".equals(w.label())) {
                try {int ping=Integer.parseInt(w.value().replace("ms","").trim());color=ping<90?0xFF96F6C2:ping<180?0xFFFFD381:0xFFFF8395;}
                catch(NumberFormatException ex){color=0xFF94C7FF;}
            }
            c.getMatrices().push();
            c.getMatrices().translate(w.x()+Math.max(4,Math.round(10*scale)),w.y()+Math.max(1,Math.round((LexyClient.compactHud?3:5)*scale)),0);
            c.getMatrices().scale(scale,scale,1);
            String line = w.label().equals("FPS") || w.label().equals("Ping") ? (w.label()+": "+w.value()) : (w.label()+"  "+w.value());
            if(LexyClient.hudShadow)c.drawTextWithShadow(mc.textRenderer,line,0,0,color);
            else c.drawText(mc.textRenderer,line,0,0,color,false);
            c.getMatrices().pop();
        }
    }
}