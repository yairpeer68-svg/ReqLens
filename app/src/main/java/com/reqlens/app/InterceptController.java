package com.reqlens.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class InterceptController {
    public static final class Item { public long id; public long createdAt; public RequestRecord request; @Override public String toString(){return request.method+"  "+request.url;} }
    private static final AtomicLong IDS=new AtomicLong(1); private static final ArrayList<Item> QUEUE=new ArrayList<>();
    private static volatile boolean enabled;
    private InterceptController(){}
    public static boolean isEnabled(){return enabled;} public static void setEnabled(boolean v){enabled=v;}
    public static synchronized Item enqueue(RequestRecord r){Item i=new Item();i.id=IDS.getAndIncrement();i.createdAt=System.currentTimeMillis();i.request=RequestExecutor.cloneRequest(r);QUEUE.add(i);while(QUEUE.size()>100)QUEUE.remove(0);return i;}
    public static synchronized List<Item> snapshot(){return new ArrayList<>(QUEUE);} public static synchronized void remove(long id){QUEUE.removeIf(i->i.id==id);} public static synchronized void clear(){QUEUE.clear();}
}
