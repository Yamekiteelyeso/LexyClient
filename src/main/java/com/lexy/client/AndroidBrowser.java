package com.lexy.client;

import java.net.URI;
import net.minecraft.util.Util;

/** Attempts the Android ACTION_VIEW intent when run by Pojav; no Android SDK compile dependency. */
public final class AndroidBrowser {
    private AndroidBrowser(){}
    public static boolean open(String url) {
        if(url==null || !url.startsWith("https://accounts.spotify.com/")
                && !url.startsWith("https://developer.spotify.com/")) return false;
        try {
            Class<?> thread = Class.forName("android.app.ActivityThread");
            Object app = thread.getMethod("currentApplication").invoke(null);
            if(app!=null){
                Class<?> uriClass=Class.forName("android.net.Uri");
                Object uri=uriClass.getMethod("parse",String.class).invoke(null,url);
                Class<?> intentClass=Class.forName("android.content.Intent");
                Object intent=intentClass.getConstructor(String.class,uriClass).newInstance("android.intent.action.VIEW",uri);
                intentClass.getMethod("addFlags",int.class).invoke(intent,0x10000000);
                Class<?> context=Class.forName("android.content.Context");
                context.getMethod("startActivity",intentClass).invoke(app,intent);
                return true;
            }
        }catch(Throwable ignored){ /* Not an Android bridge runtime. */ }
        try { Util.getOperatingSystem().open(URI.create(url));return true; }
        catch(Throwable ignored){return false;}
    }
}