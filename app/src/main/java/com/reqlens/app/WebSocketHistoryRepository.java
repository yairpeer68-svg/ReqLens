package com.reqlens.app;
import java.util.*;
public final class WebSocketHistoryRepository {
 public static final class Event{public long timestamp;public String url="",note="",requestHeaders="",responseHeaders="";public int status=-1;@Override public String toString(){return url+" • "+note;}}
 private static final ArrayList<Event>EVENTS=new ArrayList<>();private WebSocketHistoryRepository(){}
 public static synchronized void maybeRecord(RequestRecord r){String up=header(r.requestHeaders,"Upgrade"),conn=header(r.requestHeaders,"Connection"),rup=header(r.responseHeaders,"Upgrade");if("websocket".equalsIgnoreCase(up)||"websocket".equalsIgnoreCase(rup)||(r.statusCode==101&&conn.toLowerCase(Locale.ROOT).contains("upgrade"))){Event e=new Event();e.timestamp=System.currentTimeMillis();e.url=r.url;e.status=r.statusCode;e.note="Handshake "+r.statusCode;e.requestHeaders=render(r.requestHeaders);e.responseHeaders=render(r.responseHeaders);EVENTS.add(e);trim();}}
 public static synchronized List<Event>snapshot(){return new ArrayList<>(EVENTS);}public static synchronized void clear(){EVENTS.clear();}
 private static void trim(){while(EVENTS.size()>500)EVENTS.remove(0);}private static String header(Map<String,String>m,String k){for(Map.Entry<String,String>e:m.entrySet())if(k.equalsIgnoreCase(e.getKey()))return e.getValue();return"";}private static String render(Map<String,String>m){StringBuilder s=new StringBuilder();for(Map.Entry<String,String>e:m.entrySet())s.append(e.getKey()).append(": ").append(e.getValue()).append('\n');return s.toString();}
}
