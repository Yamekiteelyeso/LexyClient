package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Small reusable, allocation-light drawing primitives. The only icon resource is one 8x4 atlas. */
public final class LexyPaint {
    private LexyPaint() {}
    public static final int WHITE=0xFFF7F7F7, MUTED=0xFFA8A8A8, DIM=0xFF7F7F7F,
            GREEN=0xFFBBBBBB, RED=0xFF8C8C8C;
    public static void rect(DrawContext c,int x,int y,int w,int h,int color){
        if(w>0&&h>0)c.fill(x,y,x+w,y+h,color);
    }
    public static void rounded(DrawContext c,int x,int y,int w,int h,int color){
        if(w<=0||h<=0)return;
        if(w<10||h<10){rect(c,x,y,w,h,color);return;}
        rect(c,x+5,y,w-10,h,color);
        rect(c,x+2,y+2,3,h-4,color);rect(c,x+w-5,y+2,3,h-4,color);
        rect(c,x+1,y+4,1,h-8,color);rect(c,x+w-2,y+4,1,h-8,color);
        rect(c,x+3,y+1,w-6,1,color);rect(c,x+3,y+h-2,w-6,1,color);
    }
    public static void border(DrawContext c,int x,int y,int w,int h,int color){
        if(w<10||h<10)return;
        rect(c,x+5,y,w-10,1,color);rect(c,x+5,y+h-1,w-10,1,color);
        rect(c,x,y+5,1,h-10,color);rect(c,x+w-1,y+5,1,h-10,color);
        rect(c,x+1,y+3,1,2,color);rect(c,x+w-2,y+3,1,2,color);
        rect(c,x+2,y+1,3,1,color);rect(c,x+w-5,y+1,3,1,color);
        rect(c,x+1,y+h-5,1,2,color);rect(c,x+w-2,y+h-5,1,2,color);
        rect(c,x+2,y+h-2,3,1,color);rect(c,x+w-5,y+h-2,3,1,color);
    }
    public static void icon(DrawContext c,int atlasIndex,int x,int y,int size){
        int index=Math.floorMod(atlasIndex,32),u=(index%8)*32,v=(index/8)*32;
        c.drawTexture(LexyClient.ICON_ATLAS,x,y,size,size,u,v,32,32,256,128);
    }
    public static void iconTile(DrawContext c,int index,int x,int y,int size,int background){
        rounded(c,x,y,size,size,background);
        int glyph=Math.max(14,size-13),offset=(size-glyph)/2;
        icon(c,index,x+offset,y+offset,glyph);
    }
    public static void label(DrawContext c,String text,int x,int y,int max,int color){
        if(text==null||max<8)return;
        var tr=MinecraftClient.getInstance().textRenderer;
        if(tr.getWidth(text)<=max){c.drawTextWithShadow(tr,text,x,y,color);return;}
        int low=0,high=text.length();
        while(low<high){int mid=(low+high+1)/2;
            if(tr.getWidth(text.substring(0,mid))+tr.getWidth("...")<=max)low=mid;
            else high=mid-1;
        }
        if(low>0)c.drawTextWithShadow(tr,text.substring(0,low)+"...",x,y,color);
    }
    public static void title(DrawContext c,String s,int x,int y,float scale,int color){
        c.getMatrices().push();c.getMatrices().scale(scale,scale,1);
        c.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,s,
                (int)(x/scale),(int)(y/scale),color);
        c.getMatrices().pop();
    }
    public static void button(DrawContext c,String name,int x,int y,int w,int h,
                              int back,int outline,int textColor,boolean hover){
        rounded(c,x,y,w,h,hover?mix(back,0xFF515151,.28):back);
        border(c,x,y,w,h,hover?mix(outline,0xFFB8B8B8,.55):outline);
        var tr=MinecraftClient.getInstance().textRenderer;
        label(c,name,x+Math.max(8,(w-tr.getWidth(name))/2),y+(h-8)/2,Math.max(12,w-14),textColor);
    }
    public static boolean hit(double mx,double my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    public static int mix(int a,int b,double t){
        t=Math.max(0,Math.min(1,t));
        int al=(int)((a>>>24)*(1-t)+(b>>>24)*t);
        int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
        int g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
        int bl=(int)((a&255)*(1-t)+(b&255)*t);
        return (al<<24)|(r<<16)|(g<<8)|bl;
    }
    public static void pill(DrawContext c,String name,int x,int y,int w,int h,boolean on){
        var p=LexyTheme.palette();
        button(c,name,x,y,w,h,on?p.accentSoft():p.sidebar(),on?p.accent():p.border(),on?WHITE:MUTED,false);
    }
}