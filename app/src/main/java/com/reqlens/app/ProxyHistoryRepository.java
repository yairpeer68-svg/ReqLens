package com.reqlens.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class ProxyHistoryRepository {
    private static final int MAX = 2000;
    private static final AtomicLong IDS = new AtomicLong(1);
    private static final LinkedHashMap<Long, ProxyEvent> EVENTS = new LinkedHashMap<>();
    private static final Map<Long, Long> TCP_TO_EVENT = new LinkedHashMap<>();
    private ProxyHistoryRepository() { }

    public static synchronized long addTcp(long flowId, String host, String ip, int port, String appLabel, String appPackage) {
        ProxyEvent e = new ProxyEvent();
        e.id = IDS.getAndIncrement(); e.timestamp = System.currentTimeMillis(); e.protocol = "TCP";
        e.host = safe(host); e.ip = safe(ip); e.port = port; e.appLabel = safe(appLabel); e.appPackage = safe(appPackage);
        put(e); TCP_TO_EVENT.put(flowId, e.id); return e.id;
    }

    public static synchronized void updateTls(long flowId, TlsClientHelloInspector.Result tls) {
        Long eventId = TCP_TO_EVENT.get(flowId); if (eventId == null) return;
        ProxyEvent e = EVENTS.get(eventId); if (e == null) return;
        if (tls.clientHello) {
            e.protocol = "TLS"; e.tlsSni = safe(tls.sni); e.tlsAlpn = safe(tls.alpn); e.tlsVersion = safe(tls.tlsVersion);
            if (!e.tlsSni.isEmpty()) e.host = e.tlsSni;
        }
    }

    public static synchronized void addBytes(long flowId, long bytes) {
        Long eventId = TCP_TO_EVENT.get(flowId); if (eventId == null) return;
        ProxyEvent e = EVENTS.get(eventId); if (e != null) e.bytes += Math.max(0, bytes);
    }

    public static synchronized void addUdp(String host, String ip, int port, long bytes, String protocol, String appLabel, String appPackage) {
        ProxyEvent e = new ProxyEvent(); e.id = IDS.getAndIncrement(); e.timestamp = System.currentTimeMillis();
        e.protocol = safe(protocol); e.host = safe(host); e.ip = safe(ip); e.port = port; e.bytes = Math.max(0, bytes);
        e.appLabel = safe(appLabel); e.appPackage = safe(appPackage); put(e);
    }

    public static synchronized void addHttp(RequestRecord r, String source) {
        ProxyEvent e = new ProxyEvent(); e.id = IDS.getAndIncrement(); e.timestamp = r.timestamp == 0 ? System.currentTimeMillis() : r.timestamp;
        e.source = safe(source); e.protocol = "HTTP"; e.method = safe(r.method); e.url = safe(r.url); e.status = r.statusCode; e.bytes = r.responseBody == null ? 0 : r.responseBody.length();
        try { java.net.URL u = new java.net.URL(r.url); e.host = u.getHost(); e.port = u.getPort() > 0 ? u.getPort() : u.getDefaultPort(); } catch (Exception ignored) { }
        put(e);
        WebSocketHistoryRepository.maybeRecord(r);
    }

    public static synchronized List<ProxyEvent> snapshotNewestFirst() {
        ArrayList<ProxyEvent> out = new ArrayList<>(EVENTS.values());
        out.sort(Comparator.comparingLong((ProxyEvent e) -> e.timestamp).reversed()); return out;
    }

    public static synchronized void clear() { EVENTS.clear(); TCP_TO_EVENT.clear(); }
    private static void put(ProxyEvent e) { EVENTS.put(e.id, e); while (EVENTS.size() > MAX) EVENTS.remove(EVENTS.keySet().iterator().next()); }
    private static String safe(String s) { return s == null ? "" : s; }
}
