package com.reqlens.app;
import java.util.Locale;
public final class RewriteRule {
    public boolean enabled=true; public String hostContains=""; public String method=""; public String headerName=""; public String headerValue="";
    public boolean matches(RequestRecord r){if(!enabled)return false; if(!method.isEmpty()&&!method.equalsIgnoreCase(r.method))return false; if(!hostContains.isEmpty()&&!r.url.toLowerCase(Locale.ROOT).contains(hostContains.toLowerCase(Locale.ROOT)))return false; return true;}
    public void apply(RequestRecord r){if(matches(r)&&!headerName.trim().isEmpty())r.requestHeaders.put(headerName.trim(),headerValue);}
}
