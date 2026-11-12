package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;

/** In-world, compact drag-and-drop editor for the actual HUD positions. */
public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private String selected;
    private boolean dragging;
    private int dragDX,dragDY;
    private static final String[] TOOLS={"-","SIZE","+","STYLE","BG","SNAP","GRID"};
    private static final String[] PRESETS={"MINIMAL","COMPACT","EXTENDED","RESET"};
    public HudEditorScreen(Screen parent){super(Text.literal("Lexy / HUD editor"));this.parent=parent;}
    private int widthBar(){return Math.max(110,Math.min(470,width-10));}
    private int left(){return (width-widthBar())/2;}
    private int top(){return Math.max(3,height-91);}
    private int toolW(){return Math.max(20,(widthBar()-14)/TOOLS.length);}
    private int presetW(){return Math.max(35,(widthBar()-14)/PRESETS.length);}
    private boolean hit(double mx,double my,int x,int y,int w,int h){return LexyPaint.hit(mx,my,x,y,w,h);}
    private void btn(DrawContext c,String text,int x,int y,int w,int h,boolean selected){
        LexyPaint.button(c,text,x,y,w,h,selected?0xFF343434:0xFF171717,
                selected?0xFF777777:0xFF363636,selected?0xFFF6F6F6:0xFFB0B0B0,false);
    }
    @Override protected void init(){LexySounds.open();}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0x3A000000);
        if(LexyClient.hudShowGrid){
            int gap=Math.max(9,Math.round(LexyClient.hudGridSize*HudRenderer.scale(width,height)));
            for(int x=0;x<width;x+=gap)c.fill(x,0,x+1,height,0x164A4A4A);
            for(int y=0;y<height;y+=gap)c.fill(0,y,width,y+1,0x164A4A4A);
        }
        HudRenderer.render(c,true,selected);
        int x=left(),y=top(),w=widthBar();
        LexyPaint.rounded(c,x,y,w,87,0xF40B0B0B);
        LexyPaint.border(c,x,y,w,87,0xFF454545);
        LexyPaint.label(c,"HUD EDITOR",x+9,y+7,w-99,0xFFE5E5E5);
        LexyPaint.label(c,selected==null?"DRAG TO MOVE":selected.toUpperCase(),x+94,y+7,w-180,0xFF909090);
        btn(c,"DONE",x+w-56,y+4,49,18,false);
        int tw=toolW();
        for(int i=0;i<TOOLS.length;i++){
            int xx=x+7+i*tw,ww=tw-2;
            String name=switch(i){case 1->LexyClient.hudScalePercent+"%";
                case 3->switch(LexyClient.hudStyle){case 1->"LINE";case 2->"TEXT";default->"FLAT";};
                default->TOOLS[i];};
            boolean active=(i==4&&LexyClient.hudBackground)||(i==5&&LexyClient.hudGridSnap)||(i==6&&LexyClient.hudShowGrid);
            btn(c,name,xx,y+28,ww,22,active);
        }
        int pw=presetW();
        for(int i=0;i<PRESETS.length;i++){
            int xx=x+7+i*pw;
            btn(c,PRESETS[i],xx,y+57,pw-3,21,i<3&&LexyClient.hudPreset==i);
        }
        super.render(c,mx,my,delta);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return super.mouseClicked(mx,my,button);
        int x=left(),y=top(),w=widthBar();
        if(hit(mx,my,x,y,w,87)){
            if(hit(mx,my,x+w-56,y+4,49,18)){close();return true;}
            int tw=toolW();
            for(int i=0;i<TOOLS.length;i++){
                if(!hit(mx,my,x+7+i*tw,y+28,tw-2,22))continue;
                switch(i){
                    case 0->LexyClient.hudScalePercent=Math.max(45,LexyClient.hudScalePercent-5);
                    case 1->LexyClient.hudScalePercent=78;
                    case 2->LexyClient.hudScalePercent=Math.min(140,LexyClient.hudScalePercent+5);
                    case 3->LexyClient.hudStyle=(LexyClient.hudStyle+1)%3;
                    case 4->LexyClient.hudBackground=!LexyClient.hudBackground;
                    case 5->LexyClient.hudGridSnap=!LexyClient.hudGridSnap;
                    case 6->LexyClient.hudShowGrid=!LexyClient.hudShowGrid;
                }
                HudRenderer.invalidate();LexyClient.save();LexySounds.tap();return true;
            }
            int pw=presetW();
            for(int i=0;i<PRESETS.length;i++){
                if(!hit(mx,my,x+7+i*pw,y+57,pw-3,21))continue;
                if(i==3){HudRenderer.reset();selected=null;}
                else{LexyClient.applyHudPreset(i);selected=null;}
                HudRenderer.invalidate();LexyClient.save();LexySounds.navigate();return true;
            }
            return true;
        }
        List<HudRenderer.Widget> widgets=HudRenderer.layout();
        for(int i=widgets.size()-1;i>=0;i--){
            var widget=widgets.get(i);
            if(widget.contains(mx,my)){
                selected=widget.id();dragging=true;
                dragDX=(int)mx-widget.x();dragDY=(int)my-widget.y();return true;
            }
        }
        selected=null;return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        if(dragging&&button==0&&selected!=null){
            for(var widget:HudRenderer.layout()){
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
    @Override public boolean keyPressed(int key,int scancode,int modifiers){
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT||key==GLFW.GLFW_KEY_F8){close();return true;}
        if(key==GLFW.GLFW_KEY_ESCAPE){close();return true;}
        if(selected!=null){
            int step=(modifiers&GLFW.GLFW_MOD_SHIFT)!=0?5:1;
            int dx=key==GLFW.GLFW_KEY_LEFT?-step:key==GLFW.GLFW_KEY_RIGHT?step:0;
            int dy=key==GLFW.GLFW_KEY_UP?-step:key==GLFW.GLFW_KEY_DOWN?step:0;
            if(dx!=0||dy!=0){
                for(var w:HudRenderer.layout())if(w.id().equals(selected)){
                    HudRenderer.place(w,w.x()+dx,w.y()+dy);LexyClient.save();break;
                }
                return true;
            }
        }
        return super.keyPressed(key,scancode,modifiers);
    }
    @Override public boolean keyReleased(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT||key==GLFW.GLFW_KEY_F8)LexyClient.menuShiftReleased();
        return super.keyReleased(key,scan,mods);
    }
    @Override public void close(){
        LexyClient.suppressMenuToggle();LexySounds.close();LexyClient.save();
        if(client!=null)client.setScreen(parent);
    }
    @Override public boolean shouldPause(){return false;}
}