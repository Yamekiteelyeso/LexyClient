package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Lightweight monochrome title. All controls map to real Minecraft or Lexy screens. */
public final class LexyHomeScreen extends Screen {
    private static final Identifier BACKDROP=new Identifier("lexy-client","textures/gui/backdrop.png");
    private static final String[] MAIN={"SINGLEPLAYER","MULTIPLAYER","MINECRAFT REALMS","LEXY SETTINGS","QUIT GAME"};
    private static final int[] MAIN_ICONS={19,11,23,0,30};
    private static final String[] TOOLS={"MODULES","PROFILES","OPTIONS","VANILLA"};
    private static final int[] TOOL_ICONS={5,10,11,30};
    private final Screen vanilla;
    private int left,mainTop,buttonW,buttonH,step,toolsTop,toolW;
    public LexyHomeScreen(Screen parent){super(Text.literal("LexyClient"));vanilla=parent;}
    @Override protected void init(){
        buttonW=Math.min(300,Math.max(145,width-30));left=(width-buttonW)/2;
        buttonH=height>=420?31:height>=300?26:20;
        step=buttonH+(height>=340?7:4);
        int blockH=(MAIN.length-1)*step+buttonH;
        boolean footer=height>=240;
        int ideal=height>=340?(height-blockH)/2+18:height>=230?69:43;
        mainTop=Math.max(height>=230?68:38,Math.min(ideal,height-(footer?76:20)-blockH));
        toolsTop=height-42;
        toolW=Math.max(29,(buttonW-12)/TOOLS.length);
        System.out.println("[LexyClient 4.2.0] Mono title screen initialized "+width+"x"+height);
    }
    private int mainY(int idx){return mainTop+idx*step;}
    private void menuButton(DrawContext c,int idx,int mx,int my){
        int y=mainY(idx),h=buttonH;
        boolean hover=LexyPaint.hit(mx,my,left,y,buttonW,h);
        int bg=hover?0xF0303030:0xE8171718,edge=hover?0xFF777777:0xFF454545;
        LexyPaint.rounded(c,left,y,buttonW,h,bg);
        LexyPaint.border(c,left,y,buttonW,h,edge);
        int icon=Math.min(h-8,17),iy=y+(h-icon)/2;
        LexyPaint.icon(c,MAIN_ICONS[idx],left+14,iy,icon);
        LexyPaint.label(c,MAIN[idx],left+40,y+(h-8)/2,buttonW-65,0xFFE6E6E6);
        if(buttonW>=205)LexyPaint.label(c,"›",left+buttonW-19,y+(h-8)/2,12,hover?0xFFF2F2F2:0xFF737373);
    }
    private void secondary(DrawContext c,int index,int mx,int my){
        int x=left+index*(toolW+4),y=toolsTop;
        boolean hover=LexyPaint.hit(mx,my,x,y,toolW,22);
        LexyPaint.rounded(c,x,y,toolW,22,hover?0xE7353535:0xCC151515);
        LexyPaint.border(c,x,y,toolW,22,hover?0xFF777777:0xFF393939);
        if(toolW>=72){
            LexyPaint.icon(c,TOOL_ICONS[index],x+5,y+5,12);
            LexyPaint.label(c,TOOLS[index],x+20,y+7,toolW-25,0xFFBEBEBE);
        }else{
            LexyPaint.icon(c,TOOL_ICONS[index],x+(toolW-14)/2,y+4,14);
        }
    }
    @Override public void render(DrawContext c,int mx,int my,float dt){
        c.drawTexture(BACKDROP,0,0,width,height,0,0,1600,900,1600,900);
        c.fillGradient(0,0,width,height,0x74000000,0xBA080808);
        c.fill(0,0,width,38,0xAE080808);
        c.fill(0,37,width,38,0xFF303030);
        LexyPaint.label(c,"LEXY",12,13,80,0xFFEDEDED);
        LexyPaint.label(c,"/  CLIENT 4.2",53,13,114,0xFF8B8B8B);
        if(width>=325)LexyPaint.label(c,"RSHIFT  /  F8",width-102,13,98,0xFF979797);
        int markSize=height>=340?40:26;
        int titleY=Math.max(39,mainTop-markSize-14);
        int titleX=left+Math.max(0,buttonW/2-81);
        if(height>=230){
            c.drawTexture(LexyClient.LOGO,titleX,titleY,markSize,markSize,0,0,256,256,256,256);
            LexyPaint.title(c,"LEXY CLIENT",titleX+markSize+7,titleY+Math.max(5,(markSize-17)/2),1.2f,0xFFEEEEEE);
        }
        for(int i=0;i<MAIN.length;i++)menuButton(c,i,mx,my);
        if(height>=240)for(int i=0;i<TOOLS.length;i++)secondary(c,i,mx,my);
        LexyPaint.label(c,"FABRIC 1.20.1",12,height-15,125,0xFF858585);
        if(width>=390)LexyPaint.label(c,"MONO  /  NO OVERLAYS",width-156,height-15,148,0xFF777777);
        super.render(c,mx,my,dt);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return super.mouseClicked(mx,my,button);
        for(int i=0;i<MAIN.length;i++)if(LexyPaint.hit(mx,my,left,mainY(i),buttonW,buttonH)){
            activateMain(i);return true;
        }
        if(height>=240)for(int i=0;i<TOOLS.length;i++)if(LexyPaint.hit(mx,my,left+i*(toolW+4),toolsTop,toolW,22)){
            activateTool(i);return true;
        }
        return super.mouseClicked(mx,my,button);
    }
    private void activateMain(int i){
        LexySounds.tap();
        switch(i){
            case 0->client.setScreen(new SelectWorldScreen(this));
            case 1->client.setScreen(new MultiplayerScreen(this));
            case 2->{
                try{Class<?> type=Class.forName("net.minecraft.client.realms.gui.screen.RealmsMainScreen");
                    var ctor=type.getConstructor(Screen.class);client.setScreen((Screen)ctor.newInstance(this));
                }catch(ReflectiveOperationException ex){TitleBranding.openVanilla(client);}
            }
            case 3->client.setScreen(new LexyModernScreen(this,"Settings"));
            case 4->client.scheduleStop();
        }
    }
    private void activateTool(int i){
        LexySounds.tap();
        switch(i){
            case 0->client.setScreen(new LexyModernScreen(this));
            case 1->client.setScreen(new LexyModernScreen(this,"Profiles"));
            case 2->client.setScreen(new OptionsScreen(this,client.options));
            case 3->TitleBranding.openVanilla(client);
        }
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT||key==GLFW.GLFW_KEY_F8){client.setScreen(new LexyModernScreen(this));return true;}
        if(key==GLFW.GLFW_KEY_ESCAPE){TitleBranding.openVanilla(client);return true;}
        return super.keyPressed(key,scan,mods);
    }
    @Override public boolean shouldPause(){return false;}
}