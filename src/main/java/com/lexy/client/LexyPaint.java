package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Lightweight shapes and atlas icons. Minecraft owns the rendering state. */
public final class LexyPaint {
    private LexyPaint() {}
    public static final int WHITE=0xFFF1F6FF, MUTED=0xFF9FAFC5, DIM=0xFF6F859F, GREEN=0xFF3BCF99, RED=0xFFE26779;
    public static void rect(DrawContext c,int x,int y,int w,int h,int color){if(w>0&&h>0)c.fill(x,y,x+w,y+h,color);}
    public static void rounded(DrawContext c,int x,int y,int w,int h,int color){
        if(w<6||h<6){rect(c,x,y,w,h,color);return;}
        rect(c,x+3,y,w-6,h,color);rect(c,x,y+3,3,h-6,color);rect(c,x+w-3,y+3,3,h-6,color);
        rect(c,x+1,y+1,w-2,2,color);rect(c,x+1,y+h-3,w-2,2,color);
    }
    public static void border(DrawContext c,int x,int y,int w,int h,int color){
        rect(c,x+3,y,w-6,1,color);rect(c,x+3,y+h-1,w-6,1,color);
        rect(c,x,y+3,1,h-6,color);rect(c,x+w-1,y+3,1,h-6,color);
    }
    public static void icon(DrawContext c,int atlasIndex,int x,int y,int size){
        int u=(atlasIndex%4)*32,v=(atlasIndex/4)*32;
        c.drawTexture(LexyClient.ICON_ATLAS,x,y,size,size,u,v,32,32,128,128);
    }
    public static void label(DrawContext c,String text,int x,int y,int max,int color){
        var tr=MinecraftClient.getInstance().textRenderer;
        if(text==null)return;
        if(max<12)return;
        String trimmed=text;
        while(trimmed.length()>1 && tr.getWidth(trimmed)>max)trimmed=trimmed.substring(0,trimmed.length()-1);
        if(!trimmed.equals(text)&&trimmed.length()>3)trimmed=trimmed.substring(0,trimmed.length()-3)+"...";
        c.drawTextWithShadow(tr,trimmed,x,y,color);
    }
    public static boolean hit(double mx,double my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    public static int mix(int a,int b,double t){
        t=Math.max(0,Math.min(1,t));
        int r=(int)((a>>16&255)*(1-t)+(b>>16&255)*t);
        int g=(int)((a>>8&255)*(1-t)+(b>>8&255)*t);
        int bl=(int)((a&255)*(1-t)+(b&255)*t);
        return 0xFF000000|(r<<16)|(g<<8)|bl;
    }
    public static void pill(DrawContext c,String name,int x,int y,int w,int h,boolean on){
        var p=LexyTheme.palette();rounded(c,x,y,w,h,on?p.accentSoft():p.sidebar());
        border(c,x,y,w,h,on?p.accent():p.border());
        c.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,name,x+w/2,y+(h-8)/2,on?WHITE:MUTED);
    }
}