package com.reqlens.app;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class RequestExecutor {
    private RequestExecutor() {}
    public static RequestRecord execute(RequestRecord input, boolean redirects) {
        RequestRecord r = cloneRequest(input); long start = System.currentTimeMillis(); HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(r.url).openConnection(); c.setRequestMethod(r.method);
            c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setInstanceFollowRedirects(redirects); c.setUseCaches(false);
            c.setRequestProperty("User-Agent", "ReqLens/0.7 Android");
            for (Map.Entry<String,String> e : r.requestHeaders.entrySet()) c.setRequestProperty(e.getKey(), e.getValue());
            boolean bodyAllowed = !("GET".equals(r.method) || "HEAD".equals(r.method));
            if (bodyAllowed && r.requestBody != null && !r.requestBody.isEmpty()) {
                byte[] b = r.requestBody.getBytes(StandardCharsets.UTF_8); c.setDoOutput(true); c.setFixedLengthStreamingMode(b.length);
                try(OutputStream os=c.getOutputStream()){ os.write(b); }
            }
            r.statusCode=c.getResponseCode(); r.statusText=c.getResponseMessage()==null?"":c.getResponseMessage();
            for(Map.Entry<String,List<String>> e:c.getHeaderFields().entrySet()) if(e.getKey()!=null&&e.getValue()!=null) r.responseHeaders.put(e.getKey(),String.join(", ",e.getValue()));
            InputStream raw=r.statusCode>=400?c.getErrorStream():c.getInputStream(); r.responseBody=raw==null?"":readLimited(raw,1_000_000);
        } catch(Exception e){ r.error=e.getClass().getSimpleName()+": "+(e.getMessage()==null?"Request failed":e.getMessage()); }
        finally { r.durationMs=System.currentTimeMillis()-start; if(c!=null)c.disconnect(); }
        ProxyHistoryRepository.addHttp(r, "request-executor");
        return r;
    }
    public static RequestRecord cloneRequest(RequestRecord in){ RequestRecord r=new RequestRecord(); r.timestamp=System.currentTimeMillis(); r.method=in.method; r.url=in.url; r.requestHeaders.putAll(in.requestHeaders); r.requestBody=in.requestBody; return r; }
    private static String readLimited(InputStream input,int limit)throws Exception{ try(BufferedInputStream in=new BufferedInputStream(input);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[]buf=new byte[8192];int total=0,n;while((n=in.read(buf))>=0){int w=Math.min(n,limit-total);if(w>0)out.write(buf,0,w);total+=w;if(total>=limit)break;}String s=out.toString(StandardCharsets.UTF_8.name());return total>=limit?s+"\n\n[Response truncated]":s;}}
}
