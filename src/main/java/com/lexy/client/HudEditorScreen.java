package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;

/** Direct HUD editor with touch-friendly controls, presets and proportionally saved positions. */
public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private String selected;
    private boolean dragging;
    private int dragDX, dragDY;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("LexyClient - HUD Editor"));
        this.parent=parent;
    }

    private int barWidth(){return Math.min(520, Math.max(160, width-12));}
    private int barLeft(){return (width-barWidth())/2;}
    private int barTop(){return Math.max(2,height-102);}
    private boolean hit(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private int cellWidth(){return (barWidth()-16)/5;}
    private int presetWidth(){return (barWidth()-16)/3;}

    @Override protected void init(){LexySounds.open();}
    @Override public void render(DrawContext ctx,int mx,int my,float delta){
        ctx.fill(0,0,width,height,0x33050916);
        if(LexyClient.hudShowGrid){
            int grid=Math.max(12,Math.round(LexyClient.hudGridSize*HudRenderer.scale(width,height)));
            for(int x=0;x<width;x+=grid)ctx.fill(x,0,x+1,height,0x1E4881A4);
            for(int y=0;y<height;y+=grid)ctx.fill(0,y,width,y+1,0x1E4881A4);
        }
        HudRenderer.render(ctx,true,selected);
        int x=barLeft(),y=barTop(),w=barWidth();
        ctx.fill(x,y,x+w,y+92,0xF1101B2B);
        ctx.fill(x,y,x+w,y+3,LexyClient.accent());
        ctx.drawTextWithShadow(textRenderer,"LEXY  /  HUD STUDIO",x+8,y+8,0xFFEAF3FF);
        ctx.fill(x+w-59,y+5,x+w-7,y+25,0xFF225B89);
        ctx.drawCenteredTextWithShadow(textRenderer,"DONE",x+w-33,y+11,0xFFF6FAFF);
        ctx.drawTextWithShadow(textRenderer,"Drag widgets  |  Zoom "+LexyClient.hudScalePercent+"%",x+8,y+23,0xFFB5C6D9);
        String[] controls={"- SIZE","+ SIZE","FPS","PING","RESET"};
        for(int i=0;i<controls.length;i++){
            int cx=x+8+i*cellWidth(),cw=cellWidth()-3;
            boolean on=(i==2&&LexyClient.enabled.getOrDefault("FPS",false))||(i==3&&LexyClient.enabled.getOrDefault("Ping",false));
            ctx.fill(cx,y+37,cx+cw,y+57,on?0xFF285D86:0xFF22354D);
            ctx.drawCenteredTextWithShadow(textRenderer,controls[i],cx+cw/2,y+43,0xFFF3F7FF);
        }
        String[] presets={"MINIMAL","NORMAL","EXTENDED"};
        for(int i=0;i<presets.length;i++){
            int cx=x+8+i*presetWidth(),cw=presetWidth()-3;
            ctx.fill(cx,y+65,cx+cw,y+85,LexyClient.hudPreset==i?0xFF2D71AA:0xFF21374F);
            ctx.drawCenteredTextWithShadow(textRenderer,presets[i],cx+cw/2,y+71,0xFFF3F7FF);
        }
        super.render(ctx,mx,my,delta);
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return super.mouseClicked(mx,my,button);
        LexySounds.tap();
        int x=barLeft(),y=barTop(),w=barWidth();
        if(hit(mx,my,x,y,w,92)){
            if(hit(mx,my,x+w-59,y+5,52,20)){close();return true;}
            if(hit(mx,my,x+8,y+37,cellWidth()*5,20)){
                int i=Math.min(4,(int)(mx-x-8)/Math.max(1,cellWidth()));
                switch(i){
                    case 0 -> LexyClient.hudScalePercent=Math.max(45,LexyClient.hudScalePercent-5);
                    case 1 -> LexyClient.hudScalePercent=Math.min(150,LexyClient.hudScalePercent+5);
                    case 2 -> LexyClient.enabled.put("FPS",!LexyClient.enabled.getOrDefault("FPS",false));
                    case 3 -> LexyClient.enabled.put("Ping",!LexyClient.enabled.getOrDefault("Ping",false));
                    case 4 -> HudRenderer.reset();
                }
                HudRenderer.invalidate();LexyClient.save();LexySounds.toggle();return true;
            }
            if(hit(mx,my,x+8,y+65,presetWidth()*3,20)){
                int i=Math.min(2,(int)(mx-x-8)/Math.max(1,presetWidth()));
                LexyClient.applyHudPreset(i);LexySounds.navigate();selected=null;return true;
            }
            return true;
        }
        List<HudRenderer.Widget> widgets=HudRenderer.layout();
        for(int i=widgets.size()-1;i>=0;i--){
            HudRenderer.Widget widget=widgets.get(i);
            if(widget.contains(mx,my)){
                selected=widget.id();dragging=true;
                dragDX=(int)mx-widget.x();dragDY=(int)my-widget.y();return true;
            }
        }
        selected=null;
        return super.mouseClicked(mx,my,button);
    }

    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        if(dragging&&button==0&&selected!=null){
            for(HudRenderer.Widget widget:HudRenderer.layout()){
                if(widget.id().equals(selected)){
                    HudRenderer.place(widget,(int)mx-dragDX,(int)my-dragDY);return true;
                }
            }
        }
        return super.mouseDragged(mx,my,button,dx,dy);
    }

    @Override public boolean mouseReleased(double mx,double my,int button){
        if(dragging){dragging=false;LexyClient.save();return true;}
        return super.mouseReleased(mx,my,button);
    }

    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        if(keyCode==GLFW.GLFW_KEY_RIGHT_SHIFT){
            if(LexyClient.allowMenuToggle()){
                LexyClient.save();
                if(parent instanceof LexyScreen menu)menu.close();
                else close();
            }
            return true;
        }
        if(keyCode==GLFW.GLFW_KEY_ESCAPE){close();return true;}
        return super.keyPressed(keyCode,scanCode,modifiers);
    }

    @Override public boolean keyReleased(int keyCode,int scanCode,int modifiers){
        if(keyCode==GLFW.GLFW_KEY_RIGHT_SHIFT)LexyClient.menuShiftReleased();
        return super.keyReleased(keyCode,scanCode,modifiers);
    }
    @Override public void close(){LexySounds.close();LexyClient.save();if(client!=null)client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
