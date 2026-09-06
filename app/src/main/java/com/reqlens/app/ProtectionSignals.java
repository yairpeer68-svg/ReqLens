package com.reqlens.app;

import java.util.ArrayList;
import java.util.List;

public final class ProtectionSignals {
    private ProtectionSignals() {}

    public static List<String> infer(FlowRecord f) {
        ArrayList<String> out = new ArrayList<>();
        if (f == null) return out;
        if (f.quic) out.add("QUIC/HTTP3 in use: an HTTP/1 proxy may not see request details.");
        if ((f.destinationPort == 443 || f.sourcePort == 443) && blank(f.tlsSni))
            out.add("TLS without visible SNI: hostname may be hidden, unavailable, or encrypted by newer TLS mechanisms.");
        if (!blank(f.tlsAlpn) && f.tlsAlpn.contains("h2"))
            out.add("HTTP/2 negotiated: request parsing requires an HTTP/2-aware authorized debug path.");
        if (f.uid < 0) out.add("App owner unresolved: Android did not provide a connection owner for this flow.");
        return out;
    }

    private static boolean blank(String s) { return s == null || s.isEmpty(); }
}
