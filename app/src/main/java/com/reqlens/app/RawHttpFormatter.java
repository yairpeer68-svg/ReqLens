package com.reqlens.app;

import java.net.URL;
import java.util.Map;

public final class RawHttpFormatter {
    private RawHttpFormatter() {}
    public static String request(RequestRecord r) {
        StringBuilder s=new StringBuilder(); String path="/"; String host="";
        try { URL u=new URL(r.url); path=u.getFile().isEmpty()?"/":u.getFile(); host=u.getHost(); } catch(Exception ignored) {}
        s.append(r.method).append(' ').append(path).append(" HTTP/1.1\n");
        boolean hasHost=false; for(Map.Entry<String,String>e:r.requestHeaders.entrySet()){if(e.getKey().equalsIgnoreCase("Host"))hasHost=true;s.append(e.getKey()).append(": ").append(e.getValue()).append('\n');}
        if(!hasHost&&!host.isEmpty())s.append("Host: ").append(host).append('\n');
        s.append('\n').append(r.requestBody==null?"":r.requestBody); return s.toString();
    }
    public static String response(RequestRecord r) {
        StringBuilder s=new StringBuilder(); s.append("HTTP/1.1 ").append(r.statusCode).append(' ').append(r.statusText==null?"":r.statusText).append('\n');
        for(Map.Entry<String,String>e:r.responseHeaders.entrySet())s.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        s.append('\n').append(r.responseBody==null?"":r.responseBody); if(r.error!=null&&!r.error.isEmpty())s.append("\n\nERROR: ").append(r.error); return s.toString();
    }
}
