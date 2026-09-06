package com.reqlens.app;

import android.content.Context;import android.content.SharedPreferences;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;
public final class RequestAnnotationRepository {
 public static final class Annotation{public boolean pinned;public String tags="",note="";}
 private static Context app;private RequestAnnotationRepository(){}
 public static synchronized void init(Context c){if(app==null)app=c.getApplicationContext();}
 public static String key(String method,String url){try{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[]b=d.digest(((method==null?"":method)+"\n"+(url==null?"":url)).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}catch(Exception e){return String.valueOf((method+url).hashCode());}}
 public static Annotation get(String method,String url){Annotation a=new Annotation();if(app==null)return a;SharedPreferences p=app.getSharedPreferences("reqlens_annotations",Context.MODE_PRIVATE);String k=key(method,url);a.pinned=p.getBoolean(k+".pin",false);a.tags=p.getString(k+".tags","");a.note=p.getString(k+".note","");return a;}
 public static void save(String method,String url,boolean pin,String tags,String note){if(app==null)return;String k=key(method,url);app.getSharedPreferences("reqlens_annotations",Context.MODE_PRIVATE).edit().putBoolean(k+".pin",pin).putString(k+".tags",tags==null?"":tags).putString(k+".note",note==null?"":note).apply();}
}
