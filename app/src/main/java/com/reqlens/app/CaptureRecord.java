package com.reqlens.app;

public final class CaptureRecord {
    public long timestamp;
    public String appPackage = "";
    public int uid = -1;
    public String protocol = "";
    public String source = "";
    public String destination = "";
    public String host = "";
    public String tlsSni = "";
    public int bytes;
}
