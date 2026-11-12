package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Lexy Studio title: original backdrop, keyboard/touch navigation and a vanilla escape hatch. */
public final class LexyHomeScreen extends Screen {
    private static final Identifier BACKDROP=new Identifier("lexy-client","textures/gui/backdrop.png");
    private static final String[] ITEMS={"SINGLEPLAYER","MULTIPLAYER","MINECRAFT REALMS","MOD MANAGER","LEXY SETTINGS","PROFILES","MINECRAFT OPTIONS","QUIT GAME"};
    private static final int[] ICONS={19,11,23,0,15,10,11,30};
    private final Screen vanilla;
    private int panelX,panelY,panelW,firstY,buttonH,gap,visible,scroll;
    public LexyHomeScreen(Screen parent){super(Text.literal("LexyClient Studio"));vanilla=parent;}
    @Override protected void init(){
        panelW=Math.min(410,Math.max(170,width-24));panelX=(width-panelW)/2;
        int top=Math.max(12,height/12),header=height<330?58:91;
        panelY=top;firstY=top+header;
        buttonH=Math.max(24,Math.min(38,height/17));gap=Math.max(5,Math.min(9,buttonH/5));
        visible=Math.max(1,(height-firstY-42)/(buttonH+gap));
        scroll=Math.max(0,Math.min(scroll,ITEMS.length-visible));
        System.out.println("[LexyClient 4.1.0] Studio title screen initialized "+width+"x"+height);
    }
    private int buttonY(int index){return firstY+(index-scroll)*(buttonH+gap);}
    private void button(DrawContext c,int index,int mx,int my){
        var p=LexyTheme.palette();
        int y=buttonY(index),x=panelX,w=panelW;
        boolean hovered=LexyPaint.hit(mx,my,x,y,w,buttonH);
        int color=index==0?p.accentSoft():index==7?0xFF33212A:0xC9141F2D;
        int edge=index==0?p.accent():hovered?p.accent():0xFF3A4A5A;
        LexyPaint.rounded(c,x,y,w,buttonH,hovered?LexyPaint.mix(color,p.accentSoft(),.46):color);
        LexyPaint.border(c,x,y,w,buttonH,edge);
        LexyPaint.icon(c,ICONS[index],x+15,y+(buttonH-19)/2,19);
        LexyPaint.label(c,ITEMS[index],x+42,y+(buttonH-8)/2,w-86,LexyPaint.WHITE);
        LexyPaint.label(c,"›",x+w-21,y+(buttonH-8)/2,12,hovered?p.accent():LexyPaint.MUTED);
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        var p=LexyTheme.palette();
        c.drawTexture(BACKDROP,0,0,width,height,0,0,1600,900,1600,900);
        c.fillGradient(0,0,width,height,0x28040810,0x8F020910);
        // Anchored top header: logo + non-decorative shortcuts.
        LexyPaint.rounded(c,12,10,40,40,0xA80C1521);
        c.drawTexture(LexyClient.LOGO,16,14,32,32,0,0,256,256,256,256);
        if(width>310){
            LexyPaint.title(c,"LEXY",59,17,1.5f,LexyPaint.WHITE);
            LexyPaint.label(c,"STUDIO EDITION",59,34,142,p.accent());
        }
        if(width>=590){
            String[] nav={"MODULES","SETTINGS","VANILLA"};
            for(int i=0;i<nav.length;i++){
                int x=width-297+i*97;
                LexyPaint.button(c,nav[i],x,16,91,27,0xC51C2636,p.border(),LexyPaint.WHITE,
                        LexyPaint.hit(mx,my,x,16,91,27));
            }
        }else{
            LexyPaint.button(c,"VANILLA",width-92,16,80,25,0xC51C2636,p.border(),LexyPaint.MUTED,
                    LexyPaint.hit(mx,my,width-92,16,80,25));
        }
        // Centered identity. This isn't a fake full-page panorama shader.
        int logoSize=height<330?38:56;
        int logoY=panelY+(height<330?3:0),logoX=width/2-logoSize/2;
        c.drawTexture(LexyClient.LOGO,logoX,logoY,logoSize,logoSize,0,0,256,256,256,256);
        if(height>=330){
            LexyPaint.title(c,"LEXY CLIENT",width/2-77,panelY+61,1.55f,LexyPaint.WHITE);
            LexyPaint.label(c,"MADE FOR YOUR GAME",width/2-61,panelY+82,140,p.accent());
        }else{
            LexyPaint.title(c,"LEXY",logoX+logoSize+6,panelY+10,1.45f,LexyPaint.WHITE);
            LexyPaint.label(c,"STUDIO 4.1",logoX+logoSize+6,panelY+28,110,p.accent());
        }
        int bottom=Math.max(firstY+buttonH,height-35);
        c.enableScissor(panelX-3,firstY-3,panelX+panelW+3,bottom);
        for(int i=scroll;i<Math.min(ITEMS.length,scroll+visible+1);i++){
            int y=buttonY(i);if(y+buttonH<=bottom)button(c,i,mx,my);
        }
        c.disableScissor();
        if(ITEMS.length>visible){
            int ax=panelX+panelW-19,top=firstY-20;
            LexyPaint.button(c,"^",ax,top,19,17,0xD5142535,p.border(),LexyPaint.WHITE,
                    LexyPaint.hit(mx,my,ax,top,19,17));
            LexyPaint.button(c,"v",ax,bottom+2,19,17,0xD5142535,p.border(),LexyPaint.WHITE,
                    LexyPaint.hit(mx,my,ax,bottom+2,19,17));
        }
        LexyPaint.label(c,"LEXY 4.1  /  FABRIC 1.20.1",15,height-16,200,LexyPaint.MUTED);
        String right="RSHIFT / F8  -  MODULES";
        LexyPaint.label(c,right,Math.max(10,width-textRenderer.getWidth(right)-15),height-16,200,p.accent());
        super.render(c,mx,my,delta);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return super.mouseClicked(mx,my,button);
        if(width>=590){
            if(LexyPaint.hit(mx,my,width-297,16,91,27)){client.setScreen(new LexyModernScreen(this));return true;}
            if(LexyPaint.hit(mx,my,width-200,16,91,27)){client.setScreen(new LexyModernScreen(this,"Settings"));return true;}
            if(LexyPaint.hit(mx,my,width-103,16,91,27)){TitleBranding.openVanilla(client);return true;}
        }else if(LexyPaint.hit(mx,my,width-92,16,80,25)){TitleBranding.openVanilla(client);return true;}
        int bottom=Math.max(firstY+buttonH,height-35);
        if(ITEMS.length>visible){
            int ax=panelX+panelW-19;
            if(LexyPaint.hit(mx,my,ax,firstY-20,19,17)){scroll=Math.max(0,scroll-1);return true;}
            if(LexyPaint.hit(mx,my,ax,bottom+2,19,17)){scroll=Math.min(ITEMS.length-visible,scroll+1);return true;}
        }
        for(int i=scroll;i<ITEMS.length&&i<scroll+visible+1;i++){
            int y=buttonY(i);
            if(y+buttonH<=bottom&&LexyPaint.hit(mx,my,panelX,y,panelW,buttonH)){
                activate(i);return true;
            }
        }
        return super.mouseClicked(mx,my,button);
    }
    private void activate(int index){
        LexySounds.tap();
        switch(index){
            case 0 -> client.setScreen(new SelectWorldScreen(this));
            case 1 -> client.setScreen(new MultiplayerScreen(this));
            case 2 -> {
                try{
                    Class<?> type=Class.forName("net.minecraft.client.realms.gui.screen.RealmsMainScreen");
                    var constructor=type.getConstructor(Screen.class);
                    client.setScreen((Screen)constructor.newInstance(this));
                }catch(ReflectiveOperationException ex){TitleBranding.openVanilla(client);}
            }
            case 3 -> client.setScreen(new LexyModernScreen(this));
            case 4 -> client.setScreen(new LexyModernScreen(this,"Settings"));
            case 5 -> client.setScreen(new LexyModernScreen(this,"Profiles"));
            case 6 -> client.setScreen(new OptionsScreen(this,client.options));
            case 7 -> client.scheduleStop();
        }
    }
    @Override public boolean mouseScrolled(double mx,double my,double amount){
        scroll=Math.max(0,Math.min(Math.max(0,ITEMS.length-visible),scroll-(int)Math.signum(amount)));
        return true;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT||key==GLFW.GLFW_KEY_F8){client.setScreen(new LexyModernScreen(this));return true;}
        if(key==GLFW.GLFW_KEY_ESCAPE){TitleBranding.openVanilla(client);return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean shouldPause(){return false;}
}