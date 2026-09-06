package com.reqlens.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WebSocketHistoryRepository {
    public static final class Event { public long timestamp; public String url=""; public String note=""; @Override public String toString(){return url + " • " + note;} }
    private static final ArrayList<Event> EVENTS = new ArrayList<>();
    private WebSocketHistoryRepository() { }
    public static synchronized void maybeRecord(RequestRecord r) {
        String up = header(r.requestHeaders, "Upgrade"); String conn = header(r.requestHeaders, "Connection");
        if ("websocket".equalsIgnoreCase(up) || (r.statusCode == 101 && "upgrade".equalsIgnoreCase(conn))) {
            Event e = new Event(); e.timestamp=System.currentTimeMillis(); e.url=r.url; e.note="Handshake " + r.statusCode; EVENTS.add(e); trim();
        }
    }
    public static synchronized List<Event> snapshot(){return new ArrayList<>(EVENTS);} public static synchronized void clear(){EVENTS.clear();}
    private static void trim(){while(EVENTS.size()>500)EVENTS.remove(0);} private static String header(java.util.Map<String,String>m,String k){for(java.util.Map.Entry<String,String>e:m.entrySet())if(k.equalsIgnoreCase(e.getKey()))return e.getValue();return"";}
}
