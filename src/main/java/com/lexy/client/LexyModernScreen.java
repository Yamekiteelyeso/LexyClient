package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Lexy Studio: module grid + non-destructive settings drawer. Draws from a single icon atlas. */
public final class LexyModernScreen extends Screen {
    private static final String[] CATS={"Dashboard","Combat","Movement","Player","Render","HUD","Optimization","Utilities","Music","Cosmetics","Profiles","Settings"};
    private static final String[] HINTS={"At a glance","Automatic combat options","Movement and keys","Inventory and safety","Camera and visuals","On-screen widgets","Vanilla video options","Convenient tools","Local audio and Spotify","Cosmetic preferences","Save and load setups","Appearance and usability"};
    private static final int GAP=8, NAV_ROW=29;
    private final Screen parent;
    private String category="Dashboard",drawer=null,filter="ALL",toast="",pendingDelete="";
    private long toastUntil,deleteUntil,openedAt;
    private String hoverHint="";
    private int px,py,pw,ph,sidebar,cx,cw,viewTop,viewBottom,cols,cardW,cardH,cardStep,scroll,navScroll;
    private TextFieldWidget search,profileInput;
    private String cacheKey="";
    private List<LexyModuleCatalog.Entry> cache=List.of();
    private List<LexyModuleCatalog.Setting> drawerSettings=List.of();
    private List<String> storedProfiles=List.of();
    public LexyModernScreen(Screen parent){super(Text.literal("LexyClient Studio"));this.parent=parent;}
    public LexyModernScreen(Screen parent,String category){this(parent);this.category=category;}
    private boolean modules(){return !category.equals("Dashboard")&&!category.equals("Profiles")&&!category.equals("Settings");}
    private boolean small(){return ph<330;}
    private boolean iconOnly(){return sidebar<60;}
    private int navCapacity(){return Math.max(1,(ph-127)/NAV_ROW);}
    private int navStart(){return py+61;}
    private int bodyHeight(){return Math.max(1,viewBottom-viewTop);}
    private int visibleRows(){return Math.max(1,(bodyHeight()+GAP)/(cardH+GAP));}
    private void invalidate(){cacheKey="";}
    private void status(String text){toast=text;toastUntil=System.currentTimeMillis()+3500;}
    private String ellipsis(DrawContext c,String s,int x,int y,int w,int color){LexyPaint.label(c,s,x,y,w,color);return s;}
    private List<LexyModuleCatalog.Entry> filtered(){
        String query=search==null?"":search.getText().trim().toLowerCase(Locale.ROOT);
        String key=category+'|'+filter+'|'+query;
        if(key.equals(cacheKey))return cache;
        List<LexyModuleCatalog.Entry> list=new ArrayList<>();
        for(var m:LexyModuleCatalog.entries()){
            if(!m.category().equals(category))continue;
            if(filter.equals("FAV")&&!LexyTheme.isFavorite(m.name()))continue;
            if(filter.equals("ON")&&(!m.enabled().getAsBoolean()||LexyModuleCatalog.isAction(m)))continue;
            if(!query.isEmpty()&&!m.name().toLowerCase(Locale.ROOT).contains(query)&&!m.detail().toLowerCase(Locale.ROOT).contains(query))continue;
            list.add(m);
        }
        cache=List.copyOf(list);cacheKey=key;return cache;
    }
    private void switchTo(String name){category=name;drawer=null;drawerSettings=List.of();filter="ALL";scroll=0;
        if(category.equals("Profiles"))storedProfiles=LexyTheme.profiles();
        invalidate();updateFields();LexySounds.navigate();}
    private void updateFields(){
        if(search!=null){search.visible=modules()&&drawer==null&&cw>=235&&ph>=230;search.setFocused(false);}
        if(profileInput!=null){profileInput.visible=category.equals("Profiles");profileInput.setFocused(false);}
    }
    private int maxScroll(){
        if(drawer!=null){int number=drawerSettings.size();
            int usable=bodyHeight()-108;return Math.max(0,number-Math.max(1,usable/53));}
        if(modules())return Math.max(0,(int)Math.ceil(filtered().size()/(double)cols)-visibleRows());
        if(category.equals("Dashboard"))return Math.max(0,(430-bodyHeight()+30)/36);
        if(category.equals("Profiles"))return Math.max(0,LexyTheme.profiles().size()-Math.max(1,(bodyHeight()-80)/37));
        return Math.max(0,(7*48+120-bodyHeight()+32)/44);
    }
    @Override protected void init(){
        openedAt=System.nanoTime();
        pw=Math.min(960,Math.max(120,width-14));ph=Math.min(590,Math.max(115,height-12));
        px=(width-pw)/2;py=(height-ph)/2;
        sidebar=pw>=730?155:pw>=480?122:45;
        cx=px+sidebar+GAP;cw=Math.max(75,pw-sidebar-GAP*2);
        viewTop=py+(ph<230?76:small()?98:119);viewBottom=py+ph-(ph<230?18:30);
        cols=cw>=530?3:cw>=290?2:1;
        cardH=bodyHeight()<95?Math.max(50,bodyHeight()-4):bodyHeight()>250?106:bodyHeight()>160?93:78;
        cardStep=cardH+GAP;cardW=Math.max(67,(cw-(cols-1)*GAP)/cols);
        int textWidth=Math.max(53,Math.min(260,cw-162));
        search=new TextFieldWidget(textRenderer,cx+5,py+(small()?73:94),textWidth,19,Text.literal("Search Lexy modules"));
        search.setMaxLength(64);search.setPlaceholder(Text.literal("Search modules..."));addDrawableChild(search);
        profileInput=new TextFieldWidget(textRenderer,cx+12,viewTop+45,
                Math.max(55,Math.min(215,cw-155)),20,Text.literal("Profile name"));
        profileInput.setMaxLength(24);profileInput.setText("MyProfile");addDrawableChild(profileInput);
        if(category.equals("Profiles"))storedProfiles=LexyTheme.profiles();
        updateFields();LexySounds.open();
    }
    private void background(DrawContext c){
        var p=LexyTheme.palette();
        if(LexyTheme.dimBackground)c.fillGradient(0,0,width,height,0xDF070707,0xE70F0F0F);
        else c.fillGradient(0,0,width,height,0xB80F0F0F,0xB3131313);
        LexyPaint.rounded(c,px-5,py-4,pw+10,ph+11,0x60000000);
        LexyPaint.rounded(c,px,py,pw,ph,p.background());
        LexyPaint.border(c,px,py,pw,ph,p.border());
        // Brand rail, restrained upper accent and footer seam.
        int progress=LexyTheme.animate?(int)Math.min(pw-12,Math.max(0,(System.nanoTime()-openedAt)*(pw-12)/210_000_000L)):pw-12;
        LexyPaint.rect(c,px+6,py+3,progress,2,p.accent());
        LexyPaint.rect(c,px+sidebar,py+48,1,ph-52,p.border());
        LexyPaint.rect(c,px+sidebar,py+ph-29,pw-sidebar,1,p.border());
        LexyPaint.rect(c,px+1,py+49,pw-2,1,p.border());
    }
    private void topBar(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();
        LexyPaint.iconTile(c,0,px+12,py+10,29,p.accentSoft());
        if(!iconOnly()){
            LexyPaint.title(c,"LEXY",px+49,py+13,1.5f,LexyPaint.WHITE);
            LexyPaint.label(c,"CLIENT  /  4.2",px+49,py+31,130,p.accent());
        }
        if(pw>580){
            String profile=LexyTheme.activeProfile.equals("Default")?"DEFAULT PROFILE":LexyTheme.activeProfile.toUpperCase(Locale.ROOT);
            LexyPaint.label(c,"●  "+profile,px+pw/2-50,py+23,Math.max(80,pw/3),LexyPaint.MUTED);
        }
        LexyPaint.button(c,"X",px+pw-37,py+11,26,26,0xFF2B2B2B,p.border(),LexyPaint.WHITE,
                LexyPaint.hit(mx,my,px+pw-37,py+11,26,26));
        if(pw>=340){LexyPaint.label(c,"FABRIC 1.20.1",px+pw-148,py+20,100,LexyPaint.DIM);}
    }
    private void rail(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();
        LexyPaint.rect(c,px+1,py+50,sidebar-1,ph-51,p.sidebar());
        int start=navStart(),count=navCapacity();
        navScroll=Math.max(0,Math.min(navScroll,CATS.length-count));
        c.enableScissor(px+3,start-2,px+sidebar-3,py+ph-61);
        for(int i=navScroll;i<Math.min(CATS.length,navScroll+count);i++){
            int x=px+6,y=start+(i-navScroll)*NAV_ROW,w=sidebar-12;
            boolean active=category.equals(CATS[i]),over=LexyPaint.hit(mx,my,x,y,w,28);
            if(active||over){LexyPaint.rounded(c,x,y,w,28,active?p.accentSoft():p.card());
                if(active)LexyPaint.rounded(c,x,y+5,3,18,p.accent());}
            LexyPaint.icon(c,i,x+(iconOnly()?9:11),y+6,17);
            if(!iconOnly())LexyPaint.label(c,CATS[i],x+36,y+10,w-40,
                    active?LexyPaint.WHITE:LexyPaint.MUTED);
        }
        c.disableScissor();
        if(count<CATS.length){
            int y=py+ph-58,half=Math.max(18,(sidebar-18)/2);
            LexyPaint.button(c,"^",px+6,y,half,19,p.card(),p.border(),LexyPaint.MUTED,
                    LexyPaint.hit(mx,my,px+6,y,half,19));
            LexyPaint.button(c,"v",px+9+half,y,half,19,p.card(),p.border(),LexyPaint.MUTED,
                    LexyPaint.hit(mx,my,px+9+half,y,half,19));
        }
        if(!iconOnly()){
            LexyPaint.label(c,"LOCAL CLIENT",px+11,py+ph-31,sidebar-18,p.accent());
            LexyPaint.label(c,"RSHIFT / F8",px+11,py+ph-17,sidebar-18,LexyPaint.DIM);
        }
    }
    private void header(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();
        int catIndex=Arrays.asList(CATS).indexOf(category);
        int icon=catIndex<0?0:catIndex;
        LexyPaint.icon(c,icon,cx+2,py+57,21);
        LexyPaint.title(c,category.toUpperCase(Locale.ROOT),cx+31,py+57,1.4f,LexyPaint.WHITE);
        if(!small())LexyPaint.label(c,HINTS[Math.max(0,catIndex)],cx+4,py+77,Math.max(40,cw-150),LexyPaint.DIM);
        if(modules()&&drawer==null&&ph>=230){
            int y=py+(small()?73:94),chipW=45,first=cx+cw-147;
            for(int i=0;i<3;i++){
                String[] names={"ALL","ON","FAV"};boolean enabled=filter.equals(names[i]);
                LexyPaint.button(c,names[i],first+i*49,y,45,19,enabled?p.accentSoft():p.card(),
                        enabled?p.accent():p.border(),enabled?LexyPaint.WHITE:LexyPaint.MUTED,
                        LexyPaint.hit(mx,my,first+i*49,y,45,19));
            }
        }
        if(drawer==null && (category.equals("Dashboard")||category.equals("Profiles"))){
            LexyPaint.label(c,"YOUR WORKSPACE",cx+4,py+(small()?82:99),cw-4,p.accent());
        }
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        hoverHint="";background(c);topBar(c,mx,my);rail(c,mx,my);header(c,mx,my);
        c.enableScissor(cx,viewTop,cx+cw,viewBottom);
        switch(category){
            case "Dashboard" -> dashboard(c,mx,my);
            case "Profiles" -> profiles(c,mx,my);
            case "Settings" -> settings(c,mx,my);
            default -> catalog(c,mx,my);
        }
        c.disableScissor();
        // Footer is persistent and never blocks interactive content.
        LexyPaint.label(c,category.equals("Dashboard")?"MODULES: "+LexyModuleCatalog.entries().size():
                category.equals("Profiles")?"CONFIG PROFILES":category.equals("Settings")?"APPEARANCE":filtered().size()+" AVAILABLE",
                cx+2,py+ph-19,Math.max(40,cw-95),LexyPaint.DIM);
        LexyPaint.label(c,"4.2  /  LOCAL",cx+Math.max(20,cw-96),py+ph-19,94,LexyTheme.palette().accent());
        if(drawer!=null)detail(c,mx,my);
        else if(LexyTheme.showTooltips&&!hoverHint.isEmpty()&&System.currentTimeMillis()>toastUntil){
            int tx=cx+2,ty=py+ph-54,tw=cw-4;
            LexyPaint.rounded(c,tx,ty,tw,20,0xF22A2A2A);
            LexyPaint.label(c,hoverHint,tx+8,ty+6,tw-16,LexyPaint.MUTED);
        }
        if(!toast.isEmpty()&&System.currentTimeMillis()<toastUntil){
            int bw=Math.min(cw,Math.max(105,textRenderer.getWidth(toast)+24));int bx=cx+Math.max(0,(cw-bw)/2),by=py+ph-55;
            LexyPaint.rounded(c,bx,by,bw,23,0xF5313131);LexyPaint.border(c,bx,by,bw,23,LexyTheme.palette().accent());
            LexyPaint.label(c,toast,bx+10,by+8,bw-18,LexyPaint.WHITE);
        }
        super.render(c,mx,my,delta);
    }
    private void dashboard(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();int yy=viewTop-scroll*36;
        int hero=bodyHeight()<180?67:82;
        LexyPaint.rounded(c,cx,yy,cw,hero,LexyPaint.mix(p.card(),p.accentSoft(),.22));
        LexyPaint.border(c,cx,yy,cw,hero,p.border());
        LexyPaint.rect(c,cx+1,yy+8,3,hero-16,p.accent());
        String name=client!=null&&client.getSession()!=null?client.getSession().getUsername():"Player";
        LexyPaint.title(c,"WELCOME, "+name.toUpperCase(Locale.ROOT),cx+15,yy+12,cw<270?1f:1.4f,LexyPaint.WHITE);
        LexyPaint.label(c,"Clean controls. No clutter.",cx+15,yy+37,cw-24,p.accent());
        if(hero>70)LexyPaint.label(c,"Profile: "+LexyTheme.activeProfile+"  •  Session "+LexyClient.sessionMinutes()+" min  •  RSHIFT to resume",cx+15,yy+58,cw-27,LexyPaint.MUTED);
        int statY=yy+hero+10,statCols=cw>=420?4:2,sw=(cw-(statCols-1)*8)/statCols;
        Runtime run=Runtime.getRuntime();String[] names={"FPS","PING","MEMORY","ACTIVE"};
        String ping="--";
        if(client.player!=null&&client.getNetworkHandler()!=null){
            var entry=client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if(entry!=null)ping=entry.getLatency()+"ms";
        }
        String[] values={String.valueOf(client.getCurrentFps()),ping,
                (run.totalMemory()-run.freeMemory())/1048576L+" MB",
                String.valueOf(LexyModuleCatalog.entries().stream().filter(e->!LexyModuleCatalog.isAction(e)&&e.enabled().getAsBoolean()).count())};
        for(int i=0;i<4;i++){
            int xx=cx+(i%statCols)*(sw+8),sy=statY+(i/statCols)*66;
            LexyPaint.rounded(c,xx,sy,sw,58,p.card());LexyPaint.border(c,xx,sy,sw,58,p.border());
            LexyPaint.label(c,names[i],xx+9,sy+10,sw-14,LexyPaint.DIM);
            LexyPaint.title(c,values[i],xx+9,sy+29,sw<95?1f:1.28f,LexyPaint.WHITE);
        }
        int quickY=statY+(statCols==4?1:2)*66+16;
        LexyPaint.label(c,"QUICK ACCESS",cx+3,quickY,cw,LexyPaint.MUTED);
        String[] actions={"Combat","HUD Editor","Max FPS","Keybinds","Waypoint","Profiles"};
        int columns=cw>=425?3:2,box=(cw-(columns-1)*8)/columns;
        for(int i=0;i<actions.length;i++){
            int xx=cx+(i%columns)*(box+8),y=quickY+20+(i/columns)*39;
            LexyPaint.button(c,actions[i],xx,y,box,31,p.card(),p.border(),LexyPaint.WHITE,LexyPaint.hit(mx,my,xx,y,box,31));
        }
    }
    private void catalog(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();List<LexyModuleCatalog.Entry> entries=filtered();
        scroll=Math.max(0,Math.min(scroll,maxScroll()));
        for(int i=0;i<entries.size();i++){
            int row=i/cols-scroll;if(row<0||row>=visibleRows()+1)continue;
            int col=i%cols,x=cx+col*(cardW+GAP),y=viewTop+row*cardStep;
            boolean hover=LexyPaint.hit(mx,my,x,y,cardW,cardH),active=entries.get(i).enabled().getAsBoolean(),
                    action=LexyModuleCatalog.isAction(entries.get(i));
            if(hover)hoverHint=entries.get(i).detail();
            LexyPaint.rounded(c,x,y,cardW,cardH,p.card());
            LexyPaint.border(c,x,y,cardW,cardH,hover?p.accent():p.border());
            if(active&&!action)LexyPaint.rect(c,x+6,y+2,Math.max(1,cardW-12),2,p.accent());
            boolean dense=cardH<81;
            int iconSize=dense?26:31;
            int ix=dense?x+9:x+(cardW-iconSize)/2,iy=y+(dense?6:8);
            LexyPaint.iconTile(c,LexyModuleCatalog.iconIndex(entries.get(i)),ix,iy,iconSize,p.sidebar());
            int nameY=dense?y+9:iy+iconSize+5;
            LexyPaint.label(c,entries.get(i).name(),x+(dense?47:10),nameY,cardW-(dense?65:31),LexyPaint.WHITE);
            LexyPaint.label(c,LexyTheme.isFavorite(entries.get(i).name())?"★":"☆",x+cardW-19,y+8,14,
                    LexyTheme.isFavorite(entries.get(i).name())?p.accent():LexyPaint.MUTED);
            if(cardH>=101)LexyPaint.label(c,entries.get(i).detail(),x+10,nameY+12,cardW-20,LexyPaint.DIM);
            int by=y+cardH-(dense?22:27),btnW=Math.max(26,cardW-54);
            LexyPaint.button(c,action?"OPEN / APPLY":active?"ENABLED":"DISABLED",x+9,by,btnW,dense?18:21,
                    action?p.sidebar():active?0xFF404040:0xFF2B2B2B,
                    action?p.border():active?0xFF696969:0xFF525252,
                    action?LexyPaint.WHITE:active?0xFFE3E3E3:0xFFC1C1C1,
                    LexyPaint.hit(mx,my,x+9,by,btnW,21));
            LexyPaint.iconTile(c,11,x+cardW-33,by,dense?18:23,p.sidebar());
        }
        if(entries.isEmpty()){
            LexyPaint.iconTile(c,16,cx+cw/2-23,viewTop+16,46,p.card());
            LexyPaint.label(c,"No modules found",cx+Math.max(8,cw/2-53),viewTop+75,cw-15,LexyPaint.MUTED);
        }
    }
    private void detail(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();
        c.fill(cx,py+50,cx+cw,viewBottom,0xBA070707);
        int w=Math.min(cw,Math.max(235,cw*65/100)),x=cx+cw-w,y=py+50,h=viewBottom-y;
        LexyPaint.rounded(c,x,y,w,h,p.sidebar());LexyPaint.border(c,x,y,w,h,p.border());
        LexyPaint.rect(c,x+1,y+1,3,h-2,p.accent());
        LexyPaint.button(c,"X",x+w-34,y+10,24,24,p.card(),p.border(),LexyPaint.WHITE,
                LexyPaint.hit(mx,my,x+w-34,y+10,24,24));
        LexyModuleCatalog.Entry entry=null;
        for(var m:LexyModuleCatalog.entries())if(m.name().equals(drawer)){entry=m;break;}
        if(entry==null)return;
        LexyPaint.iconTile(c,LexyModuleCatalog.iconIndex(entry),x+15,y+13,39,p.accentSoft());
        LexyPaint.title(c,drawer,x+65,y+14,w<305?1f:1.35f,LexyPaint.WHITE);
        LexyPaint.label(c,entry.detail(),x+65,y+37,w-105,LexyPaint.MUTED);
        boolean action=LexyModuleCatalog.isAction(entry);
        LexyPaint.button(c,action?"OPEN / APPLY":entry.enabled().getAsBoolean()?"ENABLED":"DISABLED",
                x+15,y+61,w-30,26,action?p.accentSoft():entry.enabled().getAsBoolean()?0xFF3E3E3E:0xFF2E2E2E,
                action?p.accent():entry.enabled().getAsBoolean()?0xFF727272:0xFF595959,
                LexyPaint.WHITE,LexyPaint.hit(mx,my,x+15,y+61,w-30,26));
        LexyPaint.label(c,"CONFIGURATION",x+16,y+101,w-31,LexyPaint.DIM);
        List<LexyModuleCatalog.Setting> settings=drawerSettings;
        int start=y+120,end=viewBottom-38;
        c.enableScissor(x+9,start-3,x+w-8,end);
        for(int i=0;i<settings.size();i++){
            int yy=start+(i-scroll)*53;if(yy+50<start||yy>=end)continue;
            var s=settings.get(i);
            LexyPaint.rounded(c,x+12,yy,w-24,48,p.card());
            LexyPaint.label(c,s.name(),x+21,yy+8,w-128,LexyPaint.WHITE);
            LexyPaint.label(c,s.value().get(),x+21,yy+26,w-129,p.accent());
            LexyPaint.button(c,"-",x+w-83,yy+12,28,25,p.sidebar(),p.border(),LexyPaint.WHITE,LexyPaint.hit(mx,my,x+w-83,yy+12,28,25));
            LexyPaint.button(c,"+",x+w-44,yy+12,28,25,p.sidebar(),p.border(),LexyPaint.WHITE,LexyPaint.hit(mx,my,x+w-44,yy+12,28,25));
        }
        c.disableScissor();
        if(settings.isEmpty())LexyPaint.label(c,"No additional settings for this module.",x+17,start+16,w-32,LexyPaint.MUTED);
        if(entry.legacyTab()>=0){
            LexyPaint.button(c,"ADVANCED OPTIONS",x+15,viewBottom-32,w-30,25,p.accentSoft(),p.accent(),LexyPaint.WHITE,
                    LexyPaint.hit(mx,my,x+15,viewBottom-32,w-30,25));
        }else LexyPaint.label(c,"Changes are saved automatically.",x+16,viewBottom-21,w-30,LexyPaint.DIM);
    }
    private void profiles(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();int top=viewTop;
        LexyPaint.rounded(c,cx,top,cw,84,p.card());
        LexyPaint.label(c,"PROFILE NAME",cx+12,top+15,cw-26,LexyPaint.MUTED);
        int saveW=Math.min(110,Math.max(60,cw/3));
        LexyPaint.button(c,"SAVE",cx+cw-saveW-12,top+42,saveW,24,p.accentSoft(),p.accent(),LexyPaint.WHITE,
                LexyPaint.hit(mx,my,cx+cw-saveW-12,top+42,saveW,24));
        int listTop=top+98;List<String> list=storedProfiles;
        LexyPaint.label(c,"SAVED PROFILES  •  "+list.size(),cx+4,listTop-9,cw,LexyPaint.DIM);
        for(int i=0;i<list.size();i++){
            int yy=listTop+10+(i-scroll)*37;if(yy+35<listTop||yy>=viewBottom)continue;
            LexyPaint.rounded(c,cx,yy,cw,34,p.card());
            LexyPaint.label(c,list.get(i).equals(LexyTheme.activeProfile)?"●":"○",cx+9,yy+12,15,p.accent());
            LexyPaint.label(c,list.get(i),cx+26,yy+12,cw-160,LexyPaint.WHITE);
            LexyPaint.button(c,"LOAD",cx+cw-128,yy+5,65,24,p.sidebar(),p.border(),LexyPaint.WHITE,
                    LexyPaint.hit(mx,my,cx+cw-128,yy+5,65,24));
            LexyPaint.button(c,pendingDelete.equals(list.get(i))?"SURE?":"DEL",cx+cw-56,yy+5,50,24,
                    pendingDelete.equals(list.get(i))?0xFF353535:p.sidebar(),p.border(),LexyPaint.MUTED,
                    LexyPaint.hit(mx,my,cx+cw-56,yy+5,50,24));
        }
        if(list.isEmpty())LexyPaint.label(c,"No local profiles saved. Create one above.",cx+12,listTop+28,cw-22,LexyPaint.MUTED);
    }
    private void settings(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();int yy=viewTop-scroll*44;
        LexyPaint.label(c,"COLOR THEMES",cx+4,yy+7,cw-10,LexyPaint.DIM);
        int gap=6,columns=cw>=530?4:cw>=315?3:2,boxW=(cw-gap*(columns-1))/columns;
        LexyTheme.Palette[] palettes=LexyTheme.palettes();
        for(int i=0;i<palettes.length;i++){
            int xx=cx+(i%columns)*(boxW+gap),cy=yy+23+(i/columns)*50;
            LexyPaint.rounded(c,xx,cy,boxW,43,palettes[i].background());
            LexyPaint.border(c,xx,cy,boxW,43,LexyTheme.index==i?p.accent():palettes[i].border());
            LexyPaint.rounded(c,xx+7,cy+8,17,17,palettes[i].accent());
            LexyPaint.label(c,palettes[i].name(),xx+28,cy+14,boxW-34,LexyPaint.WHITE);
        }
        int rowY=yy+23+(int)Math.ceil(palettes.length/(double)columns)*50+12;
        String[] names={"CUSTOM TITLE MENU","UI ANIMATIONS","DIM GAME BACKGROUND","HELP TOOLTIPS","INTERFACE SOUNDS"};
        boolean[] val={LexyTheme.customTitle,LexyTheme.animate,LexyTheme.dimBackground,LexyTheme.showTooltips,LexyClient.uiSounds};
        for(int i=0;i<names.length;i++){
            int cy=rowY+i*39;
            LexyPaint.rounded(c,cx,cy,cw,34,p.card());
            LexyPaint.label(c,names[i],cx+12,cy+13,cw-89,LexyPaint.WHITE);
            LexyPaint.button(c,val[i]?"ON":"OFF",cx+cw-72,cy+6,60,22,val[i]?0xFF434343:0xFF2C2C2C,
                    val[i]?0xFF727272:0xFF4E4E4E,LexyPaint.WHITE,LexyPaint.hit(mx,my,cx+cw-72,cy+6,60,22));
        }
    }
    private void invokeEntry(LexyModuleCatalog.Entry e){
        if(e.name().equals("Restore Graphics")&&!OptimizationManager.hasPreviousPreset()){
            status("No previous graphics preset to restore");return;
        }
        try{e.toggle().run();LexyModuleCatalog.changed();invalidate();
            if(LexyModuleCatalog.isAction(e))status(e.name()+" applied");
        }catch(Exception ex){System.err.println("[Lexy] Module action failed: "+e.name()+": "+ex);status("Action failed: "+e.name());}
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return super.mouseClicked(mx,my,button);
        if(super.mouseClicked(mx,my,button))return true;
        if(LexyPaint.hit(mx,my,px+pw-37,py+11,26,26)){close();return true;}
        // Drawer captures every click until dismissed, never toggles underlying cards.
        if(drawer!=null){
            int w=Math.min(cw,Math.max(235,cw*65/100)),x=cx+cw-w,y=py+50;
            if(mx<x||LexyPaint.hit(mx,my,x+w-34,y+10,24,24)){drawer=null;drawerSettings=List.of();scroll=0;updateFields();return true;}
            LexyModuleCatalog.Entry e=null;for(var m:LexyModuleCatalog.entries())if(m.name().equals(drawer)){e=m;break;}
            if(e!=null&&LexyPaint.hit(mx,my,x+15,y+61,w-30,26)){invokeEntry(e);return true;}
            List<LexyModuleCatalog.Setting> opts=drawerSettings;int start=y+120,end=viewBottom-38;
            for(int i=0;i<opts.size();i++){
                int cy=start+(i-scroll)*53;
                if(cy<start||cy+48>end)continue;
                int direction=LexyPaint.hit(mx,my,x+w-83,cy+12,28,25)?-1:
                        LexyPaint.hit(mx,my,x+w-44,cy+12,28,25)?1:0;
                if(direction!=0){opts.get(i).adjust().accept(direction);LexyModuleCatalog.changed();return true;}
            }
            if(e!=null&&e.legacyTab()>=0&&LexyPaint.hit(mx,my,x+15,viewBottom-32,w-30,25)){
                client.setScreen(new LexyScreen(false,e.legacyTab(),this));return true;
            }
            return true;
        }
        int start=navStart(),count=navCapacity();
        if(LexyPaint.hit(mx,my,px+4,start,sidebar-6,Math.min(count,12)*NAV_ROW)){
            int i=navScroll+(int)((my-start)/NAV_ROW);
            if(i>=0&&i<CATS.length)switchTo(CATS[i]);return true;
        }
        if(LexyPaint.hit(mx,my,px+6,py+ph-58,sidebar-10,19)){
            if(mx<px+sidebar/2)navScroll=Math.max(0,navScroll-1);
            else navScroll=Math.min(CATS.length-navCapacity(),navScroll+1);
            return true;
        }
        if(category.equals("Dashboard")){
            int hero=bodyHeight()<180?67:82;
            int quick=viewTop-scroll*36+hero+10+(cw>=420?66:132)+16+20,columns=cw>=425?3:2,box=(cw-(columns-1)*8)/columns;
            for(int i=0;i<6;i++){
                int xx=cx+(i%columns)*(box+8),yy=quick+(i/columns)*39;
                if(LexyPaint.hit(mx,my,xx,yy,box,31)){
                    switch(i){case 0 -> switchTo("Combat");
                        case 1 -> client.setScreen(new HudEditorScreen(this));
                        case 2 -> {OptimizationManager.apply("MAX FPS");status("Max FPS preset applied");}
                        case 3 -> client.setScreen(new KeybindSettingsScreen(this));
                        case 4 -> {LexyExtras.markWaypoint(client);status("Waypoint stored");}
                        case 5 -> switchTo("Profiles");}
                    return true;
                }
            }
            return true;
        }
        if(category.equals("Profiles")){
            int saveW=Math.min(110,Math.max(60,cw/3));
            if(LexyPaint.hit(mx,my,cx+cw-saveW-12,viewTop+42,saveW,24)){
                boolean success=LexyTheme.saveProfile(profileInput.getText());storedProfiles=LexyTheme.profiles();status(success?"Profile saved":"Choose a valid profile name");return true;
            }
            List<String> list=storedProfiles;
            for(int i=0;i<list.size();i++){
                int yy=viewTop+108+(i-scroll)*37;
                if(yy<viewTop+95||yy+34>viewBottom)continue;
                if(LexyPaint.hit(mx,my,cx+cw-128,yy+5,65,24)){
                    status(LexyTheme.loadProfile(list.get(i))?"Profile loaded":"Profile could not be loaded");return true;
                }
                if(LexyPaint.hit(mx,my,cx+cw-56,yy+5,50,24)){
                    String name=list.get(i);
                    if(name.equals(pendingDelete)&&System.currentTimeMillis()<deleteUntil){
                        boolean removed=LexyTheme.deleteProfile(name);storedProfiles=LexyTheme.profiles();status(removed?"Profile deleted":"Could not delete profile");pendingDelete="";
                    }else{pendingDelete=name;deleteUntil=System.currentTimeMillis()+4000;status("Click DEL again to confirm");}return true;
                }
            }
            return true;
        }
        if(category.equals("Settings")){
            int yy=viewTop-scroll*44,columns=cw>=530?4:cw>=315?3:2,
                    boxW=(cw-6*(columns-1))/columns;
            for(int i=0;i<LexyTheme.palettes().length;i++){
                int xx=cx+(i%columns)*(boxW+6),cy=yy+23+(i/columns)*50;
                if(LexyPaint.hit(mx,my,xx,cy,boxW,43)){LexyTheme.choose(i);status("Theme applied");return true;}
            }
            int rowY=yy+23+(int)Math.ceil(LexyTheme.palettes().length/(double)columns)*50+12;
            for(int i=0;i<5;i++){
                int cy=rowY+i*39;
                if(LexyPaint.hit(mx,my,cx+cw-72,cy+6,60,22)){
                    switch(i){case 0 -> LexyTheme.customTitle=!LexyTheme.customTitle;
                        case 1 -> LexyTheme.animate=!LexyTheme.animate;
                        case 2 -> LexyTheme.dimBackground=!LexyTheme.dimBackground;
                        case 3 -> LexyTheme.showTooltips=!LexyTheme.showTooltips;
                        case 4 -> LexyClient.uiSounds=!LexyClient.uiSounds;}
                    LexyTheme.save();LexyClient.save();return true;
                }
            }
            return true;
        }
        if(modules()){
            int first=cx+cw-147,y=py+(small()?73:94);
            for(int i=0;i<3&&ph>=230;i++)if(LexyPaint.hit(mx,my,first+i*49,y,45,19)){
                filter=new String[]{"ALL","ON","FAV"}[i];scroll=0;invalidate();return true;
            }
            List<LexyModuleCatalog.Entry> entries=filtered();
            for(int i=0;i<entries.size();i++){
                int row=i/cols-scroll,col=i%cols,x=cx+col*(cardW+GAP),cy=viewTop+row*cardStep;
                if(row<0||cy>=viewBottom||cy+cardH<=viewTop||my<viewTop||my>=viewBottom)continue;
                var e=entries.get(i);int by=cy+cardH-(cardH<81?22:27),btnW=Math.max(26,cardW-54);
                if(LexyPaint.hit(mx,my,x+cardW-24,cy+2,23,29)){
                    LexyTheme.favorite(e.name());invalidate();return true;
                }
                if(LexyPaint.hit(mx,my,x+9,by,btnW,cardH<81?18:21)){
                    invokeEntry(e);return true;
                }
                if(LexyPaint.hit(mx,my,x,cy,cardW,cardH)){
                    drawer=e.name();drawerSettings=LexyModuleCatalog.settings(drawer);scroll=0;updateFields();LexySounds.navigate();return true;
                }
            }
        }
        return true;
    }
    @Override public boolean mouseScrolled(double mx,double my,double amount){
        if(drawer==null&&LexyPaint.hit(mx,my,px+4,navStart(),sidebar-6,ph-110)){
            navScroll=Math.max(0,Math.min(CATS.length-navCapacity(),navScroll-(int)Math.signum(amount)));return true;
        }
        scroll=Math.max(0,Math.min(maxScroll(),scroll-(int)Math.signum(amount)));return true;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT||key==GLFW.GLFW_KEY_F8){close();return true;}
        if(key==GLFW.GLFW_KEY_ESCAPE&&drawer!=null){drawer=null;drawerSettings=List.of();scroll=0;updateFields();return true;}
        if((modifiers&GLFW.GLFW_MOD_CONTROL)!=0&&key==GLFW.GLFW_KEY_F&&search!=null&&search.visible){
            search.setFocused(true);return true;
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean keyReleased(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT||key==GLFW.GLFW_KEY_F8)LexyClient.menuShiftReleased();
        return super.keyReleased(key,scan,modifiers);
    }
    @Override public void close(){LexyClient.suppressMenuToggle();LexyClient.save();client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}