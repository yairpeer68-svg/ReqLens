package com.reqlens.app;

import java.util.LinkedHashMap;

public final class BrowserRequest {
    public long timestamp;
    public String method = "GET";
    public String url = "";
    public int statusCode = -1;
    public String mimeType = "";
    public boolean mainFrame;
    public String error = "";
    public final LinkedHashMap<String, String> headers = new LinkedHashMap<>();

    public RequestRecord toRequestRecord() {
        RequestRecord r = new RequestRecord();
        r.timestamp = timestamp;
        r.method = method;
        r.url = url;
        r.statusCode = statusCode;
        r.requestHeaders.putAll(headers);
        r.error = error;
        return r;
    }

    @Override public String toString() {
        String code = statusCode >= 0 ? String.valueOf(statusCode) : "—";
        return method + "  " + code + "  " + url;
    }
}
