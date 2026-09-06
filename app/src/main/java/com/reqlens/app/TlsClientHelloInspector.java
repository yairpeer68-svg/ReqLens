package com.reqlens.app;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class TlsClientHelloInspector {
    private TlsClientHelloInspector() {}

    public static final class Result {
        public boolean clientHello;
        public String sni = "";
        public String alpn = "";
        public String tlsVersion = "";
    }

    public static Result inspect(byte[] b, int n) {
        Result out = new Result();
        if (b == null || n < 5 || n > b.length) return out;
        try {
            int o = 0;
            if ((b[o] & 0xff) != 22) return out;
            int recordLen = u16(b, o + 3);
            if (recordLen < 4 || o + 5 + recordLen > n) return out;
            int end = o + 5 + recordLen;
            int p = o + 5;
            if ((b[p] & 0xff) != 1) return out;
            out.clientHello = true;
            p += 4;
            if (p + 34 > end) return out;
            out.tlsVersion = tlsName(u16(b, p));
            p += 34;
            if (p >= end) return out;
            int sessionLen = b[p++] & 0xff;
            p += sessionLen;
            if (p + 2 > end) return out;
            int cipherLen = u16(b, p);
            p += 2 + cipherLen;
            if (p + 1 > end) return out;
            int compLen = b[p++] & 0xff;
            p += compLen;
            if (p + 2 > end) return out;
            int extLen = u16(b, p);
            p += 2;
            int extEnd = Math.min(end, p + extLen);
            while (p + 4 <= extEnd) {
                int type = u16(b, p);
                int len = u16(b, p + 2);
                p += 4;
                if (len < 0 || p + len > extEnd) return out;
                if (type == 0) out.sni = parseSni(b, p, len);
                else if (type == 16) out.alpn = parseAlpn(b, p, len);
                else if (type == 43) {
                    String v = parseVersions(b, p, len);
                    if (!v.isEmpty()) out.tlsVersion = v;
                }
                p += len;
            }
        } catch (RuntimeException ignored) { }
        return out;
    }

    private static String parseSni(byte[] b, int p, int len) {
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

    private static String parseAlpn(byte[] b, int p, int len) {
        if (len < 3) return "";
        int total = u16(b, p), q = p + 2, end = Math.min(p + 2 + total, p + len);
        List<String> values = new ArrayList<>();
        while (q < end && values.size() < 8) {
            int l = b[q++] & 0xff;
            if (l == 0 || q + l > end) break;
            values.add(new String(b, q, l, StandardCharsets.US_ASCII));
            q += l;
        }
        StringBuilder s = new StringBuilder();
        for (String value : values) { if (s.length() > 0) s.append(','); s.append(value); }
        return s.toString();
    }

    private static String parseVersions(byte[] b, int p, int len) {
        if (len < 3) return "";
        int count = b[p] & 0xff, q = p + 1, end = Math.min(p + len, q + count);
        String best = "";
        while (q + 1 < end) { String v = tlsName(u16(b, q)); if (!v.isEmpty()) best = v; q += 2; }
        return best;
    }

    private static String tlsName(int v) {
        if (v == 0x0301) return "TLS 1.0";
        if (v == 0x0302) return "TLS 1.1";
        if (v == 0x0303) return "TLS 1.2";
        if (v == 0x0304) return "TLS 1.3";
        return "";
    }
    private static int u16(byte[] b, int o) { return ((b[o] & 255) << 8) | (b[o + 1] & 255); }
}
