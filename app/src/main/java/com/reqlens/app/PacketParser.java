package com.reqlens.app;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PacketParser {
    private PacketParser() {}

    public static final class ParsedPacket {
        public int ipVersion;
        public int protocol;
        public String sourceIp = "";
        public String destinationIp = "";
        public int sourcePort = -1;
        public int destinationPort = -1;
        public String dnsName = "";
        public String tlsSni = "";
        public String tlsAlpn = "";
        public String tlsVersion = "";
        public boolean quic;
        public int payloadOffset = -1;
    }

    public static ParsedPacket parse(byte[] data, int length) {
        if (data == null || length < 1 || length > data.length) return null;
        int version = (data[0] >>> 4) & 0x0f;
        if (version == 4) return parseIpv4(data, length);
        if (version == 6) return parseIpv6(data, length);
        return null;
    }

    private static ParsedPacket parseIpv4(byte[] data, int length) {
        if (length < 20) return null;
        int ihl = (data[0] & 0x0f) * 4;
        if (ihl < 20 || length < ihl) return null;
        ParsedPacket p = new ParsedPacket();
        p.ipVersion = 4; p.protocol = data[9] & 0xff;
        p.sourceIp = ipv4(data, 12); p.destinationIp = ipv4(data, 16);
        parseTransport(data, length, ihl, p);
        return p;
    }

    private static ParsedPacket parseIpv6(byte[] data, int length) {
        if (length < 40) return null;
        ParsedPacket p = new ParsedPacket();
        p.ipVersion = 6; p.protocol = data[6] & 0xff;
        p.sourceIp = ipv6(data, 8); p.destinationIp = ipv6(data, 24);
        int offset = 40;
        int next = p.protocol;
        int guard = 0;
        while (guard++ < 8 && isIpv6Extension(next)) {
            if (offset + 2 > length) return p;
            int following = data[offset] & 0xff;
            int extLen;
            if (next == 44) extLen = 8;
            else if (next == 51) extLen = ((data[offset + 1] & 0xff) + 2) * 4;
            else extLen = ((data[offset + 1] & 0xff) + 1) * 8;
            if (extLen <= 0 || offset + extLen > length) return p;
            offset += extLen; next = following;
        }
        p.protocol = next;
        parseTransport(data, length, offset, p);
        return p;
    }

    private static boolean isIpv6Extension(int next) {
        return next == 0 || next == 43 || next == 44 || next == 50 || next == 51 || next == 60;
    }

    private static void parseTransport(byte[] data, int length, int offset, ParsedPacket p) {
        if ((p.protocol == 6 || p.protocol == 17) && length >= offset + 4) {
            p.sourcePort = u16(data, offset); p.destinationPort = u16(data, offset + 2);
        }
        if (p.protocol == 17 && length >= offset + 8) {
            p.payloadOffset = offset + 8;
            if (p.sourcePort == 53 || p.destinationPort == 53) p.dnsName = parseDnsQuestion(data, p.payloadOffset, length);
            p.quic = looksLikeQuic(data, p.payloadOffset, length, p.sourcePort, p.destinationPort);
        } else if (p.protocol == 6 && length >= offset + 20) {
            int tcpHeader = ((data[offset + 12] >>> 4) & 0x0f) * 4;
            if (tcpHeader < 20 || offset + tcpHeader > length) return;
            p.payloadOffset = offset + tcpHeader;
            if (p.payloadOffset < length && (p.sourcePort == 443 || p.destinationPort == 443 || p.sourcePort == 8443 || p.destinationPort == 8443)) {
                parseTlsClientHello(data, p.payloadOffset, length, p);
            }
        }
    }

    private static String parseDnsQuestion(byte[] b, int o, int n) {
        if (o + 12 > n || u16(b, o + 4) < 1) return "";
        int pos = o + 12; StringBuilder name = new StringBuilder(); int labels = 0;
        while (pos < n && labels++ < 64) {
            int len = b[pos++] & 0xff;
            if (len == 0) return name.toString();
            if ((len & 0xc0) != 0 || len > 63 || pos + len > n) return "";
            if (name.length() > 0) name.append('.');
            name.append(new String(b, pos, len, StandardCharsets.US_ASCII)); pos += len;
        }
        return "";
    }

    private static void parseTlsClientHello(byte[] b, int o, int n, ParsedPacket out) {
        try {
            if (o + 5 > n || (b[o] & 0xff) != 22) return;
            int recordLen = u16(b, o + 3); int end = Math.min(n, o + 5 + recordLen); int p = o + 5;
            if (p + 4 > end || (b[p] & 0xff) != 1) return;
            p += 4; if (p + 34 > end) return;
            int legacyVersion = u16(b, p); out.tlsVersion = tlsName(legacyVersion); p += 34;
            int sessionLen = b[p++] & 0xff; p += sessionLen;
            if (p + 2 > end) return; int cipherLen = u16(b, p); p += 2 + cipherLen;
            if (p + 1 > end) return; int compLen = b[p++] & 0xff; p += compLen;
            if (p + 2 > end) return; int extLen = u16(b, p); p += 2; int extEnd = Math.min(end, p + extLen);
            while (p + 4 <= extEnd) {
                int type = u16(b, p), len = u16(b, p + 2); p += 4;
                if (p + len > extEnd) return;
                if (type == 0) out.tlsSni = parseSniExtension(b, p, len);
                else if (type == 16) out.tlsAlpn = parseAlpnExtension(b, p, len);
                else if (type == 43) {
                    String v = parseSupportedVersions(b, p, len);
                    if (!v.isEmpty()) out.tlsVersion = v;
                }
                p += len;
            }
        } catch (RuntimeException ignored) { }
    }

    private static String parseSniExtension(byte[] b, int p, int len) {
        if (len < 5) return "";
        int listLen = u16(b, p), q = p + 2, end = Math.min(p + 2 + listLen, p + len);
        while (q + 3 <= end) {
            int type = b[q++] & 0xff, nameLen = u16(b, q); q += 2;
            if (q + nameLen > end) return "";
            if (type == 0) return new String(b, q, nameLen, StandardCharsets.US_ASCII);
            q += nameLen;
        }
        return "";
    }

    private static String parseAlpnExtension(byte[] b, int p, int len) {
        if (len < 3) return "";
        int total = u16(b, p), q = p + 2, end = Math.min(p + 2 + total, p + len);
        List<String> values = new ArrayList<>();
        while (q < end && values.size() < 8) {
            int l = b[q++] & 0xff; if (l == 0 || q + l > end) break;
            values.add(new String(b, q, l, StandardCharsets.US_ASCII)); q += l;
        }
        return join(values, ",");
    }

    private static String parseSupportedVersions(byte[] b, int p, int len) {
        if (len < 3) return "";
        int count = b[p] & 0xff; int q = p + 1, end = Math.min(p + len, q + count);
        String best = "";
        while (q + 1 < end) { String v = tlsName(u16(b, q)); if (!v.isEmpty()) best = v; q += 2; }
        return best;
    }

    private static boolean looksLikeQuic(byte[] b, int o, int n, int sp, int dp) {
        if (!(sp == 443 || dp == 443 || sp == 784 || dp == 784 || sp == 8853 || dp == 8853)) return false;
        if (o >= n) return false;
        int first = b[o] & 0xff;
        return (first & 0x40) != 0;
    }

    private static String tlsName(int v) {
        if (v == 0x0301) return "TLS 1.0";
        if (v == 0x0302) return "TLS 1.1";
        if (v == 0x0303) return "TLS 1.2";
        if (v == 0x0304) return "TLS 1.3";
        return "";
    }

    public static String protocolName(int protocol, boolean quic) {
        if (quic) return "QUIC/UDP";
        if (protocol == 6) return "TCP";
        if (protocol == 17) return "UDP";
        if (protocol == 1) return "ICMP";
        if (protocol == 58) return "ICMPv6";
        return "IP(" + protocol + ")";
    }

    private static int u16(byte[] b, int o) { return ((b[o] & 0xff) << 8) | (b[o + 1] & 0xff); }
    private static String ipv4(byte[] b, int o) { return (b[o]&255)+"."+(b[o+1]&255)+"."+(b[o+2]&255)+"."+(b[o+3]&255); }
    private static String ipv6(byte[] b, int o) {
        try { byte[] a = new byte[16]; System.arraycopy(b, o, a, 0, 16); return InetAddress.getByAddress(a).getHostAddress(); }
        catch (Exception e) { return ""; }
    }
    private static String join(List<String> values, String sep) {
        StringBuilder s = new StringBuilder();
        for (String v : values) { if (s.length() > 0) s.append(sep); s.append(v); }
        return s.toString();
    }
}
