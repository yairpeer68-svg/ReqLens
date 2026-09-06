package com.reqlens.app;

import java.nio.charset.StandardCharsets;

public final class DnsInspector {
    private DnsInspector() {}
    public static String questionName(byte[] b, int n) {
        if (b == null || n < 13 || n > b.length || u16(b, 4) < 1) return "";
        int p = 12, labels = 0;
        StringBuilder name = new StringBuilder();
        while (p < n && labels++ < 64) {
            int len = b[p++] & 255;
            if (len == 0) return name.toString();
            if ((len & 0xc0) != 0 || len > 63 || p + len > n) return "";
            if (name.length() > 0) name.append('.');
            name.append(new String(b, p, len, StandardCharsets.US_ASCII));
            p += len;
        }
        return "";
    }
    private static int u16(byte[] b, int o) { return ((b[o] & 255) << 8) | (b[o + 1] & 255); }
}
