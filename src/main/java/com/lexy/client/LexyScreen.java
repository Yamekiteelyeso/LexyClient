package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;

/** Handmade graphite/blue dashboard. Each visible control invokes a real setting or API call. */
public final class LexyScreen extends Screen {
    private static final int BG=0xFF11151D, PANEL=0xFF1A202B, SURFACE=0xFF273142;
    private static final int OUTLINE=0xFF364354, WHITE=0xFFF4F7FC, MUTED=0xFFA5B2C4;
    private static final String[] TABS={"All Modules","Combat","HUD","Statistics","Utilities","Performance","Optimization","Spotify","Local Music","Appearance","Cheats"};
    private static final int ROW_H=54, NAV_ROW=34;
    private final boolean editor;
    private int tab, scroll=0, navScroll=0, selected=-1, dragDX=0, dragDY=0;
    private final Screen parent;
    private boolean dragging=false, showDevices=false, totemSettingsOpen=false;
    private String query="";
    private TextFieldWidget search,spotifyClientId,spotifySearch,spotifyRedirect;
    private int panelX,panelY,panelW,panelH,sidebarW;
    private float menuScale=1f;
    private String spotifyHint="";
    private boolean playedOpenSound;
    public LexyScreen(boolean editor){this(editor,0,null);}
    public LexyScreen(boolean editor,int tab){this(editor,tab,null);}
    public LexyScreen(boolean editor,int tab,Screen parent){super(Text.literal("LexyClient"));this.editor=editor;this.tab=tab;this.parent=parent;}
    @Override protected void init(){
        if(!playedOpenSound){LexySounds.open();playedOpenSound=true;}
        // Logical pixel coordinates are scaled together: text, click targets, fields and cards.
        // The smaller the Minecraft window, the smaller the *entire* UI.
        float auto=Math.min(1.0f,Math.min((width-8f)/780f,(height-8f)/460f));
        menuScale=Math.max(.24f,auto*LexyClient.menuScalePercent/100f);
        int vw=Math.round(width/menuScale), vh=Math.round(height/menuScale);
        panelW=Math.min(972,Math.max(410,vw-18));panelH=Math.min(590,Math.max(360,vh-18));
        panelX=(vw-panelW)/2;panelY=(vh-panelH)/2;sidebarW=vw<760?145:170;
        int rx=rightX();int rw=rightW();
        search=new TextFieldWidget(textRenderer,rx+10,panelY+63,rw-20,21,Text.literal("Search modules"));
        search.setMaxLength(80);search.setPlaceholder(Text.literal("Search your modules..."));
        search.setText(query);search.setChangedListener(value->{query=value;scroll=0;});addDrawableChild(search);
        spotifyClientId=new TextFieldWidget(textRenderer,rx+11,panelY+84,Math.max(95,rw-22),20,Text.literal("Spotify Client ID"));
        spotifyClientId.setMaxLength(90);spotifyClientId.setPlaceholder(Text.literal("Paste Spotify Client ID (not secret)"));
        spotifyClientId.setText(LexyClient.spotifyClientId);addDrawableChild(spotifyClientId);
        spotifyRedirect=new TextFieldWidget(textRenderer,rx+11,panelY+228,Math.max(95,rw-22),22,Text.literal("Spotify callback URL"));
        spotifyRedirect.setMaxLength(4096);spotifyRedirect.setPlaceholder(Text.literal("Paste http://127.0.0.1:43871/callback?..."));addDrawableChild(spotifyRedirect);
        spotifySearch=new TextFieldWidget(textRenderer,rx+11,panelY+88,Math.max(95,rw-100),20,Text.literal("Search tracks"));
        spotifySearch.setMaxLength(80);spotifySearch.setPlaceholder(Text.literal("Search songs or artists..."));addDrawableChild(spotifySearch);
        updateTextFields();
    }
    private int accent(){return LexyClient.accent();}
    private int rightX(){return panelX+sidebarW+9;}
    private int rightW(){return panelW-sidebarW-20;}
    private int contentY(){return panelY+96;}
    private int contentH(){return Math.max(35,panelH-157);}
    private int columns(){return rightW()>480?2:1;}
    private int navCapacity(){return Math.max(2,(panelH-164)/NAV_ROW);}
    private int navLimit(){return Math.max(0,TABS.length-navCapacity());}
    private int rowsVisible(){return Math.max(1,contentH()/ROW_H);}
    private int capacity(){return columns()*rowsVisible();}
    private int maxScroll(int n){return Math.max(0,(int)Math.ceil(n/(double)columns())-rowsVisible());}
    private int page(){return scroll/Math.max(1,rowsVisible())+1;}
    private int pages(int n){return Math.max(1,(int)Math.ceil(n/(double)Math.max(1,capacity())));}
    private void updateTextFields(){
        if(search!=null)search.visible=!editor&&tab<=5;
        if(spotifyClientId!=null)spotifyClientId.visible=!editor&&tab==7&&!SpotifyController.INSTANCE.isConnected();
        if(spotifySearch!=null)spotifySearch.visible=!editor&&tab==7&&SpotifyController.INSTANCE.isConnected();
        if(spotifyRedirect!=null)spotifyRedirect.visible=!editor&&tab==7&&!SpotifyController.INSTANCE.isConnected();
        if(search!=null&&!search.visible)search.setFocused(false);
        if(spotifySearch!=null&&!spotifySearch.visible)spotifySearch.setFocused(false);
        if(spotifyClientId!=null&&!spotifyClientId.visible)spotifyClientId.setFocused(false);
        if(spotifyRedirect!=null&&!spotifyRedirect.visible)spotifyRedirect.setFocused(false);
    }
    private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private void rect(DrawContext c,int x,int y,int w,int h,int color){c.fill(x,y,x+w,y+h,color);}
    private double logical(double n){return n/menuScale;}
    private void clip(DrawContext c,int x1,int y1,int x2,int y2){
        c.enableScissor(Math.round(x1*menuScale),Math.round(y1*menuScale),Math.round(x2*menuScale),Math.round(y2*menuScale));
    }
    private void moveHud(){if(client!=null)client.setScreen(new HudEditorScreen(this));}
    private void openSpotifyDashboard(){
        if(AndroidBrowser.open("https://developer.spotify.com/dashboard"))spotifyHint="Spotify Developer Dashboard opened";
        else {try{if(client!=null)client.keyboard.setClipboard("https://developer.spotify.com/dashboard");spotifyHint="Developer Dashboard URL copied";}catch(Throwable e){spotifyHint="Visit developer.spotify.com/dashboard";}}
    }
    private void rounded(DrawContext c,int x,int y,int w,int h,int color){
        if(w<=8||h<=8){rect(c,x,y,w,h,color);return;}
        rect(c,x+4,y,w-8,h,color);rect(c,x,y+4,4,h-8,color);rect(c,x+w-4,y+4,4,h-8,color);
        rect(c,x+2,y+1,w-4,2,color);rect(c,x+1,y+3,w-2,h-6,color);
        rect(c,x+2,y+h-3,w-4,2,color);
    }
    private void frame(DrawContext c,int x,int y,int w,int h,int color){
        rect(c,x,y,w,1,color);rect(c,x,y+h-1,w,1,color);rect(c,x,y,1,h,color);rect(c,x+w-1,y,1,h,color);
    }
    private void text(DrawContext c,String s,int x,int y,int color){c.drawTextWithShadow(textRenderer,s,x,y,color);}
    private String ellipsis(String s,int max){if(s==null)return "";if(max<15)return "";
        if(textRenderer.getWidth(s)<=max)return s;while(s.length()>1&&textRenderer.getWidth(s+"...")>max)s=s.substring(0,s.length()-1);return s+"...";
    }
    private void label(DrawContext c,String s,int x,int y,int max,int color){text(c,ellipsis(s,max),x,y,color);}
    private List<LexyClient.Module> filtered(){
        String needle=query.trim().toLowerCase(Locale.ROOT);
        ArrayList<LexyClient.Module> list=new ArrayList<>();
        for(LexyClient.Module m:LexyClient.MODULES){
            if ("AutoTotem".equals(m.category()) || "Music".equals(m.category())) continue;
            if(tab>0&&tab<=5&&!m.category().equals(TABS[tab]))continue;
            if(!m.id().toLowerCase(Locale.ROOT).contains(needle))continue;
            list.add(m);
        }
        return list;
    }
    private boolean modular(){return tab<=5;}
    @Override public void render(DrawContext c,int mx,int my,float dt){
        renderBackground(c);rect(c,0,0,width,height,0xCC060A10);
        int logicalMX=(int)logical(mx),logicalMY=(int)logical(my);
        c.getMatrices().push();c.getMatrices().scale(menuScale,menuScale,1);
        rounded(c,panelX-8,panelY-5,panelW+16,panelH+13,0x51000000);
        rounded(c,panelX-3,panelY-3,panelW+6,panelH+6,0x603B82F6);
        rounded(c,panelX,panelY,panelW,panelH,BG);frame(c,panelX+4,panelY,panelW-8,1,0xFF39465B);
        rect(c,panelX+4,panelY,sidebarW,panelH,PANEL);
        rect(c,panelX+4,panelY,3,panelH,accent());
        rounded(c,panelX+15,panelY+12,40,40,0xFF0A1322);c.drawTexture(LexyClient.LOGO,panelX+15,panelY+12,40,40,0,0,256,256,256,256);
        label(c,"LexyClient",panelX+60,panelY+18,Math.max(35,sidebarW-57),WHITE);
        label(c,"Blue Edition 2.0  |  Fabric 1.20.1",panelX+60,panelY+35,Math.max(36,sidebarW-59),MUTED);
        rect(c,panelX+15,panelY+60,sidebarW-24,1,OUTLINE);
        int count=navCapacity(); navScroll=Math.max(0,Math.min(navScroll,navLimit()));
        for(int i=navScroll;i<Math.min(TABS.length,navScroll+count);i++){
            int yy=panelY+68+(i-navScroll)*NAV_ROW;
            boolean active=i==tab&&!editor;
            if(active){rounded(c,panelX+10,yy,sidebarW-16,26,0xFF2B3B50);rect(c,panelX+10,yy+4,3,18,accent());}
            String navName = sidebarW<100?switch(i){case 0->"All";case 3->"Stats";case 5->"FPS HUD";case 6->"FPS Setup";case 8->"Music";case 9->"Themes";default->TABS[i];}:TABS[i];
            label(c,navName,panelX+21,yy+9,sidebarW-30,active?WHITE:MUTED);
        }
        if(navLimit()>0){
            rounded(c,panelX+12,panelY+panelH-65,(sidebarW-28)/2,23,0xFF253A57);
            rounded(c,panelX+16+(sidebarW-28)/2,panelY+panelH-65,(sidebarW-28)/2,23,0xFF253A57);
            label(c,"UP",panelX+26,panelY+panelH-56,(sidebarW-28)/2-10,WHITE);
            label(c,"DOWN",panelX+23+(sidebarW-28)/2,panelY+panelH-56,(sidebarW-28)/2-6,WHITE);
        }
        rect(c,panelX+14,panelY+panelH-56,sidebarW-21,1,OUTLINE);
        label(c,editor?"HUD EDITOR":"HUD WIDGETS",panelX+18,panelY+panelH-43,sidebarW-25,accent());
        rounded(c,panelX+13,panelY+panelH-93,sidebarW-22,24,0xFF265A94);
        label(c,"MOVE HUD",panelX+24,panelY+panelH-85,sidebarW-39,WHITE);
        label(c,"RSHIFT: open",panelX+18,panelY+panelH-31,sidebarW-24,MUTED);
        label(c,"LSHIFT: close",panelX+18,panelY+panelH-19,sidebarW-24,MUTED);
        int rx=rightX(),rw=rightW();
        rounded(c,rx,panelY+12,rw,41,PANEL);rect(c,rx,panelY+19,3,27,accent());
        String title=editor?"HUD LAYOUT EDITOR":tab==6?"FPS OPTIMIZATION":tab==7?"SPOTIFY CONNECT":tab==8?"IN-GAME MUSIC PLAYER":tab==9?"APPEARANCE":tab==10?"CHEATS / AUTOTOTEM":TABS[tab].toUpperCase(Locale.ROOT);
        label(c,title,rx+12,panelY+19,rw-171,WHITE);
        rounded(c,rx+rw-157,panelY+17,107,28,0xFF245891);
        label(c,"MOVE HUD",rx+rw-143,panelY+27,82,WHITE);
        rounded(c,rx+rw-44,panelY+17,35,28,0xFF51394C);
        label(c,"X",rx+rw-30,panelY+27,14,WHITE);
        label(c,editor?"Drag readouts to position them":tab==6?"Real Minecraft graphics options":tab==7?"Spotify login works on PC and PojavLauncher":tab==8?"Audio runs inside Minecraft - no other app":tab==9?"Theme, overlay and density settings":tab==10?"AutoTotem controls and configuration":"Enable modules and readouts; switch categories below",rx+12,panelY+35,rw-20,MUTED);
        if(editor)renderEditor(c,logicalMX,logicalMY);
        else if(modular())renderModules(c,logicalMX,logicalMY);
        else if(tab==6)renderOptimization(c,logicalMX,logicalMY);
        else if(tab==7)renderSpotify(c,logicalMX,logicalMY);
        else if(tab==8)renderLocalMusic(c,logicalMX,logicalMY);
        else if(tab==10)renderAutoTotem(c,logicalMX,logicalMY);
        else renderStyle(c,logicalMX,logicalMY);
        updateTextFields();
        super.render(c,logicalMX,logicalMY,dt);
        String footer=editor?"Drag preview chips  |  Reset restores positions":tab==7?"Pojav: paste the callback URL if redirect fails":tab==8?"WAV / AIFF  |  Playback inside Minecraft":"RSHIFT: menu | LSHIFT: close | RCTRL: HUD | Touch: X";
        label(c,footer,rx+7,panelY+panelH-17,rw-14,MUTED);
        c.getMatrices().pop();
    }
    private void renderModules(DrawContext c,int mx,int my){
        List<LexyClient.Module> list=filtered();int rx=rightX(),rw=rightW(),y=contentY(),cols=columns();
        scroll=Math.min(scroll,maxScroll(list.size()));
        clip(c,rx,y,rx+rw,y+contentH());
        for(int i=0;i<capacity()&&scroll*cols+i<list.size();i++){
            int idx=scroll*cols+i;LexyClient.Module mod=list.get(idx);
            int col=i%cols,row=i/cols,cw=rw/cols-6,x=rx+col*(rw/cols),cy=y+row*ROW_H;
            boolean on=LexyClient.enabled.getOrDefault(mod.id(),false);
            boolean hover=inside(mx,my,x,cy,cw,ROW_H-4);
            rounded(c,x,cy,cw,ROW_H-4,hover?0xFF2D3C50:PANEL);
            rect(c,x,cy,3,ROW_H-4,on?accent():0xFF46556C);
            label(c,mod.id(),x+11,cy+9,cw-79,WHITE);
            String value;try{value=mod.value().get();}catch(Exception ex){value="Unavailable";}
            label(c,value,x+11,cy+29,cw-28,MUTED);
            rounded(c,x+cw-49,cy+13,37,20,on?accent():0xFF445269);
            rounded(c,x+cw-(on?29:46),cy+15,16,16,WHITE);
        }
        c.disableScissor();
        if(list.isEmpty())text(c,"No matching modules",rx+16,y+20,MUTED);
        drawScrollbar(c,list.size(),y,contentH());
        int pagerY=panelY+panelH-42;
        rounded(c,rx,pagerY,62,20,scroll>0?SURFACE:OUTLINE);label(c,"< PREV",rx+8,pagerY+7,52,WHITE);
        rounded(c,rx+rw-62,pagerY,62,20,scroll<maxScroll(list.size())?SURFACE:OUTLINE);label(c,"NEXT >",rx+rw-54,pagerY+7,52,WHITE);
        label(c,"Page "+page()+" / "+pages(list.size())+"    "+list.size()+" modules",rx+68,pagerY+7,rw-142,WHITE);
    }
    private void drawScrollbar(DrawContext c,int count,int y,int h){
        int mx=maxScroll(count);if(mx<1)return;int xx=rightX()+rightW()-4;
        rect(c,xx,y,3,h,OUTLINE);int thumb=Math.max(18,(int)(h*((double)rowsVisible()/(mx+rowsVisible()))));
        rect(c,xx,y+(h-thumb)*scroll/mx,3,thumb,accent());
    }
    private void renderOptimization(DrawContext c,int mx,int my){
        int rx=rightX(),rw=rightW(),yy=panelY+63;
        int buttonWidth=(rw-20)/3;
        for(int i=0;i<3;i++){
            String name=new String[]{"MAX FPS","BALANCED","QUALITY"}[i];int x=rx+i*buttonWidth;
            rect(c,x,yy,buttonWidth-5,29,inside(mx,my,x,yy,buttonWidth-5,29)?accent():SURFACE);
            label(c,name,x+8,yy+10,buttonWidth-17,WHITE);
        }
        text(c,"Video settings  -  changes are saved to options.txt",rx+4,yy+37,MUTED);
        int listY=panelY+115,rowH=48,avail=Math.max(40,panelY+panelH-35-listY);
        scroll=Math.min(scroll,Math.max(0,OptimizationManager.OPTIONS.size()-(avail/rowH)));
        clip(c,rx,listY,rx+rw,listY+avail);
        for(int j=0;j<=avail/rowH&&j+scroll<OptimizationManager.OPTIONS.size();j++){
            var item=OptimizationManager.OPTIONS.get(j+scroll);int y=listY+j*rowH;
            rect(c,rx,y,rw-6,rowH-4,PANEL);rect(c,rx,y,3,rowH-4,accent());
            label(c,item.name(),rx+12,y+6,rw-152,WHITE);
            label(c,item.help(),rx+12,y+23,rw-151,MUTED);
            rect(c,rx+rw-143,y+9,24,26,SURFACE);text(c,"-",rx+rw-134,y+18,WHITE);
            rect(c,rx+rw-110,y+9,75,26,SURFACE);label(c,item.value().get(),rx+rw-105,y+18,65,WHITE);
            rect(c,rx+rw-31,y+9,24,26,SURFACE);text(c,"+",rx+rw-23,y+18,WHITE);
        }
        c.disableScissor();
    }
    private void renderSpotify(DrawContext c,int mx,int my){
        int rx=rightX(),rw=rightW(),top=panelY+65;
        SpotifyController spotify=SpotifyController.INSTANCE;
        boolean connected=spotify.isConnected();
        if(!connected){
            label(c,"CONNECT SPOTIFY - POJAV / PC",rx+12,top,rw-25,WHITE);
            label(c,"1. Paste your public Client ID (not the secret)",rx+12,top+14,rw-22,MUTED);
            int loginY=top+47,buttonW=(rw-35)/2;
            rounded(c,rx+11,loginY,buttonW,28,spotify.pending()?SURFACE:0xFF186BA6);
            label(c,spotify.pending()?"CANCEL LOGIN":"LOGIN WITH SPOTIFY",rx+20,loginY+10,buttonW-15,WHITE);
            rounded(c,rx+17+buttonW,loginY,rw-buttonW-28,28,SURFACE);
            label(c,spotify.pending()?"COPY LOGIN URL":"GET CLIENT ID",rx+25+buttonW,loginY+10,rw-buttonW-37,WHITE);
            label(c,"2. Approve in your phone browser",rx+12,top+86,rw-20,WHITE);
            label(c,"If the browser cannot return to Minecraft:",rx+12,top+105,rw-20,MUTED);
            label(c,"Copy the browser's final 127.0.0.1/callback URL",rx+12,top+123,rw-20,0xFF8FC6FF);
            label(c,"3. Paste that entire URL here:",rx+12,top+147,rw-20,WHITE);
            rounded(c,rx+11,top+192,rw-22,28,0xFF2469A3);
            label(c,"FINISH LOGIN FROM CALLBACK URL",rx+20,top+202,rw-33,WHITE);
            rounded(c,rx+11,top+231,rw-22,42,PANEL);
            label(c,spotify.status(),rx+20,top+244,rw-43,0xFFB4D9FF);
            label(c,"Registered redirect:",rx+13,top+287,rw-25,MUTED);
            label(c,spotify.redirectUri(),rx+13,top+303,rw-25,WHITE);
            label(c,"Required: Spotify Developer app + permitted account",rx+13,top+325,rw-24,MUTED);
            label(c,"Music plays in the Spotify app; Lexy controls it.",rx+13,top+341,rw-24,MUTED);
            return;
        }
        label(c,"SPOTIFY CONNECTED",rx+13,top,rw-24,0xFF8CE2BD);
        rounded(c,rx+rw-103,top-4,91,23,0xFF42465B);
        label(c,"DISCONNECT",rx+rw-95,top+4,78,WHITE);
        label(c,spotify.playing(),rx+13,top+24,rw-26,WHITE);
        label(c,spotify.status(),rx+13,top+40,rw-26,MUTED);
        rect(c,rx+rw-84,panelY+88,73,20,SURFACE);label(c,"SEARCH",rx+rw-73,panelY+95,60,WHITE);
        int by=panelY+122,bw=(rw-28)/5;
        String[] actions={"|<","PAUSE/PLAY",">|","TRACKS","DEVICES"};
        for(int i=0;i<5;i++){
            int xx=rx+11+i*bw;
            rounded(c,xx,by,bw-4,28,(i==4&&showDevices||i==3&&!showDevices)?0xFF285B92:SURFACE);
            label(c,actions[i],xx+5,by+10,bw-12,WHITE);
        }
        int pcY=by+37;
        rounded(c,rx+11,pcY,(rw-30)/2,24,0xFF2A4569);
        label(c,"USE COMPUTER",rx+19,pcY+8,(rw-30)/2-13,WHITE);
        rounded(c,rx+18+(rw-30)/2,pcY,(rw-30)/2,24,SURFACE);
        label(c,"REFRESH DEVICES",rx+27+(rw-30)/2,pcY+8,(rw-30)/2-13,WHITE);
        int listY=pcY+33,listHeight=Math.max(25,panelY+panelH-27-listY);
        clip(c,rx+10,listY,rx+rw-10,listY+listHeight);
        if(showDevices){
            List<SpotifyController.Device> devices=spotify.devices();
            if(devices.isEmpty())label(c,"Open Spotify on your phone and press REFRESH.",rx+17,listY+12,rw-35,MUTED);
            for(int i=0;i<devices.size()&&i*31<listHeight;i++){
                var dev=devices.get(i);int y=listY+i*31;rect(c,rx+11,y,rw-22,28,PANEL);
                label(c,(dev.active()?"[ACTIVE] ":"")+dev.name()+" ("+dev.type()+")",rx+19,y+10,rw-40,WHITE);
            }
        }else{
            List<SpotifyController.Track> songs=spotify.tracks();
            if(songs.isEmpty())label(c,"Search tracks and start playback in Spotify.",rx+17,listY+12,rw-34,MUTED);
            int maxRows=Math.max(1,listHeight/32);
            scroll=Math.min(scroll,Math.max(0,songs.size()-maxRows));
            for(int i=0;i<maxRows&&i+scroll<songs.size();i++){
                var track=songs.get(i+scroll);int y=listY+i*32;
                rect(c,rx+11,y,rw-22,29,PANEL);
                label(c,track.title()+" - "+track.artist(),rx+19,y+11,rw-94,WHITE);
                rect(c,rx+rw-76,y+4,38,21,SURFACE);text(c,"PLAY",rx+rw-71,y+11,WHITE);
                rect(c,rx+rw-34,y+4,23,21,SURFACE);text(c,"+",rx+rw-26,y+11,WHITE);
            }
        }
        c.disableScissor();
    }
    private void renderLocalMusic(DrawContext c,int mx,int my){
        LocalMusicPlayer player=LocalMusicPlayer.INSTANCE;
        int rx=rightX(),rw=rightW(),top=panelY+64;
        rounded(c,rx,top,rw,62,PANEL);
        rect(c,rx,top+9,3,44,accent());
        label(c,player.current(),rx+13,top+13,rw-24,WHITE);
        label(c,player.status(),rx+13,top+33,rw-25,MUTED);
        int buttonY=top+69;
        int refreshW=Math.min(89,(rw-6)/2),folderX=rx+refreshW+6,folderW=rw-refreshW-6;
        rounded(c,rx,buttonY,refreshW,24,SURFACE);label(c,"REFRESH",rx+10,buttonY+8,refreshW-15,WHITE);
        rounded(c,folderX,buttonY,folderW,24,SURFACE);label(c,"OPEN FOLDER",folderX+10,buttonY+8,folderW-15,WHITE);
        label(c,"config/lexy-music  -  PCM WAV / AIFF",rx+4,buttonY+31,rw-10,MUTED);
        int playerY=buttonY+46;
        int bw=Math.max(24,(rw-10)/5);
        String[] controls={"PREV",player.paused()?"RESUME":"PAUSE","NEXT","STOP",player.looping()?"LOOP ON":"LOOP OFF"};
        for(int i=0;i<5;i++){
            int x=rx+i*bw;
            rounded(c,x,playerY,bw-5,28,i==1?0xFF315A90:SURFACE);
            label(c,controls[i],x+7,playerY+10,bw-13,WHITE);
        }
        int volY=playerY+36;
        text(c,"VOLUME",rx+3,volY+9,MUTED);
        int plusX=rx+rw-35,minusX=rx+rw-115;
        rounded(c,minusX,volY,25,25,SURFACE);text(c,"-",minusX+10,volY+9,WHITE);
        rounded(c,plusX,volY,25,25,SURFACE);text(c,"+",plusX+9,volY+9,WHITE);
        label(c,player.volume()+"%",rx+rw-85,volY+9,42,WHITE);
        int listY=volY+33,listHeight=Math.max(30,panelY+panelH-27-listY);
        List<String> files=player.tracks();
        int maxRows=Math.max(1,listHeight/31);
        scroll=Math.max(0,Math.min(scroll,Math.max(0,files.size()-maxRows)));
        clip(c,rx,listY,rx+rw,listY+listHeight);
        if(files.isEmpty())label(c,"Add .wav files using OPEN FOLDER, then REFRESH.",rx+10,listY+12,rw-20,MUTED);
        for(int i=0;i<maxRows&&i+scroll<files.size();i++){
            int index=i+scroll,y=listY+i*31;
            rounded(c,rx+1,y,rw-3,28,index==player.selected()&&player.playing()?0xFF2D4C70:PANEL);
            rect(c,rx+1,y+5,3,18,index==player.selected()?accent():OUTLINE);
            label(c,(index==player.selected()?"♪  ":"▷  ")+files.get(index),rx+11,y+9,rw-20,WHITE);
        }
        c.disableScissor();
    }
    private void renderAutoTotem(DrawContext c,int mx,int my){
        int x=rightX(),w=rightW(),top=panelY+68;
        rounded(c,x,top,w,71,0xFF112F52);rect(c,x,top,4,71,accent());
        label(c,"AutoTotem  |  "+AutoTotem.status(),x+13,top+10,w-25,WHITE);
        label(c,"Real inventory offhand swap. Starts disabled.",x+13,top+26,w-24,MUTED);
        int toggleX=x+9,buttonY=top+42,bw=Math.max(60,(w-26)/2);
        rounded(c,toggleX,buttonY,bw,24,LexyClient.enabled.getOrDefault("AutoTotem",false)?0xFF245A86:SURFACE);
        label(c,LexyClient.enabled.getOrDefault("AutoTotem",false)?"ENABLED - TAP TO OFF":"DISABLED - TAP TO ON",toggleX+8,buttonY+8,bw-15,WHITE);
        rounded(c,toggleX+bw+7,buttonY,w-bw-25,24,totemSettingsOpen?0xFF2D638A:SURFACE);
        label(c,totemSettingsOpen?"HIDE SETTINGS":"CONFIGURE",toggleX+bw+15,buttonY+8,w-bw-40,WHITE);
        if(!totemSettingsOpen){
            label(c,"Tap CONFIGURE to edit swap rules, thresholds and alerts.",x+8,top+96,w-16,MUTED);
            label(c,"AutoTotem appears only here - not in other tabs.",x+8,top+117,w-16,MUTED);
            return;
        }
        String[] labels={"ONLY WHEN OFFHAND EMPTY","LOW HEALTH ONLY","HEALTH THRESHOLD","DELAY BETWEEN SWAPS","SHOW NOTIFICATIONS"};
        String[] values={AutoTotem.onlyWhenEmpty?"YES":"NO",AutoTotem.lowHealthOnly?"YES":"NO",AutoTotem.healthThreshold+" HP",AutoTotem.intervalTicks+" ticks",AutoTotem.notify?"ON":"OFF"};
        int start=top+81;int visible=Math.min(labels.length,Math.max(1,(panelY+panelH-43-start)/48));
        scroll=Math.max(0,Math.min(scroll,Math.max(0,labels.length-visible)));
        clip(c,x,start,x+w,panelY+panelH-28);
        for(int i=scroll;i<Math.min(labels.length,scroll+visible);i++){
            int y=start+(i-scroll)*48;rounded(c,x+7,y,w-15,41,PANEL);rect(c,x+7,y+8,3,25,accent());
            label(c,labels[i],x+19,y+16,w-143,WHITE);rounded(c,x+w-111,y+8,96,25,SURFACE);
            label(c,values[i],x+w-102,y+16,83,WHITE);
        }
        c.disableScissor();
        label(c,"Tap a setting to cycle. Changes save automatically.",x+9,panelY+panelH-21,w-20,MUTED);
    }
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        if(keyCode==org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT){
            close();return true;
        }
        return super.keyPressed(keyCode,scanCode,modifiers);
    }
    private void renderStyle(DrawContext c,int mx,int my){
        int rx=rightX(),rw=rightW(),yy=panelY+66;
        text(c,"ACCENT COLOR",rx+8,yy,WHITE);yy+=20;
        for(int i=0;i<LexyClient.ACCENTS.length;i++){
            int xx=rx+8+i*55;rect(c,xx,yy,45,28,LexyClient.ACCENTS[i]);
            if(i==Math.floorMod(LexyClient.accentChoice,LexyClient.ACCENTS.length))frame(c,xx-2,yy-2,49,32,WHITE);
        }
        yy+=45;
        String[] names={"MENU SIZE", "HUD SIZE", "HUD SNAP TO GRID", "HUD GRID VISIBLE", "HUD GRID SPACING", "HUD Background","Text Shadow","Compact Readouts","Spotify Overlay","Background Opacity","Widget Spacing","Low HP Warning","Armor Warning %","HUD Text Color","UI Sounds","UI Sound Volume","Max HUD Widgets"};
        int listY=yy;int showCount=Math.max(1,(panelY+panelH-45-listY)/44);
        scroll=Math.min(scroll,Math.max(0,names.length-showCount));
        clip(c,rx,listY,rx+rw,listY+showCount*44);
        for(int i=scroll;i<Math.min(names.length,scroll+showCount);i++){
            int y=yy+(i-scroll)*44;rect(c,rx+7,y,rw-15,39,PANEL);rect(c,rx+7,y,3,39,accent());
            label(c,names[i],rx+19,y+14,rw-150,WHITE);
            String value=switch(i){case 0->LexyClient.menuScalePercent+"%";case 1->LexyClient.hudScalePercent+"%";case 2->LexyClient.hudGridSnap?"ON":"OFF";case 3->LexyClient.hudShowGrid?"ON":"OFF";case 4->LexyClient.hudGridSize+" px";case 5->LexyClient.hudBackground?"ON":"OFF";case 6->LexyClient.hudShadow?"ON":"OFF";case 7->LexyClient.compactHud?"ON":"OFF";case 8->LexyClient.spotifyHud?"ON":"OFF";case 9->LexyClient.hudOpacity+" / 245";case 10->LexyClient.hudSpacing+" px";case 11->LexyClient.lowHealthThreshold+" HP";case 12->LexyClient.armorWarnPercent+"%";case 13->new String[]{"White","Blue","Cool Gray"}[LexyClient.hudTextColor];case 14->LexyClient.uiSounds?"ON":"OFF";case 15->LexyClient.uiSoundVolume+"%";default->LexyClient.maxHudWidgets+" max";};
            rect(c,rx+rw-124,y+7,111,25,SURFACE);label(c,value,rx+rw-113,y+15,97,WHITE);
        }
        c.disableScissor();label(c,"Scroll for more visual options",rx+9,panelY+panelH-25,rw-22,MUTED);
    }
    private double editorScale(){return Math.min((rightW()-18.0)/Math.max(1,width),(contentH()-4.0)/Math.max(1,height));}
    private void renderEditor(DrawContext c,int mx,int my){
        int x=rightX(),y=contentY(),w=rightW(),h=contentH();
        rect(c,x,y,w,h,0xFF121E2E);double scale=editorScale();
        for(int xx=x+18;xx<x+w;xx+=18)rect(c,xx,y,1,h,0xFF1B2939);
        for(int yy=y+18;yy<y+h;yy+=18)rect(c,x,yy,w,1,0xFF1B2939);
        clip(c,x,y,x+w,y+h);
        int fallback=8;
        for(int i=0;i<LexyClient.MODULES.size();i++){
            var m=LexyClient.MODULES.get(i);if(!m.drawInHud()||!LexyClient.enabled.getOrDefault(m.id(),false))continue;
            int px=x+(int)Math.round(LexyClient.positions.getOrDefault(m.id(),8)*scale);
            int py=y+(int)Math.round(LexyClient.positions.getOrDefault(m.id()+".y",fallback)*scale);
            int ww=Math.min(177,w-15);
            rect(c,px,py,ww,19,i==selected?0xFF2A5E90:SURFACE);rect(c,px,py,3,19,accent());
            label(c,m.id(),px+7,py+6,ww-11,WHITE);fallback+=LexyClient.hudSpacing;
        }
        c.disableScissor();rect(c,x+w-60,y+h-27,55,22,SURFACE);text(c,"RESET",x+w-50,y+h-20,WHITE);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        mx=logical(mx);my=logical(my);
        if(button!=0)return super.mouseClicked(mx,my,button);
        if(inside(mx,my,panelX,panelY,panelW,panelH)) {
            if(modular() && inside(mx,my,rightX(),contentY(),rightW(),contentH()))LexySounds.toggle();
            else LexySounds.tap();
        }
        if(inside(mx,my,rightX()+rightW()-44,panelY+17,35,28)){close();return true;}
        if(inside(mx,my,rightX()+rightW()-157,panelY+17,107,28)||inside(mx,my,panelX+13,panelY+panelH-93,sidebarW-22,24)){
            moveHud();return true;
        }
        if(navLimit()>0 && inside(mx,my,panelX+12,panelY+panelH-65,sidebarW-24,23)){
            if(mx<panelX+15+(sidebarW-28)/2)navScroll=Math.max(0,navScroll-2);
            else navScroll=Math.min(navLimit(),navScroll+2);
            return true;
        }
        for(int i=navScroll;i<Math.min(TABS.length,navScroll+navCapacity());i++){
            int yy=panelY+68+(i-navScroll)*NAV_ROW;
            if(inside(mx,my,panelX+10,yy,sidebarW-16,26)){
                if(!editor){tab=i;scroll=0;selected=-1;updateTextFields();}
                return true;
            }
        }
        if(editor){
            int x=rightX(),y=contentY(),w=rightW(),h=contentH();
            if(inside(mx,my,x+w-60,y+h-27,55,22)){LexyClient.positions.clear();LexyClient.save();return true;}
            int fallback=8;double scale=editorScale();
            for(int i=0;i<LexyClient.MODULES.size();i++){
                var m=LexyClient.MODULES.get(i);if(!m.drawInHud()||!LexyClient.enabled.getOrDefault(m.id(),false))continue;
                int px=x+(int)Math.round(LexyClient.positions.getOrDefault(m.id(),8)*scale);
                int py=y+(int)Math.round(LexyClient.positions.getOrDefault(m.id()+".y",fallback)*scale);
                if(inside(mx,my,px,py,Math.min(177,w-15),19)){selected=i;dragging=true;dragDX=(int)mx-px;dragDY=(int)my-py;return true;}
                fallback+=LexyClient.hudSpacing;
            }
            return super.mouseClicked(mx,my,button);
        }
        int rx=rightX(),rw=rightW();
        if(modular()){
            int yy=contentY();int cols=columns();
            if(inside(mx,my,rx,panelY+panelH-42,rw,22)){
                if(mx<rx+65)scroll=Math.max(0,scroll-rowsVisible());
                else if(mx>rx+rw-65)scroll=Math.min(maxScroll(filtered().size()),scroll+rowsVisible());
                return true;
            }
            if(inside(mx,my,rx,yy,rw,contentH())){
                int cw=rw/cols;int col=(int)(mx-rx)/cw,row=(int)(my-yy)/ROW_H;
                int i=(row+scroll)*cols+col;List<LexyClient.Module> list=filtered();
                if(i>=0&&i<list.size()){var m=list.get(i);LexyClient.enabled.put(m.id(),!LexyClient.enabled.getOrDefault(m.id(),false));LexyClient.save();return true;}
            }
        }else if(tab==6){
            int pw=(rw-20)/3,yy=panelY+63;
            for(int i=0;i<3;i++)if(inside(mx,my,rx+i*pw,yy,pw-5,29)){
                OptimizationManager.apply(new String[]{"MAX FPS","BALANCED","QUALITY"}[i]);return true;
            }
            int listY=panelY+115,rowH=48,avail=Math.max(40,panelY+panelH-35-listY);
            if(inside(mx,my,rx,listY,rw,avail)){
                int idx=scroll+(int)(my-listY)/rowH;
                if(idx<OptimizationManager.OPTIONS.size()){
                    if(inside(mx,my,rx+rw-143,listY+(idx-scroll)*rowH+9,24,26)){OptimizationManager.adjust(idx,-1);return true;}
                    if(inside(mx,my,rx+rw-31,listY+(idx-scroll)*rowH+9,24,26)){OptimizationManager.adjust(idx,1);return true;}
                }
            }
        }else if(tab==7){
            SpotifyController spotify=SpotifyController.INSTANCE;
            if(!spotify.isConnected()){
                int top=panelY+65,buttonW=(rw-35)/2,loginY=top+47;
                if(inside(mx,my,rx+11,loginY,buttonW,28)){
                    if(spotify.pending())spotify.cancelLogin();
                    else{LexyClient.spotifyClientId=spotifyClientId.getText().trim();LexyClient.save();spotify.setClientId(LexyClient.spotifyClientId);spotify.connect();}
                    return true;
                }
                if(inside(mx,my,rx+17+buttonW,loginY,rw-buttonW-28,28)){
                    if(spotify.pending())spotify.copyAuthUrl();else openSpotifyDashboard();return true;
                }
                if(inside(mx,my,rx+11,top+192,rw-22,28)){
                    spotify.completeFromRedirect(spotifyRedirect.getText());return true;
                }
                return super.mouseClicked(mx,my,button);
            }
            if(inside(mx,my,rx+rw-103,panelY+61,91,23)){spotify.disconnect();return true;}
            if(inside(mx,my,rx+rw-84,panelY+88,73,20)){
                spotify.search(spotifySearch.getText());scroll=0;showDevices=false;return true;
            }
            int by=panelY+122,bw=(rw-28)/5;
            for(int i=0;i<5;i++)if(inside(mx,my,rx+11+i*bw,by,bw-4,28)){
                switch(i){case 0->spotify.previous();case 1->spotify.toggle();case 2->spotify.next();case 3->{showDevices=false;scroll=0;}case 4->{showDevices=true;scroll=0;}}
                return true;
            }
            int pcY=by+37;
            if(inside(mx,my,rx+11,pcY,(rw-30)/2,24)){spotify.useComputer();return true;}
            if(inside(mx,my,rx+18+(rw-30)/2,pcY,(rw-30)/2,24)){spotify.refreshDevices();return true;}
            int listY=pcY+33,listHeight=Math.max(25,panelY+panelH-27-listY);
            if(inside(mx,my,rx+11,listY,rw-22,listHeight)){
                int row=(int)(my-listY)/(showDevices?31:32);
                if(showDevices){var devices=spotify.devices();if(row<devices.size())spotify.transfer(devices.get(row));}
                else {var tracks=spotify.tracks();if(row+scroll<tracks.size()){
                    var track=tracks.get(row+scroll);if(mx>=rx+rw-38)spotify.queue(track);else spotify.play(track);
                }}return true;
            }
        }else if(tab==8){
            LocalMusicPlayer player=LocalMusicPlayer.INSTANCE;
            int top=panelY+64,buttonY=top+69,playerY=buttonY+46;
            int refreshW=Math.min(89,(rw-6)/2),folderX=rx+refreshW+6,folderW=rw-refreshW-6;
            if(inside(mx,my,rx,buttonY,refreshW,24)){player.refresh();scroll=0;return true;}
            if(inside(mx,my,folderX,buttonY,folderW,24)){player.openFolder();return true;}
            int bw=Math.max(24,(rw-10)/5);
            for(int i=0;i<5;i++)if(inside(mx,my,rx+i*bw,playerY,bw-5,28)){
                switch(i){case 0->player.previous();case 1->player.togglePause();case 2->player.next();case 3->player.stop();case 4->{player.toggleLoop();LexyClient.save();}}
                return true;
            }
            int volY=playerY+36;
            if(inside(mx,my,rx+rw-115,volY,25,25)){player.setVolume(player.volume()-5);LexyClient.save();return true;}
            if(inside(mx,my,rx+rw-35,volY,25,25)){player.setVolume(player.volume()+5);LexyClient.save();return true;}
            int listY=volY+33,listHeight=Math.max(30,panelY+panelH-27-listY);
            if(inside(mx,my,rx,listY,rw,listHeight)){
                int idx=scroll+(int)(my-listY)/31;
                if(idx<player.tracks().size()){player.play(idx);return true;}
            }
        }else if(tab==10){
            int top=panelY+68,buttonY=top+42,bw=Math.max(60,(rw-26)/2);
            if(inside(mx,my,rx+9,buttonY,bw,24)){
                LexyClient.enabled.put("AutoTotem",!LexyClient.enabled.getOrDefault("AutoTotem",false));LexyClient.save();return true;
            }
            if(inside(mx,my,rx+9+bw+7,buttonY,rw-bw-25,24)){
                totemSettingsOpen=!totemSettingsOpen;scroll=0;return true;
            }
            if(totemSettingsOpen){
                int yy=top+81;int row=48;
                int visible=Math.min(5,Math.max(1,(panelY+panelH-43-yy)/row));
                for(int i=scroll;i<Math.min(5,scroll+visible);i++)if(inside(mx,my,rx+7,yy+(i-scroll)*row,rw-16,42)){
                    switch(i){
                        case 0->AutoTotem.onlyWhenEmpty=!AutoTotem.onlyWhenEmpty;
                        case 1->AutoTotem.lowHealthOnly=!AutoTotem.lowHealthOnly;
                        case 2->AutoTotem.healthThreshold=AutoTotem.healthThreshold>=20?2:AutoTotem.healthThreshold+2;
                        case 3->AutoTotem.intervalTicks=AutoTotem.intervalTicks>=30?2:AutoTotem.intervalTicks+2;
                        case 4->AutoTotem.notify=!AutoTotem.notify;
                    }
                    LexyClient.save();return true;
                }
            }
        }else if(tab==9){
            int yy=panelY+86;
            for(int i=0;i<LexyClient.ACCENTS.length;i++)if(inside(mx,my,rx+8+i*55,yy,45,28)){
                LexyClient.accentChoice=i;LexyClient.save();return true;
            }
            int base=yy+45;int visibleCount=Math.max(1,(panelY+panelH-45-base)/44);
            for(int i=scroll;i<Math.min(17,scroll+visibleCount);i++)if(inside(mx,my,rx+7,base+(i-scroll)*44,rw-15,39)){
                boolean relayout=false;
                switch(i){
                    case 0->{LexyClient.menuScalePercent=LexyClient.menuScalePercent>=110?70:LexyClient.menuScalePercent+5;relayout=true;}
                    case 1->LexyClient.hudScalePercent=LexyClient.hudScalePercent>=150?45:LexyClient.hudScalePercent+5;
                    case 2->LexyClient.hudGridSnap=!LexyClient.hudGridSnap;
                    case 3->LexyClient.hudShowGrid=!LexyClient.hudShowGrid;
                    case 4->LexyClient.hudGridSize=LexyClient.hudGridSize>=28?4:LexyClient.hudGridSize+4;
                    case 5->LexyClient.hudBackground=!LexyClient.hudBackground;
                    case 6->LexyClient.hudShadow=!LexyClient.hudShadow;
                    case 7->LexyClient.compactHud=!LexyClient.compactHud;
                    case 8->LexyClient.spotifyHud=!LexyClient.spotifyHud;
                    case 9->LexyClient.hudOpacity=LexyClient.hudOpacity>=245?65:LexyClient.hudOpacity+30;
                    case 10->LexyClient.hudSpacing=LexyClient.hudSpacing>=33?18:LexyClient.hudSpacing+3;
                    case 11->LexyClient.lowHealthThreshold=LexyClient.lowHealthThreshold>=20?2:LexyClient.lowHealthThreshold+2;
                    case 12->LexyClient.armorWarnPercent=LexyClient.armorWarnPercent>=65?5:LexyClient.armorWarnPercent+5;
                    case 13->LexyClient.hudTextColor=(LexyClient.hudTextColor+1)%3;
                    case 14->LexyClient.uiSounds=!LexyClient.uiSounds;
                    case 15->LexyClient.uiSoundVolume=LexyClient.uiSoundVolume>=80?10:LexyClient.uiSoundVolume+10;
                    case 16->LexyClient.maxHudWidgets=LexyClient.maxHudWidgets>=10?3:LexyClient.maxHudWidgets+1;
                }
                LexyClient.save();if(relayout)clearAndInit();return true;
            }
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        mx=logical(mx);my=logical(my);dx=logical(dx);dy=logical(dy);
        if(editor&&dragging&&selected>=0&&selected<LexyClient.MODULES.size()){
            var m=LexyClient.MODULES.get(selected);double scale=editorScale();
            LexyClient.positions.put(m.id(),(int)Math.max(0,Math.min(width-55,(mx-dragDX-rightX())/scale)));
            LexyClient.positions.put(m.id()+".y",(int)Math.max(0,Math.min(height-20,(my-dragDY-contentY())/scale)));
            return true;
        }return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button){
        mx=logical(mx);my=logical(my);
        if(dragging){dragging=false;LexyClient.save();return true;}
        return super.mouseReleased(mx,my,button);
    }
    @Override public boolean mouseScrolled(double mx,double my,double amount){
        mx=logical(mx);my=logical(my);
        if(inside(mx,my,panelX+7,panelY+65,sidebarW-8,panelH-117)){
            navScroll=Math.max(0,Math.min(navLimit(),navScroll-(int)Math.signum(amount)*2));return true;
        }
        if(inside(mx,my,rightX(),contentY(),rightW(),contentH())||tab==6||tab==7||tab==8||tab==9||tab==10){
            int max=0;
            if(modular())max=maxScroll(filtered().size());
            else if(tab==6)max=Math.max(0,OptimizationManager.OPTIONS.size()-Math.max(1,(panelY+panelH-35-(panelY+115))/48));
            else if(tab==7)max=Math.max(0,(showDevices?SpotifyController.INSTANCE.devices().size():SpotifyController.INSTANCE.tracks().size())-Math.max(1,(panelY+panelH-27-(panelY+318))/(showDevices?31:32)));
            else if(tab==8)max=Math.max(0,LocalMusicPlayer.INSTANCE.tracks().size()-Math.max(1,(panelY+panelH-27-(panelY+248))/31));
            else if(tab==9)max=Math.max(0,17-Math.max(1,(panelY+panelH-45-(panelY+131))/44));
            else if(tab==10)max=totemSettingsOpen?Math.max(0,5-Math.max(1,(panelY+panelH-43-(panelY+149))/48)):0;
            scroll=Math.max(0,Math.min(max,scroll-(int)Math.signum(amount)*2));return true;
        }return super.mouseScrolled(mx,my,amount);
    }
    @Override public void close(){LexySounds.close();LexyClient.save();if(client!=null)client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}