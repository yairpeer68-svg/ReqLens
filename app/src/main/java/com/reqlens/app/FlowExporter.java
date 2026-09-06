package com.reqlens.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public final class FlowExporter {
    private FlowExporter() {}

    public static String toJson(List<FlowRecord> records, boolean redact) {
        JSONArray arr = new JSONArray();
        if (records != null) for (FlowRecord r : records) {
            if (r == null) continue;
            JSONObject o = new JSONObject();
            try {
                o.put("id", r.id); o.put("firstSeen", r.firstSeen); o.put("lastSeen", r.lastSeen);
                o.put("app", safe(r.appLabel)); o.put("package", safe(r.appPackage)); o.put("uid", r.uid);
                o.put("ipVersion", r.ipVersion); o.put("protocol", safe(r.protocolName));
                o.put("source", endpoint(r.sourceIp, r.sourcePort)); o.put("destination", endpoint(r.destinationIp, r.destinationPort));
                o.put("host", safe(r.host)); o.put("tlsSni", safe(r.tlsSni)); o.put("tlsAlpn", safe(r.tlsAlpn));
                o.put("tlsVersion", safe(r.tlsVersion)); o.put("quic", r.quic); o.put("packets", r.packets); o.put("bytes", r.bytes);
                JSONArray signals = new JSONArray(); for (String x : ProtectionSignals.infer(r)) signals.put(x); o.put("signals", signals);
                arr.put(o);
            } catch (Exception ignored) { }
        }
        JSONObject root = new JSONObject();
        try {
            root.put("format", "reqlens-flow-export-v1");
            root.put("redacted", redact);
            root.put("exportedAt", System.currentTimeMillis());
            root.put("flows", arr);
        } catch (Exception ignored) { }
        String text;
        try { text = root.toString(2); }
        catch (Exception e) { text = root.toString(); }
        return redact ? SecretRedactor.redact(text) : text;
    }

    public static String toCsv(List<FlowRecord> records) {
        StringBuilder b = new StringBuilder("id,firstSeen,lastSeen,app,package,uid,protocol,source,destination,host,tlsSni,tlsAlpn,tlsVersion,quic,packets,bytes\n");
        if (records != null) for (FlowRecord r : records) {
            if (r == null) continue;
            b.append(r.id).append(',').append(r.firstSeen).append(',').append(r.lastSeen).append(',')
                    .append(csv(r.appLabel)).append(',').append(csv(r.appPackage)).append(',').append(r.uid).append(',')
                    .append(csv(r.protocolName)).append(',').append(csv(endpoint(r.sourceIp, r.sourcePort))).append(',')
                    .append(csv(endpoint(r.destinationIp, r.destinationPort))).append(',').append(csv(r.host)).append(',')
                    .append(csv(r.tlsSni)).append(',').append(csv(r.tlsAlpn)).append(',').append(csv(r.tlsVersion)).append(',')
                    .append(r.quic).append(',').append(r.packets).append(',').append(r.bytes).append('\n');
        }
        return b.toString();
    }

    private static String endpoint(String ip, int port) { return safe(ip) + ":" + port; }
    private static String safe(String s) { return s == null ? "" : s; }
    private static String csv(String s) { return "\"" + safe(s).replace("\"", "\"\"") + "\""; }
}
