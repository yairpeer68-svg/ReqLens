package com.reqlens.app;

import java.util.Locale;
import java.util.regex.Pattern;

public final class SecretRedactor {
    private static final Pattern AUTH = Pattern.compile("(?im)^(authorization|proxy-authorization|cookie|set-cookie|x-api-key|api-key|x-auth-token)\\s*:\\s*.*$");
    private static final Pattern JSON_SECRET = Pattern.compile("(?i)(\\\"(?:password|passwd|token|access_token|refresh_token|secret|api[_-]?key)\\\"\\s*:\\s*\\\")([^\\\"]*)(\\\")");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/-]+=*");

    private SecretRedactor() {}

    public static String redact(String text) {
        if (text == null || text.isEmpty()) return "";
        String out = AUTH.matcher(text).replaceAll("$1: <redacted>");
        out = JSON_SECRET.matcher(out).replaceAll("$1<redacted>$3");
        out = BEARER.matcher(out).replaceAll("Bearer <redacted>");
        return out;
    }

    public static boolean looksSensitiveHeader(String name) {
        if (name == null) return false;
        String n = name.trim().toLowerCase(Locale.US);
        return n.equals("authorization") || n.equals("proxy-authorization") || n.equals("cookie") ||
                n.equals("set-cookie") || n.equals("x-api-key") || n.equals("api-key") || n.equals("x-auth-token");
    }
}
