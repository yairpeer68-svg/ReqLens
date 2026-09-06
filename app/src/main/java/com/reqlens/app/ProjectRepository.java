package com.reqlens.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class ProjectRepository {
    public static final class Project { public final long id; public String name; public long createdAt; Project(long i,String n){id=i;name=n;createdAt=System.currentTimeMillis();} @Override public String toString(){return name;} }
    private static final ArrayList<Project> ITEMS=new ArrayList<>(); private static long next=2; private static long active=1; private static Context app;
    static { ITEMS.add(new Project(1,"Default project")); }
    private ProjectRepository(){}
    public static synchronized void init(Context c){if(app!=null)return;app=c.getApplicationContext();load();}
    public static synchronized List<Project> snapshot(){return new ArrayList<>(ITEMS);} public static synchronized Project active(){for(Project p:ITEMS)if(p.id==active)return p;return ITEMS.get(0);}
    public static synchronized Project add(String name){Project p=new Project(next++,name);ITEMS.add(p);active=p.id;save();return p;} public static synchronized void activate(long id){for(Project p:ITEMS)if(p.id==id){active=id;save();return;}}
    private static void load(){try{SharedPreferences p=app.getSharedPreferences("reqlens_projects",Context.MODE_PRIVATE);String raw=p.getString("projects","");if(raw.isEmpty())return;JSONArray a=new JSONArray(raw);ITEMS.clear();long max=0;for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Project x=new Project(o.getLong("id"),o.optString("name","Project"));x.createdAt=o.optLong("createdAt",System.currentTimeMillis());ITEMS.add(x);max=Math.max(max,x.id);}if(ITEMS.isEmpty())ITEMS.add(new Project(1,"Default project"));next=max+1;active=p.getLong("active",ITEMS.get(0).id);}catch(Exception ignored){if(ITEMS.isEmpty())ITEMS.add(new Project(1,"Default project"));}}
    private static void save(){if(app==null)return;try{JSONArray a=new JSONArray();for(Project x:ITEMS){JSONObject o=new JSONObject();o.put("id",x.id);o.put("name",x.name);o.put("createdAt",x.createdAt);a.put(o);}app.getSharedPreferences("reqlens_projects",Context.MODE_PRIVATE).edit().putString("projects",a.toString()).putLong("active",active).apply();}catch(Exception ignored){}}
}
