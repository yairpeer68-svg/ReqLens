package com.reqlens.app;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CurlBuilder {
    private CurlBuilder() {}

    public static String shellQuote(String value) {
        if (value == null) return "''";
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    public static boolean isSensitiveHeader(String name) {
        if (name == null) return false;
        String n = name.trim().toLowerCase(Locale.ROOT);
        return n.equals("authorization") || n.equals("proxy-authorization") ||
                n.equals("cookie") || n.equals("set-cookie") || n.equals("x-api-key") ||
                n.equals("api-key");
    }

    public static String build(String method, String url, Map<String, String> headers,
                               String body, boolean redactSecrets, boolean multiline) {
        StringBuilder sb = new StringBuilder();
        String sep = multiline ? " \\\n  " : " ";
        sb.append("curl").append(sep)
          .append("-X ").append(shellQuote(method == null ? "GET" : method.toUpperCase(Locale.ROOT))).append(sep)
          .append(shellQuote(url == null ? "" : url));

        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                String name = e.getKey();
                String value = e.getValue();
                if (redactSecrets && isSensitiveHeader(name)) value = "<REDACTED>";
                sb.append(sep).append("-H ")
                  .append(shellQuote(name + ": " + (value == null ? "" : value)));
            }
        }

        if (body != null && !body.isEmpty()) {
            sb.append(sep).append("--data-raw ").append(shellQuote(body));
        }
        return sb.toString();
    }

    public static Map<String, String> copyHeaders(Map<String, String> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
