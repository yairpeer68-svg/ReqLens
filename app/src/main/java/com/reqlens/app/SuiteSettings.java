package com.reqlens.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class SuiteSettings {
    private static final String P="reqlens_suite_settings";
    private SuiteSettings(){}
    public static boolean javascript(Context c){return prefs(c).getBoolean("javascript",true);}
    public static void javascript(Context c,boolean v){prefs(c).edit().putBoolean("javascript",v).apply();}
    public static boolean desktopUa(Context c){return prefs(c).getBoolean("desktopUa",false);}
    public static void desktopUa(Context c,boolean v){prefs(c).edit().putBoolean("desktopUa",v).apply();}
    public static boolean incognito(Context c){return prefs(c).getBoolean("incognito",false);}
    public static void incognito(Context c,boolean v){prefs(c).edit().putBoolean("incognito",v).apply();}
    public static boolean redact(Context c){return prefs(c).getBoolean("redact",true);}
    public static void redact(Context c,boolean v){prefs(c).edit().putBoolean("redact",v).apply();}
    public static int retentionDays(Context c){return prefs(c).getInt("retentionDays",7);}
    public static void retentionDays(Context c,int v){prefs(c).edit().putInt("retentionDays",Math.max(1,Math.min(90,v))).apply();}
    private static SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences(P,Context.MODE_PRIVATE);}
}
