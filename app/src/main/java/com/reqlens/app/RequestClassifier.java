package com.reqlens.app;
import android.graphics.Color;import java.util.*;
public final class RequestClassifier{
 private RequestClassifier(){}
 public static String kind(String method,int status,String mime,String url){String u=url==null?"":url.toLowerCase(Locale.ROOT),m=mime==null?"":mime.toLowerCase(Locale.ROOT);if(status>=500)return"SERVER_ERROR";if(status>=400)return"CLIENT_ERROR";if(status>=300)return"REDIRECT";if(u.contains("login")||u.contains("auth")||u.contains("token"))return"AUTH";if("POST".equalsIgnoreCase(method)&&(u.contains("upload")||m.contains("multipart")))return"UPLOAD";if(m.contains("json")||u.contains("/api/"))return"API";if(m.startsWith("image/")||m.contains("font")||m.contains("css")||m.contains("javascript"))return"STATIC";return"NORMAL";}
 public static int color(String kind){switch(kind){case"SERVER_ERROR":return Color.rgb(255,220,220);case"CLIENT_ERROR":return Color.rgb(255,235,210);case"REDIRECT":return Color.rgb(235,225,255);case"AUTH":return Color.rgb(255,245,200);case"UPLOAD":return Color.rgb(220,245,255);case"API":return Color.rgb(220,255,230);case"STATIC":return Color.rgb(240,240,240);default:return Color.TRANSPARENT;}}
}
