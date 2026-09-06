package com.reqlens.app;

import android.content.*;import org.json.*;import java.util.*;
public final class BrowserHistoryRepository {
 private static final int MAX=1500;private static final ArrayList<BrowserRequest>ITEMS=new ArrayList<>();private static Context app;private BrowserHistoryRepository(){}
 public static synchronized void init(Context c){if(app!=null)return;app=c.getApplicationContext();load();}
 public static synchronized void add(BrowserRequest item){if(item.projectId==0){try{item.projectId=ProjectRepository.active().id;}catch(Exception ignored){item.projectId=1;}}ITEMS.add(item);trim();save();ProxyHistoryRepository.addHttp(item.toRequestRecord(),"browser");}
 public static synchronized List<BrowserRequest> snapshotNewestFirst(){purgeExpired();ArrayList<BrowserRequest>out=new ArrayList<>(ITEMS);Collections.reverse(out);return out;}
 public static synchronized List<BrowserRequest> snapshotActiveProject(){long id=1;try{id=ProjectRepository.active().id;}catch(Exception ignored){}ArrayList<BrowserRequest>out=new ArrayList<>();for(BrowserRequest x:snapshotNewestFirst())if(x.projectId==id||x.projectId==0)out.add(x);return out;}
 public static synchronized int size(){return ITEMS.size();}public static synchronized void clear(){ITEMS.clear();save();}
 private static void trim(){while(ITEMS.size()>MAX)ITEMS.remove(0);purgeExpired();}
 private static void purgeExpired(){if(app==null)return;long cutoff=System.currentTimeMillis()-SuiteSettings.retentionDays(app)*86400000L;boolean changed=ITEMS.removeIf(x->x.timestamp>0&&x.timestamp<cutoff);if(changed)save();}
 private static void load(){try{String raw=app.getSharedPreferences("reqlens_browser_history",Context.MODE_PRIVATE).getString("items","");if(raw.isEmpty())return;JSONArray a=new JSONArray(raw);ITEMS.clear();for(int i=0;i<a.length();i++)ITEMS.add(BrowserRequest.fromJson(a.getJSONObject(i)));trim();}catch(Exception ignored){}}
 private static void save(){if(app==null)return;try{JSONArray a=new JSONArray();for(BrowserRequest x:ITEMS)a.put(x.toJson());app.getSharedPreferences("reqlens_browser_history",Context.MODE_PRIVATE).edit().putString("items",a.toString()).apply();}catch(Exception ignored){}}
}
