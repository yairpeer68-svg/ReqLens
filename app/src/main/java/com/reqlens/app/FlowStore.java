package com.reqlens.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FlowStore {
    private static final int MAX_FLOWS = 5000;
    private final LinkedHashMap<String, FlowRecord> flows = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized FlowRecord observe(FlowRecord sample) {
        String key = sample.key();
        FlowRecord current = flows.get(key);
        if (current == null) {
            sample.id = nextId++;
            sample.packets = Math.max(1, sample.packets);
            flows.put(key, sample);
            trim();
            return sample;
        }
        current.lastSeen = Math.max(current.lastSeen, sample.lastSeen);
        current.packets += Math.max(1, sample.packets);
        current.bytes += sample.bytes;
        if (current.host.isEmpty()) current.host = sample.host;
        if (current.tlsSni.isEmpty()) current.tlsSni = sample.tlsSni;
        if (current.tlsAlpn.isEmpty()) current.tlsAlpn = sample.tlsAlpn;
        if (current.tlsVersion.isEmpty()) current.tlsVersion = sample.tlsVersion;
        current.quic |= sample.quic;
        if (current.appPackage.isEmpty()) current.appPackage = sample.appPackage;
        if (current.appLabel.isEmpty()) current.appLabel = sample.appLabel;
        return current;
    }

    public synchronized List<FlowRecord> snapshotNewestFirst() {
        ArrayList<FlowRecord> out = new ArrayList<>(flows.values());
        java.util.Collections.reverse(out);
        return out;
    }

    public synchronized void clear() { flows.clear(); }
    public synchronized int size() { return flows.size(); }

    private void trim() {
        while (flows.size() > MAX_FLOWS) {
            Map.Entry<String, FlowRecord> first = flows.entrySet().iterator().next();
            flows.remove(first.getKey());
        }
    }
}
