package com.reqlens.app;
import org.json.*;
public final class WorkspaceExporter{
 private WorkspaceExporter(){}
 public static String currentProject(boolean redact){try{JSONObject o=new JSONObject();ProjectRepository.Project p=ProjectRepository.active();o.put("format","ReqLensWorkspace");o.put("version",1);o.put("projectId",p.id);o.put("projectName",p.name);o.put("exportedAt",System.currentTimeMillis());o.put("proxy",new JSONArray(HistoryExporter.proxyJson(ProxyHistoryRepository.snapshotActiveProject())));o.put("browser",new JSONArray(HistoryExporter.browserJson(BrowserHistoryRepository.snapshotActiveProject(),redact)));return o.toString();}catch(JSONException e){throw new IllegalStateException(e);}}
}
