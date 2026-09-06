package com.reqlens.app;
import java.util.*;import java.util.concurrent.atomic.AtomicLong;
public final class InterceptController {
 public static final class Item { public long id,createdAt; public RequestRecord request; @Override public String toString(){return request.method+"  "+request.url;} }
 private static final AtomicLong IDS=new AtomicLong(1);private static final ArrayList<Item>QUEUE=new ArrayList<>();private static volatile boolean enabled;private static volatile String hostFilter="",methodFilter="";
 private InterceptController(){}
 public static boolean isEnabled(){return enabled;} public static void setEnabled(boolean v){enabled=v;}
 public static void setFilters(String host,String method){hostFilter=host==null?"":host.trim().toLowerCase(Locale.ROOT);methodFilter=method==null?"":method.trim().toUpperCase(Locale.ROOT);}
 public static String filters(){return "host="+(hostFilter.isEmpty()?"ANY":hostFilter)+" • method="+(methodFilter.isEmpty()?"ANY":methodFilter);}
 public static boolean shouldIntercept(RequestRecord r){if(!enabled)return false;if(!methodFilter.isEmpty()&&!methodFilter.equalsIgnoreCase(r.method))return false;return hostFilter.isEmpty()||r.url.toLowerCase(Locale.ROOT).contains(hostFilter);}
 public static synchronized Item enqueue(RequestRecord r){Item i=new Item();i.id=IDS.getAndIncrement();i.createdAt=System.currentTimeMillis();i.request=RequestExecutor.cloneRequest(r);QUEUE.add(i);while(QUEUE.size()>100)QUEUE.remove(0);return i;}
 public static synchronized List<Item> snapshot(){return new ArrayList<>(QUEUE);} public static synchronized void remove(long id){QUEUE.removeIf(i->i.id==id);} public static synchronized void clear(){QUEUE.clear();}
}
