package com.lexy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Lunar/Feather-inspired original card-based interface. Data-driven: switches call live modules. */
public final class LexyModernScreen extends Screen {
    private static final String[] CATEGORIES={"Dashboard","Combat","Movement","Player","Render","HUD","Optimization","Utilities","Music","Cosmetics","Profiles","Settings"};
    private static final int[] ICONS={0,10,13,3,6,2,7,15,8,4,12,5};
    private final Screen parent;
    private String category="Dashboard",selected=null,filter="ALL",toast="";
    private int scroll=0,navScroll=0,px,py,pw,ph,rail,cx,cw,viewTop,viewBottom,cols,cardW;
    private TextFieldWidget search,profileInput;
    private long openedAt;
    public LexyModernScreen(Screen parent){super(Text.literal("LexyClient 4.0 Ultimate"));this.parent=parent;}
    public LexyModernScreen(Screen parent,String category){this(parent);this.category=category;}
    @Override protected void init(){
        openedAt=System.nanoTime();
        pw=Math.min(1160,Math.max(1,width-12));ph=Math.min(690,Math.max(1,height-12));px=(width-pw)/2;py=(height-ph)/2;
        rail=pw>=560?154:42;cx=px+rail+14;cw=pw-rail-26;
        viewTop=py+99;viewBottom=py+ph-45;
        cols=cw>=720?3:cw>=420?2:1;cardW=Math.max(90,(cw-12*(cols-1))/cols);
        search=new TextFieldWidget(textRenderer,Math.max(cx+6,px+rail+12),py+58,Math.max(65,Math.min(280,cw-160)),19,Text.literal("Search modules"));
        search.setMaxLength(60);search.setPlaceholder(Text.literal("Search modules..."));addDrawableChild(search);
        profileInput=new TextFieldWidget(textRenderer,cx+12,py+116,Math.max(75,Math.min(200,cw-155)),20,Text.literal("Profile name"));
        profileInput.setMaxLength(24);profileInput.setText("MyProfile");addDrawableChild(profileInput);
        updateFields();
        LexySounds.open();
    }
    private boolean compact(){return rail<100;}
    private void updateFields(){if(search!=null){search.visible=isModules()&&selected==null;search.setFocused(false);}if(profileInput!=null){profileInput.visible=category.equals("Profiles")&&selected==null;profileInput.setFocused(false);}}
    private boolean isModules(){return !category.equals("Dashboard")&&!category.equals("Profiles")&&!category.equals("Settings");}
    private void switchCategory(String newCategory){category=newCategory;selected=null;scroll=0;filter="ALL";updateFields();LexySounds.navigate();}
    private void changeToast(String message){toast=message;}
    private void caption(DrawContext c,String s,int x,int y,int w,int color){LexyPaint.label(c,s,x,y,w,color);}
    private void chip(DrawContext c,String s,int x,int y,int w,boolean active){LexyPaint.pill(c,s,x,y,w,20,active);}
    private int filteredRows(){return Math.max(1,(viewBottom-viewTop)/114);}
    private List<LexyModuleCatalog.Entry> filtered(){
        String query=search==null?"":search.getText().trim().toLowerCase(Locale.ROOT);
        List<LexyModuleCatalog.Entry> all=new ArrayList<>();
        for(var e:LexyModuleCatalog.entries()){
            if(!e.category().equals(category))continue;
            if(filter.equals("ENABLED")&&!e.enabled().getAsBoolean())continue;
            if(filter.equals("FAVORITES")&&!LexyTheme.isFavorite(e.name()))continue;
            if(!query.isEmpty()&&!e.name().toLowerCase(Locale.ROOT).contains(query)&&!e.detail().toLowerCase(Locale.ROOT).contains(query))continue;
            all.add(e);
        }
        return all;
    }
    private int maxScroll(){return Math.max(0,(int)Math.ceil(filtered().size()/(double)cols)-filteredRows());}
    @Override public void render(DrawContext c,int mx,int my,float tickDelta){
        var theme=LexyTheme.palette();
        c.fillGradient(0,0,width,height,0xF204080F,theme.background());
        LexyPaint.rounded(c,px-3,py-3,pw+6,ph+6,0x77000000);
        LexyPaint.rounded(c,px,py,pw,ph,theme.background());
        LexyPaint.border(c,px,py,pw,ph,theme.border());
        LexyPaint.rect(c,px+1,py+1,pw-2,3,theme.accent());
        LexyPaint.rect(c,px+3,py+4,rail-2,ph-8,theme.sidebar());
        LexyPaint.icon(c,0,px+11,py+13,24);
        if(!compact()){
            caption(c,"LEXY",px+41,py+13,rail-47,LexyPaint.WHITE);
            caption(c,"CLIENT 4.0",px+41,py+28,rail-45,theme.accent());
        }
        LexyPaint.rect(c,px+9,py+49,rail-16,1,theme.border());
        int navTop=py+62, navBottom=py+ph-68,navCap=Math.max(1,(navBottom-navTop)/32);
        navScroll=Math.max(0,Math.min(navScroll,CATEGORIES.length-navCap));
        c.enableScissor(px+3,navTop-2,px+rail-3,navBottom+1);
        for(int i=navScroll;i<Math.min(CATEGORIES.length,navScroll+navCap);i++){
            int yy=navTop+(i-navScroll)*32;
            boolean active=CATEGORIES[i].equals(category),hover=LexyPaint.hit(mx,my,px+7,yy,rail-12,28);
            if(active||hover){LexyPaint.rounded(c,px+7,yy,rail-12,27,active?theme.accentSoft():theme.card());
                if(active)LexyPaint.rect(c,px+7,yy+4,3,19,theme.accent());}
            LexyPaint.icon(c,ICONS[i],px+17,yy+5,17);
            if(!compact())caption(c,CATEGORIES[i],px+42,yy+10,rail-51,active?LexyPaint.WHITE:LexyPaint.MUTED);
        }
        c.disableScissor();
        caption(c,compact()?"L":"LEXY / ONLINE",px+11,py+ph-39,rail-17,theme.accent());
        if(!compact())caption(c,"RSHIFT  /  F8",px+11,py+ph-24,rail-17,LexyPaint.DIM);
        // Header and page title.
        caption(c,category.toUpperCase(Locale.ROOT),cx,py+17,cw-75,LexyPaint.WHITE);
        caption(c,selected==null?"LEX Y  /  MODULE CENTER":"CONFIGURING / "+selected,cx,py+34,cw-77,LexyPaint.MUTED);
        LexyPaint.rounded(c,px+pw-38,py+12,25,25,0xFF3D202A);
        caption(c,"X",px+pw-30,py+21,18,0xFFFFB7C5);
        LexyPaint.rect(c,cx,py+53,cw,1,theme.border());
        if(isModules()&&selected==null){
            chip(c,"ALL",cx+Math.max(0,cw-177),py+60,52,filter.equals("ALL"));
            chip(c,"ON",cx+Math.max(0,cw-118),py+60,49,filter.equals("ENABLED"));
            chip(c,"FAV",cx+Math.max(0,cw-61),py+60,54,filter.equals("FAVORITES"));
        }
        if(selected!=null)renderDetail(c,mx,my);
        else switch(category){
            case "Dashboard" -> renderDashboard(c,mx,my);
            case "Profiles" -> renderProfiles(c,mx,my);
            case "Settings" -> renderSettings(c,mx,my);
            default -> renderCatalog(c,mx,my);
        }
        LexyPaint.rect(c,cx,py+ph-42,cw,1,theme.border());
        caption(c,"Fabric 1.20.1  ·  Java 17  ·  local settings",cx,py+ph-29,cw-60,LexyPaint.DIM);
        caption(c,"4.0.0",px+pw-48,py+ph-29,43,theme.accent());
        if(!toast.isEmpty())caption(c,toast,cx,py+ph-55,cw,theme.accent());
        super.render(c,mx,my,tickDelta);
    }
    private void renderDashboard(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();
        String name=client!=null&&client.getSession()!=null?client.getSession().getUsername():"Player";
        LexyPaint.rounded(c,cx,viewTop,cw,60,p.card());
        LexyPaint.rect(c,cx,viewTop,4,60,p.accent());
        caption(c,"WELCOME BACK, "+name.toUpperCase(Locale.ROOT),cx+14,viewTop+12,cw-28,LexyPaint.WHITE);
        caption(c,"LexyClient 4.0  ·  "+LexyTheme.activeProfile+" profile  ·  "+LexyModuleCatalog.entries().size()+" real actions",cx+14,viewTop+33,cw-25,LexyPaint.MUTED);
        int w=(cw-16)/3,y=viewTop+71;
        String[] k={"FPS","RAM","ACTIVE"};
        Runtime r=Runtime.getRuntime();
        String[] v={client.getCurrentFps()+"",(r.totalMemory()-r.freeMemory())/1048576L+" MB",
                LexyModuleCatalog.entries().stream().filter(e->e.enabled().getAsBoolean()).count()+""};
        for(int i=0;i<3;i++){
            int xx=cx+i*(w+8);LexyPaint.rounded(c,xx,y,w,68,p.card());
            LexyPaint.border(c,xx,y,w,68,p.border());
            caption(c,k[i],xx+12,y+10,w-20,LexyPaint.MUTED);
            caption(c,v[i],xx+12,y+34,w-22,p.accent());
        }
        int yy=viewTop+151;
        caption(c,"QUICK ACTIONS",cx,yy,cw,LexyPaint.MUTED);
        String[] actions={"COMBAT", "HUD EDITOR", "MAX FPS", "KEYBINDS", "WAYPOINT", "PROFILES"};
        int columns=cw>=430?3:2;int card=(cw-8*(columns-1))/columns;
        for(int i=0;i<actions.length;i++){
            int x=cx+(i%columns)*(card+8),cy=yy+18+(i/columns)*42;
            if(cy+34>=viewBottom)break;
            LexyPaint.pill(c,actions[i],x,cy,card,34,LexyPaint.hit(mx,my,x,cy,card,34));
        }
        caption(c,"Installed mod: lexy-client · "+(client.world==null?"in menus":LexyTools.server(client)),cx,viewBottom-18,cw,LexyPaint.DIM);
    }
    private void renderCatalog(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();List<LexyModuleCatalog.Entry> list=filtered();
        scroll=Math.max(0,Math.min(scroll,maxScroll()));
        c.enableScissor(cx,viewTop,cx+cw,viewBottom);
        for(int i=0;i<list.size();i++){
            int row=i/cols-scroll;if(row<0||row>=filteredRows()+1)continue;
            int col=i%cols,x=cx+col*(cardW+12),y=viewTop+row*114,w=cardW;
            var entry=list.get(i);boolean active=entry.enabled().getAsBoolean(),hover=LexyPaint.hit(mx,my,x,y,w,105);
            LexyPaint.rounded(c,x,y,w,105,hover?LexyPaint.mix(p.card(),p.accentSoft(),.23):p.card());
            LexyPaint.border(c,x,y,w,105,hover?p.accent():p.border());
            LexyPaint.icon(c,ICONS[Math.max(0,Arrays.asList(CATEGORIES).indexOf(entry.category()))],x+12,y+12,23);
            caption(c,entry.name(),x+41,y+12,w-70,LexyPaint.WHITE);
            caption(c,LexyTheme.isFavorite(entry.name())?"★":"☆",x+w-18,y+11,17,LexyTheme.isFavorite(entry.name())?p.accent():LexyPaint.MUTED);
            caption(c,entry.detail(),x+12,y+44,w-23,LexyPaint.MUTED);
            LexyPaint.pill(c,LexyModuleCatalog.isAction(entry)?"APPLY / OPEN":active?"ENABLED":"DISABLED",
                    x+10,y+76,Math.max(55,w-51),23,active);
            LexyPaint.rounded(c,x+w-36,y+76,26,23,p.sidebar());LexyPaint.icon(c,5,x+w-31,y+80,15);
        }
        c.disableScissor();
        if(list.isEmpty())caption(c,"No modules match. Try ALL or another search.",cx+15,viewTop+32,cw-30,LexyPaint.MUTED);
        int total=(int)Math.ceil(list.size()/(double)cols);
        caption(c,""+list.size()+" options   •   "+(scroll+1)+"/"+Math.max(1,total),cx,viewBottom+8,cw,LexyPaint.DIM);
    }
    private void renderDetail(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();int x=cx,y=viewTop,w=cw;
        LexyPaint.rounded(c,x,y,w,Math.max(50,viewBottom-viewTop),p.card());LexyPaint.border(c,x,y,w,Math.max(50,viewBottom-viewTop),p.border());
        caption(c,"‹ BACK",x+13,y+13,w-20,p.accent());
        caption(c,selected,x+18,y+37,w-38,LexyPaint.WHITE);
        LexyModuleCatalog.Entry entry=LexyModuleCatalog.entries().stream().filter(e->e.name().equals(selected)).findFirst().orElse(null);
        if(entry!=null){
            caption(c,entry.detail(),x+18,y+58,w-40,LexyPaint.MUTED);
            LexyPaint.pill(c,LexyModuleCatalog.isAction(entry)?"APPLY / OPEN":entry.enabled().getAsBoolean()?"ENABLED":"DISABLED",x+18,y+80,Math.min(140,w-35),23,entry.enabled().getAsBoolean());
        }
        var settings=LexyModuleCatalog.settings(selected);int sh=36;
        for(int i=0;i<settings.size();i++){
            int yy=y+116+i*sh;if(yy+sh>viewBottom-28)break;
            var setting=settings.get(i);
            caption(c,setting.name(),x+18,yy,w-125,LexyPaint.WHITE);
            caption(c,setting.value().get(),x+18,yy+13,w-120,p.accent());
            LexyPaint.pill(c,"−",x+w-84,yy+4,28,25,false);
            LexyPaint.pill(c,"+",x+w-44,yy+4,28,25,false);
        }
        int bottom=Math.min(viewBottom-28,y+118+settings.size()*sh);
        caption(c,"Advanced settings / keybinds via original panel",x+17,bottom,w-35,LexyPaint.DIM);
        LexyPaint.pill(c,"ADVANCED",x+17,Math.min(viewBottom-27,bottom+13),Math.min(130,w-32),24,false);
    }
    private void renderProfiles(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();
        caption(c,"LOCAL PROFILES",cx+4,viewTop+10,cw-18,LexyPaint.WHITE);
        caption(c,"Snapshots save modules, combat settings, HUD layout and UI themes.",cx+4,viewTop+31,cw-13,LexyPaint.MUTED);
        int b=Math.min(cw-10,119);
        LexyPaint.pill(c,"SAVE PROFILE",cx+Math.max(12,cw-b-5),viewTop+16,b,23,true);
        int yy=viewTop+73;var list=LexyTheme.profiles();
        for(int i=0;i<list.size()&&i<8;i++){
            int y=yy+i*37;if(y+36>viewBottom)break;
            LexyPaint.rounded(c,cx+3,y,cw-6,33,p.card());
            caption(c,list.get(i),cx+15,y+12,cw-100,LexyPaint.WHITE);
            LexyPaint.pill(c,"LOAD",cx+cw-77,y+5,65,23,false);
        }
        if(list.isEmpty())caption(c,"No profiles yet. Type a name and choose SAVE PROFILE.",cx+10,yy+16,cw-20,LexyPaint.MUTED);
    }
    private void renderSettings(DrawContext c,int mx,int my){
        var p=LexyTheme.palette();
        String[] labels={"THEME", "CUSTOM TITLE SCREEN", "UI ANIMATIONS", "BACKGROUND DIMMING", "HELP TOOLTIPS", "UI SOUND", "RENDER PROFILE"};
        String[] values={p.name(),bool(LexyTheme.customTitle),bool(LexyTheme.animate),bool(LexyTheme.dimBackground),
                bool(LexyTheme.showTooltips),bool(LexyClient.uiSounds),LexyTheme.activeProfile};
        for(int i=0;i<labels.length;i++){
            int y=viewTop+i*43;if(y+41>viewBottom)break;
            LexyPaint.rounded(c,cx,y,cw,38,p.card());
            caption(c,labels[i],cx+12,y+7,cw-115,LexyPaint.WHITE);
            caption(c,values[i],cx+12,y+22,cw-110,p.accent());
            LexyPaint.pill(c,i==0?"NEXT":"CHANGE",cx+cw-90,y+8,78,23,false);
        }
    }
    private String bool(boolean b){return b?"ON":"OFF";}
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return super.mouseClicked(mx,my,button);
        if(super.mouseClicked(mx,my,button))return true;
        if(LexyPaint.hit(mx,my,px+pw-40,py+8,37,34)){close();return true;}
        int navTop=py+62,navCap=Math.max(1,(ph-130)/32);
        if(LexyPaint.hit(mx,my,px+4,navTop,rail-6,Math.min(navCap,12)*32)){
            int i=navScroll+(int)((my-navTop)/32);
            if(i>=0&&i<CATEGORIES.length)switchCategory(CATEGORIES[i]);return true;
        }
        if(selected!=null){
            if(LexyPaint.hit(mx,my,cx+6,viewTop+4,160,26)){selected=null;updateFields();return true;}
            var entry=LexyModuleCatalog.entries().stream().filter(e->e.name().equals(selected)).findFirst().orElse(null);
            if(entry!=null&&LexyPaint.hit(mx,my,cx+18,viewTop+80,Math.min(140,cw-35),23)){entry.toggle().run();return true;}
            var s=LexyModuleCatalog.settings(selected);
            for(int i=0;i<s.size();i++){
                int yy=viewTop+116+i*36;
                if(yy+36>viewBottom-28)break;
                if(LexyPaint.hit(mx,my,cx+cw-84,yy+4,28,25)){s.get(i).adjust().accept(-1);LexyModuleCatalog.changed();return true;}
                if(LexyPaint.hit(mx,my,cx+cw-44,yy+4,28,25)){s.get(i).adjust().accept(1);LexyModuleCatalog.changed();return true;}
            }
            int bottom=Math.min(viewBottom-28,viewTop+118+s.size()*36),btnY=Math.min(viewBottom-27,bottom+13);
            if(LexyPaint.hit(mx,my,cx+17,btnY,Math.min(130,cw-32),24)){
                if(entry!=null&&entry.legacyTab()>=0)client.setScreen(new LexyScreen(false,entry.legacyTab(),this));
                else client.setScreen(new KeybindSettingsScreen(this));return true;
            }
            return true;
        }
        if(category.equals("Dashboard")){
            String[] actions={"Combat","HUD","Optimization","Utilities","Utilities","Profiles"};
            int y=viewTop+169,columns=cw>=430?3:2,card=(cw-8*(columns-1))/columns;
            for(int i=0;i<6;i++){
                int xx=cx+(i%columns)*(card+8),yy=y+(i/columns)*42;
                if(LexyPaint.hit(mx,my,xx,yy,card,34)){
                    if(i==1)client.setScreen(new HudEditorScreen(this));
                    else if(i==2){OptimizationManager.apply("MAX FPS");changeToast("MAX FPS preset applied");}
                    else if(i==3)client.setScreen(new KeybindSettingsScreen(this));
                    else if(i==4){LexyExtras.markWaypoint(client);changeToast("Waypoint saved");}
                    else switchCategory(actions[i]);return true;
                }
            }
            return true;
        }
        if(category.equals("Profiles")){
            if(LexyPaint.hit(mx,my,cx+cw-Math.min(cw-10,119)-5,viewTop+16,Math.min(cw-10,119),23)){
                changeToast(LexyTheme.saveProfile(profileInput.getText())?"Profile saved":"Choose a valid name");return true;}
            List<String> list=LexyTheme.profiles();
            for(int i=0;i<list.size()&&i<8;i++)if(LexyPaint.hit(mx,my,cx+cw-77,viewTop+73+i*37+5,65,23)){
                changeToast(LexyTheme.loadProfile(list.get(i))?"Profile loaded":"Profile invalid");return true;}
            return true;
        }
        if(category.equals("Settings")){
            for(int i=0;i<7;i++)if(LexyPaint.hit(mx,my,cx+cw-90,viewTop+i*43+8,78,23)){
                switch(i){case 0 -> LexyTheme.next();case 1 -> LexyTheme.customTitle=!LexyTheme.customTitle;
                    case 2 -> LexyTheme.animate=!LexyTheme.animate;case 3 -> LexyTheme.dimBackground=!LexyTheme.dimBackground;
                    case 4 -> LexyTheme.showTooltips=!LexyTheme.showTooltips;case 5 -> LexyClient.uiSounds=!LexyClient.uiSounds;
                    case 6 -> {switchCategory("Profiles");return true;}}
                LexyTheme.save();LexyClient.save();return true;
            }
            return true;
        }
        if(isModules()){
            int[] x={cx+Math.max(0,cw-177),cx+Math.max(0,cw-118),cx+Math.max(0,cw-61)};
            int[] w={52,49,54};String[] f={"ALL","ENABLED","FAVORITES"};
            for(int i=0;i<3;i++)if(LexyPaint.hit(mx,my,x[i],py+60,w[i],20)){filter=f[i];scroll=0;return true;}
            List<LexyModuleCatalog.Entry> list=filtered();
            for(int i=0;i<list.size();i++){
                int row=i/cols-scroll,col=i%cols,xx=cx+col*(cardW+12),yy=viewTop+row*114;
                if(row<0||yy+105>viewBottom)continue;
                if(LexyPaint.hit(mx,my,xx+cardW-25,yy+5,25,29)){LexyTheme.favorite(list.get(i).name());return true;}
                if(LexyPaint.hit(mx,my,xx+10,yy+76,Math.max(55,cardW-51),23)){
                    list.get(i).toggle().run();LexyModuleCatalog.changed();return true;
                }
                if(LexyPaint.hit(mx,my,xx,yy,cardW,105)){
                    selected=list.get(i).name();updateFields();LexySounds.navigate();return true;
                }
            }
        }
        return true;
    }
    @Override public boolean mouseScrolled(double mx,double my,double amount){
        if(LexyPaint.hit(mx,my,px+4,py+58,rail-6,ph-117)){
            int cap=Math.max(1,(ph-130)/32);navScroll=Math.max(0,Math.min(CATEGORIES.length-cap,navScroll-(int)Math.signum(amount)));return true;
        }
        scroll=Math.max(0,Math.min(maxScroll(),scroll-(int)Math.signum(amount)));return true;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT||key==GLFW.GLFW_KEY_F8){close();return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean keyReleased(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT||key==GLFW.GLFW_KEY_F8)LexyClient.menuShiftReleased();
        return super.keyReleased(key,scan,modifiers);
    }
    @Override public void close(){LexyClient.suppressMenuToggle();client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}