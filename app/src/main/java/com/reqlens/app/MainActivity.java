package com.reqlens.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int MAX_BODY_BYTES = 1_000_000;
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 20_000;
    private static final int MAX_HISTORY = 200;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ArrayList<RequestRecord> history = new ArrayList<>();
    private final ArrayList<RequestRecord> visibleHistory = new ArrayList<>();

    private Spinner methodSpinner;
    private EditText urlInput;
    private EditText headersInput;
    private EditText bodyInput;
    private EditText filterInput;
    private CheckBox followRedirects;
    private ProgressBar progress;
    private ListView historyList;
    private ArrayAdapter<RequestRecord> adapter;
    private Button sendButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ProjectRepository.init(this);
        RequestAnnotationRepository.init(this);
        BrowserHistoryRepository.init(this);
        loadHistory();
        setContentView(buildUi());
        applyFilter();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView title = new TextView(this);
        title.setText("ReqLens — HTTP Inspector");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title, matchWrap());

        TextView note = new TextView(this);
        note.setText("Send, inspect, save and export HTTP requests. Use only on systems you own or are authorized to test.");
        note.setTextSize(12);
        note.setPadding(0, dp(4), 0, dp(10));
        root.addView(note, matchWrap());

        LinearLayout primaryRow = new LinearLayout(this);
        primaryRow.setOrientation(LinearLayout.HORIZONTAL);
        Button browserButton = new Button(this);
        browserButton.setText("BROWSER");
        browserButton.setOnClickListener(v -> startActivity(new Intent(this, BrowserActivity.class)));
        primaryRow.addView(browserButton, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button captureButton = new Button(this);
        captureButton.setText("APP CAPTURE");
        captureButton.setOnClickListener(v -> startActivity(new Intent(this, CaptureActivity.class)));
        primaryRow.addView(captureButton, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button dashboardButton = new Button(this);
        dashboardButton.setText("DASHBOARD");
        dashboardButton.setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        primaryRow.addView(dashboardButton, new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(primaryRow, matchWrap());

        LinearLayout toolsRow = new LinearLayout(this);
        toolsRow.setOrientation(LinearLayout.HORIZONTAL);
        Button repeaterTool = new Button(this);
        repeaterTool.setText("REPEATER");
        repeaterTool.setOnClickListener(v -> { RepeaterActivity.pending = new RequestRecord(); startActivity(new Intent(this, RepeaterActivity.class)); });
        toolsRow.addView(repeaterTool, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button decoderTool = new Button(this);
        decoderTool.setText("DECODER");
        decoderTool.setOnClickListener(v -> startActivity(new Intent(this, DecoderActivity.class)));
        toolsRow.addView(decoderTool, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button comparerTool = new Button(this);
        comparerTool.setText("COMPARER");
        comparerTool.setOnClickListener(v -> startActivity(new Intent(this, ComparerActivity.class)));
        toolsRow.addView(comparerTool, new LinearLayout.LayoutParams(0, dp(46), 1));
        root.addView(toolsRow, matchWrap());

        LinearLayout suiteRow = new LinearLayout(this);
        suiteRow.setOrientation(LinearLayout.HORIZONTAL);
        Button proxyTool = new Button(this); proxyTool.setText("PROXY"); proxyTool.setOnClickListener(v -> startActivity(new Intent(this, ProxyHistoryActivity.class)));
        suiteRow.addView(proxyTool, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button interceptTool = new Button(this); interceptTool.setText("INTERCEPT"); interceptTool.setOnClickListener(v -> startActivity(new Intent(this, InterceptActivity.class)));
        suiteRow.addView(interceptTool, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button rulesTool = new Button(this); rulesTool.setText("RULES"); rulesTool.setOnClickListener(v -> startActivity(new Intent(this, RulesActivity.class)));
        suiteRow.addView(rulesTool, new LinearLayout.LayoutParams(0, dp(46), 1));
        root.addView(suiteRow, matchWrap());

        LinearLayout labRow = new LinearLayout(this);
        labRow.setOrientation(LinearLayout.HORIZONTAL);
        Button projectsTool = new Button(this); projectsTool.setText("PROJECTS"); projectsTool.setOnClickListener(v -> startActivity(new Intent(this, ProjectsActivity.class)));
        labRow.addView(projectsTool, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button variationsTool = new Button(this); variationsTool.setText("VARIATIONS"); variationsTool.setOnClickListener(v -> startActivity(new Intent(this, IntruderActivity.class)));
        labRow.addView(variationsTool, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button wsTool = new Button(this); wsTool.setText("WEBSOCKET"); wsTool.setOnClickListener(v -> startActivity(new Intent(this, WebSocketHistoryActivity.class)));
        labRow.addView(wsTool, new LinearLayout.LayoutParams(0, dp(46), 1));
        root.addView(labRow, matchWrap());
        Button allTools = new Button(this);
        allTools.setText("ALL TOOLS & SETTINGS");
        allTools.setOnClickListener(v -> startActivity(new Intent(this, ToolsActivity.class)));
        root.addView(allTools, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        methodSpinner = new Spinner(this);
        String[] methods = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"};
        methodSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, methods));
        row.addView(methodSpinner, new LinearLayout.LayoutParams(dp(115), dp(52)));

        urlInput = new EditText(this);
        urlInput.setHint("https://api.example.com/v1/test");
        urlInput.setSingleLine(true);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        row.addView(urlInput, new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(row, matchWrap());

        headersInput = new EditText(this);
        headersInput.setHint("Headers, one per line\nContent-Type: application/json\nAuthorization: Bearer ...");
        headersInput.setMinLines(3);
        headersInput.setGravity(Gravity.TOP);
        root.addView(headersInput, matchWrap());

        bodyInput = new EditText(this);
        bodyInput.setHint("Request body (optional)");
        bodyInput.setMinLines(4);
        bodyInput.setGravity(Gravity.TOP);
        bodyInput.setTypeface(Typeface.MONOSPACE);
        root.addView(bodyInput, matchWrap());

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        sendButton = new Button(this);
        sendButton.setText("SEND");
        sendButton.setOnClickListener(v -> sendCurrentRequest());
        actionRow.addView(sendButton, new LinearLayout.LayoutParams(0, dp(50), 1));

        followRedirects = new CheckBox(this);
        followRedirects.setText("Follow redirects");
        actionRow.addView(followRedirects, new LinearLayout.LayoutParams(0, dp(50), 1));

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        actionRow.addView(progress, new LinearLayout.LayoutParams(dp(50), dp(50)));
        root.addView(actionRow, matchWrap());

        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterInput = new EditText(this);
        filterInput.setHint("Filter method / status / host / text");
        filterInput.setSingleLine(true);
        filterInput.setOnEditorActionListener((v, actionId, event) -> { applyFilter(); return false; });
        filterRow.addView(filterInput, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button filterBtn = new Button(this);
        filterBtn.setText("FILTER");
        filterBtn.setOnClickListener(v -> applyFilter());
        filterRow.addView(filterBtn, new LinearLayout.LayoutParams(dp(100), dp(48)));
        root.addView(filterRow, matchWrap());

        historyList = new ListView(this);
        adapter = new ArrayAdapter<RequestRecord>(this, android.R.layout.simple_list_item_2, android.R.id.text1, visibleHistory) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                RequestRecord r = getItem(position);
                TextView t1 = v.findViewById(android.R.id.text1);
                TextView t2 = v.findViewById(android.R.id.text2);
                String status = r.statusCode >= 0 ? String.valueOf(r.statusCode) : "ERR";
                t1.setText(r.method + "  " + status + "  " + r.url);
                String when = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(r.timestamp));
                t2.setText(r.durationMs + " ms  •  " + when);
                return v;
            }
        };
        historyList.setAdapter(adapter);
        historyList.setOnItemClickListener((parent, view, position, id) -> showDetails(visibleHistory.get(position)));
        root.addView(historyList, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        Button clear = new Button(this);
        clear.setText("CLEAR HISTORY");
        clear.setOnClickListener(v -> confirmClear());
        bottom.addView(clear, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button sample = new Button(this);
        sample.setText("LOAD SAMPLE");
        sample.setOnClickListener(v -> {
            methodSpinner.setSelection(0);
            urlInput.setText("https://httpbin.org/anything");
            headersInput.setText("Accept: application/json");
            bodyInput.setText("");
        });
        bottom.addView(sample, new LinearLayout.LayoutParams(0, dp(46), 1));
        root.addView(bottom, matchWrap());

        return root;
    }

    private void sendCurrentRequest() {
        String method = String.valueOf(methodSpinner.getSelectedItem());
        String url = urlInput.getText().toString().trim();
        String body = bodyInput.getText().toString();
        LinkedHashMap<String, String> headers;
        try {
            if (!(url.startsWith("https://") || url.startsWith("http://"))) throw new IllegalArgumentException("URL must start with http:// or https://");
            new URL(url);
            headers = parseHeaders(headersInput.getText().toString());
        } catch (Exception e) {
            toast(e.getMessage());
            return;
        }

        RequestRecord record = new RequestRecord();
        record.timestamp = System.currentTimeMillis();
        record.method = method;
        record.url = url;
        record.requestHeaders.putAll(headers);
        record.requestBody = body;

        RequestPipeline.Prepared prepared = RequestPipeline.prepare(record, true);
        if (prepared.decision == RequestPipeline.Decision.QUEUED) {
            toast("Request queued in Intercept");
            return;
        }
        RequestRecord preparedRecord = prepared.request;
        setBusy(true);
        boolean redirects = followRedirects.isChecked();
        executor.submit(() -> executeRequest(preparedRecord, redirects));
    }

    private void executeRequest(RequestRecord record, boolean followRedirects) {
        long start = System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(record.url).openConnection();
            conn.setRequestMethod(record.method);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(followRedirects);
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", "ReqLens/0.7 Android");
            for (Map.Entry<String, String> e : record.requestHeaders.entrySet()) conn.setRequestProperty(e.getKey(), e.getValue());

            boolean bodyAllowed = !(record.method.equals("GET") || record.method.equals("HEAD"));
            if (bodyAllowed && !record.requestBody.isEmpty()) {
                if (!hasHeader(record.requestHeaders, "Content-Type")) {
                    String trimmed = record.requestBody.trim();
                    conn.setRequestProperty("Content-Type", (trimmed.startsWith("{") || trimmed.startsWith("[")) ? "application/json; charset=utf-8" : "text/plain; charset=utf-8");
                }
                byte[] bytes = record.requestBody.getBytes(StandardCharsets.UTF_8);
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream os = conn.getOutputStream()) { os.write(bytes); }
            }

            record.statusCode = conn.getResponseCode();
            record.statusText = conn.getResponseMessage() == null ? "" : conn.getResponseMessage();
            for (Map.Entry<String, List<String>> e : conn.getHeaderFields().entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                record.responseHeaders.put(e.getKey(), String.join(", ", e.getValue()));
            }
            InputStream raw = record.statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
            record.responseBody = raw == null ? "" : readLimited(raw, MAX_BODY_BYTES);
        } catch (Exception e) {
            record.error = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "Request failed" : e.getMessage());
        } finally {
            record.durationMs = System.currentTimeMillis() - start;
            if (conn != null) conn.disconnect();
            ProxyHistoryRepository.addHttp(record, "http-client");
            runOnUiThread(() -> {
                setBusy(false);
                history.add(0, record);
                while (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
                saveHistory();
                applyFilter();
                showDetails(record);
            });
        }
    }

    private LinkedHashMap<String, String> parseHeaders(String text) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (text.trim().isEmpty()) return out;
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            int colon = line.indexOf(':');
            if (colon <= 0) throw new IllegalArgumentException("Invalid header on line " + (i + 1));
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (name.contains(" ") || name.contains("\t")) throw new IllegalArgumentException("Invalid header name on line " + (i + 1));
            out.put(name, value);
        }
        return out;
    }

    private static boolean hasHeader(Map<String, String> headers, String name) {
        for (String k : headers.keySet()) if (k.equalsIgnoreCase(name)) return true;
        return false;
    }

    private static String readLimited(InputStream input, int limit) throws Exception {
        try (BufferedInputStream in = new BufferedInputStream(input); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int total = 0;
            while (true) {
                int n = in.read(buf);
                if (n < 0) break;
                int write = Math.min(n, limit - total);
                if (write > 0) out.write(buf, 0, write);
                total += write;
                if (total >= limit) break;
            }
            String s = out.toString(StandardCharsets.UTF_8.name());
            if (total >= limit) s += "\n\n[Response truncated at " + limit + " bytes]";
            return s;
        }
    }

    private void showDetails(RequestRecord r) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(8), dp(12), dp(8));
        scroll.addView(box);

        addSection(box, "REQUEST", r.method + " " + r.url);
        addSection(box, "REQUEST HEADERS", mapText(r.requestHeaders));
        addSection(box, "REQUEST BODY", emptyLabel(r.requestBody));
        addSection(box, "RESPONSE", r.statusCode >= 0 ? (r.statusCode + " " + r.statusText + " • " + r.durationMs + " ms") : ("ERROR • " + r.durationMs + " ms"));
        if (!r.error.isEmpty()) addSection(box, "ERROR", r.error);
        addSection(box, "RESPONSE HEADERS", mapText(r.responseHeaders));
        addSection(box, "RESPONSE BODY", emptyLabel(r.responseBody));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Request details")
                .setView(scroll)
                .setNegativeButton("CLOSE", null)
                .setNeutralButton("DELETE", null)
                .setPositiveButton("COPY", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> showCopyMenu(r));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                history.remove(r);
                saveHistory();
                applyFilter();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void showCopyMenu(RequestRecord r) {
        String[] items = {"Send to Repeater", "Copy safe cURL", "Copy full cURL", "Copy safe Bash", "Copy full Bash", "Load into composer"};
        new AlertDialog.Builder(this).setTitle("Export request").setItems(items, (d, which) -> {
            switch (which) {
                case 0: RepeaterActivity.pending = r; startActivity(new Intent(this, RepeaterActivity.class)); break;
                case 1: copy("cURL", CurlBuilder.build(r.method, r.url, r.requestHeaders, r.requestBody, true, false)); break;
                case 2: copy("cURL", CurlBuilder.build(r.method, r.url, r.requestHeaders, r.requestBody, false, false)); break;
                case 3: copy("Bash", CurlBuilder.build(r.method, r.url, r.requestHeaders, r.requestBody, true, true)); break;
                case 4: copy("Bash", CurlBuilder.build(r.method, r.url, r.requestHeaders, r.requestBody, false, true)); break;
                case 5: loadIntoComposer(r); break;
            }
        }).show();
    }

    private void loadIntoComposer(RequestRecord r) {
        String[] methods = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"};
        for (int i = 0; i < methods.length; i++) if (methods[i].equals(r.method)) methodSpinner.setSelection(i);
        urlInput.setText(r.url);
        headersInput.setText(mapText(r.requestHeaders).replace("(none)", ""));
        bodyInput.setText(r.requestBody);
        toast("Loaded into composer");
    }

    private void addSection(LinearLayout box, String title, String value) {
        TextView h = new TextView(this);
        h.setText(title);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setTextSize(13);
        h.setPadding(0, dp(8), 0, dp(3));
        box.addView(h, matchWrap());
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextIsSelectable(true);
        t.setTypeface(Typeface.MONOSPACE);
        t.setTextSize(12);
        box.addView(t, matchWrap());
    }

    private void applyFilter() {
        String q = filterInput == null ? "" : filterInput.getText().toString().trim().toLowerCase();
        visibleHistory.clear();
        for (RequestRecord r : history) {
            String hay = (r.method + " " + r.statusCode + " " + r.url + " " + r.requestBody + " " + r.responseBody + " " + r.error).toLowerCase();
            if (q.isEmpty() || hay.contains(q)) visibleHistory.add(r);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this).setTitle("Clear history?").setMessage("This removes locally saved request history.")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("CLEAR", (d, w) -> {
                    history.clear();
                    saveHistory();
                    applyFilter();
                }).show();
    }

    private void loadHistory() {
        try {
            String encrypted = new SecureJsonStore(this).readOrNull();
            String legacy = getSharedPreferences("reqlens", MODE_PRIVATE).getString("history", "[]");
            JSONArray arr = new JSONArray(encrypted == null ? legacy : encrypted);
            for (int i = 0; i < arr.length(); i++) history.add(RequestRecord.fromJson(arr.getJSONObject(i)));
            if (encrypted == null && arr.length() > 0) {
                saveHistory();
                getSharedPreferences("reqlens", MODE_PRIVATE).edit().remove("history").apply();
            }
        } catch (Exception ignored) { history.clear(); }
    }

    private void saveHistory() {
        try {
            JSONArray arr = new JSONArray();
            for (RequestRecord r : history) arr.put(r.toJson());
            new SecureJsonStore(this).write(arr.toString());
        } catch (Exception e) { toast("Could not save encrypted history"); }
    }

    private void setBusy(boolean busy) {
        sendButton.setEnabled(!busy);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void copy(String label, String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
        toast(label + " copied");
    }

    private void toast(String s) { Toast.makeText(this, s == null ? "Error" : s, Toast.LENGTH_SHORT).show(); }
    private static String emptyLabel(String s) { return s == null || s.isEmpty() ? "(empty)" : s; }
    private static String mapText(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "(none)";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) sb.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        return sb.toString().trim();
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
