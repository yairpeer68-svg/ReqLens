package com.reqlens.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class FlowRepository {
    private final File file;
    public FlowRepository(Context context) { file = new File(context.getFilesDir(), "capture_flows.json"); }

    public synchronized void save(List<FlowRecord> flows) {
        JSONArray a = new JSONArray();
        int count = Math.min(flows.size(), 1000);
        for (int i = 0; i < count; i++) a.put(flows.get(i).toJson());
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(a.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }

    public File exportJson(Context context, List<FlowRecord> flows) throws Exception {
        JSONArray a = new JSONArray();
        for (FlowRecord r : flows) a.put(r.toJson());
        JSONObject root = new JSONObject();
        root.put("schema", "reqlens.flow.v1");
        root.put("exportedAt", System.currentTimeMillis());
        root.put("flows", a);
        File out = new File(context.getCacheDir(), "reqlens-flows-" + System.currentTimeMillis() + ".json");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        return out;
    }
}
