package com.reqlens.app;

import android.content.Context;
import android.os.Build;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;

public final class MitmProxyServer {
    private static final int MAX_HEADER = 65536;
    private static final int MAX_BODY = 8 * 1024 * 1024;
    private static MitmProxyServer INSTANCE;
    private final Context app;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private final MitmCaManager ca;
    private volatile ServerSocket server;
    private volatile boolean running;
    private volatile String lastError = "";

    private MitmProxyServer(Context c) { app = c.getApplicationContext(); ca = new MitmCaManager(app); }
    public static synchronized MitmProxyServer get(Context c) { if (INSTANCE == null) INSTANCE = new MitmProxyServer(c); return INSTANCE; }
    public synchronized int start() throws Exception {
        if (running && server != null) return server.getLocalPort();
        ca.ensureCa();
        server = new ServerSocket();
        server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
        running = true; lastError = "";
        workers.submit(this::acceptLoop);
        return server.getLocalPort();
    }
    public synchronized void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        server = null;
    }
    public boolean isRunning() { return running && server != null && !server.isClosed(); }
    public int port() { ServerSocket s = server; return s == null ? -1 : s.getLocalPort(); }
    public String lastError() { return lastError; }

    private void acceptLoop() {
        while (running) {
            try { Socket s = server.accept(); s.setSoTimeout(30000); workers.submit(() -> handle(s)); }
            catch (Exception e) { if (running) lastError = e.getClass().getSimpleName() + ": " + safe(e.getMessage()); }
        }
    }

    private void handle(Socket client) {
        try {
            BufferedInputStream in = new BufferedInputStream(client.getInputStream());
            OutputStream out = client.getOutputStream();
            HttpRequest req = readRequest(in);
            if (req == null) return;
            if ("CONNECT".equalsIgnoreCase(req.method)) handleConnect(client, in, out, req.target);
            else forwardPlain(client, out, req);
        } catch (Exception e) { lastError = e.getClass().getSimpleName() + ": " + safe(e.getMessage()); }
        finally { try { client.close(); } catch (Exception ignored) {} }
    }

    private void handleConnect(Socket raw, InputStream in, OutputStream out, String authority) throws Exception {
        HostPort hp = parseAuthority(authority, 443);
        out.write("HTTP/1.1 200 Connection Established\r\nProxy-Agent: ReqLens\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1)); out.flush();
        MitmCaManager.HostIdentity id = ca.identityFor(hp.host);
        char[] pass = "reqlens".toCharArray();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType()); ks.load(null);
        ks.setKeyEntry("leaf", id.key, pass, new Certificate[]{id.cert, ca.ensureCa()});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); kmf.init(ks, pass);
        SSLContext ctx = SSLContext.getInstance("TLS"); ctx.init(kmf.getKeyManagers(), null, new SecureRandom());
        SSLSocket tls = (SSLSocket) ctx.getSocketFactory().createSocket(raw, raw.getInetAddress().getHostAddress(), raw.getPort(), false);
        tls.setUseClientMode(false);
        if (Build.VERSION.SDK_INT >= 29) { SSLParameters p = tls.getSSLParameters(); p.setApplicationProtocols(new String[]{"http/1.1"}); tls.setSSLParameters(p); }
        tls.startHandshake();
        BufferedInputStream tin = new BufferedInputStream(tls.getInputStream()); OutputStream tout = tls.getOutputStream();
        HttpRequest inner = readRequest(tin);
        if (inner != null) {
            String path = inner.target.startsWith("http://") || inner.target.startsWith("https://") ? URI.create(inner.target).getRawPath() : inner.target;
            if (path == null || path.isEmpty()) path = "/";
            inner.target = "https://" + hp.host + (hp.port == 443 ? "" : ":" + hp.port) + path;
            forwardPlain(tls, tout, inner);
        }
        try { tls.close(); } catch (Exception ignored) {}
    }

    private void forwardPlain(Socket client, OutputStream browserOut, HttpRequest req) throws Exception {
        URL url = new URL(req.target);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(30000); c.setInstanceFollowRedirects(false); c.setUseCaches(false); c.setRequestMethod(req.method);
        for (Map.Entry<String,String> e : req.headers.entrySet()) {
            String k = e.getKey();
            if (eq(k,"Host") || eq(k,"Connection") || eq(k,"Proxy-Connection") || eq(k,"Content-Length") || eq(k,"Transfer-Encoding") || eq(k,"Accept-Encoding")) continue;
            c.setRequestProperty(k, e.getValue());
        }
        c.setRequestProperty("Accept-Encoding", "identity");
        if (req.body.length > 0 && !("GET".equals(req.method) || "HEAD".equals(req.method))) {
            c.setDoOutput(true); c.setFixedLengthStreamingMode(req.body.length); try(OutputStream os=c.getOutputStream()){os.write(req.body);}
        }
        long start = System.currentTimeMillis();
        int code = c.getResponseCode(); String msg = c.getResponseMessage();
        InputStream ris = code >= 400 ? c.getErrorStream() : c.getInputStream();
        byte[] body = ris == null ? new byte[0] : readAllLimited(ris, MAX_BODY);
        StringBuilder head = new StringBuilder("HTTP/1.1 ").append(code).append(' ').append(msg == null ? "" : msg).append("\r\n");
        LinkedHashMap<String,String> responseHeaders = new LinkedHashMap<>();
        for (Map.Entry<String,List<String>> e : c.getHeaderFields().entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            String k=e.getKey(); if (eq(k,"Transfer-Encoding") || eq(k,"Content-Length") || eq(k,"Connection") || eq(k,"Content-Encoding")) continue;
            responseHeaders.put(k, String.join(", ", e.getValue()));
            for (String v : e.getValue()) head.append(k).append(": ").append(v).append("\r\n");
        }
        head.append("Content-Length: ").append(body.length).append("\r\nConnection: close\r\n\r\n");
        browserOut.write(head.toString().getBytes(StandardCharsets.ISO_8859_1)); browserOut.write(body); browserOut.flush();

        RequestRecord r = new RequestRecord(); r.timestamp=System.currentTimeMillis(); r.method=req.method; r.url=req.target; r.requestHeaders.putAll(req.headers);
        r.requestBody = textBody(req.body, req.headers.get("Content-Type")); r.statusCode=code; r.statusText=msg==null?"":msg; r.responseHeaders.putAll(responseHeaders);
        r.responseBody=textBody(body, c.getContentType()); r.durationMs=System.currentTimeMillis()-start; MitmHistoryRepository.add(r); c.disconnect();
    }

    private static HttpRequest readRequest(InputStream in) throws Exception {
        byte[] headerBytes = readUntilHeaderEnd(in); if (headerBytes == null || headerBytes.length == 0) return null;
        String text = new String(headerBytes, StandardCharsets.ISO_8859_1); String[] lines = text.split("\\r?\\n"); if (lines.length == 0) return null;
        String[] first = lines[0].split(" ",3); if (first.length < 2) throw new IOException("Malformed HTTP request line");
        HttpRequest r=new HttpRequest(); r.method=first[0].toUpperCase(Locale.ROOT); r.target=first[1];
        int len=0; boolean chunked=false;
        for(int i=1;i<lines.length;i++){String line=lines[i];int p=line.indexOf(':');if(p<=0)continue;String k=line.substring(0,p).trim();String v=line.substring(p+1).trim();r.headers.put(k,v);if(eq(k,"Content-Length"))try{len=Integer.parseInt(v);}catch(Exception ignored){}if(eq(k,"Transfer-Encoding")&&v.toLowerCase(Locale.ROOT).contains("chunked"))chunked=true;}
        if (chunked) r.body=readChunked(in); else if(len>0) r.body=readFixed(in,Math.min(len,MAX_BODY));
        return r;
    }
    private static byte[] readUntilHeaderEnd(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();int state=0;while(b.size()<MAX_HEADER){int x=in.read();if(x<0)break;b.write(x);state=(state==0&&x=='\r')?1:(state==1&&x=='\n')?2:(state==2&&x=='\r')?3:(state==3&&x=='\n')?4:0;if(state==4)break;}return b.toByteArray();}
    private static byte[] readFixed(InputStream in,int n)throws Exception{byte[] b=new byte[n];int p=0;while(p<n){int x=in.read(b,p,n-p);if(x<0)break;p+=x;}return p==n?b:Arrays.copyOf(b,p);}
    private static byte[] readChunked(InputStream in)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.ISO_8859_1));while(out.size()<MAX_BODY){String line=r.readLine();if(line==null)break;int semi=line.indexOf(';');String hex=(semi>=0?line.substring(0,semi):line).trim();int n=Integer.parseInt(hex,16);if(n==0){r.readLine();break;}char[] ch=new char[n];int p=0;while(p<n){int x=r.read(ch,p,n-p);if(x<0)break;p+=x;}out.write(new String(ch,0,p).getBytes(StandardCharsets.ISO_8859_1));r.readLine();}return out.toByteArray();}
    private static byte[] readAllLimited(InputStream in,int max)throws Exception{try(InputStream x=in;ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[]b=new byte[8192];int n,total=0;while((n=x.read(b))>=0){int w=Math.min(n,max-total);if(w>0)o.write(b,0,w);total+=w;if(total>=max)break;}return o.toByteArray();}}
    private static String textBody(byte[] body,String type){if(body==null||body.length==0)return"";String t=type==null?"":type.toLowerCase(Locale.ROOT);if(t.contains("text")||t.contains("json")||t.contains("xml")||t.contains("javascript")||t.contains("x-www-form-urlencoded"))return new String(body,StandardCharsets.UTF_8);return "[binary body: "+body.length+" bytes]";}
    private static HostPort parseAuthority(String s,int def){String h=s;int p=def;int c=s.lastIndexOf(':');if(c>0&&s.indexOf(']')<c)try{p=Integer.parseInt(s.substring(c+1));h=s.substring(0,c);}catch(Exception ignored){}if(h.startsWith("[")&&h.endsWith("]"))h=h.substring(1,h.length()-1);return new HostPort(h,p);}
    private static boolean eq(String a,String b){return a!=null&&a.equalsIgnoreCase(b);} private static String safe(String s){return s==null?"unknown":s;}
    private static final class HttpRequest{String method,target;byte[]body=new byte[0];LinkedHashMap<String,String>headers=new LinkedHashMap<>();}
    private static final class HostPort{final String host;final int port;HostPort(String h,int p){host=h;port=p;}}
}
