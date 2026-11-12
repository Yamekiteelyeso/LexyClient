package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import java.util.*;

/** Renders the exact same HUD during gameplay and while arranging widgets. */
public final class HudRenderer {
    private HudRenderer() {}
    public record Widget(String id, String label, String value, int x, int y, int width, int height) {
        public boolean contains(double mx, double my) { return mx>=x && my>=y && mx<x+width && my<y+height; }
    }
    public static float scale(int sw,int sh) {
        float auto = Math.min(1.0f,Math.min(sw/960f,sh/540f));
        return Math.max(.25f,auto*LexyClient.hudScalePercent/100f);
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
            try {value=module.value().get();} catch(RuntimeException ex) {value="Unavailable";}
            String label=module.id();
            int w=Math.max(Math.round(75*scale),Math.round((mc.textRenderer.getWidth(label+"  "+value)+21)*scale));
            w=Math.min(sw,w);
            if(fallbackY+entryH>sh-6){fallbackX+=columnW+8;fallbackY=8;columnW=0;}
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
            int color=w.label().equals("Low Health Alert")&&w.value().contains("!")?0xFFFF7687:
                    LexyClient.hudTextColor==1?LexyClient.accent():LexyClient.hudTextColor==2?0xFFD5DFED:0xFFF2F7FF;
            c.getMatrices().push();
            c.getMatrices().translate(w.x()+Math.max(4,Math.round(10*scale)),w.y()+Math.max(1,Math.round((LexyClient.compactHud?3:5)*scale)),0);
            c.getMatrices().scale(scale,scale,1);
            if(LexyClient.hudShadow)c.drawTextWithShadow(mc.textRenderer,w.label()+"  "+w.value(),0,0,color);
            else c.drawText(mc.textRenderer,w.label()+"  "+w.value(),0,0,color,false);
            c.getMatrices().pop();
        }
    }
}