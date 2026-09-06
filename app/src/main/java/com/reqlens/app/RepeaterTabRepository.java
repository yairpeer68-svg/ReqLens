package com.reqlens.app;
import java.util.*;import java.util.concurrent.atomic.AtomicLong;
public final class RepeaterTabRepository{
 public static final class Tab{public long id;public String name="Request";public RequestRecord request,baseline,last;public final ArrayList<RequestRecord>history=new ArrayList<>();@Override public String toString(){return name;}}
 private static final AtomicLong IDS=new AtomicLong(1);private static final ArrayList<Tab>TABS=new ArrayList<>();private RepeaterTabRepository(){}
 public static synchronized Tab open(RequestRecord r){Tab t=new Tab();t.id=IDS.getAndIncrement();t.request=RequestExecutor.cloneRequest(r==null?new RequestRecord():r);t.baseline=RequestExecutor.cloneRequest(t.request);t.name=name(t.request,t.id);TABS.add(t);while(TABS.size()>12)TABS.remove(0);return t;}
 public static synchronized Tab ensure(){if(TABS.isEmpty())return open(new RequestRecord());return TABS.get(TABS.size()-1);}public static synchronized List<Tab>snapshot(){return new ArrayList<>(TABS);}public static synchronized void close(long id){TABS.removeIf(t->t.id==id);if(TABS.isEmpty())open(new RequestRecord());}
 public static synchronized Tab duplicate(Tab src){return open(src==null?new RequestRecord():src.request);}public static synchronized void rename(Tab t){if(t!=null)t.name=name(t.request,t.id);}private static String name(RequestRecord r,long id){String u=r==null?"":r.url;if(u==null||u.isEmpty())return "Tab "+id;try{java.net.URL x=new java.net.URL(u);String p=x.getPath();return (r.method==null?"GET":r.method)+" "+x.getHost()+(p==null||p.isEmpty()?"":p);}catch(Exception e){return "Tab "+id;}}
}
