package com.reqlens.app;
import org.json.*;import java.util.*;
public final class InterceptRule {
 public boolean enabled=true;public String group="default",hostContains="",method="",action="SET_HEADER",name="",value="";
 public boolean matches(RequestRecord r){if(!enabled)return false;if(!method.isEmpty()&&!method.equalsIgnoreCase(r.method))return false;return hostContains.isEmpty()||r.url.toLowerCase(Locale.ROOT).contains(hostContains.toLowerCase(Locale.ROOT));}
 public void apply(RequestRecord r){if(!matches(r))return;try{if("SET_HEADER".equals(action)&&!name.trim().isEmpty())r.requestHeaders.put(name.trim(),value);else if("REMOVE_HEADER".equals(action)&&!name.trim().isEmpty())removeHeader(r,name.trim());else if("REPLACE_URL".equals(action)&&!name.isEmpty())r.url=r.url.replace(name,value);else if("REPLACE_BODY".equals(action)&&!name.isEmpty())r.requestBody=r.requestBody.replace(name,value);else if("REGEX_URL".equals(action)&&!name.isEmpty())r.url=r.url.replaceAll(name,value);else if("REGEX_BODY".equals(action)&&!name.isEmpty())r.requestBody=r.requestBody.replaceAll(name,value);else if("JSON_SET".equals(action)&&!name.trim().isEmpty()){JSONObject o=new JSONObject(r.requestBody);o.put(name.trim(),parseValue(value));r.requestBody=o.toString();}}catch(Exception ignored){} }
 private static Object parseValue(String v){String x=v==null?"":v.trim();if("true".equalsIgnoreCase(x))return true;if("false".equalsIgnoreCase(x))return false;if("null".equalsIgnoreCase(x))return JSONObject.NULL;try{return x.contains(".")?Double.parseDouble(x):Long.parseLong(x);}catch(Exception e){return v;}}
 private static void removeHeader(RequestRecord r,String name){String hit=null;for(String k:r.requestHeaders.keySet())if(name.equalsIgnoreCase(k)){hit=k;break;}if(hit!=null)r.requestHeaders.remove(hit);}
 @Override public String toString(){return(enabled?"✓ ":"○ ")+"["+group+"] "+action+" • "+(method.isEmpty()?"ANY":method)+" • "+(hostContains.isEmpty()?"all hosts":hostContains);}
}
