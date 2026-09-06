package com.reqlens.app;

public final class ProxyEvent {
    public long id;
    public long timestamp;
    public String source = "capture";
    public String protocol = "";
    public String host = "";
    public String ip = "";
    public int port = -1;
    public String method = "";
    public String url = "";
    public int status = -1;
    public String appLabel = "";
    public String appPackage = "";
    public String tlsSni = "";
    public String tlsAlpn = "";
    public String tlsVersion = "";
    public long bytes;
    public String note = "";

    @Override public String toString() {
        String left = !method.isEmpty() ? method : protocol;
        String target = !url.isEmpty() ? url : (!host.isEmpty() ? host : ip) + (port > 0 ? ":" + port : "");
        String app = appLabel.isEmpty() ? "" : " • " + appLabel;
        return left + "  " + target + app;
    }
}
