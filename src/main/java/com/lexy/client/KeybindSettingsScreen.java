package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Keyboard-first, touch-friendly key and macro editor. Bindings also appear in vanilla Controls. */
public final class KeybindSettingsScreen extends Screen {
    private final Screen parent;
    private int page=0,group=0,waiting=-1;
    private TextFieldWidget[] macros=new TextFieldWidget[4];
    private int top,left,panelWidth;
    public KeybindSettingsScreen(Screen parent){super(Text.literal("LexyClient · Keybinds"));this.parent=parent;}
    private ButtonWidget button(String label,int x,int y,int w,int h,ButtonWidget.PressAction callback) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(label),callback).dimensions(x,y,w,h).build());
    }
    @Override protected void init() {
        left=Math.max(10,(width-Math.min(620,width-20))/2);
        panelWidth=Math.min(620,width-20);top=Math.max(8,(height-330)/2);
        int half=(panelWidth-20)/2;
        button("KEYS",left+8,top+50,half,25,b->{page=0;group=0;waiting=-1;clearAndInit();});
        button("MACROS",left+12+half,top+50,half,25,b->{page=1;waiting=-1;clearAndInit();});
        if(page==0){
            button("‹",left+8,top+258,54,25,b->{group=Math.max(0,group-1);clearAndInit();});
            button("›",left+panelWidth-62,top+258,54,25,b->{group=Math.min(1,group+1);clearAndInit();});
            int start=group*4;
            for(int n=0;n<4;n++){
                final int index=start+n;
                if(index>=LexyKeybinds.all().size())break;
                int rowY=top+88+n*40;
                button(LexyKeybinds.keyName(index),left+panelWidth-158,rowY,142,29,b->{waiting=index;clearAndInit();});
            }
        } else {
            for(int i=0;i<4;i++){
                int y=top+90+i*43;
                TextFieldWidget field=new TextFieldWidget(textRenderer,left+120,y,panelWidth-135,25,Text.literal("Macro "+(i+1)));
                field.setMaxLength(160);field.setText(LexyKeybinds.macro(i));
                final int id=i;field.setChangedListener(value->LexyKeybinds.setMacro(id,value));
                macros[i]=field;addDrawableChild(field);
            }
        }
        button("SAVE / BACK",left+panelWidth-135,top+295,127,24,b->close());
    }
    @Override public void render(DrawContext c,int mx,int my,float dt) {
        renderBackground(c);
        c.fill(0,0,width,height,0xDD080E18);
        c.fill(left-2,top-2,left+panelWidth+2,top+330,0xFF26384F);
        c.fill(left,top,left+panelWidth,top+328,0xFF131D2B);
        c.fill(left,top,left+4,top+328,LexyClient.accent());
        c.drawTextWithShadow(textRenderer,"LEXY / KEYBINDS & MACROS",left+17,top+14,0xFFF0F6FF);
        c.drawTextWithShadow(textRenderer,"Direct controls · All keys are editable from Minecraft Controls too",left+17,top+29,0xFF90A8BF);
        if(page==0){
            int start=group*4;
            for(int n=0;n<4;n++){
                int i=start+n;if(i>=LexyKeybinds.all().size())break;
                int y=top+88+n*40;
                c.fill(left+8,y,left+panelWidth-8,y+31,0xFF1E2D40);
                c.drawTextWithShadow(textRenderer,LexyKeybinds.label(i),left+18,y+10,0xFFE5F2FF);
            }
            c.drawCenteredTextWithShadow(textRenderer,"Group "+(group+1)+" / 2",width/2,top+266,0xFF9CBAD9);
            if(waiting>=0){
                c.fill(left+30,top+191,left+panelWidth-30,top+230,0xFA0D2943);
                c.drawCenteredTextWithShadow(textRenderer,"Press a key · BACKSPACE to clear · ESC to cancel",width/2,top+204,0xFFEAF5FF);
            }
        }else{
            for(int i=0;i<4;i++){
                c.drawTextWithShadow(textRenderer,"MACRO "+(i+1),left+17,top+97+i*43,0xFFA8C7E8);
            }
            c.drawTextWithShadow(textRenderer,"Send a command like /spawn or a single chat message.",left+18,top+271,0xFF9CB6CE);
        }
        super.render(c,mx,my,dt);
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(waiting>=0) {
            if(key==GLFW.GLFW_KEY_ESCAPE){waiting=-1;clearAndInit();return true;}
            LexyKeybinds.setKey(waiting,key==GLFW.GLFW_KEY_BACKSPACE?-1:key,scan);
            waiting=-1;clearAndInit();LexySounds.confirm();return true;
        }
        return super.keyPressed(key,scan,mods);
    }
    @Override public void close(){LexyKeybinds.save();if(client!=null)client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}