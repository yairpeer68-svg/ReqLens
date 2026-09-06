package com.reqlens.app;
import java.util.*;
public final class InterceptAuditRepository{
 public static final class Entry{public long timestamp;public String action="",method="",url="";@Override public String toString(){return action+" • "+method+" "+url;}}
 private static final ArrayList<Entry>ITEMS=new ArrayList<>();private InterceptAuditRepository(){}
 public static synchronized void add(String action,RequestRecord r){Entry e=new Entry();e.timestamp=System.currentTimeMillis();e.action=action;e.method=r.method;e.url=r.url;ITEMS.add(e);while(ITEMS.size()>500)ITEMS.remove(0);}
 public static synchronized List<Entry>snapshot(){return new ArrayList<>(ITEMS);}public static synchronized void clear(){ITEMS.clear();}
}
