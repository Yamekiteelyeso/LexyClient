package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Native Minecraft title alternative, with original stylized procedural night background. */
public final class LexyHomeScreen extends Screen {
    private static final String[] BUTTONS={"SINGLEPLAYER","MULTIPLAYER","REALMS","MODS","LEXY SETTINGS","PROFILES","MINECRAFT OPTIONS","QUIT GAME"};
    private final Screen fallback;
    private int x,y,w,buttonW,buttonH,columns,firstY;
    private int scroll;
    public LexyHomeScreen(Screen vanilla){super(Text.literal("LexyClient 4.0"));this.fallback=vanilla;}
    @Override protected void init(){
        w=Math.min(560,Math.max(140,width-20));x=(width-w)/2;
        columns=width>=590?2:1;buttonW=(w-(columns-1)*12)/columns;
        buttonH=Math.max(23,Math.min(33,height/15));
        firstY=Math.max(100,height/2-82);
        System.out.println("[LexyClient 4.0.0] Custom title screen initialized "+width+"x"+height);
    }
    private int cardX(int i){return x+(i%columns)*(buttonW+12);}
    private int cardY(int i){return firstY+(i/columns-scroll)*(buttonH+10);}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        var theme=LexyTheme.palette();
        c.fillGradient(0,0,width,height,0xFF040912,0xFF13283D);
        // Original parallax-style layered backdrop: no GPU shaders or disk textures.
        long t=LexyTheme.animate?System.currentTimeMillis()/60:0;
        for(int i=0;i<34;i++){
            int starX=(int)((i*193L+41)%Math.max(1,width));
            int starY=(int)((i*79L+17)%Math.max(1,height*2/3));
            int size=(i%7==0)?2:1;int brightness=70+(int)((t+i*37)%30);
            LexyPaint.rect(c,starX,starY,size,size,(brightness<<24)|0xCDEAFF);
        }
        c.fillGradient(0,height*2/3,width,height,0xA5091729,0xFD05090E);
        int moonX=width*3/4,moonY=height/4;
        LexyPaint.rounded(c,moonX-26,moonY-26,52,52,0x1A9BCDF4);
        LexyPaint.rounded(c,moonX-16,moonY-16,32,32,0x95DDEAFF);
        LexyPaint.rounded(c,moonX-8,moonY-20,23,32,0xFF1A3047);
        // Header and branding visible without depending on world rendering.
        int headerY=Math.max(12,firstY-83);
        c.drawTexture(LexyClient.LOGO,width/2-25,headerY,50,50,0,0,256,256,256,256);
        c.drawCenteredTextWithShadow(textRenderer,"LEXY CLIENT",width/2,headerY+52,LexyPaint.WHITE);
        c.drawCenteredTextWithShadow(textRenderer,"ULTIMATE EDITION   •   4.0.0",width/2,headerY+67,theme.accent());
        int top=Math.max(0,firstY-2),bottom=Math.max(top+buttonH,height-36);
        c.enableScissor(x,top,x+w,bottom);
        for(int i=0;i<BUTTONS.length;i++){
            int xx=cardX(i),yy=cardY(i);
            if(yy+buttonH<top||yy>bottom)continue;
            boolean hover=LexyPaint.hit(mx,my,xx,yy,buttonW,buttonH);
            LexyPaint.rounded(c,xx,yy,buttonW,buttonH,hover?theme.accentSoft():0xDB111D2E);
            LexyPaint.border(c,xx,yy,buttonW,buttonH,hover?theme.accent():theme.border());
            LexyPaint.icon(c,(i+1)%16,xx+9,yy+(buttonH-19)/2,19);
            LexyPaint.label(c,BUTTONS[i],xx+34,yy+(buttonH-8)/2,buttonW-44,LexyPaint.WHITE);
        }
        c.disableScissor();
        int footerY=height-21;
        LexyPaint.label(c,"Minecraft 1.20.1  •  Fabric  •  Press F8 to open modules",12,footerY,Math.max(40,width-20),LexyPaint.MUTED);
        LexyPaint.rounded(c,width-25,6,20,20,0xFF3B232D);
        LexyPaint.label(c,"X",width-18,12,15,LexyPaint.WHITE);
        super.render(c,mx,my,delta);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return super.mouseClicked(mx,my,button);
        if(LexyPaint.hit(mx,my,width-27,3,27,28)){TitleBranding.openVanilla(client);return true;}
        for(int i=0;i<BUTTONS.length;i++)if(LexyPaint.hit(mx,my,cardX(i),cardY(i),buttonW,buttonH)
                &&cardY(i)>firstY-3&&cardY(i)+buttonH<=height-33){activate(i);return true;}
        return super.mouseClicked(mx,my,button);
    }
    private void activate(int id){
        LexySounds.tap();
        switch(id){
            case 0 -> client.setScreen(new SelectWorldScreen(this));
            case 1 -> client.setScreen(new MultiplayerScreen(this));
            case 2 -> {
                try{
                    Class<?> cls=Class.forName("net.minecraft.client.realms.gui.screen.RealmsMainScreen");
                    java.lang.reflect.Constructor<?> con=cls.getConstructor(Screen.class);
                    client.setScreen((Screen)con.newInstance(this));
                }catch(Exception ex){TitleBranding.openVanilla(client);}
            }
            case 3 -> client.setScreen(new LexyModernScreen(this,"Dashboard"));
            case 4 -> client.setScreen(new LexyModernScreen(this,"Settings"));
            case 5 -> client.setScreen(new LexyModernScreen(this,"Profiles"));
            case 6 -> client.setScreen(new OptionsScreen(this,client.options));
            case 7 -> client.scheduleStop();
        }
    }
    @Override public boolean mouseScrolled(double mx,double my,double amount){
        int rows=(int)Math.ceil(BUTTONS.length/(double)columns);
        int visible=Math.max(1,(height-firstY-30)/(buttonH+10));
        scroll=Math.max(0,Math.min(Math.max(0,rows-visible),scroll-(int)Math.signum(amount)));
        return true;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT||key==GLFW.GLFW_KEY_F8){client.setScreen(new LexyModernScreen(this));return true;}
        if(key==GLFW.GLFW_KEY_ESCAPE){TitleBranding.openVanilla(client);return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean shouldPause(){return false;}
}