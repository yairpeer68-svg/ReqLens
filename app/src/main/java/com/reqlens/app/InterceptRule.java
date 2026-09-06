package com.reqlens.app;

import java.util.Locale;

public final class InterceptRule {
    public boolean enabled = true;
    public String hostContains = "";
    public String method = "";
    public String action = "SET_HEADER";
    public String name = "";
    public String value = "";

    public boolean matches(RequestRecord r) {
        if (!enabled) return false;
        if (!method.isEmpty() && !method.equalsIgnoreCase(r.method)) return false;
        return hostContains.isEmpty() || r.url.toLowerCase(Locale.ROOT).contains(hostContains.toLowerCase(Locale.ROOT));
    }

    public void apply(RequestRecord r) {
        if (!matches(r)) return;
        if ("SET_HEADER".equals(action) && !name.trim().isEmpty()) r.requestHeaders.put(name.trim(), value);
        else if ("REMOVE_HEADER".equals(action) && !name.trim().isEmpty()) removeHeader(r, name.trim());
        else if ("REPLACE_URL".equals(action) && !name.isEmpty()) r.url = r.url.replace(name, value);
        else if ("REPLACE_BODY".equals(action) && !name.isEmpty()) r.requestBody = r.requestBody.replace(name, value);
    }

    private static void removeHeader(RequestRecord r, String name) {
        String hit = null; for (String k : r.requestHeaders.keySet()) if (name.equalsIgnoreCase(k)) { hit = k; break; }
        if (hit != null) r.requestHeaders.remove(hit);
    }

    @Override public String toString(){return (enabled?"✓ ":"○ ")+action+" • "+(method.isEmpty()?"ANY":method)+" • "+(hostContains.isEmpty()?"all hosts":hostContains);}
}
