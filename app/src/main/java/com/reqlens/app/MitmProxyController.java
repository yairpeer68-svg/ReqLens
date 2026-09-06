package com.reqlens.app;

import android.content.Context;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;
import java.util.concurrent.Executor;

public final class MitmProxyController {
    private static final String PREF="reqlens_mitm", ENABLED="enabled";
    private MitmProxyController(){}
    public static boolean enabled(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getBoolean(ENABLED,false);}
    public static void setEnabled(Context c,boolean on){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putBoolean(ENABLED,on).apply();}
    public static int ensureStarted(Context c)throws Exception{return MitmProxyServer.get(c).start();}
    public static void stop(Context c){MitmProxyServer.get(c).stop();}
    public static boolean proxyOverrideSupported(){return WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE);}
    public static boolean applyToWebViewProcess(Context c,Runnable done)throws Exception{
        if(!proxyOverrideSupported())return false;
        int port=ensureStarted(c);
        ProxyConfig cfg=new ProxyConfig.Builder().addProxyRule("127.0.0.1:"+port).build();
        Executor direct=Runnable::run;
        ProxyController.getInstance().setProxyOverride(cfg,direct,done==null?()->{}:done);
        return true;
    }
    public static void clear(Runnable done){
        if(!proxyOverrideSupported()){if(done!=null)done.run();return;}
        ProxyController.getInstance().clearProxyOverride(Runnable::run,done==null?()->{}:done);
    }
}
