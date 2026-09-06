package com.reqlens.app;

public final class ProtocolClassifier {
    private ProtocolClassifier() {}

    public static String classify(PacketParser.ParsedPacket p) {
        if (p == null) return "UNKNOWN";
        if (p.quic) return "QUIC/HTTP3 candidate";
        if (p.destinationPort == 53 || p.sourcePort == 53) return "DNS";
        if (p.destinationPort == 853 || p.sourcePort == 853) return "DNS-over-TLS";
        if (p.destinationPort == 443 || p.sourcePort == 443) {
            if (!empty(p.tlsAlpn)) {
                if (p.tlsAlpn.contains("h2")) return "TLS/HTTP2";
                if (p.tlsAlpn.contains("http/1.1")) return "TLS/HTTP1.1";
            }
            return "TLS/HTTPS";
        }
        if (p.destinationPort == 80 || p.sourcePort == 80) return "HTTP candidate";
        if (p.destinationPort == 22 || p.sourcePort == 22) return "SSH";
        if (p.destinationPort == 5228 || p.sourcePort == 5228) return "Push/FCM candidate";
        return PacketParser.protocolName(p.protocol, false);
    }

    private static boolean empty(String s) { return s == null || s.isEmpty(); }
}
