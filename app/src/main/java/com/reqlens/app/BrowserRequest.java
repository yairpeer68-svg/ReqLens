package com.reqlens.app;

import org.json.*;import java.util.*;
public final class BrowserRequest {
 public long timestamp,projectId; public String method="GET",url="",mimeType="",error=""; public int statusCode=-1; public boolean mainFrame; public final LinkedHashMap<String,String>headers=new LinkedHashMap<>();
 public RequestRecord toRequestRecord(){RequestRecord r=new RequestRecord();r.timestamp=timestamp;r.method=method;r.url=url;r.statusCode=statusCode;r.requestHeaders.putAll(headers);r.error=error;return r;}
 public JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("timestamp",timestamp);o.put("projectId",projectId);o.put("method",method);o.put("url",url);o.put("statusCode",statusCode);o.put("mimeType",mimeType);o.put("mainFrame",mainFrame);o.put("error",error);JSONObject h=new JSONObject();for(Map.Entry<String,String>e:headers.entrySet())h.put(e.getKey(),e.getValue());o.put("headers",h);}catch(JSONException e){throw new IllegalStateException(e);}return o;}
 public static BrowserRequest fromJson(JSONObject o){BrowserRequest x=new BrowserRequest();x.timestamp=o.optLong("timestamp");x.projectId=o.optLong("projectId");x.method=o.optString("method","GET");x.url=o.optString("url","");x.statusCode=o.optInt("statusCode",-1);x.mimeType=o.optString("mimeType","");x.mainFrame=o.optBoolean("mainFrame");x.error=o.optString("error","");JSONObject h=o.optJSONObject("headers");if(h!=null){Iterator<String>it=h.keys();while(it.hasNext()){String k=it.next();x.headers.put(k,h.optString(k,""));}}return x;}
 @Override public String toString(){String code=statusCode>=0?String.valueOf(statusCode):"—";return method+"  "+code+"  "+url;}
}
