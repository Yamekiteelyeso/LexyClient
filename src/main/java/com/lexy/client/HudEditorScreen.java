package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** In-world WYSIWYG HUD positioning, not a miniature preview. */
public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private String selected=null;
    private boolean dragging=false;
    private int dragDX, dragDY;
    public HudEditorScreen(Screen parent){super(Text.literal("LexyClient - Mover HUD"));this.parent=parent;}
    private int barWidth(){return Math.min(520,width-12);}
    private boolean hit(double mx,double my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    @Override public void render(DrawContext ctx,int mx,int my,float delta){
        ctx.fill(0,0,width,height,0x48050C17);
        // Grid and guides remain visible while editing.
        int grid=Math.max(10,Math.round(LexyClient.hudGridSize*HudRenderer.scale(width,height)));
        if(LexyClient.hudShowGrid){
            for(int x=0;x<width;x+=grid)ctx.fill(x,0,x+1,height,0x253A6389);
            for(int y=0;y<height;y+=grid)ctx.fill(0,y,width,y+1,0x253A6389);
        }
        HudRenderer.render(ctx,true,selected);
        int barW=barWidth(),left=(width-barW)/2;
        ctx.fill(left,6,left+barW,58,0xF3121925);
        ctx.fill(left,6,left+barW,8,LexyClient.accent());
        ctx.drawTextWithShadow(textRenderer,"LexyClient | MOVER HUD",left+9,14,0xFFF5F8FF);
        ctx.drawTextWithShadow(textRenderer,"Arrastra FPS, Ping y otros widgets | Zoom: "+LexyClient.hudScalePercent+"%",left+9,27,0xFFA6B8CB);
        int bw=Math.max(40,(barW-16)/5);
        String[] buttons={"-","+","FPS","PING","RESET"};
        for(int i=0;i<5;i++){
            int x=left+8+i*bw,y=40;
            ctx.fill(x,y,x+bw-4,55,(i==2&&LexyClient.enabled.getOrDefault("FPS",false)||i==3&&LexyClient.enabled.getOrDefault("Ping",false))?0xFF254F80:0xFF2B3546);
            ctx.drawCenteredTextWithShadow(textRenderer,buttons[i],x+(bw-4)/2,y+4,0xFFF5F8FF);
        }
        ctx.drawCenteredTextWithShadow(textRenderer,"ESC o SHIFT IZQUIERDO: guardar y cerrar",width/2,height-12,0xFFAFCDF0);
        super.render(ctx,mx,my,delta);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return super.mouseClicked(mx,my,button);
        int bw=Math.max(40,(barWidth()-16)/5),left=(width-barWidth())/2;
        if(hit(mx,my,left+8,40,5*bw,16)){
            int i=(int)((mx-left-8)/bw);
            switch(i){
                case 0 -> LexyClient.hudScalePercent=Math.max(45,LexyClient.hudScalePercent-5);
                case 1 -> LexyClient.hudScalePercent=Math.min(150,LexyClient.hudScalePercent+5);
                case 2 -> LexyClient.enabled.put("FPS",!LexyClient.enabled.getOrDefault("FPS",false));
                case 3 -> LexyClient.enabled.put("Ping",!LexyClient.enabled.getOrDefault("Ping",false));
                case 4 -> HudRenderer.reset();
            }
            LexyClient.save();return true;
        }
        List<HudRenderer.Widget> widgets=HudRenderer.layout();
        for(int i=widgets.size()-1;i>=0;i--){
            HudRenderer.Widget w=widgets.get(i);
            if(w.contains(mx,my)){
                selected=w.id();dragging=true;dragDX=(int)mx-w.x();dragDY=(int)my-w.y();return true;
            }
        }
        selected=null;return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        if(dragging&&button==0&&selected!=null){
            for(HudRenderer.Widget w:HudRenderer.layout()){
                if(w.id().equals(selected)){HudRenderer.place(w,(int)mx-dragDX,(int)my-dragDY);return true;}
            }
        }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button){
        if(dragging){dragging=false;LexyClient.save();return true;}
        return super.mouseReleased(mx,my,button);
    }
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        if(keyCode==GLFW.GLFW_KEY_LEFT_SHIFT||keyCode==GLFW.GLFW_KEY_ESCAPE){close();return true;}
        return super.keyPressed(keyCode,scanCode,modifiers);
    }
    @Override public void close(){LexyClient.save();if(client!=null)client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}