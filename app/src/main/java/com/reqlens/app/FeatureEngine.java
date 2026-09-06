package com.reqlens.app;

import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import org.json.*;
import java.net.URL;
import java.util.*;

public final class FeatureEngine {
 private FeatureEngine(){}
 public static String report(Context c,int id,String input){
  ProxyHistoryRepository.init(c); ProjectRepository.init(c); EvidenceRepository.init(c);
  List<ProxyEvent> events=ProxyHistoryRepository.snapshotActiveProject();
  List<RequestRecord> https=MitmHistoryRepository.snapshotNewestFirst();
  String pkg=c.getSharedPreferences("reqlens_capture",0).getString("selected_package","");
  StringBuilder o=new StringBuilder();
  o.append(FeatureCatalog.NAMES[id-1]).append("\n").append(FeatureCatalog.group(id)).append("\n\n");
  o.append("Target: ").append(pkg.isEmpty()?"none selected":pkg).append("\nCaptured events: ").append(events.size()).append("\nDecrypted HTTP(S): ").append(https.size()).append("\n\n");
  if(id<=10) protocolReport(o,id,events,https,input);
  else if(id<=20) appReport(c,o,id,pkg,input);
  else if(id<=30) apiReport(o,id,events,https,input);
  else if(id<=40) securityReport(o,id,https,input);
  else if(id<=50) automationReport(o,id,https,input);
  else if(id<=60) intelligenceReport(o,id,events,https,input);
  else if(id<=70) evidenceReport(o,id,events,https,input);
  else experienceReport(c,o,id,events,https,input);
  return o.toString();
 }
 private static void protocolReport(StringBuilder o,int id,List<ProxyEvent> e,List<RequestRecord> r,String input){
  Map<String,Integer> proto=countProto(e); Set<String> hosts=hosts(e);
  switch(id){
   case 1:o.append("HTTP/2 signals\n").append("ALPN h2: ").append(countContains(e,"tlsAlpn","h2")).append("\nHTTP events: ").append(proto.getOrDefault("HTTP",0));break;
   case 2:o.append("QUIC/HTTP3 signals\nQUIC events: ").append(proto.getOrDefault("QUIC/UDP",0)).append("\nUDP/443 candidates: ").append(countPort(e,443,"UDP"));break;
   case 3:o.append("gRPC candidates\n");for(RequestRecord x:r)if(headerContains(x.requestHeaders,"Content-Type","grpc"))o.append(shortReq(x)).append('\n');break;
   case 4:o.append("Protobuf decoder workspace\nProvide a .proto schema or paste a field map in the input box. Current binary responses: ").append(binaryCount(r));if(!blank(input))o.append("\nInput registered: ").append(trim(input,500));break;
   case 5:o.append("GraphQL operations observed\n");for(RequestRecord x:r)if(x.url.toLowerCase(Locale.ROOT).contains("graphql")||x.requestBody.contains("query")||x.requestBody.contains("mutation"))o.append(shortReq(x)).append('\n');break;
   case 6:o.append("WebSocket evidence\nUpgrade requests: ").append(countHeader(r,"Upgrade","websocket")).append("\nUse WebSocket History for frame-level records captured by ReqLens.");break;
   case 7:o.append("SSE candidates\ntext/event-stream responses: ").append(countResponseHeader(r,"Content-Type","text/event-stream"));break;
   case 8:o.append("HTTP/2 stream events are inferred from ALPN and request timing. Distinct hosts using h2: ").append(hostsWithAlpn(e,"h2").size());break;
   case 9:o.append("DNS intelligence\nDNS events: ").append(proto.getOrDefault("DNS/UDP",0)).append("\nDistinct hosts: ").append(hosts.size()).append("\nDoH/DoT candidates: ").append(dohCandidates(e));break;
   case 10:o.append("TLS overview\nTLS events: ").append(proto.getOrDefault("TLS",0)).append("\nVersions: ").append(countTlsVersions(e)).append("\nALPN: ").append(countAlpn(e));break;
  }
 }
 private static void appReport(Context c,StringBuilder o,int id,String pkg,String input){
  if(pkg.isEmpty()){o.append("Select a target app first.");return;} PackageManager pm=c.getPackageManager();
  try{PackageInfo pi=pm.getPackageInfo(pkg,PackageManager.GET_PERMISSIONS|PackageManager.GET_ACTIVITIES|PackageManager.GET_SERVICES|PackageManager.GET_RECEIVERS|PackageManager.GET_PROVIDERS);ApplicationInfo ai=pm.getApplicationInfo(pkg,PackageManager.GET_META_DATA);String label=String.valueOf(pm.getApplicationLabel(ai));
   switch(id){
    case 11:o.append("App: ").append(label).append("\nPackage: ").append(pkg).append("\nVersion: ").append(pi.versionName).append("\nTarget SDK: ").append(ai.targetSdkVersion).append("\nDebuggable: ").append((ai.flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0).append("\nPermissions: ").append(pi.requestedPermissions==null?0:pi.requestedPermissions.length);break;
    case 12:o.append("Installed APK path: ").append(ai.sourceDir).append("\nSplit APKs: ").append(ai.splitSourceDirs==null?0:ai.splitSourceDirs.length).append("\nStatic import can compare this installed package with a user-selected APK in a future file import step.");break;
    case 13:o.append("Declared components\nActivities: ").append(len(pi.activities)).append("\nServices: ").append(len(pi.services)).append("\nReceivers: ").append(len(pi.receivers)).append("\nProviders: ").append(len(pi.providers)).append("\nExported components are listed from Android package metadata where visible.\n");appendExported(o,pi.activities);appendExported(o,pi.services);appendExported(o,pi.receivers);appendExported(o,pi.providers);break;
    case 14:o.append("Deep-link mapper\nLaunch intent: ").append(pm.getLaunchIntentForPackage(pkg)).append("\nPaste known app links in the input box to add them to project notes.");if(!blank(input))o.append("\nCandidate: ").append(trim(input,500));break;
    case 15:o.append("App Links verification helper\nPackage: ").append(pkg).append("\nThis view inventories declared/observed hosts; verification still depends on the domain's assetlinks.json.");break;
    case 16:o.append("Network security profile\nTarget SDK: ").append(ai.targetSdkVersion).append("\nCleartext flag: ").append((ai.flags&ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC)!=0).append("\nUser-CA trust cannot be inferred reliably from package metadata alone.");break;
    case 17:o.append("Certificate inventory\nSigning info visibility depends on platform API. Package version: ").append(pi.versionName).append("\nObserved TLS peers are available in TLS Handshake Explorer.");break;
    case 18:o.append("SDK / tracker inventory\nPassive evidence is derived from contacted hostnames and package metadata. Capture traffic first for stronger results.");break;
    case 19:o.append("Requested permissions\n");if(pi.requestedPermissions!=null)for(String x:pi.requestedPermissions)o.append("• ").append(x).append(permissionRisk(x)).append('\n');break;
    case 20:o.append("Version comparison workspace\nCurrent: ").append(pi.versionName).append(" (code ").append(pi.getLongVersionCode()).append(")\nPaste/import a second package snapshot to compare permissions, endpoints and components.");break;
   }
  }catch(Exception ex){o.append("Package inspection failed: ").append(ex.getClass().getSimpleName()).append(": ").append(ex.getMessage());}
 }
 private static void apiReport(StringBuilder o,int id,List<ProxyEvent> e,List<RequestRecord> r,String input){
  Map<String,Set<String>> api=inventory(r,e);int endpoints=0;for(Set<String>s:api.values())endpoints+=s.size();
  switch(id){
   case 21:o.append("Hosts: ").append(api.size()).append("\nEndpoints: ").append(endpoints).append("\n").append(apiText(api,40));break;
   case 22:o.append(openApiDraft(api,r));break;
   case 23:o.append("OpenAPI validation workspace\nPaste an OpenAPI path/method list into input to compare with observed inventory.\nObserved endpoints: ").append(endpoints);if(!blank(input))o.append("\nInput length: ").append(input.length());break;
   case 24:o.append("Parameters discovered\n");appendParams(o,r,60);break;
   case 25:o.append("JSON schema inference\n");appendJsonShapes(o,r,30);break;
   case 26:o.append("API version groups\n");appendVersionGroups(o,r);break;
   case 27:o.append("Undocumented endpoint finder\nProvide documented paths in input; observed inventory contains ").append(endpoints).append(" endpoint(s).");if(!blank(input))o.append("\nPotentially undocumented: ").append(countUndocumented(api,input));break;
   case 28:o.append("Dependency graph\n");appendDependencyHints(o,r);break;
   case 29:o.append("Coverage map\nObserved endpoints: ").append(endpoints).append("\nProvide an OpenAPI/path list in input to compute exact coverage.");break;
   case 30:o.append("Change detection baseline\nCurrent fingerprint: ").append(simpleFingerprint(api,r)).append("\nSave this result to Evidence Vault and compare after the next session.");break;
  }
 }
 private static void securityReport(StringBuilder o,int id,List<RequestRecord> r,String input){
  switch(id){
   case 31:o.append("Test-case builder\nCaptured requests available: ").append(r.size()).append("\nCreate assertions from status, headers, body and latency. No destructive payloads are generated automatically.");break;
   case 32:o.append("Authorization matrix\nUse two or more test accounts you control and save equivalent requests. ReqLens can compare status/body differences without guessing credentials.\nCurrent decrypted requests: ").append(r.size());break;
   case 33:o.append("Object-access test assistant\nIdentifies numeric/UUID-like object parameters for authorized cross-account comparison.\nCandidates:\n");appendObjectIds(o,r,30);break;
   case 34:o.append("JWT inventory\n");appendJwt(o,r);if(!blank(input))o.append("\nPasted token decode:\n").append(decodeJwt(input));break;
   case 35:o.append("OAuth/OIDC signals\n");appendOauth(o,r);break;
   case 36:o.append("Session lifecycle signals\nLogin-like requests: ").append(countUrlWords(r,"login","signin","auth")).append("\nLogout-like: ").append(countUrlWords(r,"logout","signout")).append("\nRefresh-like: ").append(countUrlWords(r,"refresh","token"));break;
   case 37:o.append("CSRF signals\nRequests with CSRF-like headers/fields: ").append(csrfSignals(r));break;
   case 38:o.append("CORS response inventory\n");appendResponseHeaders(o,r,"Access-Control-Allow-Origin","Access-Control-Allow-Credentials");break;
   case 39:o.append("Security headers\n");appendSecurityHeaders(o,r);break;
   case 40:o.append("Rate-limit behavior lab\nPassive 429 responses: ").append(countStatus(r,429)).append("\nRetry-After seen: ").append(countResponseHeader(r,"Retry-After",null)).append("\nActive tests remain capped and should only target systems you are authorized to test.");break;
  }
 }
 private static void automationReport(StringBuilder o,int id,List<RequestRecord> r,String input){
  switch(id){
   case 41:o.append("Workflow builder\nAvailable primitives: request → extract → transform → assert → save evidence.\nCaptured requests: ").append(r.size());break;
   case 42:o.append("Environment vault\nValues are stored separately from exported evidence. Paste KEY=VALUE pairs in input to prepare an environment; secrets are never included in this report.");break;
   case 43:o.append("Dynamic extraction\nJSON/token candidates found: ").append(jsonCandidateCount(r)).append("\nSupports extracting response fields for later authorized requests.");break;
   case 44:o.append("Conditional branches\nConditions supported by the workbench design: status code, header presence, JSON field, body match and latency threshold.");break;
   case 45:o.append("Dataset runner\nUse a bounded data set for QA variations. Current safety default remains limited; no credential-stuffing automation.");break;
   case 46:o.append("Scheduled test runs\nProject test definitions can be persisted; Android background execution still depends on OS scheduling/battery constraints.");break;
   case 47:o.append("Regression suite\nCurrent captured baseline has ").append(r.size()).append(" HTTP record(s). Save representative records and compare status/schema/timing later.");break;
   case 48:o.append("Assertion library\nBuilt-in assertion concepts: status, header, JSON key/type, substring, maximum latency, minimum/maximum response size.");break;
   case 49:o.append("Mock API server\nDesign target: local loopback responses derived from saved records. No external listener is exposed by default.");break;
   case 50:o.append("Traffic replay lab\nReplay should target a staging/owned host, with secrets redacted or replaced. Current captured candidates: ").append(r.size());break;
  }
 }
 private static void intelligenceReport(StringBuilder o,int id,List<ProxyEvent> e,List<RequestRecord> r,String input){
  switch(id){
   case 51:o.append("Traffic analyst summary\n").append(summary(e,r));break;
   case 52:o.append("Anomalies\n").append(anomalies(e,r));break;
   case 53:o.append("Endpoint risk ranking\n");appendRisk(o,r,30);break;
   case 54:o.append("Sensitive data flow\n");appendSensitive(o,r);break;
   case 55:o.append("Third-party sharing\nDistinct contacted hosts: ").append(hosts(e).size()).append("\n");for(String h:hosts(e))o.append("• ").append(h).append('\n');break;
   case 56:o.append("Authentication flow graph\n");appendAuthGraph(o,r);break;
   case 57:o.append("Error clusters\n");appendErrorClusters(o,r);break;
   case 58:o.append("Root-cause assistant\n").append(rootCause(r));break;
   case 59:o.append("Natural-language traffic search\nExamples: 'POST 403', 'host api.example.com', 'slow', 'errors'.\n");if(blank(input))o.append("Enter a query above.");else appendSearch(o,e,r,input);break;
   case 60:o.append("Test plan generator\n").append(testPlan(r));break;
  }
 }
 private static void evidenceReport(StringBuilder o,int id,List<ProxyEvent> e,List<RequestRecord> r,String input){
  switch(id){
   case 61:o.append("Evidence Vault items: ").append(EvidenceRepository.size()).append("\nSaving a feature report records SHA-256, timestamp and source feature.");break;
   case 62:o.append("Finding management\nUse Save Finding from any research feature. Evidence items currently: ").append(EvidenceRepository.size()).append(". Add severity/status in project notes during triage.");break;
   case 63:o.append("Reproduction steps\n1. Select the same target app.\n2. Start an authorized session.\n3. Reproduce the action.\n4. Locate the request in History.\n5. Compare/save the relevant response.\nCurrent HTTP records: ").append(r.size());break;
   case 64:o.append("Professional report builder\nAvailable evidence: ").append(EvidenceRepository.size()).append(" item(s)\nTraffic records: ").append(e.size()).append("\nUse exported evidence as the factual basis; unverified claims are not auto-generated.");break;
   case 65:o.append("SARIF export preview\n{\"version\":\"2.1.0\",\"runs\":[{\"tool\":{\"driver\":{\"name\":\"ReqLens\"}},\"results\":[]}]}\nFindings saved: ").append(EvidenceRepository.size());break;
   case 66:o.append("HAR import/export workspace\nHTTP records available: ").append(r.size()).append(". HAR export should redact secrets by default.");break;
   case 67:o.append("Postman collection export\nCandidate requests: ").append(r.size()).append("\nCollection generation uses observed method/URL/header structure with secret redaction.");break;
   case 68:o.append("Redaction review\nSensitive header/body signals currently: ").append(sensitiveCount(r)).append("\nDefault exports should remain redacted until explicitly reviewed.");break;
   case 69:o.append("Encrypted team sharing\nProject bundles should be encrypted before transport. ReqLens does not silently upload projects.");break;
   case 70:o.append("Audit trail\nEvidence items: ").append(EvidenceRepository.size()).append("\nProject: ").append(ProjectRepository.active().name).append("\nTraffic history is timestamped and project-scoped.");break;
  }
 }
 private static void experienceReport(Context c,StringBuilder o,int id,List<ProxyEvent> e,List<RequestRecord> r,String input){
  switch(id){
   case 71:o.append("Design system\nMaterial theme active. Research Suite uses consistent cards, spacing, typography and search.");break;
   case 72:o.append("Workspace navigation\nTarget, Inspect, Research and Project surfaces are grouped from the home screen.");break;
   case 73:o.append("Tablet layout readiness\nThe research model supports list/detail separation; phone UI remains single-pane.");break;
   case 74:o.append("Command palette\nUse Research Suite search to jump to any of 80 capabilities by name/category.");break;
   case 75:o.append("Custom workspace\nFeature favorites/pinning can be layered on the catalog; project state is already persistent.");break;
   case 76:o.append("Session health\nTarget: ").append(c.getSharedPreferences("reqlens_capture",0).getString("selected_package","none")).append("\nTraffic events: ").append(e.size()).append("\nMITM records: ").append(r.size()).append("\nEvidence: ").append(EvidenceRepository.size());break;
   case 77:o.append("Diagnostic bundle\nIncludes app target, project, counts, protocol mix and last-known evidence hashes. Secrets should be redacted before sharing.\n").append(summary(e,r));break;
   case 78:o.append("Guided setup\n1. Select target app\n2. Verify VPN permission\n3. Optional CA setup for apps that trust user CAs\n4. Start session\n5. Generate traffic\n6. Inspect History/Research\n7. Save evidence");break;
   case 79:o.append("Offline tutorials\nModules: capture basics, TLS metadata, HTTPS trust, request inspection, API mapping, evidence workflow, safe authorized testing.");break;
   case 80:o.append("Plugin SDK design\nExtension points: passive decoders, analyzers, import/export formatters and report processors. Plugins should be signed, permission-scoped and isolated from secrets by default.");break;
  }
 }
 private static Map<String,Integer> countProto(List<ProxyEvent> e){Map<String,Integer>m=new TreeMap<>();for(ProxyEvent x:e)m.put(x.protocol,m.getOrDefault(x.protocol,0)+1);return m;}
 private static Set<String> hosts(List<ProxyEvent> e){Set<String>s=new TreeSet<>();for(ProxyEvent x:e)if(!blank(x.host))s.add(x.host);return s;}
 private static int countPort(List<ProxyEvent>e,int p,String proto){int n=0;for(ProxyEvent x:e)if(x.port==p&&(proto==null||x.protocol.contains(proto)))n++;return n;}
 private static int countContains(List<ProxyEvent>e,String f,String v){int n=0;for(ProxyEvent x:e){String z="tlsAlpn".equals(f)?x.tlsAlpn:"";if(z!=null&&z.contains(v))n++;}return n;}
 private static Set<String> hostsWithAlpn(List<ProxyEvent>e,String a){Set<String>s=new HashSet<>();for(ProxyEvent x:e)if(x.tlsAlpn.contains(a)&&!x.host.isEmpty())s.add(x.host);return s;}
 private static int dohCandidates(List<ProxyEvent>e){int n=0;for(ProxyEvent x:e)if(x.host.contains("dns")||x.port==853)n++;return n;}
 private static Map<String,Integer> countTlsVersions(List<ProxyEvent>e){Map<String,Integer>m=new TreeMap<>();for(ProxyEvent x:e)if(!blank(x.tlsVersion))m.put(x.tlsVersion,m.getOrDefault(x.tlsVersion,0)+1);return m;}
 private static Map<String,Integer> countAlpn(List<ProxyEvent>e){Map<String,Integer>m=new TreeMap<>();for(ProxyEvent x:e)if(!blank(x.tlsAlpn))m.put(x.tlsAlpn,m.getOrDefault(x.tlsAlpn,0)+1);return m;}
 private static int binaryCount(List<RequestRecord>r){int n=0;for(RequestRecord x:r)if(x.responseBody.startsWith("[binary body:"))n++;return n;}
 private static boolean headerContains(Map<String,String>h,String k,String v){for(Map.Entry<String,String>x:h.entrySet())if(x.getKey().equalsIgnoreCase(k)&&(v==null||x.getValue().toLowerCase(Locale.ROOT).contains(v.toLowerCase(Locale.ROOT))))return true;return false;}
 private static int countHeader(List<RequestRecord>r,String k,String v){int n=0;for(RequestRecord x:r)if(headerContains(x.requestHeaders,k,v))n++;return n;}
 private static int countResponseHeader(List<RequestRecord>r,String k,String v){int n=0;for(RequestRecord x:r)if(headerContains(x.responseHeaders,k,v))n++;return n;}
 private static String shortReq(RequestRecord r){return r.method+" "+trim(r.url,140)+" → "+r.statusCode;}
 private static int len(Object[]a){return a==null?0:a.length;}
 private static void appendExported(StringBuilder o,ComponentInfo[] a){if(a==null)return;for(ComponentInfo x:a)if(x.exported)o.append("• exported: ").append(x.name).append('\n');}
 private static String permissionRisk(String p){String x=p.toLowerCase(Locale.ROOT);return x.contains("location")||x.contains("camera")||x.contains("microphone")||x.contains("contacts")||x.contains("sms")||x.contains("phone")?"  [sensitive]":"";}
 private static Map<String,Set<String>> inventory(List<RequestRecord>r,List<ProxyEvent>e){Map<String,Set<String>>m=new TreeMap<>();for(RequestRecord x:r){try{URL u=new URL(x.url);m.computeIfAbsent(u.getHost(),k->new TreeSet<>()).add(x.method+" "+normPath(u.getPath()));}catch(Exception ignored){}}for(ProxyEvent x:e)if(!blank(x.url)){try{URL u=new URL(x.url);m.computeIfAbsent(u.getHost(),k->new TreeSet<>()).add((blank(x.method)?"?":x.method)+" "+normPath(u.getPath()));}catch(Exception ignored){}}return m;}
 private static String normPath(String p){if(p==null||p.isEmpty())return "/";return p.replaceAll("/[0-9]{2,}","/{id}").replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,}","/{uuid}");}
 private static String apiText(Map<String,Set<String>>m,int max){StringBuilder o=new StringBuilder();int n=0;for(Map.Entry<String,Set<String>>e:m.entrySet()){o.append(e.getKey()).append('\n');for(String p:e.getValue()){o.append("  ").append(p).append('\n');if(++n>=max)return o.toString();}}return o.toString();}
 private static String openApiDraft(Map<String,Set<String>>m,List<RequestRecord>r){StringBuilder o=new StringBuilder("openapi: 3.1.0\ninfo:\n  title: ReqLens Observed API\n  version: 0.0-observed\npaths:\n");Set<String>done=new HashSet<>();for(Set<String>s:m.values())for(String q:s){int sp=q.indexOf(' ');if(sp<0)continue;String method=q.substring(0,sp).toLowerCase(Locale.ROOT),path=q.substring(sp+1);String key=method+" "+path;if(!done.add(key))continue;o.append("  ").append(path).append(":\n    ").append(method).append(":\n      summary: Observed by ReqLens\n      responses:\n        '200':\n          description: Observed response placeholder\n");}return o.toString();}
 private static void appendParams(StringBuilder o,List<RequestRecord>r,int max){Set<String>s=new TreeSet<>();for(RequestRecord x:r)try{Uri u=Uri.parse(x.url);for(String k:u.getQueryParameterNames())s.add("query: "+k);if(x.requestBody.trim().startsWith("{")){JSONObject j=new JSONObject(x.requestBody);Iterator<String>it=j.keys();while(it.hasNext())s.add("json: "+it.next());}}catch(Exception ignored){}int n=0;for(String x:s){o.append("• ").append(x).append('\n');if(++n>=max)break;}}
 private static void appendJsonShapes(StringBuilder o,List<RequestRecord>r,int max){int n=0;for(RequestRecord x:r){String b=x.responseBody.trim();if(!(b.startsWith("{")||b.startsWith("[")))continue;try{Object j=b.startsWith("{")?new JSONObject(b):new JSONArray(b);o.append(trim(x.url,100)).append(" → ").append(shape(j)).append('\n');if(++n>=max)break;}catch(Exception ignored){}}if(n==0)o.append("No JSON responses available.");}
 private static String shape(Object j){if(j instanceof JSONObject){JSONObject o=(JSONObject)j;List<String>k=new ArrayList<>();Iterator<String>it=o.keys();while(it.hasNext()&&k.size()<20){String x=it.next();Object v=o.opt(x);k.add(x+":"+(v==null?"null":v.getClass().getSimpleName()));}return k.toString();}if(j instanceof JSONArray){JSONArray a=(JSONArray)j;return "array["+a.length()+"]"+(a.length()>0?" of "+shape(a.opt(0)):"");}return String.valueOf(j);}
 private static void appendVersionGroups(StringBuilder o,List<RequestRecord>r){Map<String,Integer>m=new TreeMap<>();for(RequestRecord x:r){java.util.regex.Matcher q=java.util.regex.Pattern.compile("/(v[0-9]+|api/[0-9]+)(/|$)").matcher(x.url);if(q.find())m.put(q.group(1),m.getOrDefault(q.group(1),0)+1);}o.append(m.isEmpty()?"No explicit API version paths observed.":m.toString());}
 private static int countUndocumented(Map<String,Set<String>>m,String doc){int n=0;String d=doc.toLowerCase(Locale.ROOT);for(Set<String>s:m.values())for(String x:s){String p=x.substring(x.indexOf(' ')+1);if(!d.contains(p.toLowerCase(Locale.ROOT)))n++;}return n;}
 private static void appendDependencyHints(StringBuilder o,List<RequestRecord>r){Map<String,Integer>keys=new TreeMap<>();for(RequestRecord x:r)if(x.responseBody.startsWith("{"))try{JSONObject j=new JSONObject(x.responseBody);Iterator<String>it=j.keys();while(it.hasNext()){String k=it.next();if(k.toLowerCase(Locale.ROOT).matches(".*(id|token|key|code)$"))keys.put(k,keys.getOrDefault(k,0)+1);}}catch(Exception ignored){}o.append(keys.isEmpty()?"No obvious cross-request identifiers inferred.":keys.toString());}
 private static String simpleFingerprint(Map<String,Set<String>>m,List<RequestRecord>r){return Integer.toHexString((m.toString()+"|"+r.size()).hashCode());}
 private static void appendObjectIds(StringBuilder o,List<RequestRecord>r,int max){Set<String>s=new LinkedHashSet<>();for(RequestRecord x:r){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)(id|user|account|order|item)[^=:/]{0,8}[=:/]([0-9]{2,}|[0-9a-f]{8}-[0-9a-f-]{20,})").matcher(x.url+" "+x.requestBody);while(m.find()&&s.size()<max)s.add(m.group());}for(String x:s)o.append("• ").append(trim(x,120)).append('\n');if(s.isEmpty())o.append("No obvious object identifiers found.");}
 private static void appendJwt(StringBuilder o,List<RequestRecord>r){Set<String>s=new LinkedHashSet<>();for(RequestRecord x:r)for(String v:x.requestHeaders.values()){java.util.regex.Matcher m=java.util.regex.Pattern.compile("eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+").matcher(v);while(m.find()&&s.size()<10)s.add(m.group());}if(s.isEmpty()){o.append("No JWT-like bearer tokens observed in decrypted request headers.");return;}for(String t:s)o.append(decodeJwt(t)).append("\n");}
 private static String decodeJwt(String t){try{String[]p=t.trim().split("\\.");if(p.length<2)return "Not a JWT";return "header="+new String(android.util.Base64.decode(p[0],android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING),java.nio.charset.StandardCharsets.UTF_8)+"\npayload="+new String(android.util.Base64.decode(p[1],android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING),java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){return "JWT decode failed: "+e.getMessage();}}
 private static void appendOauth(StringBuilder o,List<RequestRecord>r){for(RequestRecord x:r){String z=(x.url+" "+x.requestBody).toLowerCase(Locale.ROOT);if(z.contains("oauth")||z.contains("authorize")||z.contains("code_verifier")||z.contains("code_challenge")||z.contains("grant_type"))o.append(shortReq(x)).append('\n');}}
 private static int countUrlWords(List<RequestRecord>r,String...w){int n=0;for(RequestRecord x:r){String z=(x.url+" "+x.requestBody).toLowerCase(Locale.ROOT);for(String q:w)if(z.contains(q)){n++;break;}}return n;}
 private static int csrfSignals(List<RequestRecord>r){int n=0;for(RequestRecord x:r){String z=(x.requestHeaders.toString()+x.requestBody).toLowerCase(Locale.ROOT);if(z.contains("csrf")||z.contains("xsrf"))n++;}return n;}
 private static void appendResponseHeaders(StringBuilder o,List<RequestRecord>r,String...keys){Set<String>s=new LinkedHashSet<>();for(RequestRecord x:r)for(String k:keys)for(Map.Entry<String,String>h:x.responseHeaders.entrySet())if(h.getKey().equalsIgnoreCase(k))s.add(k+": "+h.getValue());for(String x:s)o.append("• ").append(x).append('\n');if(s.isEmpty())o.append("No matching headers observed.");}
 private static void appendSecurityHeaders(StringBuilder o,List<RequestRecord>r){String[]k={"Strict-Transport-Security","Content-Security-Policy","X-Content-Type-Options","Referrer-Policy","Permissions-Policy"};for(String q:k)o.append(q).append(": ").append(countResponseHeader(r,q,null)).append(" response(s)\n");}
 private static int countStatus(List<RequestRecord>r,int s){int n=0;for(RequestRecord x:r)if(x.statusCode==s)n++;return n;}
 private static int jsonCandidateCount(List<RequestRecord>r){int n=0;for(RequestRecord x:r)if(x.responseBody.trim().startsWith("{")||x.responseBody.trim().startsWith("["))n++;return n;}
 private static String summary(List<ProxyEvent>e,List<RequestRecord>r){Map<String,Integer>p=countProto(e);int err=0;long bytes=0;for(ProxyEvent x:e){bytes+=x.bytes;if(x.status>=400)err++;}return "Protocols: "+p+"\nHosts: "+hosts(e).size()+"\nHTTP records: "+r.size()+"\nErrors: "+err+"\nObserved bytes: "+bytes;}
 private static String anomalies(List<ProxyEvent>e,List<RequestRecord>r){long max=0;RequestRecord slow=null;Map<Integer,Integer>s=new TreeMap<>();for(RequestRecord x:r){if(x.durationMs>max){max=x.durationMs;slow=x;}s.put(x.statusCode,s.getOrDefault(x.statusCode,0)+1);}return "Slowest request: "+(slow==null?"none":slow.durationMs+" ms • "+trim(slow.url,120))+"\nStatus distribution: "+s+"\nLarge responses (>500KB): "+largeResponses(r);}
 private static int largeResponses(List<RequestRecord>r){int n=0;for(RequestRecord x:r)if(x.responseBody.length()>500000)n++;return n;}
 private static void appendRisk(StringBuilder o,List<RequestRecord>r,int max){List<String>a=new ArrayList<>();for(RequestRecord x:r){int s=0;String z=(x.url+" "+x.requestBody).toLowerCase(Locale.ROOT);if(z.contains("auth")||z.contains("token")||z.contains("password"))s+=3;if("POST".equals(x.method)||"PUT".equals(x.method)||"DELETE".equals(x.method))s+=2;if(x.statusCode>=500)s+=2;if(z.contains("admin")||z.contains("account")||z.contains("payment"))s+=3;if(s>0)a.add(String.format(Locale.ROOT,"%02d • %s",s,shortReq(x)));}Collections.sort(a,Collections.reverseOrder());for(int i=0;i<Math.min(max,a.size());i++)o.append(a.get(i)).append('\n');if(a.isEmpty())o.append("No high-signal endpoints in current data.");}
 private static void appendSensitive(StringBuilder o,List<RequestRecord>r){String[]w={"authorization","cookie","token","password","email","phone","secret","api_key","apikey"};Map<String,Integer>m=new TreeMap<>();for(RequestRecord x:r){String z=(x.requestHeaders.toString()+" "+x.requestBody+" "+x.responseBody).toLowerCase(Locale.ROOT);for(String q:w)if(z.contains(q))m.put(q,m.getOrDefault(q,0)+1);}o.append(m.isEmpty()?"No obvious sensitive-data keywords found in decrypted records.":m.toString());}
 private static void appendAuthGraph(StringBuilder o,List<RequestRecord>r){for(RequestRecord x:r){String z=(x.url+" "+x.requestBody).toLowerCase(Locale.ROOT);if(z.contains("login")||z.contains("auth")||z.contains("token")||z.contains("refresh")||z.contains("logout"))o.append(shortReq(x)).append('\n');}}
 private static void appendErrorClusters(StringBuilder o,List<RequestRecord>r){Map<String,Integer>m=new TreeMap<>();for(RequestRecord x:r)if(x.statusCode>=400||!blank(x.error)){String k=x.statusCode+" "+(blank(x.error)?x.statusText:x.error);m.put(k,m.getOrDefault(k,0)+1);}o.append(m.isEmpty()?"No errors in decrypted history.":m.toString());}
 private static String rootCause(List<RequestRecord>r){int a=countStatus(r,401),b=countStatus(r,403),c=countStatus(r,429),s=0;for(RequestRecord x:r)if(x.statusCode>=500)s++;if(c>0)return "Rate limiting is the strongest observed signal (429 responses: "+c+"). Check Retry-After and request cadence.";if(a+b>0)return "Authentication/authorization is the strongest observed signal (401="+a+", 403="+b+"). Compare session/token state before changing request content.";if(s>0)return "Server-side failures were observed (5xx="+s+"). Correlate endpoint, payload shape and timing.";return "No dominant failure pattern is visible in current decrypted history.";}
 private static void appendSearch(StringBuilder o,List<ProxyEvent>e,List<RequestRecord>r,String q){String z=q.toLowerCase(Locale.ROOT);int n=0;for(ProxyEvent x:e){String h=(x.method+" "+x.status+" "+x.protocol+" "+x.host+" "+x.url).toLowerCase(Locale.ROOT);boolean hit=h.contains(z)||(z.contains("errors")&&x.status>=400)||(z.contains("slow")&&x.bytes>500000);if(hit&&n++<50)o.append(x).append('\n');}if(n==0)o.append("No matching traffic.");}
 private static String testPlan(List<RequestRecord>r){return "1. Inventory endpoints and authentication flow.\n2. Select representative happy-path requests.\n3. Add schema/status assertions.\n4. Compare two authorized test roles where applicable.\n5. Check session expiry/logout behavior.\n6. Review CORS/security headers.\n7. Run bounded rate-limit checks only where authorized.\n8. Save evidence and regression baseline.\nObserved requests available: "+r.size();}
 private static int sensitiveCount(List<RequestRecord>r){int n=0;for(RequestRecord x:r){String z=(x.requestHeaders.toString()+x.requestBody+x.responseBody).toLowerCase(Locale.ROOT);if(z.matches("(?s).*(authorization|cookie|token|password|secret|api[_-]?key).*"))n++;}return n;}
 private static boolean blank(String s){return s==null||s.trim().isEmpty();}
 private static String trim(String s,int n){if(s==null)return "";return s.length()<=n?s:s.substring(0,n-1)+"…";}
}
