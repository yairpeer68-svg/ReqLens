package com.reqlens.app;

import android.content.Context;
import org.json.*;
import java.security.MessageDigest;
import java.util.*;

public final class EvidenceRepository {
 public static final class Item{public long ts;public int featureId;public String title="",body="",hash="";public String toString(){return title+" • "+new java.text.SimpleDateFormat("HH:mm:ss",Locale.ROOT).format(new Date(ts));}}
 private static final ArrayList<Item> ITEMS=new ArrayList<>();private static Context app;private EvidenceRepository(){}
 public static synchronized void init(Context c){if(app!=null)return;app=c.getApplicationContext();load();}
 public static synchronized Item add(int id,String title,String body){Item x=new Item();x.ts=System.currentTimeMillis();x.featureId=id;x.title=title;x.body=body;x.hash=sha256(body);ITEMS.add(x);while(ITEMS.size()>500)ITEMS.remove(0);save();return x;}
 public static synchronized List<Item> snapshotNewest(){ArrayList<Item> out=new ArrayList<>(ITEMS);Collections.reverse(out);return out;}
 public static synchronized int size(){return ITEMS.size();}
 public static synchronized void clear(){ITEMS.clear();save();}
 private static String sha256(String s){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte v:b)x.append(String.format(Locale.ROOT,"%02x",v));return x.toString();}catch(Exception e){return "";}}
 private static void save(){if(app==null)return;try{JSONArray a=new JSONArray();for(Item x:ITEMS){JSONObject o=new JSONObject();o.put("ts",x.ts);o.put("featureId",x.featureId);o.put("title",x.title);o.put("body",x.body);o.put("hash",x.hash);a.put(o);}app.getSharedPreferences("reqlens_evidence",0).edit().putString("items",a.toString()).apply();}catch(Exception ignored){}}
 private static void load(){try{String raw=app.getSharedPreferences("reqlens_evidence",0).getString("items","");if(raw.isEmpty())return;JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Item x=new Item();x.ts=o.optLong("ts");x.featureId=o.optInt("featureId");x.title=o.optString("title");x.body=o.optString("body");x.hash=o.optString("hash");ITEMS.add(x);}}catch(Exception ignored){}}
}
