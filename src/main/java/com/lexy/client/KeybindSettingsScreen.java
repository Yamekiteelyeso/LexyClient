package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Native, game-scaled keybind and macro settings. Every visible button is actionable. */
public final class KeybindSettingsScreen extends Screen {
    private static final int BACKGROUND=0xFF0B1421, PANEL=0xFF152337, SURFACE=0xFF23384E;
    private static final int BORDER=0xFF3A5470, TEXT=0xFFF0F7FF, MUTED=0xFF95AFC8;
    private final Screen parent;
    private int page=0,group=0,waiting=-1;
    private final TextFieldWidget[] macros=new TextFieldWidget[4];
    private int top,left,panelWidth;
    private float viewScale = 1f;
    private double logical(double coordinate){return coordinate / viewScale;}

    public KeybindSettingsScreen(Screen parent){super(Text.literal("LexyClient / Keybinds"));this.parent=parent;}
    private int half(){return (panelWidth-28)/2;}
    private int totalGroups(){return Math.max(1,(LexyKeybinds.all().size()+3)/4);}
    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private void fill(DrawContext c,int x,int y,int w,int h,int color){c.fill(x,y,x+w,y+h,color);}
    private void frame(DrawContext c,int x,int y,int w,int h,int color){
        fill(c,x,y,w,1,color);fill(c,x,y+h-1,w,1,color);fill(c,x,y,1,h,color);fill(c,x+w-1,y,1,h,color);
    }
    private void label(DrawContext c,String name,int x,int y,int max,int color){
        if(max<=0)return;
        while(name.length()>2&&textRenderer.getWidth(name)>max)name=name.substring(0,name.length()-2)+"…";
        c.drawTextWithShadow(textRenderer,name,x,y,color);
    }
    private void control(DrawContext c,String name,int x,int y,int w,int h,int mx,int my,boolean selected){
        boolean hover=inside(mx,my,x,y,w,h);
        fill(c,x,y,w,h,selected?0xFF214E78:(hover?0xFF2A435E:SURFACE));
        frame(c,x,y,w,h,selected?LexyClient.accent():BORDER);
        label(c,name,x+10,y+(h-9)/2,w-20,TEXT);
    }
    @Override protected void init(){
        // Keep the *entire* keybind editor on-screen even at Minecraft GUI scale 4 or on Pojav.
        viewScale = Math.max(.25f, Math.min(1f,
                Math.min((width - 12f) / 690f, (height - 12f) / 358f)));
        int vw = Math.round(width / viewScale), vh = Math.round(height / viewScale);
        panelWidth=Math.min(670,Math.max(300,vw-24));
        left=(vw-panelWidth)/2;
        top=Math.max(7,(vh-346)/2);
        if(page==1){
            for(int i=0;i<4;i++){
                int y=top+100+i*42;
                TextFieldWidget field=new TextFieldWidget(textRenderer,left+124,y,panelWidth-143,26,Text.literal("Macro "+(i+1)));
                field.setMaxLength(160);field.setText(LexyKeybinds.macro(i));
                final int index=i;
                field.setChangedListener(value->LexyKeybinds.setMacro(index,value));
                macros[i]=field;addDrawableChild(field);
            }
        }
    }
    @Override public void render(DrawContext c,int mx,int my,float dt){
        renderBackground(c);
        c.fillGradient(0,0,width,height,0xEE050D18,0xEF102035);
        c.getMatrices().push();
        c.getMatrices().scale(viewScale,viewScale,1);
        fill(c,left-7,top-7,panelWidth+14,352,0x80000000);
        fill(c,left-2,top-2,panelWidth+4,342,BORDER);
        fill(c,left,top,panelWidth,338,BACKGROUND);
        c.fillGradient(left+2,top+2,left+panelWidth-2,top+338,0xFF17283B,0xFF0C1523);
        fill(c,left,top,Math.min(184,panelWidth/3),3,LexyClient.accent());
        label(c,"LEXY CLIENT    /    CONTROL CENTER",left+17,top+16,panelWidth-34,TEXT);
        label(c,"Assign keys, manage macros and keep your settings synchronized",left+17,top+33,panelWidth-34,MUTED);
        int hw=half();
        control(c,"KEYBINDS",left+12,top+58,hw,29,mx,my,page==0);
        control(c,"MACROS",left+16+hw,top+58,hw,29,mx,my,page==1);
        if(page==0){
            for(int i=0;i<4;i++){
                int index=group*4+i;if(index>=LexyKeybinds.all().size())break;
                int rowY=top+99+i*41;
                fill(c,left+12,rowY,panelWidth-24,34,PANEL);
                fill(c,left+12,rowY+5,3,24,LexyClient.accent());
                label(c,LexyKeybinds.label(index),left+24,rowY+12,panelWidth-210,TEXT);
                control(c,LexyKeybinds.keyName(index),left+panelWidth-171,rowY+3,155,28,mx,my,waiting==index);
            }
            control(c,"< PREVIOUS",left+12,top+271,109,26,mx,my,false);
            control(c,"NEXT >",left+panelWidth-121,top+271,109,26,mx,my,false);
            label(c,"GROUP "+(group+1)+" / "+totalGroups(),left+panelWidth/2-38,top+280,78,MUTED);
            if(waiting>=0){
                fill(c,left+28,top+207,panelWidth-56,42,0xFF0C304E);
                frame(c,left+28,top+207,panelWidth-56,42,LexyClient.accent());
                label(c,"PRESS A KEY  |  BACKSPACE = CLEAR  |  ESC = CANCEL",
                        left+41,top+223,panelWidth-82,TEXT);
            }
        }else{
            for(int i=0;i<4;i++){
                int y=top+100+i*42;
                fill(c,left+12,y-3,panelWidth-24,32,PANEL);
                fill(c,left+12,y+5,3,17,LexyClient.accent());
                label(c,"MACRO "+(i+1),left+24,y+9,92,0xFFB8D6F3);
            }
            label(c,"One command or chat message per key  /  Example: /home",left+17,top+281,panelWidth-35,MUTED);
        }
        fill(c,left+12,top+308,panelWidth-24,1,BORDER);
        label(c,"RSHIFT: CLOSE MENU   ·   ESC: BACK",left+17,top+318,panelWidth-166,0xFF85A1BC);
        control(c,"SAVE / BACK",left+panelWidth-150,top+309,136,26,mx,my,false);
        super.render(c,(int)logical(mx),(int)logical(my),dt);
        c.getMatrices().pop();
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        mx=logical(mx);my=logical(my);
        if(button==0){
            int hw=half();
            if(inside(mx,my,left+12,top+58,hw,29)){
                page=0;waiting=-1;group=0;LexySounds.navigate();clearAndInit();return true;
            }
            if(inside(mx,my,left+16+hw,top+58,hw,29)){
                page=1;waiting=-1;LexySounds.navigate();clearAndInit();return true;
            }
            if(inside(mx,my,left+panelWidth-150,top+309,136,26)){
                LexySounds.save();close();return true;
            }
            if(page==0&&waiting<0){
                if(inside(mx,my,left+12,top+271,109,26)){
                    group=Math.max(0,group-1);LexySounds.navigate();clearAndInit();return true;
                }
                if(inside(mx,my,left+panelWidth-121,top+271,109,26)){
                    group=Math.min(totalGroups()-1,group+1);LexySounds.navigate();clearAndInit();return true;
                }
                for(int i=0;i<4;i++){
                    int index=group*4+i;if(index>=LexyKeybinds.all().size())break;
                    if(inside(mx,my,left+panelWidth-171,top+102+i*41,155,28)){
                        waiting=index;LexySounds.tap();return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseScrolled(double mx,double my,double amount){
        mx=logical(mx);my=logical(my);
        if(page==0&&amount!=0){
            group=Math.max(0,Math.min(totalGroups()-1,group-(int)Math.signum(amount)));
            LexySounds.navigate();return true;
        }
        return super.mouseScrolled(mx,my,amount);
    }
    @Override public void mouseMoved(double mx,double my){super.mouseMoved(logical(mx),logical(my));}
    @Override public boolean mouseReleased(double mx,double my,int button){return super.mouseReleased(logical(mx),logical(my),button);}
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        return super.mouseDragged(logical(mx),logical(my),button,logical(dx),logical(dy));
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT){
            if(LexyClient.allowMenuToggle()){
                LexyKeybinds.save();
                if(parent instanceof LexyScreen menu)menu.close();else close();
            }
            return true;
        }
        if(waiting>=0){
            if(key==GLFW.GLFW_KEY_ESCAPE){waiting=-1;LexySounds.navigate();return true;}
            LexyKeybinds.setKey(waiting,key==GLFW.GLFW_KEY_BACKSPACE?-1:key,scan);
            waiting=-1;LexySounds.confirm();return true;
        }
        if(key==GLFW.GLFW_KEY_ESCAPE){close();return true;}
        return super.keyPressed(key,scan,mods);
    }
    @Override public boolean keyReleased(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT)LexyClient.menuShiftReleased();
        return super.keyReleased(key,scan,mods);
    }
    @Override public void close(){LexyKeybinds.save();LexySounds.close();if(client!=null)client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
