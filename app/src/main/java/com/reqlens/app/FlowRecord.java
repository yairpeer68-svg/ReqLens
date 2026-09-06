package com.reqlens.app;

import org.json.JSONObject;

public final class FlowRecord {
    public long id;
    public String flowTag = "";
    public long firstSeen;
    public long lastSeen;
    public int uid = -1;
    public String appPackage = "";
    public String appLabel = "";
    public int ipVersion;
    public int protocol;
    public String protocolName = "";
    public String sourceIp = "";
    public int sourcePort = -1;
    public String destinationIp = "";
    public int destinationPort = -1;
    public String host = "";
    public String tlsSni = "";
    public String tlsAlpn = "";
    public String tlsVersion = "";
    public boolean quic;
    public long packets;
    public long bytes;

    public String key() {
        if (flowTag != null && !flowTag.isEmpty()) return flowTag;
        return uid + "|" + protocol + "|" + sourceIp + ":" + sourcePort + "|" + destinationIp + ":" + destinationPort;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("flowTag", flowTag); o.put("firstSeen", firstSeen); o.put("lastSeen", lastSeen);
        o.put("uid", uid); o.put("appPackage", appPackage); o.put("appLabel", appLabel);
        o.put("ipVersion", ipVersion); o.put("protocol", protocol); o.put("protocolName", protocolName);
        o.put("sourceIp", sourceIp); o.put("sourcePort", sourcePort);
        o.put("destinationIp", destinationIp); o.put("destinationPort", destinationPort);
        o.put("host", host); o.put("tlsSni", tlsSni); o.put("tlsAlpn", tlsAlpn); o.put("tlsVersion", tlsVersion);
        o.put("quic", quic); o.put("packets", packets); o.put("bytes", bytes);
        return o;
    }
}
