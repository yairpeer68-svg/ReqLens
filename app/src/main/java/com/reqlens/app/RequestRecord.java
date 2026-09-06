package com.reqlens.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RequestRecord {
    public long timestamp;
    public String method = "GET";
    public String url = "";
    public final LinkedHashMap<String, String> requestHeaders = new LinkedHashMap<>();
    public String requestBody = "";
    public int statusCode = -1;
    public String statusText = "";
    public final LinkedHashMap<String, String> responseHeaders = new LinkedHashMap<>();
    public String responseBody = "";
    public long durationMs;
    public String error = "";

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("timestamp", timestamp);
            o.put("method", method);
            o.put("url", url);
            o.put("requestHeaders", mapToJson(requestHeaders));
            o.put("requestBody", requestBody);
            o.put("statusCode", statusCode);
            o.put("statusText", statusText);
            o.put("responseHeaders", mapToJson(responseHeaders));
            o.put("responseBody", responseBody);
            o.put("durationMs", durationMs);
            o.put("error", error);
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to serialize RequestRecord", e);
        }
        return o;
    }

    public static RequestRecord fromJson(JSONObject o) {
        RequestRecord r = new RequestRecord();
        r.timestamp = o.optLong("timestamp");
        r.method = o.optString("method", "GET");
        r.url = o.optString("url", "");
        jsonToMap(o.optJSONObject("requestHeaders"), r.requestHeaders);
        r.requestBody = o.optString("requestBody", "");
        r.statusCode = o.optInt("statusCode", -1);
        r.statusText = o.optString("statusText", "");
        jsonToMap(o.optJSONObject("responseHeaders"), r.responseHeaders);
        r.responseBody = o.optString("responseBody", "");
        r.durationMs = o.optLong("durationMs");
        r.error = o.optString("error", "");
        return r;
    }

    private static JSONObject mapToJson(Map<String, String> map) {
        JSONObject o = new JSONObject();
        try {
            for (Map.Entry<String, String> e : map.entrySet()) {
                o.put(e.getKey(), e.getValue());
            }
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to serialize headers", e);
        }
        return o;
    }

    private static void jsonToMap(JSONObject o, Map<String, String> out) {
        if (o == null) return;
        JSONArray names = o.names();
        if (names == null) return;
        for (int i = 0; i < names.length(); i++) {
            String k = names.optString(i);
            out.put(k, o.optString(k, ""));
        }
    }
}
