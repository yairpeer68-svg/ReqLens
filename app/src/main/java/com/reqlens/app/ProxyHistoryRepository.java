package com.reqlens.app;
import android.content.*;import org.json.*;import java.util.*;import java.util.concurrent.atomic.AtomicLong;
public final class ProxyHistoryRepository {
 private static final int MAX=2000;private static final AtomicLong IDS=new AtomicLong(1);private static final LinkedHashMap<Long,ProxyEvent>EVENTS=new LinkedHashMap<>();private static final Map<Long,Long>TCP_TO_EVENT=new LinkedHashMap<>();private static Context app;private ProxyHistoryRepository(){}
 public static synchronized void init(Context c){if(app!=null)return;app=c.getApplicationContext();load();}
 private static long project(){try{return ProjectRepository.active().id;}catch(Exception e){return 1;}}
 public static synchronized long addTcp(long flowId,String host,String ip,int port,String appLabel,String appPackage){ProxyEvent e=base("capture","TCP");e.host=safe(host);e.ip=safe(ip);e.port=port;e.appLabel=safe(appLabel);e.appPackage=safe(appPackage);put(e);TCP_TO_EVENT.put(flowId,e.id);save();return e.id;}
 public static synchronized void updateTls(long flowId,TlsClientHelloInspector.Result tls){Long eventId=TCP_TO_EVENT.get(flowId);if(eventId==null)return;ProxyEvent e=EVENTS.get(eventId);if(e==null)return;if(tls.clientHello){e.protocol="TLS";e.tlsSni=safe(tls.sni);e.tlsAlpn=safe(tls.alpn);e.tlsVersion=safe(tls.tlsVersion);if(!e.tlsSni.isEmpty())e.host=e.tlsSni;save();}}
 public static synchronized void addBytes(long flowId,long bytes){Long eventId=TCP_TO_EVENT.get(flowId);if(eventId==null)return;ProxyEvent e=EVENTS.get(eventId);if(e!=null)e.bytes+=Math.max(0,bytes);}
 public static synchronized void addUdp(String host,String ip,int port,long bytes,String protocol,String appLabel,String appPackage){ProxyEvent e=base("capture",safe(protocol));e.host=safe(host);e.ip=safe(ip);e.port=port;e.bytes=Math.max(0,bytes);e.appLabel=safe(appLabel);e.appPackage=safe(appPackage);put(e);save();}
 public static synchronized void addHttp(RequestRecord r,String source){ProxyEvent e=base(safe(source),"HTTP");e.timestamp=r.timestamp==0?System.currentTimeMillis():r.timestamp;e.method=safe(r.method);e.url=safe(r.url);e.status=r.statusCode;e.bytes=r.responseBody==null?0:r.responseBody.length();try{java.net.URL u=new java.net.URL(r.url);e.host=u.getHost();e.port=u.getPort()>0?u.getPort():u.getDefaultPort();}catch(Exception ignored){}put(e);save();WebSocketHistoryRepository.maybeRecord(r);}
 public static synchronized List<ProxyEvent> snapshotAll(){return new ArrayList<>(EVENTS.values());}
 public static synchronized List<ProxyEvent> snapshotNewestFirst(){purge();ArrayList<ProxyEvent>out=new ArrayList<>(EVENTS.values());out.sort(Comparator.comparingLong((ProxyEvent e)->e.timestamp).reversed());return out;}
 public static synchronized List<ProxyEvent> snapshotActiveProject(){long p=project();ArrayList<ProxyEvent>out=new ArrayList<>();for(ProxyEvent e:snapshotNewestFirst())if(e.projectId==p||e.projectId==0)out.add(e);return out;}

 public static synchronized int importItems(JSONArray a,long projectId){int n=0;if(a==null)return 0;for(int i=0;i<a.length()&&EVENTS.size()<MAX;i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;ProxyEvent e=ProxyEvent.fromJson(o);e.id=IDS.getAndIncrement();e.projectId=projectId;EVENTS.put(e.id,e);n++;}save();return n;}
 public static synchronized void clear(){EVENTS.clear();TCP_TO_EVENT.clear();save();}
 private static ProxyEvent base(String source,String protocol){ProxyEvent e=new ProxyEvent();e.id=IDS.getAndIncrement();e.timestamp=System.currentTimeMillis();e.projectId=project();e.source=source;e.protocol=protocol;return e;}
 private static void put(ProxyEvent e){EVENTS.put(e.id,e);while(EVENTS.size()>MAX)EVENTS.remove(EVENTS.keySet().iterator().next());}
 private static String safe(String s){return s==null?"":s;}
 private static void purge(){if(app==null)return;long cutoff=System.currentTimeMillis()-SuiteSettings.retentionDays(app)*86400000L;boolean c=EVENTS.values().removeIf(e->e.timestamp>0&&e.timestamp<cutoff);if(c)save();}
 private static void load(){try{String raw=app.getSharedPreferences("reqlens_proxy_history",Context.MODE_PRIVATE).getString("items","");if(raw.isEmpty())return;JSONArray a=new JSONArray(raw);EVENTS.clear();long max=0;for(int i=0;i<a.length();i++){ProxyEvent e=ProxyEvent.fromJson(a.getJSONObject(i));EVENTS.put(e.id,e);max=Math.max(max,e.id);}IDS.set(max+1);purge();}catch(Exception ignored){}}
 private static void save(){if(app==null)return;try{JSONArray a=new JSONArray();for(ProxyEvent e:EVENTS.values())a.put(e.toJson());app.getSharedPreferences("reqlens_proxy_history",Context.MODE_PRIVATE).edit().putString("items",a.toString()).apply();}catch(Exception ignored){}}
}
