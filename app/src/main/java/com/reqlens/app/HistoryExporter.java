package com.reqlens.app;
import org.json.*;import java.util.*;
public final class HistoryExporter{
 private HistoryExporter(){}
 public static String browserJson(List<BrowserRequest>x,boolean redact){JSONArray a=new JSONArray();for(BrowserRequest r:x){JSONObject o=r.toJson();if(redact){JSONObject h=o.optJSONObject("headers");if(h!=null)redactHeaders(h);}a.put(o);}return a.toString();}
 public static String proxyJson(List<ProxyEvent>x){JSONArray a=new JSONArray();for(ProxyEvent e:x){JSONObject o=new JSONObject();try{o.put("timestamp",e.timestamp);o.put("source",e.source);o.put("protocol",e.protocol);o.put("host",e.host);o.put("ip",e.ip);o.put("port",e.port);o.put("method",e.method);o.put("url",e.url);o.put("status",e.status);o.put("appLabel",e.appLabel);o.put("appPackage",e.appPackage);o.put("tlsSni",e.tlsSni);o.put("tlsAlpn",e.tlsAlpn);o.put("tlsVersion",e.tlsVersion);o.put("bytes",e.bytes);o.put("note",e.note);}catch(JSONException ex){throw new IllegalStateException(ex);}a.put(o);}return a.toString();}
 public static String browserCsv(List<BrowserRequest>x){StringBuilder s=new StringBuilder("timestamp,method,status,url,mime,mainFrame,error\n");for(BrowserRequest r:x)s.append(r.timestamp).append(',').append(csv(r.method)).append(',').append(r.statusCode).append(',').append(csv(r.url)).append(',').append(csv(r.mimeType)).append(',').append(r.mainFrame).append(',').append(csv(r.error)).append('\n');return s.toString();}
 private static void redactHeaders(JSONObject h){Iterator<String>it=h.keys();ArrayList<String>keys=new ArrayList<>();while(it.hasNext())keys.add(it.next());for(String k:keys){String l=k.toLowerCase(Locale.ROOT);if(l.contains("authorization")||l.contains("cookie")||l.contains("token")||l.contains("api-key"))try{h.put(k,"[REDACTED]");}catch(JSONException ignored){}}}
 private static String csv(String x){if(x==null)x="";return '"'+x.replace("\"","\"\"")+'"';}
}
