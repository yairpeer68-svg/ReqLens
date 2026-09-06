package com.reqlens.app;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SessionStats {
    public int flows;
    public long bytes;
    public long packets;
    public int apps;
    public int dnsFlows;
    public int tlsFlows;
    public int quicFlows;

    public static SessionStats from(List<FlowRecord> records) {
        SessionStats s = new SessionStats();
        Set<String> appSet = new HashSet<>();
        if (records == null) return s;
        for (FlowRecord r : records) {
            if (r == null) continue;
            s.flows++;
            s.bytes += Math.max(0, r.bytes);
            s.packets += Math.max(0, r.packets);
            if (r.appPackage != null && !r.appPackage.isEmpty()) appSet.add(r.appPackage);
            if (r.destinationPort == 53 || r.sourcePort == 53) s.dnsFlows++;
            if (r.destinationPort == 443 || r.sourcePort == 443) s.tlsFlows++;
            if (r.quic) s.quicFlows++;
        }
        s.apps = appSet.size();
        return s;
    }
}
