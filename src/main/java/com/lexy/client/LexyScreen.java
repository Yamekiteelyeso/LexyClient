package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;

/** Handmade graphite/blue dashboard. Each visible control invokes a real setting or API call. */
public final class LexyScreen extends Screen {
    private static final int BG=0xFF0A101B, PANEL=0xFF152235, SURFACE=0xFF20334A;
    private static final int OUTLINE=0xFF334B64, WHITE=0xFFF3F8FF, MUTED=0xFF9FB3CA;
    private static final int BG_HOVER=0xFF203852, BG_ACTIVE=0xFF1C4268;
    private static final String[] NAV_SHORT={"ALL","CBT","HUD","STS","UTL","VID","FPS","SPT","MUS","UI","PVP","KEY"};
    private static final String[] TABS={"All Modules","Combat","HUD","Statistics","Utilities","Performance","Optimization","Spotify","Local Music","Appearance","Cheats","Keybinds"};
    private static final int ROW_H=54, NAV_ROW=34;
    private final boolean editor;
    private int tab, scroll=0, navScroll=0, selected=-1, dragDX=0, dragDY=0;
    private final Screen parent;
    private boolean dragging=false, totemSettingsOpen=false, triggerSettingsOpen=false, aimSettingsOpen=false, clickerSettingsOpen=false, spotifyFreeView=true;
    private int cheatSection=0; // 0 AutoTotem, 1 TriggerBot, 2 Aim Assist, 3 AutoClicker
    private int spotifyView=0; // 0=tracks, 1=devices, 2=playlists
    private String query="";
    private TextFieldWidget search,spotifyClientId,spotifySearch,spotifyRedirect,spotifyFreeSearch;
    private int panelX,panelY,panelW,panelH,sidebarW;
    private float menuScale=1f;
    private String spotifyHint="";
    private boolean playedOpenSound;
    private final float[] navHover=new float[TABS.length];
    private final Map<String,String> valuesCache=new HashMap<>();
    private long valuesCacheAt;
    private int lastHoveredTab=-1;
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
        panelW=Math.min(1010,Math.max(410,vw-18));panelH=Math.min(620,Math.max(360,vh-18));
        panelX=(vw-panelW)/2;panelY=(vh-panelH)/2;sidebarW=vw<760?141:177;
        int rx=rightX();int rw=rightW();
        search=new TextFieldWidget(textRenderer,rx+10,panelY+63,rw-20,21,Text.literal("Search modules"));
        search.setMaxLength(80);search.setPlaceholder(Text.literal("Search your modules..."));
        search.setText(query);search.setChangedListener(value->{query=value;scroll=0;});addDrawableChild(search);
        spotifyClientId=new TextFieldWidget(textRenderer,rx+11,panelY+84,Math.max(95,rw-22),20,Text.literal("Spotify Client ID"));
        spotifyClientId.setMaxLength(90);spotifyClientId.setPlaceholder(Text.literal("Paste Spotify Client ID (not secret)"));
        spotifyClientId.setText(LexyClient.spotifyClientId);addDrawableChild(spotifyClientId);
        spotifyRedirect=new TextFieldWidget(textRenderer,rx+11,panelY+228,Math.max(95,rw-22),22,Text.literal("Spotify callback URL"));
        spotifyRedirect.setMaxLength(4096);spotifyRedirect.setPlaceholder(Text.literal("Paste http://127.0.0.1:43871/callback?..."));addDrawableChild(spotifyRedirect);
        spotifySearch=new TextFieldWidget(textRenderer,rx+11,panelY+149,Math.max(95,rw-100),20,Text.literal("Search tracks"));
        spotifySearch.setMaxLength(80);spotifySearch.setPlaceholder(Text.literal("Search songs or artists..."));addDrawableChild(spotifySearch);
        spotifyFreeSearch=new TextFieldWidget(textRenderer,rx+11,panelY+173,Math.max(95,rw-22),23,Text.literal("Free Spotify search"));
        spotifyFreeSearch.setMaxLength(120);spotifyFreeSearch.setPlaceholder(Text.literal("Song, artist or playlist..."));addDrawableChild(spotifyFreeSearch);
        updateTextFields();
    }
    private int accent(){return LexyClient.accent();}
    private int rightX(){return panelX+sidebarW+9;}
    private int rightW(){return panelW-sidebarW-20;}
    private int contentY(){return panelY+96;}
    private int contentH(){return Math.max(35,panelH-157);}
    private int columns(){return rightW()>480?2:1;}
    private int navCapacity(){return Math.max(2,(panelH-254)/NAV_ROW);}
    private int navLimit(){return Math.max(0,TABS.length-navCapacity());}
    private int rowsVisible(){return Math.max(1,contentH()/ROW_H);}
    private int capacity(){return columns()*rowsVisible();}
    private int maxScroll(int n){return Math.max(0,(int)Math.ceil(n/(double)columns())-rowsVisible());}
    private int page(){return scroll/Math.max(1,rowsVisible())+1;}
    private int pages(int n){return Math.max(1,(int)Math.ceil(n/(double)Math.max(1,capacity())));}
    private void updateTextFields(){
        if(search!=null)search.visible=!editor&&tab<=4;
        if(spotifyClientId!=null)spotifyClientId.visible=!editor&&tab==7&&!SpotifyController.INSTANCE.isConnected()&&!spotifyFreeView;
        if(spotifySearch!=null)spotifySearch.visible=!editor&&tab==7&&SpotifyController.INSTANCE.isConnected();
        if(spotifyRedirect!=null)spotifyRedirect.visible=!editor&&tab==7&&!SpotifyController.INSTANCE.isConnected()&&!spotifyFreeView;
        if(spotifyFreeSearch!=null)spotifyFreeSearch.visible=!editor&&tab==7&&!SpotifyController.INSTANCE.isConnected()&&spotifyFreeView;
        if(spotifyFreeSearch!=null&&!spotifyFreeSearch.visible)spotifyFreeSearch.setFocused(false);
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
    private static int blend(int from,int to,float t){
        t=Math.max(0f,Math.min(1f,t));
        int r=(int)(((from>>16)&255)*(1-t)+((to>>16)&255)*t);
        int g=(int)(((from>>8)&255)*(1-t)+((to>>8)&255)*t);
        int b=(int)((from&255)*(1-t)+(to&255)*t);
        return 0xFF000000|(r<<16)|(g<<8)|b;
    }
    private int navStart(){return panelY+100;}
    private int navButtonsY(){return panelY+panelH-144;}
    private int countActiveHud(){
        int n=0;
        for(var m:LexyClient.MODULES)
            if(HudRenderer.isRelevant(m)&&LexyClient.enabled.getOrDefault(m.id(),false))n++;
        return n;
    }
    private void actionButton(DrawContext c,String title,int x,int y,int w,int h,boolean hover,boolean active){
        rounded(c,x,y,w,h,active?BG_ACTIVE:(hover?BG_HOVER:SURFACE));
        frame(c,x,y,w,h,active?accent():OUTLINE);
        label(c,title,x+9,y+(h-9)/2,w-18,WHITE);
    }
    private String sectionTitle(){
        if(editor)return "HUD EDITOR";
        return switch(tab){case 5,6->"PERFORMANCE";case 7,8->"AUDIO";case 9->"PERSONALIZATION";case 10->"COMBAT";case 11->"KEYBOARD";default->"DASHBOARD";};
    }

    private List<LexyClient.Module> filtered(){
        String needle=query.trim().toLowerCase(Locale.ROOT);
        ArrayList<LexyClient.Module> list=new ArrayList<>();
        for(LexyClient.Module m:LexyClient.MODULES){
            if ("AutoTotem".equals(m.category()) || "TriggerBot".equals(m.category()) || "AimAssist".equals(m.category()) || "AutoClicker".equals(m.category()) || "Music".equals(m.category()) || "Performance".equals(m.category())) continue;
            // All Modules is a compact list of actual toggleable HUD indicators, not 80 fake switches.
            if(tab==0 && !HudRenderer.isRelevant(m))continue;
            if(tab>0&&tab<=4&&!m.category().equals(TABS[tab]))continue;
            if(!m.id().toLowerCase(Locale.ROOT).contains(needle))continue;
            list.add(m);
        }
        return list;
    }
    private boolean modular(){return tab<=4;}
    @Override public void render(DrawContext c,int mx,int my,float dt){
        // A consistent two-pane layout. Every interactive hit target uses these same coordinates.
        renderBackground(c);
        c.fillGradient(0,0,width,height,0xEF030914,0xE9091626);
        int logicalMX=(int)logical(mx),logicalMY=(int)logical(my);
        c.getMatrices().push();c.getMatrices().scale(menuScale,menuScale,1);
        rounded(c,panelX-9,panelY-7,panelW+18,panelH+16,0x90000000);
        rounded(c,panelX-3,panelY-3,panelW+6,panelH+6,OUTLINE);
        rounded(c,panelX,panelY,panelW,panelH,BG);
        c.fillGradient(panelX+2,panelY+2,panelX+panelW-2,panelY+panelH-2,0xFF101D2D,0xFF09111C);
        rect(c,panelX+2,panelY+2,panelW-4,3,0xFF214361);
        rect(c,panelX+2,panelY+2,Math.min(245,panelW/3),3,accent());

        // Branding and navigation rail.
        rect(c,panelX+3,panelY+5,sidebarW,panelH-8,0xFF111D2B);
        rect(c,panelX+sidebarW+4,panelY+12,1,panelH-24,OUTLINE);
        rounded(c,panelX+13,panelY+14,43,43,0xFF1D344A);
        frame(c,panelX+13,panelY+14,43,43,0xFF365A7A);
        c.drawTexture(LexyClient.LOGO,panelX+16,panelY+17,37,37,0,0,256,256,256,256);
        label(c,"LEXY",panelX+64,panelY+19,sidebarW-75,WHITE);
        label(c,"CLIENT  /  2.8",panelX+64,panelY+35,sidebarW-75,0xFF7DB8F2);
        rect(c,panelX+15,panelY+69,sidebarW-27,1,OUTLINE);
        label(c,"NAVIGATION",panelX+18,panelY+81,sidebarW-32,0xFF718DA9);
        navScroll=Math.max(0,Math.min(navScroll,navLimit()));
        int visible=navCapacity();
        for(int i=navScroll;i<Math.min(TABS.length,navScroll+visible);i++){
            int yy=navStart()+(i-navScroll)*NAV_ROW;
            boolean active=i==tab&&!editor;
            boolean hover=inside(logicalMX,logicalMY,panelX+10,yy,sidebarW-16,29);
            navHover[i]+=( (hover?1f:0f)-navHover[i])*.27f;
            if(active){
                rounded(c,panelX+10,yy,sidebarW-17,29,BG_ACTIVE);
                rect(c,panelX+10,yy+5,3,19,accent());
            }else if(navHover[i]>.015f){
                rounded(c,panelX+10,yy,sidebarW-17,29,blend(0xFF111D2B,BG_HOVER,navHover[i]));
            }
            rounded(c,panelX+17,yy+6,27,17,active?0xFF2D6799:0xFF203146);
            label(c,NAV_SHORT[i],panelX+20,yy+11,21,active?WHITE:0xFF87A5C4);
            String title=sidebarW<150?switch(i){case 0->"Modules";case 3->"Stats";case 5->"Video";case 6->"FPS Tuning";case 8->"Music";default->TABS[i];}:TABS[i];
            label(c,title,panelX+50,yy+10,sidebarW-61,active?WHITE:MUTED);
        }
        if(navLimit()>0){
            int by=navButtonsY(),bw=(sidebarW-31)/2;
            actionButton(c,"< UP",panelX+13,by,bw,25,inside(logicalMX,logicalMY,panelX+13,by,bw,25),false);
            actionButton(c,"DOWN >",panelX+19+bw,by,bw,25,inside(logicalMX,logicalMY,panelX+19+bw,by,bw,25),false);
        }
        int statusY=panelY+panelH-113;
        rounded(c,panelX+13,statusY,sidebarW-22,21,0xFF192C40);
        rect(c,panelX+20,statusY+8,5,5,0xFF75DFC0);
        label(c,countActiveHud()+" HUD  /  "+TABS.length+" SECTIONS",panelX+31,statusY+6,sidebarW-42,0xFFB5CDE3);
        actionButton(c,"MOVE HUD",panelX+13,panelY+panelH-85,sidebarW-22,28,
                inside(logicalMX,logicalMY,panelX+13,panelY+panelH-85,sidebarW-22,28),false);
        rect(c,panelX+14,panelY+panelH-47,sidebarW-24,1,OUTLINE);
        label(c,"RSHIFT  TO TOGGLE",panelX+17,panelY+panelH-36,sidebarW-31,0xFF9BC9FC);
        label(c,"ESC  TO CLOSE",panelX+17,panelY+panelH-22,sidebarW-31,0xFF7694B1);

        // The title bar deliberately reserves the right-hand side for actions.
        int rx=rightX(),rw=rightW();
        rounded(c,rx,panelY+12,rw,44,PANEL);
        rect(c,rx,panelY+14,3,39,accent());
        label(c,sectionTitle()+"   /   "+(editor?"LAYOUT":String.format(Locale.ROOT,"%02d",tab+1)),rx+13,panelY+18,rw-180,0xFF7EABD4);
        String heading=editor?"Position widgets":tab==5?"Video options":tab==6?"Performance tuning":tab==7?"Spotify & Connect":tab==8?"Local audio":tab==9?"Appearance":tab==10?"Combat modules":TABS[tab];
        label(c,heading,rx+13,panelY+35,rw-179,WHITE);
        actionButton(c,"EDIT HUD",rx+rw-157,panelY+18,106,28,
                inside(logicalMX,logicalMY,rx+rw-157,panelY+18,106,28),false);
        actionButton(c,"X",rx+rw-43,panelY+18,34,28,
                inside(logicalMX,logicalMY,rx+rw-43,panelY+18,34,28),false);

        if(editor)renderEditor(c,logicalMX,logicalMY);
        else if(modular())renderModules(c,logicalMX,logicalMY);
        else if(tab==5||tab==6)renderOptimization(c,logicalMX,logicalMY);
        else if(tab==7)renderSpotify(c,logicalMX,logicalMY);
        else if(tab==8)renderLocalMusic(c,logicalMX,logicalMY);
        else if(tab==10)renderCheats(c,logicalMX,logicalMY);
        else renderStyle(c,logicalMX,logicalMY);
        updateTextFields();
        super.render(c,logicalMX,logicalMY,dt);
        int footY=panelY+panelH-25;
        rect(c,rx,footY-7,rw,1,OUTLINE);
        String footer=editor?"EDIT MODE   ·   Drag elements to reposition":tab==7?"SPOTIFY   ·   Official Connect or local files":tab==8?"LOCAL PLAYER   ·   MP3 / WAV / AIFF":"RSHIFT  Toggle menu     ·     RCTRL  HUD     ·     C  Zoom";
        label(c,footer,rx+8,footY,rw-16,0xFF819DB8);
        c.getMatrices().pop();
    }
    private void renderModules(DrawContext c,int mx,int my){
        List<LexyClient.Module> list=filtered();int rx=rightX(),rw=rightW(),y=contentY(),cols=columns();
        scroll=Math.min(scroll,maxScroll(list.size()));
        clip(c,rx,y,rx+rw,y+contentH());
        for(int i=0;i<capacity()&&scroll*cols+i<list.size();i++){
            int idx=scroll*cols+i;LexyClient.Module mod=list.get(idx);
            int col=i%cols,row=i/cols,cw=rw/cols-6,x=rx+col*(rw/cols),cy=y+row*ROW_H;
            boolean editable=HudRenderer.isRelevant(mod);
            boolean on=editable&&LexyClient.enabled.getOrDefault(mod.id(),false);
            boolean hover=inside(mx,my,x,cy,cw,ROW_H-4);
            rounded(c,x,cy,cw,ROW_H-4,hover?BG_HOVER:PANEL);
            frame(c,x,cy,cw,ROW_H-4,hover?0xFF456583:OUTLINE);
            rect(c,x,cy+8,3,ROW_H-20,on?accent():0xFF364E67);
            label(c,mod.id(),x+12,cy+10,cw-87,WHITE);
            long currentTime=System.nanoTime();
            if(currentTime-valuesCacheAt>220_000_000L){valuesCache.clear();valuesCacheAt=currentTime;}
            String value=valuesCache.get(mod.id());
            if(value==null){
                try{value=mod.value().get();}catch(Exception ex){value="Unavailable";}
                if(value==null)value="–";
                valuesCache.put(mod.id(),value);
            }
            label(c,value,x+12,cy+31,cw-28,MUTED);
            if(editable){
                rounded(c,x+cw-59,cy+13,47,23,on?0xFF276A9E:0xFF304359);
                rounded(c,x+cw-(on?32:55),cy+16,17,17,on?WHITE:0xFFB2C0D0);
            }else{
                rounded(c,x+cw-47,cy+15,37,20,0xFF20483F);
                label(c,"LIVE",x+cw-40,cy+21,30,0xFF92E6C9);
            }
        }
        c.disableScissor();
        if(list.isEmpty()){
            rounded(c,rx+10,y+10,rw-20,75,PANEL);
            label(c,"NO MATCHES",rx+24,y+30,rw-45,WHITE);
            label(c,"Try another term or category.",rx+24,y+49,rw-45,MUTED);
        }
        drawScrollbar(c,list.size(),y,contentH());
        int pagerY=panelY+panelH-53;
        actionButton(c,"< PREV",rx,pagerY,73,23,inside(mx,my,rx,pagerY,73,23),scroll>0);
        actionButton(c,"NEXT >",rx+rw-73,pagerY,73,23,inside(mx,my,rx+rw-73,pagerY,73,23),scroll<maxScroll(list.size()));
        label(c,"PAGE "+page()+" / "+pages(list.size())+"   ·   "+list.size()+" WIDGETS",rx+83,pagerY+7,rw-172,MUTED);
    }
    private void drawScrollbar(DrawContext c,int count,int y,int h){
        int mx=maxScroll(count);if(mx<1)return;int xx=rightX()+rightW()-4;
        rect(c,xx,y,3,h,OUTLINE);int thumb=Math.max(18,(int)(h*((double)rowsVisible()/(mx+rowsVisible()))));
        rect(c,xx,y+(h-thumb)*scroll/mx,3,thumb,accent());
    }
    private void renderOptimization(DrawContext c,int mx,int my){
        int rx=rightX(),rw=rightW(),yy=panelY+63;
        int buttonWidth=(rw-10)/5;
        for(int i=0;i<5;i++){
            String name=new String[]{"MAX FPS","BALANCED","QUALITY","POJAV",OptimizationManager.adaptiveEnabled?"AUTO ON":"AUTO OFF"}[i];int x=rx+i*buttonWidth;
            boolean hover=inside(mx,my,x,yy,buttonWidth-5,29);
            rounded(c,x,yy,buttonWidth-5,29,hover?BG_ACTIVE:SURFACE);
            frame(c,x,yy,buttonWidth-5,29,hover?accent():OUTLINE);
            label(c,name,x+8,yy+10,buttonWidth-17,WHITE);
        }
        text(c,"Video settings  -  changes are saved to options.txt",rx+4,yy+37,MUTED);
        int listY=panelY+115,rowH=48,avail=Math.max(40,panelY+panelH-35-listY);
        scroll=Math.min(scroll,Math.max(0,OptimizationManager.OPTIONS.size()-(avail/rowH)));
        clip(c,rx,listY,rx+rw,listY+avail);
        for(int j=0;j<=avail/rowH&&j+scroll<OptimizationManager.OPTIONS.size();j++){
            var item=OptimizationManager.OPTIONS.get(j+scroll);int y=listY+j*rowH;
            rounded(c,rx,y,rw-6,rowH-4,PANEL);
            frame(c,rx,y,rw-6,rowH-4,OUTLINE);
            rect(c,rx,y+8,3,rowH-20,accent());
            label(c,item.name(),rx+12,y+6,rw-152,WHITE);
            label(c,item.help(),rx+12,y+23,rw-151,MUTED);
            actionButton(c,"-",rx+rw-143,y+9,24,26,inside(mx,my,rx+rw-143,y+9,24,26),false);
            rounded(c,rx+rw-110,y+9,75,26,SURFACE);
            label(c,item.value().get(),rx+rw-105,y+18,65,WHITE);
            actionButton(c,"+",rx+rw-31,y+9,24,26,inside(mx,my,rx+rw-31,y+9,24,26),false);
        }
        c.disableScissor();
    }
    private void renderSpotify(DrawContext c,int mx,int my){
        int rx=rightX(),rw=rightW(),top=panelY+63;
        SpotifyController sp=SpotifyController.INSTANCE;
        if(!sp.isConnected() && spotifyFreeView){
            rounded(c,rx+11,top+3,rw-22,51,PANEL);
            rect(c,rx+11,top+8,3,40,0xFF1DB954);
            label(c,"SPOTIFY FREE",rx+22,top+13,rw-43,WHITE);
            label(c,"No developer app or login in LexyClient",rx+22,top+32,rw-43,MUTED);
            label(c,"Search Spotify on your phone or computer:",rx+13,top+88,rw-26,MUTED);
            rounded(c,rx+11,top+143,rw-22,30,0xFF1C7049);
            label(c,"SEARCH IN SPOTIFY",rx+23,top+154,rw-39,WHITE);
            int freeY=top+181,freeW=(rw-34)/2;
            rounded(c,rx+11,freeY,freeW,30,SURFACE);
            label(c,"OPEN SPOTIFY",rx+20,freeY+10,freeW-13,WHITE);
            rounded(c,rx+18+freeW,freeY,rw-freeW-29,30,SURFACE);
            label(c,"YOUR LIBRARY",rx+27+freeW,freeY+10,rw-freeW-43,WHITE);
            rounded(c,rx+11,top+238,rw-22,30,PANEL);
            label(c,"API LOGIN / ADVANCED CONTROLS",rx+23,top+248,rw-40,0xFF9DD4FF);
            label(c,"Free mode opens the official Spotify app or website.",rx+12,top+294,rw-23,MUTED);
            label(c,"Playback stays in Spotify and follows Free plan rules.",rx+12,top+310,rw-23,MUTED);
            return;
        }
        if(!sp.isConnected()){
            label(c,"ACCOUNT CONNECTION",rx+12,top+3,rw-24,WHITE);
            label(c,"Enter your Spotify Developer Client ID",rx+12,top+16,rw-24,MUTED);
            int bw=(rw-35)/2,by=top+50;
            rounded(c,rx+11,by,bw,28,sp.pending()?SURFACE:0xFF245C8B);
            label(c,sp.pending()?"CANCEL LOGIN":"CONNECT ACCOUNT",rx+19,by+10,bw-14,WHITE);
            rounded(c,rx+17+bw,by,rw-bw-28,28,SURFACE);
            label(c,sp.pending()?"COPY LOGIN LINK":"DEVELOPER PORTAL",rx+25+bw,by+10,rw-bw-39,WHITE);
            label(c,"Mobile: authorize in your browser, then return here.",rx+12,top+91,rw-20,MUTED);
            label(c,"If it fails to return, copy the browser's callback URL.",rx+12,top+108,rw-20,MUTED);
            label(c,"CALLBACK URL  /  MOBILE FALLBACK",rx+12,top+148,rw-25,WHITE);
            rounded(c,rx+11,top+198,rw-22,25,SURFACE);
            label(c,"PASTE FROM CLIPBOARD",rx+20,top+206,rw-35,WHITE);
            rounded(c,rx+11,top+231,rw-22,28,0xFF245C8B);
            label(c,sp.busy()?"VERIFYING ACCOUNT...":"FINISH LOGIN FROM CALLBACK",rx+20,top+241,rw-36,WHITE);
            rounded(c,rx+11,top+264,rw-22,34,PANEL);
            label(c,sp.status(),rx+18,top+274,rw-34,0xFF97C7F5);
            if(panelH>440){
                label(c,"Redirect: "+sp.redirectUri(),rx+12,top+309,rw-25,MUTED);
                label(c,"Developer apps need Premium + authorized Spotify user.",rx+12,top+324,rw-25,MUTED);
                label(c,"Music plays in Spotify, not inside Minecraft.",rx+12,top+339,rw-25,MUTED);
                rounded(c,rx+11,top+354,rw-22,28,SURFACE);
                label(c,"USE FREE SPOTIFY INSTEAD",rx+20,top+364,rw-36,WHITE);
            }
            return;
        }
        label(c,"CONNECTED"+(sp.profileName().isBlank()?"":"  /  "+sp.profileName()),rx+13,top+4,rw-240,0xFF91E1BC);
        rounded(c,rx+rw-206,top,99,24,SURFACE);
        label(c,"OPEN SPOTIFY",rx+rw-198,top+8,88,WHITE);
        rounded(c,rx+rw-102,top,92,24,0xFF333F4B);
        label(c,"LOG OUT",rx+rw-94,top+8,78,WHITE);
        rounded(c,rx+10,top+31,rw-20,54,PANEL);
        rect(c,rx+10,top+39,2,38,0xFF65B89A);
        label(c,sp.playing(),rx+20,top+40,rw-40,WHITE);
        label(c,sp.status(),rx+20,top+56,rw-40,MUTED);
        label(c,SpotifyController.timeString(sp.progressMs())+" / "+SpotifyController.timeString(sp.durationMs()),rx+20,top+69,rw-38,MUTED);
        int trackX=rx+11, trackY=top+87, trackW=rw-22;
        rect(c,trackX,trackY,trackW,3,SURFACE);
        if(sp.durationMs()>0)rect(c,trackX,trackY,Math.min(trackW,Math.max(0,Math.round(trackW*(sp.progressMs()/(float)sp.durationMs())))),3,accent());
        rounded(c,rx+rw-85,panelY+149,74,20,SURFACE);
        label(c,"SEARCH",rx+rw-75,panelY+156,60,WHITE);
        int by=panelY+180,bw=(rw-28)/5;
        String[] transport={"PREV",sp.paused()?"PLAY":"PAUSE","NEXT","TRACKS","REFRESH"};
        for(int i=0;i<5;i++){
            int xx=rx+11+i*bw;
            rounded(c,xx,by,bw-4,27,SURFACE);
            label(c,transport[i],xx+6,by+9,bw-13,WHITE);
        }
        int optionsY=panelY+214,optionsW=(rw-28)/5;
        String[] options={sp.shuffleEnabled()?"SHUF ON":"SHUFFLE", "REPEAT "+sp.repeatMode().toUpperCase(Locale.ROOT),"VOL -","VOL +",sp.volumePercent()<0?"VOL --":sp.volumePercent()+"%"};
        for(int i=0;i<5;i++){
            int xx=rx+11+i*optionsW;
            rounded(c,xx,optionsY,optionsW-4,25,(i<2&&((i==0&&sp.shuffleEnabled())||(i==1&&!sp.repeatMode().equals("off"))))?0xFF2D5272:SURFACE);
            label(c,options[i],xx+5,optionsY+8,optionsW-11,WHITE);
        }
        int tabsY=panelY+248,tabW=(rw-28)/4;
        String[] sources={"TRACKS","DEVICES","PLAYLISTS","USE PHONE"};
        for(int i=0;i<4;i++){
            int xx=rx+11+i*tabW;
            rounded(c,xx,tabsY,tabW-4,25,i==spotifyView?0xFF2D5272:SURFACE);
            label(c,sources[i],xx+6,tabsY+8,tabW-12,WHITE);
        }
        int listY=panelY+282,listHeight=Math.max(20,panelY+panelH-30-listY);
        clip(c,rx+10,listY,rx+rw-10,listY+listHeight);
        if(spotifyView==1){
            List<SpotifyController.Device> list=sp.devices();
            if(list.isEmpty())label(c,"Play music in Spotify, then tap REFRESH.",rx+17,listY+12,rw-35,MUTED);
            int count=Math.max(1,listHeight/31);
            scroll=Math.max(0,Math.min(scroll,Math.max(0,list.size()-count)));
            for(int i=0;i<count&&i+scroll<list.size();i++){
                var d=list.get(i+scroll);int y=listY+i*31;
                rounded(c,rx+11,y,rw-22,28,PANEL);
                label(c,(d.active()?"ACTIVE  /  ":"")+d.name()+" · "+d.type(),rx+20,y+10,rw-42,WHITE);
            }
        }else if(spotifyView==2){
            List<SpotifyController.Playlist> list=sp.playlists();
            if(list.isEmpty())label(c,"No playlists yet. Tap PLAYLISTS to reload.",rx+17,listY+12,rw-35,MUTED);
            int count=Math.max(1,listHeight/32);
            scroll=Math.max(0,Math.min(scroll,Math.max(0,list.size()-count)));
            for(int i=0;i<count&&i+scroll<list.size();i++){
                var pl=list.get(i+scroll);int y=listY+i*32;
                rounded(c,rx+11,y,rw-22,29,PANEL);
                label(c,pl.name(),rx+19,y+11,rw-90,WHITE);
                rounded(c,rx+rw-65,y+4,54,21,SURFACE);label(c,"PLAY",rx+rw-54,y+11,40,WHITE);
            }
        }else{
            List<SpotifyController.Track> list=sp.tracks();
            if(list.isEmpty())label(c,"Search tracks or open one of your playlists.",rx+17,listY+12,rw-35,MUTED);
            int count=Math.max(1,listHeight/32);
            scroll=Math.max(0,Math.min(scroll,Math.max(0,list.size()-count)));
            for(int i=0;i<count&&i+scroll<list.size();i++){
                var t=list.get(i+scroll);int y=listY+i*32;
                rounded(c,rx+11,y,rw-22,29,PANEL);
                label(c,t.title()+"  ·  "+t.artist(),rx+19,y+11,rw-94,WHITE);
                rounded(c,rx+rw-76,y+4,38,21,SURFACE);label(c,"PLAY",rx+rw-71,y+11,35,WHITE);
                rounded(c,rx+rw-34,y+4,23,21,SURFACE);label(c,"+",rx+rw-26,y+11,18,WHITE);
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
        label(c,"config/lexy-music  -  MP3 / WAV / AIFF",rx+4,buttonY+31,rw-10,MUTED);
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
        if(files.isEmpty())label(c,"Add your MP3/WAV files, then press REFRESH.",rx+10,listY+12,rw-20,MUTED);
        for(int i=0;i<maxRows&&i+scroll<files.size();i++){
            int index=i+scroll,y=listY+i*31;
            rounded(c,rx+1,y,rw-3,28,index==player.selected()&&player.playing()?0xFF2D4C70:PANEL);
            rect(c,rx+1,y+5,3,18,index==player.selected()?accent():OUTLINE);
            label(c,(index==player.selected()?"♪  ":"▷  ")+files.get(index),rx+11,y+9,rw-20,WHITE);
        }
        c.disableScissor();
    }
    private static final int TRIGGER_ROWS=14;
    private static final int AIM_ROWS=13;
    private static final int TOTEM_ROWS=15;
    private static final int CLICKER_ROWS=16;
    private int cheatTabX(int section){return rightX()+7+section*((rightW()-14)/4);}
    private int cheatTabW(int section){return section==3 ? rightX()+rightW()-7-cheatTabX(section) : (rightW()-14)/4-4;}
    private void renderCheats(DrawContext c,int mx,int my){
        int x=rightX(),w=rightW(),top=panelY+66;
        String[] items={"TOTEM","TRIGGER","AIM","CLICKER"};
        String[] ids={"AutoTotem","TriggerBot","AimAssist","AutoClicker"};
        for(int i=0;i<4;i++){
            int xx=cheatTabX(i),ww=cheatTabW(i);
            boolean selectedTab=i==cheatSection;
            boolean hover=inside(mx,my,xx,top,ww,27);
            rounded(c,xx,top,ww,27,selectedTab?BG_ACTIVE:hover?BG_HOVER:PANEL);
            frame(c,xx,top,ww,27,selectedTab?accent():OUTLINE);
            if(selectedTab)rect(c,xx+7,top+24,ww-14,2,accent());
            boolean enabled=LexyClient.enabled.getOrDefault(ids[i],false);
            rect(c,xx+ww-15,top+11,6,6,enabled?0xFF70E6C7:0xFF536B83);
            label(c,items[i],xx+10,top+9,ww-29,selectedTab?WHITE:MUTED);
        }
        if(cheatSection==0)renderAutoTotem(c,mx,my);
        else if(cheatSection==1)renderTriggerBot(c,mx,my);
        else if(cheatSection==2)renderAimAssist(c,mx,my);
        else renderAutoClicker(c,mx,my);
    }
    private void renderAutoClicker(DrawContext c,int mx,int my){
        int x=rightX(),w=rightW(),top=panelY+102;
        rounded(c,x,top,w,71,PANEL);
        frame(c,x,top,w,71,OUTLINE);rect(c,x,top+9,3,53,accent());
        label(c,"AutoClicker | "+AutoClicker.status(),x+13,top+10,w-25,WHITE);
        label(c,"Vanilla interactions · no GUI clicks · up to 18 tick/s",x+13,top+26,w-25,MUTED);
        int bw=Math.max(60,(w-26)/2),by=top+42;
        rounded(c,x+9,by,bw,24,LexyClient.enabled.getOrDefault("AutoClicker",false)?0xFF245A86:SURFACE);
        label(c,LexyClient.enabled.getOrDefault("AutoClicker",false)?"ENABLED - TAP TO OFF":"DISABLED - TAP TO ON",x+17,by+8,bw-15,WHITE);
        rounded(c,x+16+bw,by,w-bw-25,24,clickerSettingsOpen?0xFF2D638A:SURFACE);
        label(c,clickerSettingsOpen?"HIDE SETTINGS":"CONFIGURE",x+24+bw,by+8,w-bw-40,WHITE);
        if(!clickerSettingsOpen){
            label(c,"Left/right modes, CPS range, hold/toggle and safe clicks.",x+9,top+95,w-18,MUTED);
            label(c,"Off by default. Assign clicker keys in Keybinds.",x+9,top+114,w-18,MUTED);
            return;
        }
        String[] names={"MOUSE BUTTONS","ACTIVATION MODE","MIN CPS","MAX CPS","TIMING VARIATION","LEFT REACH","ATTACK CHARGE","WEAPON ONLY","IGNORE TEAM","IGNORE INVISIBLE","SWING IN AIR","RIGHT CLICK BLOCKS","RIGHT CLICK ITEMS","REQUIRE SNEAK","PAUSE WHILE USING","PAUSE FOR TOTEM"};
        String[] values={AutoClicker.buttons.name(),AutoClicker.activation.name().replace('_',' '),
          AutoClicker.minCps+" CPS",AutoClicker.maxCps+" CPS",AutoClicker.jitterMs+" ms",
          String.format(Locale.ROOT,"%.1f",AutoClicker.leftReachTenths/10f)+" m",
          AutoClicker.attackChargePercent+"%", AutoClicker.requireWeapon?"YES":"NO",
          AutoClicker.ignoreTeam?"YES":"NO", AutoClicker.ignoreInvisible?"YES":"NO",
          AutoClicker.airSwings?"ON":"OFF", AutoClicker.rightClickBlocks?"ON":"OFF",
          AutoClicker.rightClickItems?"ON":"OFF",AutoClicker.requireSneak?"YES":"NO",
          AutoClicker.pauseWhileUsing?"YES":"NO",AutoClicker.pauseDuringTotem?"YES":"NO"};
        int start=top+81,end=panelY+panelH-65;
        int visible=Math.min(CLICKER_ROWS,Math.max(1,(end-start)/48));
        scroll=Math.max(0,Math.min(scroll,Math.max(0,CLICKER_ROWS-visible)));
        clip(c,x,start,x+w,end);
        for(int i=scroll;i<Math.min(CLICKER_ROWS,scroll+visible);i++){
            int y=start+(i-scroll)*48;
            rounded(c,x+7,y,w-15,41,PANEL);
            frame(c,x+7,y,w-15,41,OUTLINE);rect(c,x+7,y+8,3,25,accent());
            label(c,names[i],x+19,y+16,w-145,WHITE);
            rounded(c,x+w-113,y+8,98,25,SURFACE);label(c,values[i],x+w-104,y+16,86,WHITE);
        }
        c.disableScissor();
        int y=panelY+panelH-60;
        rounded(c,x+7,y,65,25,scroll>0?SURFACE:OUTLINE);label(c,"< UP",x+18,y+9,50,WHITE);
        rounded(c,x+w-74,y,67,25,scroll<Math.max(0,CLICKER_ROWS-visible)?SURFACE:OUTLINE);
        label(c,"DOWN >",x+w-67,y+9,57,WHITE);
        label(c,"Settings "+(scroll+1)+"-"+Math.min(CLICKER_ROWS,scroll+visible)+" / "+CLICKER_ROWS,x+80,y+9,w-164,MUTED);
    }
    private static void stepClickerSetting(int i,int direction){
        switch(i){
            case 0 -> AutoClicker.buttons=AutoClicker.Buttons.values()[Math.floorMod(AutoClicker.buttons.ordinal()+direction,AutoClicker.Buttons.values().length)];
            case 1 -> AutoClicker.activation=AutoClicker.Activation.values()[Math.floorMod(AutoClicker.activation.ordinal()+direction,AutoClicker.Activation.values().length)];
            case 2 -> {AutoClicker.minCps=Math.floorMod(AutoClicker.minCps-1+direction,18)+1;AutoClicker.maxCps=Math.max(AutoClicker.minCps,AutoClicker.maxCps);}
            case 3 -> AutoClicker.maxCps=Math.max(AutoClicker.minCps,Math.min(18,AutoClicker.maxCps+direction));
            case 4 -> AutoClicker.jitterMs=Math.floorMod(AutoClicker.jitterMs+direction*10,110);
            case 5 -> AutoClicker.leftReachTenths=AutoClicker.leftReachTenths+direction>30?20:AutoClicker.leftReachTenths+direction<20?30:AutoClicker.leftReachTenths+direction;
            case 6 -> AutoClicker.attackChargePercent=Math.floorMod(AutoClicker.attackChargePercent+direction*5,105);
            case 7 -> AutoClicker.requireWeapon=!AutoClicker.requireWeapon;
            case 8 -> AutoClicker.ignoreTeam=!AutoClicker.ignoreTeam;
            case 9 -> AutoClicker.ignoreInvisible=!AutoClicker.ignoreInvisible;
            case 10 -> AutoClicker.airSwings=!AutoClicker.airSwings;
            case 11 -> AutoClicker.rightClickBlocks=!AutoClicker.rightClickBlocks;
            case 12 -> AutoClicker.rightClickItems=!AutoClicker.rightClickItems;
            case 13 -> AutoClicker.requireSneak=!AutoClicker.requireSneak;
            case 14 -> AutoClicker.pauseWhileUsing=!AutoClicker.pauseWhileUsing;
            case 15 -> AutoClicker.pauseDuringTotem=!AutoClicker.pauseDuringTotem;
        }
        AutoClicker.reset();LexyClient.save();LexySounds.save();
    }
    private void renderAimAssist(DrawContext c,int mx,int my){
        int x=rightX(),w=rightW(),top=panelY+102;
        rounded(c,x,top,w,71,PANEL);
        frame(c,x,top,w,71,OUTLINE);rect(c,x,top+9,3,53,accent());
        label(c,"Aim Assist | "+AimAssist.status(),x+13,top+10,w-25,WHITE);
        label(c,"Smooth aim · visible targets · no packet/reach changes",x+13,top+26,w-24,MUTED);
        int buttonY=top+42,bw=Math.max(60,(w-26)/2);
        rounded(c,x+9,buttonY,bw,24,LexyClient.enabled.getOrDefault("AimAssist",false)?0xFF245A86:SURFACE);
        label(c,LexyClient.enabled.getOrDefault("AimAssist",false)?"ENABLED · TAP TO OFF":"DISABLED · TAP TO ON",x+17,buttonY+8,bw-15,WHITE);
        rounded(c,x+16+bw,buttonY,w-bw-25,24,aimSettingsOpen?0xFF2D638A:SURFACE);
        label(c,aimSettingsOpen?"HIDE SETTINGS":"CONFIGURE",x+24+bw,buttonY+8,w-bw-40,WHITE);
        if(!aimSettingsOpen){
            label(c,"Set the FOV, smoothing, rotation speed and tracking.",x+9,top+95,w-18,MUTED);
            label(c,"Add a toggle / hold key in Keybinds. Disabled by default.",x+9,top+114,w-18,MUTED);
            return;
        }
        String[] names={"TARGETS","AIM DISTANCE","AIM FOV","SMOOTHNESS","MAX TURN / TICK","AIM HEIGHT","MOVEMENT LEAD","TARGET LOCK","REQUIRE HOLD KEY","WEAPON ONLY","IGNORE TEAMMATES","IGNORE INVISIBLE","RESET TRACKING"};
        String[] values={AimAssist.targets.name(),String.format(Locale.ROOT,"%.1f",AimAssist.rangeTenths/10f)+" m",
                AimAssist.fovDegrees+"°",AimAssist.smoothness+"%",String.format(Locale.ROOT,"%.1f",AimAssist.maxTurnTenths/10f)+"°",
                AimAssist.aimHeight+"%",AimAssist.leadPercent+"%",AimAssist.lockMs+" ms",
                AimAssist.requireHold?"ON":"OFF",AimAssist.requireWeapon?"YES":"NO",
                AimAssist.ignoreTeammates?"YES":"NO",AimAssist.ignoreInvisible?"YES":"NO","RESET"};
        int start=top+81,end=panelY+panelH-65;
        int visible=Math.min(AIM_ROWS,Math.max(1,(end-start)/48));
        scroll=Math.max(0,Math.min(scroll,Math.max(0,AIM_ROWS-visible)));
        clip(c,x,start,x+w,end);
        for(int i=scroll;i<Math.min(AIM_ROWS,scroll+visible);i++){
            int y=start+(i-scroll)*48;
            rounded(c,x+7,y,w-15,41,PANEL);
            frame(c,x+7,y,w-15,41,OUTLINE);rect(c,x+7,y+8,3,25,accent());
            label(c,names[i],x+19,y+16,w-145,WHITE);
            rounded(c,x+w-113,y+8,98,25,SURFACE);label(c,values[i],x+w-104,y+16,86,WHITE);
        }
        c.disableScissor();
        int navY=panelY+panelH-60;
        rounded(c,x+7,navY,65,25,scroll>0?SURFACE:OUTLINE);label(c,"< UP",x+18,navY+9,50,WHITE);
        rounded(c,x+w-74,navY,67,25,scroll<Math.max(0,AIM_ROWS-visible)?SURFACE:OUTLINE);
        label(c,"DOWN >",x+w-67,navY+9,57,WHITE);
        label(c,"Settings "+(scroll+1)+"-"+Math.min(AIM_ROWS,scroll+visible)+" / "+AIM_ROWS,x+80,navY+9,w-164,MUTED);
    }
    private void renderTriggerBot(DrawContext c,int mx,int my){
        int x=rightX(),w=rightW(),top=panelY+102;
        rounded(c,x,top,w,71,PANEL);
        frame(c,x,top,w,71,OUTLINE);rect(c,x,top+9,3,53,accent());
        label(c,"TriggerBot | "+TriggerBot.status(),x+13,top+10,w-25,WHITE);
        label(c,"Only crosshair targets / vanilla combat range",x+13,top+26,w-24,MUTED);
        int toggleX=x+9,buttonY=top+42,bw=Math.max(60,(w-26)/2);
        rounded(c,toggleX,buttonY,bw,24,LexyClient.enabled.getOrDefault("TriggerBot",false)?0xFF245A86:SURFACE);
        label(c,LexyClient.enabled.getOrDefault("TriggerBot",false)?"ENABLED - TAP TO OFF":"DISABLED - TAP TO ON",toggleX+8,buttonY+8,bw-15,WHITE);
        rounded(c,toggleX+bw+7,buttonY,w-bw-25,24,triggerSettingsOpen?0xFF2D638A:SURFACE);
        label(c,triggerSettingsOpen?"HIDE SETTINGS":"CONFIGURE",toggleX+bw+15,buttonY+8,w-bw-40,WHITE);
        if(!triggerSettingsOpen){
            label(c,"Target selection, timing and weapon conditions.",x+8,top+94,w-16,MUTED);
            label(c,"OFF by default; enable only where permitted.",x+8,top+114,w-16,MUTED);
            return;
        }
        String[] names={"TARGETS", "CLICKS PER SECOND", "CHARGED HITS", "MIN CHARGE", "SWORD/AXE ONLY", "REQUIRE SNEAK", "IGNORE TEAMMATES", "MAX DISTANCE", "HOLD KEY REQUIRED", "IGNORE MANUAL CLICKS", "IGNORE INVISIBLE", "ONLY CRITICALS", "STABLE TARGET TICKS", "TIMING VARIATION"};
        String[] values={TriggerBot.targets.name(),TriggerBot.clicksPerSecond+" CPS",TriggerBot.requireChargedHit?"YES":"NO",TriggerBot.cooldownPercent+"%",TriggerBot.requireWeapon?"YES":"NO",TriggerBot.requireSneak?"YES":"NO",TriggerBot.ignoreTeam?"YES":"NO",String.format(Locale.ROOT,"%.1f",TriggerBot.reachTenths/10f)+" m",TriggerBot.requireHold?"YES":"NO",TriggerBot.avoidManualClicks?"YES":"NO",TriggerBot.ignoreInvisible?"YES":"NO",TriggerBot.onlyCriticalHits?"YES":"NO",TriggerBot.targetStableTicks+" ticks",TriggerBot.jitterMs+" ms"};
        int start=top+81,end=panelY+panelH-65;
        int visible=Math.min(TRIGGER_ROWS,Math.max(1,(end-start)/48));
        scroll=Math.max(0,Math.min(scroll,Math.max(0,TRIGGER_ROWS-visible)));
        clip(c,x,start,x+w,end);
        for(int i=scroll;i<Math.min(TRIGGER_ROWS,scroll+visible);i++){
            int y=start+(i-scroll)*48;
            rounded(c,x+7,y,w-15,41,PANEL);
            frame(c,x+7,y,w-15,41,OUTLINE);rect(c,x+7,y+8,3,25,accent());
            label(c,names[i],x+19,y+16,w-145,WHITE);
            rounded(c,x+w-113,y+8,98,25,SURFACE);
            label(c,values[i],x+w-104,y+16,86,WHITE);
        }
        c.disableScissor();
        int navY=panelY+panelH-60;
        rounded(c,x+7,navY,65,25,scroll>0?SURFACE:OUTLINE);label(c,"< UP",x+18,navY+9,50,WHITE);
        rounded(c,x+w-74,navY,67,25,scroll<Math.max(0,TRIGGER_ROWS-visible)?SURFACE:OUTLINE);
        label(c,"DOWN >",x+w-67,navY+9,57,WHITE);
        label(c,"Settings "+(scroll+1)+"-"+Math.min(TRIGGER_ROWS,scroll+visible)+" / "+TRIGGER_ROWS,x+80,navY+9,w-164,MUTED);
    }
    private void renderAutoTotem(DrawContext c,int mx,int my){
        int x=rightX(),w=rightW(),top=panelY+102;
        rounded(c,x,top,w,71,PANEL);
        frame(c,x,top,w,71,OUTLINE);rect(c,x,top+9,3,53,accent());
        label(c,"AutoTotem | "+AutoTotem.status(),x+13,top+10,w-25,WHITE);
        label(c,"Mouse-safe inventory or instant direct swap",x+13,top+26,w-24,MUTED);
        int toggleX=x+9,buttonY=top+42,bw=Math.max(60,(w-26)/2);
        rounded(c,toggleX,buttonY,bw,24,LexyClient.enabled.getOrDefault("AutoTotem",false)?0xFF245A86:SURFACE);
        label(c,LexyClient.enabled.getOrDefault("AutoTotem",false)?"ENABLED - TAP TO OFF":"DISABLED - TAP TO ON",toggleX+8,buttonY+8,bw-15,WHITE);
        rounded(c,toggleX+bw+7,buttonY,w-bw-25,24,totemSettingsOpen?0xFF2D638A:SURFACE);
        label(c,totemSettingsOpen?"HIDE SETTINGS":"CONFIGURE",toggleX+bw+15,buttonY+8,w-bw-40,WHITE);
        if(!totemSettingsOpen){
            label(c,"Open CONFIGURE to choose mode and timing in ms.",x+8,top+96,w-16,MUTED);
            label(c,"Inventory opens only while idle; combat remains uninterrupted.",x+8,top+116,w-16,MUTED);
            return;
        }
        String[] labels={"MODE", "RANDOM TOTEM SLOT", "MIN DELAY", "MAX DELAY", "INVENTORY STEP", "AFTER-SWAP COOLDOWN", "ONLY WHEN OFFHAND EMPTY", "LOW HEALTH ONLY", "HEALTH THRESHOLD", "SHOW NOTIFICATIONS", "COMBAT DIRECT FALLBACK", "SAFE IDLE WAIT", "EMERGENCY DIRECT", "EMERGENCY HEALTH", "QUICK EMPTY OFFHAND"};
        String[] values={AutoTotem.mode==AutoTotem.Mode.DIRECT?"DIRECT":"INVENTORY", AutoTotem.randomSource?"ON":"OFF",AutoTotem.minDelayMs+" ms",AutoTotem.maxDelayMs+" ms",AutoTotem.inventoryStepMs+" ms",AutoTotem.cooldownMs+" ms",AutoTotem.onlyWhenEmpty?"YES":"NO",AutoTotem.lowHealthOnly?"YES":"NO",AutoTotem.healthThreshold+" HP",AutoTotem.notify?"ON":"OFF",AutoTotem.combatFallbackDirect?"ON":"OFF",AutoTotem.safeIdleMs+" ms",AutoTotem.emergencyDirect?"YES":"NO",AutoTotem.emergencyHealth+" HP",AutoTotem.priorityOnEmpty?"YES":"NO"};
        int start=top+81,bodyEnd=panelY+panelH-65;
        int visible=Math.min(TOTEM_ROWS,Math.max(1,(bodyEnd-start)/48));
        scroll=Math.max(0,Math.min(scroll,Math.max(0,TOTEM_ROWS-visible)));
        clip(c,x,start,x+w,bodyEnd);
        for(int i=scroll;i<Math.min(TOTEM_ROWS,scroll+visible);i++){
            int yy=start+(i-scroll)*48;rounded(c,x+7,yy,w-15,41,PANEL);rect(c,x+7,yy+8,3,25,accent());
            label(c,labels[i],x+19,yy+16,w-145,WHITE);rounded(c,x+w-113,yy+8,98,25,SURFACE);
            label(c,values[i],x+w-104,yy+16,86,WHITE);
        }
        c.disableScissor();
        int controlsY=panelY+panelH-60;
        rounded(c,x+7,controlsY,65,25,scroll>0?SURFACE:OUTLINE);
        label(c,"< UP",x+18,controlsY+9,52,WHITE);
        rounded(c,x+w-74,controlsY,67,25,scroll<Math.max(0,TOTEM_ROWS-visible)?SURFACE:OUTLINE);
        label(c,"DOWN >",x+w-67,controlsY+9,58,WHITE);
        label(c,"Settings "+(scroll+1)+"-"+Math.min(TOTEM_ROWS,scroll+visible)+" / "+TOTEM_ROWS,x+80,controlsY+9,w-164,MUTED);
        label(c,"Direct fallback protects your mouse while fighting.",x+9,panelY+panelH-25,w-20,MUTED);
    }
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        if(keyCode==org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT){
            if(LexyClient.allowMenuToggle())close();
            return true;
        }
        if(keyCode==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){close();return true;}
        if((modifiers&org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL)!=0
                && keyCode==org.lwjgl.glfw.GLFW.GLFW_KEY_F&&modular()&&search!=null){
            setFocused(search);search.setFocused(true);return true;
        }
        return super.keyPressed(keyCode,scanCode,modifiers);
    }
    private void renderStyle(DrawContext c,int mx,int my){
        int rx=rightX(),rw=rightW(),yy=panelY+66;
        text(c,"ACCENT COLOR",rx+8,yy,WHITE);yy+=20;
        for(int i=0;i<LexyClient.ACCENTS.length;i++){
            int step=Math.min(55,Math.max(33,(rw-16)/LexyClient.ACCENTS.length));
            int ww=step-8,xx=rx+8+i*step;rounded(c,xx,yy,ww,28,LexyClient.ACCENTS[i]);
            if(i==Math.floorMod(LexyClient.accentChoice,LexyClient.ACCENTS.length))frame(c,xx-2,yy-2,ww+4,32,WHITE);
        }
        yy+=45;
        String[] names={"MENU SIZE", "HUD SIZE", "HUD SNAP TO GRID", "HUD GRID VISIBLE", "HUD GRID SPACING", "HUD Background","Text Shadow","Compact Readouts","Hover Sounds","Background Opacity","Widget Spacing","Low HP Warning","Armor Warning %","HUD Text Color","UI Sounds","UI Sound Volume","Max HUD Widgets","Zoom Power","Zoom Toggle Mode"};
        int listY=yy;int showCount=Math.max(1,(panelY+panelH-45-listY)/44);
        scroll=Math.min(scroll,Math.max(0,names.length-showCount));
        clip(c,rx,listY,rx+rw,listY+showCount*44);
        for(int i=scroll;i<Math.min(names.length,scroll+showCount);i++){
            int y=yy+(i-scroll)*44;rect(c,rx+7,y,rw-15,39,PANEL);rect(c,rx+7,y,3,39,accent());
            label(c,names[i],rx+19,y+14,rw-150,WHITE);
            String value=switch(i){case 0->LexyClient.menuScalePercent+"%";case 1->LexyClient.hudScalePercent+"%";case 2->LexyClient.hudGridSnap?"ON":"OFF";case 3->LexyClient.hudShowGrid?"ON":"OFF";case 4->LexyClient.hudGridSize+" px";case 5->LexyClient.hudBackground?"ON":"OFF";case 6->LexyClient.hudShadow?"ON":"OFF";case 7->LexyClient.compactHud?"ON":"OFF";case 8->LexyClient.uiHoverSounds?"ON":"OFF";case 9->LexyClient.hudOpacity+" / 245";case 10->LexyClient.hudSpacing+" px";case 11->LexyClient.lowHealthThreshold+" HP";case 12->LexyClient.armorWarnPercent+"%";case 13->new String[]{"White","Blue","Cool Gray"}[LexyClient.hudTextColor];case 14->LexyClient.uiSounds?"ON":"OFF";case 15->LexyClient.uiSoundVolume+"%";case 16->LexyClient.maxHudWidgets+" max";case 17->ZoomController.zoomPercent+"%";default->ZoomController.toggleMode?"TOGGLE":"HOLD";};
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
        if(button==1 && !editor && tab==10 && cheatSection==3 && clickerSettingsOpen){
            int x=rightX(),top=panelY+102,start=top+81,end=panelY+panelH-65;
            int visible=Math.min(CLICKER_ROWS,Math.max(1,(end-start)/48));
            for(int i=scroll;i<Math.min(CLICKER_ROWS,scroll+visible);i++)
                if(inside(mx,my,x+7,start+(i-scroll)*48,rightW()-16,41)){
                    stepClickerSetting(i,-1);return true;
                }
        }
        if(button!=0)return super.mouseClicked(mx,my,button);
        if(inside(mx,my,panelX,panelY,panelW,panelH))LexySounds.tap();
        if(inside(mx,my,rightX()+rightW()-43,panelY+18,34,28)){close();return true;}
        if(inside(mx,my,rightX()+rightW()-157,panelY+18,106,28)||inside(mx,my,panelX+13,panelY+panelH-85,sidebarW-22,28)){
            LexySounds.navigate();moveHud();return true;
        }
        if(navLimit()>0 && inside(mx,my,panelX+13,panelY+panelH-144,sidebarW-23,25)){
            if(mx<panelX+17+(sidebarW-31)/2)navScroll=Math.max(0,navScroll-2);
            else navScroll=Math.min(navLimit(),navScroll+2);
            LexySounds.navigate();return true;
        }
        for(int i=navScroll;i<Math.min(TABS.length,navScroll+navCapacity());i++){
            int yy=navStart()+(i-navScroll)*NAV_ROW;
            if(inside(mx,my,panelX+10,yy,sidebarW-16,29)){
                if(!editor){if(i==11){LexySounds.confirm();client.setScreen(new KeybindSettingsScreen(this));return true;}tab=i;scroll=0;selected=-1;updateTextFields();LexySounds.navigate();}
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
            if(inside(mx,my,rx,panelY+panelH-53,rw,23)){
                if(mx<rx+77)scroll=Math.max(0,scroll-rowsVisible());
                else if(mx>rx+rw-77)scroll=Math.min(maxScroll(filtered().size()),scroll+rowsVisible());
                return true;
            }
            if(inside(mx,my,rx,yy,rw,contentH())){
                int cw=rw/cols;int col=(int)(mx-rx)/cw,row=(int)(my-yy)/ROW_H;
                int i=(row+scroll)*cols+col;List<LexyClient.Module> list=filtered();
                if(i>=0&&i<list.size()){
                    var m=list.get(i);
                    if(HudRenderer.isRelevant(m)){
                        LexyClient.enabled.put(m.id(),!LexyClient.enabled.getOrDefault(m.id(),false));
                        HudRenderer.invalidate();LexyClient.save();LexySounds.toggle();
                    }
                    return true;
                }
            }
        }else if(tab==5||tab==6){
            int pw=(rw-10)/5,yy=panelY+63;
            for(int i=0;i<5;i++)if(inside(mx,my,rx+i*pw,yy,pw-5,29)){
                if(i==4)OptimizationManager.adaptiveEnabled=!OptimizationManager.adaptiveEnabled;
                else OptimizationManager.apply(new String[]{"MAX FPS","BALANCED","QUALITY","POJAV"}[i]);
                LexyClient.save();LexySounds.confirm();return true;
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
            if(!spotify.isConnected() && spotifyFreeView){
                int top=panelY+63,freeW=(rw-34)/2;
                if(inside(mx,my,rx+11,top+143,rw-22,30)){
                    spotify.openFreeSearch(spotifyFreeSearch.getText());return true;
                }
                if(inside(mx,my,rx+11,top+181,freeW,30)){spotify.openPlayer();return true;}
                if(inside(mx,my,rx+18+freeW,top+181,rw-freeW-29,30)){
                    spotify.openFreeLibrary();return true;
                }
                if(inside(mx,my,rx+11,top+238,rw-22,30)){
                    spotifyFreeView=false;updateTextFields();LexySounds.navigate();return true;
                }
                return super.mouseClicked(mx,my,button);
            }
            if(!spotify.isConnected()){
                int top=panelY+63,buttonW=(rw-35)/2,loginY=top+50;
                if(panelH>440 && inside(mx,my,rx+11,top+354,rw-22,28)){
                    spotifyFreeView=true;updateTextFields();LexySounds.navigate();return true;
                }
                if(inside(mx,my,rx+11,loginY,buttonW,28)){
                    if(spotify.pending()) {spotify.cancelLogin();LexySounds.deny();}
                    else {LexyClient.spotifyClientId=spotifyClientId.getText().trim();LexyClient.save();
                        spotify.setClientId(LexyClient.spotifyClientId);spotify.connect();LexySounds.confirm();}
                    return true;
                }
                if(inside(mx,my,rx+17+buttonW,loginY,rw-buttonW-28,28)){
                    if(spotify.pending())spotify.copyAuthUrl();else openSpotifyDashboard();return true;
                }
                if(inside(mx,my,rx+11,top+198,rw-22,25)){
                    if(client!=null){try{spotifyRedirect.setText(client.keyboard.getClipboard());}catch(Throwable ignored){}}
                    return true;
                }
                if(inside(mx,my,rx+11,top+231,rw-22,28)){
                    spotify.completeFromRedirect(spotifyRedirect.getText());return true;
                }
                return super.mouseClicked(mx,my,button);
            }
            int top=panelY+63;
            if(inside(mx,my,rx+rw-206,top,99,24)){spotify.openPlayer();return true;}
            if(inside(mx,my,rx+rw-102,top,92,24)){spotify.disconnect();LexySounds.deny();return true;}
            if(inside(mx,my,rx+rw-85,panelY+149,74,20)){
                spotify.search(spotifySearch.getText());scroll=0;spotifyView=0;return true;
            }
            int by=panelY+180,bw=(rw-28)/5;
            for(int i=0;i<5;i++)if(inside(mx,my,rx+11+i*bw,by,bw-4,27)){
                switch(i){
                    case 0->spotify.previous();case 1->spotify.toggle();case 2->spotify.next();
                    case 3->{spotifyView=0;scroll=0;}
                    case 4->spotify.refresh();
                }
                return true;
            }
            int optionsY=panelY+214,optionsW=(rw-28)/5;
            for(int i=0;i<5;i++)if(inside(mx,my,rx+11+i*optionsW,optionsY,optionsW-4,25)){
                switch(i){case 0->spotify.shuffle();case 1->spotify.repeat();case 2->spotify.changeVolume(-10);case 3->spotify.changeVolume(10);case 4->spotify.refresh();}
                return true;
            }
            int tabsY=panelY+248,tabW=(rw-28)/4;
            for(int i=0;i<4;i++)if(inside(mx,my,rx+11+i*tabW,tabsY,tabW-4,25)){
                if(i==3)spotify.usePhone();
                else{spotifyView=i;scroll=0;if(i==1)spotify.refreshDevices();else if(i==2)spotify.loadPlaylists();}
                return true;
            }
            int listY=panelY+282,listHeight=Math.max(20,panelY+panelH-30-listY);
            if(inside(mx,my,rx+11,listY,rw-22,listHeight)){
                int row=(int)(my-listY)/(spotifyView==1?31:32),idx=row+scroll;
                if(row>=0){
                    if(spotifyView==1){var ds=spotify.devices();if(idx<ds.size())spotify.transfer(ds.get(idx));}
                    else if(spotifyView==2){var ps=spotify.playlists();if(idx<ps.size())spotify.playPlaylist(ps.get(idx));}
                    else {var ts=spotify.tracks();if(idx<ts.size()){
                        var t=ts.get(idx);if(mx>=rx+rw-38)spotify.queue(t);else spotify.play(t);
                    }}
                }
                return true;
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
            int switchY=panelY+66;
            for(int tabIndex=0;tabIndex<4;tabIndex++){
                if(inside(mx,my,cheatTabX(tabIndex),switchY,cheatTabW(tabIndex),27)){
                    cheatSection=tabIndex;scroll=0;LexySounds.navigate();return true;
                }
            }
            if(cheatSection==3){
                int top=panelY+102,by=top+42,bw=Math.max(60,(rw-26)/2);
                if(inside(mx,my,rx+9,by,bw,24)){
                    boolean active=!LexyClient.enabled.getOrDefault("AutoClicker",false);
                    LexyClient.enabled.put("AutoClicker",active);AutoClicker.reset();
                    LexyClient.save();LexySounds.toggle();return true;
                }
                if(inside(mx,my,rx+16+bw,by,rw-bw-25,24)){
                    clickerSettingsOpen=!clickerSettingsOpen;scroll=0;LexySounds.navigate();return true;
                }
                if(clickerSettingsOpen){
                    int yy=top+81,end=panelY+panelH-65;
                    int visible=Math.min(CLICKER_ROWS,Math.max(1,(end-yy)/48));
                    int max=Math.max(0,CLICKER_ROWS-visible),navY=panelY+panelH-60;
                    if(inside(mx,my,rx+7,navY,65,25)){scroll=Math.max(0,scroll-1);return true;}
                    if(inside(mx,my,rx+rw-74,navY,67,25)){scroll=Math.min(max,scroll+1);return true;}
                    for(int i=scroll;i<Math.min(CLICKER_ROWS,scroll+visible);i++)
                        if(inside(mx,my,rx+7,yy+(i-scroll)*48,rw-16,41)){
                            stepClickerSetting(i,1);return true;
                        }
                }
                return true;
            }
            if(cheatSection==2){
                int top=panelY+102,buttonY=top+42,bw=Math.max(60,(rw-26)/2);
                if(inside(mx,my,rx+9,buttonY,bw,24)){
                    boolean next=!LexyClient.enabled.getOrDefault("AimAssist",false);
                    LexyClient.enabled.put("AimAssist",next);
                    AimAssist.reset();LexyClient.save();LexySounds.toggle();return true;
                }
                if(inside(mx,my,rx+9+bw+7,buttonY,rw-bw-25,24)){
                    aimSettingsOpen=!aimSettingsOpen;scroll=0;return true;
                }
                if(aimSettingsOpen){
                    int yy=top+81,row=48,end=panelY+panelH-65;
                    int visible=Math.min(AIM_ROWS,Math.max(1,(end-yy)/row));
                    int max=Math.max(0,AIM_ROWS-visible),navY=panelY+panelH-60;
                    if(inside(mx,my,rx+7,navY,65,25)){scroll=Math.max(0,scroll-1);return true;}
                    if(inside(mx,my,rx+rw-74,navY,67,25)){scroll=Math.min(max,scroll+1);return true;}
                    for(int i=scroll;i<Math.min(AIM_ROWS,scroll+visible);i++)if(inside(mx,my,rx+7,yy+(i-scroll)*row,rw-16,41)){
                        switch(i){
                            case 0->AimAssist.targets=AimAssist.Targets.values()[(AimAssist.targets.ordinal()+1)%AimAssist.Targets.values().length];
                            case 1->AimAssist.rangeTenths=AimAssist.rangeTenths>=60?25:AimAssist.rangeTenths+5;
                            case 2->AimAssist.fovDegrees=AimAssist.fovDegrees>=150?10:AimAssist.fovDegrees+10;
                            case 3->AimAssist.smoothness=AimAssist.smoothness>=95?10:AimAssist.smoothness+5;
                            case 4->AimAssist.maxTurnTenths=AimAssist.maxTurnTenths>=240?5:AimAssist.maxTurnTenths+10;
                            case 5->AimAssist.aimHeight=AimAssist.aimHeight>=95?20:AimAssist.aimHeight+5;
                            case 6->AimAssist.leadPercent=AimAssist.leadPercent>=100?0:AimAssist.leadPercent+10;
                            case 7->AimAssist.lockMs=AimAssist.lockMs>=2000?200:AimAssist.lockMs+150;
                            case 8->AimAssist.requireHold=!AimAssist.requireHold;
                            case 9->AimAssist.requireWeapon=!AimAssist.requireWeapon;
                            case 10->AimAssist.ignoreTeammates=!AimAssist.ignoreTeammates;
                            case 11->AimAssist.ignoreInvisible=!AimAssist.ignoreInvisible;
                            case 12->AimAssist.reset();
                        }
                        AimAssist.reset();LexyClient.save();LexySounds.save();return true;
                    }
                }
                return true;
            }
            if(cheatSection==1){
                int top=panelY+102,buttonY=top+42,bw=Math.max(60,(rw-26)/2);
                if(inside(mx,my,rx+9,buttonY,bw,24)){
                    boolean next=!LexyClient.enabled.getOrDefault("TriggerBot",false);
                    LexyClient.enabled.put("TriggerBot",next);TriggerBot.reset();
                    LexyClient.save();LexySounds.toggle();return true;
                }
                if(inside(mx,my,rx+9+bw+7,buttonY,rw-bw-25,24)){
                    triggerSettingsOpen=!triggerSettingsOpen;scroll=0;return true;
                }
                if(triggerSettingsOpen){
                    int yy=top+81,row=48,end=panelY+panelH-65;
                    int visible=Math.min(TRIGGER_ROWS,Math.max(1,(end-yy)/row));
                    int max=Math.max(0,TRIGGER_ROWS-visible),navY=panelY+panelH-60;
                    if(inside(mx,my,rx+7,navY,65,25)){scroll=Math.max(0,scroll-1);return true;}
                    if(inside(mx,my,rx+rw-74,navY,67,25)){scroll=Math.min(max,scroll+1);return true;}
                    for(int i=scroll;i<Math.min(TRIGGER_ROWS,scroll+visible);i++)if(inside(mx,my,rx+7,yy+(i-scroll)*row,rw-16,41)){
                        switch(i){
                            case 0->TriggerBot.targets=TriggerBot.Targets.values()[(TriggerBot.targets.ordinal()+1)%TriggerBot.Targets.values().length];
                            case 1->TriggerBot.clicksPerSecond=TriggerBot.clicksPerSecond>=12?1:TriggerBot.clicksPerSecond+1;
                            case 2->TriggerBot.requireChargedHit=!TriggerBot.requireChargedHit;
                            case 3->TriggerBot.cooldownPercent=TriggerBot.cooldownPercent>=100?40:TriggerBot.cooldownPercent+5;
                            case 4->TriggerBot.requireWeapon=!TriggerBot.requireWeapon;
                            case 5->TriggerBot.requireSneak=!TriggerBot.requireSneak;
                            case 6->TriggerBot.ignoreTeam=!TriggerBot.ignoreTeam;
                            case 7->TriggerBot.reachTenths=TriggerBot.reachTenths>=30?20:TriggerBot.reachTenths+1;
                            case 8->TriggerBot.requireHold=!TriggerBot.requireHold;
                            case 9->TriggerBot.avoidManualClicks=!TriggerBot.avoidManualClicks;
                            case 10->TriggerBot.ignoreInvisible=!TriggerBot.ignoreInvisible;
                            case 11->TriggerBot.onlyCriticalHits=!TriggerBot.onlyCriticalHits;
                            case 12->TriggerBot.targetStableTicks=TriggerBot.targetStableTicks>=8?1:TriggerBot.targetStableTicks+1;
                            case 13->TriggerBot.jitterMs=TriggerBot.jitterMs>=150?0:TriggerBot.jitterMs+15;
                        }
                        TriggerBot.reset();LexyClient.save();LexySounds.save();return true;
                    }
                }
                return true;
            }
            int top=panelY+102,buttonY=top+42,bw=Math.max(60,(rw-26)/2);
            if(inside(mx,my,rx+9,buttonY,bw,24)){
                boolean next=!LexyClient.enabled.getOrDefault("AutoTotem",false);
                LexyClient.enabled.put("AutoTotem",next);
                if(!next)AutoTotem.onDisabled();
                LexyClient.save();return true;
            }
            if(inside(mx,my,rx+9+bw+7,buttonY,rw-bw-25,24)){
                totemSettingsOpen=!totemSettingsOpen;scroll=0;return true;
            }
            if(totemSettingsOpen){
                int yy=top+81,row=48,bodyEnd=panelY+panelH-65;
                int visible=Math.min(TOTEM_ROWS,Math.max(1,(bodyEnd-yy)/row));
                int max=Math.max(0,TOTEM_ROWS-visible),controlsY=panelY+panelH-60;
                if(inside(mx,my,rx+7,controlsY,65,25)){scroll=Math.max(0,scroll-1);return true;}
                if(inside(mx,my,rx+rw-74,controlsY,67,25)){scroll=Math.min(max,scroll+1);return true;}
                for(int i=scroll;i<Math.min(TOTEM_ROWS,scroll+visible);i++)if(inside(mx,my,rx+7,yy+(i-scroll)*row,rw-16,41)){
                    switch(i){
                        case 0->AutoTotem.mode=AutoTotem.mode==AutoTotem.Mode.DIRECT?AutoTotem.Mode.INVENTORY:AutoTotem.Mode.DIRECT;
                        case 1->AutoTotem.randomSource=!AutoTotem.randomSource;
                        case 2->{AutoTotem.minDelayMs=AutoTotem.minDelayMs>=1000?50:AutoTotem.minDelayMs+50;
                                  AutoTotem.maxDelayMs=Math.max(AutoTotem.maxDelayMs,AutoTotem.minDelayMs);}
                        case 3->AutoTotem.maxDelayMs=AutoTotem.maxDelayMs>=1800?AutoTotem.minDelayMs:AutoTotem.maxDelayMs+100;
                        case 4->AutoTotem.inventoryStepMs=AutoTotem.inventoryStepMs>=900?100:AutoTotem.inventoryStepMs+50;
                        case 5->AutoTotem.cooldownMs=AutoTotem.cooldownMs>=2600?200:AutoTotem.cooldownMs+150;
                        case 6->AutoTotem.onlyWhenEmpty=!AutoTotem.onlyWhenEmpty;
                        case 7->AutoTotem.lowHealthOnly=!AutoTotem.lowHealthOnly;
                        case 8->AutoTotem.healthThreshold=AutoTotem.healthThreshold>=20?2:AutoTotem.healthThreshold+2;
                        case 9->AutoTotem.notify=!AutoTotem.notify;
                        case 10->AutoTotem.combatFallbackDirect=!AutoTotem.combatFallbackDirect;
                        case 11->AutoTotem.safeIdleMs=AutoTotem.safeIdleMs>=2500?300:AutoTotem.safeIdleMs+100;
                        case 12->AutoTotem.emergencyDirect=!AutoTotem.emergencyDirect;
                        case 13->AutoTotem.emergencyHealth=AutoTotem.emergencyHealth>=20?2:AutoTotem.emergencyHealth+2;
                        case 14->AutoTotem.priorityOnEmpty=!AutoTotem.priorityOnEmpty;
                    }
                    LexyClient.save();LexySounds.save();return true;
                }
            }
        }else if(tab==9){
            int yy=panelY+86;
            for(int i=0;i<LexyClient.ACCENTS.length;i++){
                int step=Math.min(55,Math.max(33,(rw-16)/LexyClient.ACCENTS.length));
                if(!inside(mx,my,rx+8+i*step,yy,step-8,28))continue;
                LexyClient.accentChoice=i;LexyClient.save();return true;
            }
            int base=yy+45;int visibleCount=Math.max(1,(panelY+panelH-45-base)/44);
            for(int i=scroll;i<Math.min(19,scroll+visibleCount);i++)if(inside(mx,my,rx+7,base+(i-scroll)*44,rw-15,39)){
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
                    case 8->LexyClient.uiHoverSounds=!LexyClient.uiHoverSounds;
                    case 9->LexyClient.hudOpacity=LexyClient.hudOpacity>=245?65:LexyClient.hudOpacity+30;
                    case 10->LexyClient.hudSpacing=LexyClient.hudSpacing>=33?18:LexyClient.hudSpacing+3;
                    case 11->LexyClient.lowHealthThreshold=LexyClient.lowHealthThreshold>=20?2:LexyClient.lowHealthThreshold+2;
                    case 12->LexyClient.armorWarnPercent=LexyClient.armorWarnPercent>=65?5:LexyClient.armorWarnPercent+5;
                    case 13->LexyClient.hudTextColor=(LexyClient.hudTextColor+1)%3;
                    case 14->LexyClient.uiSounds=!LexyClient.uiSounds;
                    case 15->LexyClient.uiSoundVolume=LexyClient.uiSoundVolume>=80?10:LexyClient.uiSoundVolume+10;
                    case 16->LexyClient.maxHudWidgets=LexyClient.maxHudWidgets>=10?3:LexyClient.maxHudWidgets+1;
                    case 17->ZoomController.zoomPercent=ZoomController.zoomPercent>=70?15:ZoomController.zoomPercent+5;
                    case 18->ZoomController.toggleMode=!ZoomController.toggleMode;
                }
                LexyClient.save();LexySounds.save();HudRenderer.invalidate();if(relayout)clearAndInit();return true;
            }
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public void mouseMoved(double mx,double my){
        super.mouseMoved(mx,my);
        double px=logical(mx),py=logical(my);
        int found=-1;
        for(int i=navScroll;i<Math.min(TABS.length,navScroll+navCapacity());i++){
            int yy=navStart()+(i-navScroll)*NAV_ROW;
            if(inside(px,py,panelX+10,yy,sidebarW-16,29)){found=i;break;}
        }
        if(found>=0 && found!=lastHoveredTab && LexyClient.uiHoverSounds)LexySounds.hover();
        lastHoveredTab=found;
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
        if(inside(mx,my,panelX+7,panelY+98,sidebarW-8,panelH-256)){
            navScroll=Math.max(0,Math.min(navLimit(),navScroll-(int)Math.signum(amount)*2));return true;
        }
        if(inside(mx,my,rightX(),contentY(),rightW(),contentH())||tab==6||tab==7||tab==8||tab==9||tab==10){
            int max=0;
            if(modular())max=maxScroll(filtered().size());
            else if(tab==5||tab==6)max=Math.max(0,OptimizationManager.OPTIONS.size()-Math.max(1,(panelY+panelH-35-(panelY+115))/48));
            else if(tab==7){
                SpotifyController sp=SpotifyController.INSTANCE;
                int count=spotifyView==1?sp.devices().size():spotifyView==2?sp.playlists().size():sp.tracks().size();
                max=Math.max(0,count-Math.max(1,(panelY+panelH-30-(panelY+282))/(spotifyView==1?31:32)));
            }
            else if(tab==8)max=Math.max(0,LocalMusicPlayer.INSTANCE.tracks().size()-Math.max(1,(panelY+panelH-27-(panelY+248))/31));
            else if(tab==9)max=Math.max(0,19-Math.max(1,(panelY+panelH-45-(panelY+131))/44));
            else if(tab==10){
                if(cheatSection==0)max=totemSettingsOpen?Math.max(0,TOTEM_ROWS-Math.max(1,(panelY+panelH-65-(panelY+183))/48)):0;
                else if(cheatSection==1)max=triggerSettingsOpen?Math.max(0,TRIGGER_ROWS-Math.max(1,(panelY+panelH-65-(panelY+183))/48)):0;
                else if(cheatSection==2)max=aimSettingsOpen?Math.max(0,AIM_ROWS-Math.max(1,(panelY+panelH-65-(panelY+183))/48)):0;
                else max=clickerSettingsOpen?Math.max(0,CLICKER_ROWS-Math.max(1,(panelY+panelH-65-(panelY+183))/48)):0;
            }
            scroll=Math.max(0,Math.min(max,scroll-(int)Math.signum(amount)*2));return true;
        }return super.mouseScrolled(mx,my,amount);
    }
    @Override public void close(){LexySounds.close();LexyClient.save();if(client!=null)client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
