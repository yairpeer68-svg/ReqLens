package com.reqlens.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;

import java.util.Locale;
import java.util.Map;

public final class BrowserActivity extends Activity {
    private WebView web;
    private EditText address;
    private TextView stats;
    private ProgressBar progress;
    private int requestCount;
    private int errorCount;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildUi());
        configureWebView();
        String start = getIntent().getStringExtra("url");
        navigate(start == null || start.trim().isEmpty() ? "https://example.com" : start);
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8), dp(8), dp(8), dp(8));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("ReqLens Browser");
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button history = new Button(this);
        history.setText("HISTORY");
        history.setOnClickListener(v -> startActivity(new Intent(this, BrowserHistoryActivity.class)));
        titleRow.addView(history, new LinearLayout.LayoutParams(dp(120), dp(42)));
        root.addView(titleRow);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        Button back = button("‹", v -> { if (web.canGoBack()) web.goBack(); });
        Button forward = button("›", v -> { if (web.canGoForward()) web.goForward(); });
        Button reload = button("↻", v -> web.reload());
        nav.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        nav.addView(forward, new LinearLayout.LayoutParams(dp(48), dp(48)));
        nav.addView(reload, new LinearLayout.LayoutParams(dp(48), dp(48)));

        address = new EditText(this);
        address.setSingleLine(true);
        address.setHint("https://example.com");
        address.setOnEditorActionListener((v, action, event) -> { navigate(address.getText().toString()); return true; });
        nav.addView(address, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button go = button("GO", v -> navigate(address.getText().toString()));
        nav.addView(go, new LinearLayout.LayoutParams(dp(64), dp(48)));
        root.addView(nav);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        root.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));

        stats = new TextView(this);
        stats.setTextSize(12);
        stats.setPadding(0, dp(4), 0, dp(4));
        root.addView(stats);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        Button external = button("OPEN ↗", v -> { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(web.getUrl()))); } catch (Exception e) { Toast.makeText(this, "Cannot open externally", Toast.LENGTH_SHORT).show(); } });
        controls.addView(external, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button clearSite = button("CLEAR SITE DATA", v -> { CookieManager.getInstance().removeAllCookies(null); web.clearCache(true); web.clearHistory(); Toast.makeText(this, "Browser site data cleared", Toast.LENGTH_SHORT).show(); });
        controls.addView(clearSite, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button settings = button("SETTINGS", v -> startActivity(new Intent(this, SettingsActivity.class)));
        controls.addView(settings, new LinearLayout.LayoutParams(0, dp(42), 1));
        root.addView(controls);

        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        updateStats();
        return root;
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this); b.setText(text); b.setOnClickListener(listener); return b;
    }

    private void configureWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(SuiteSettings.javascript(this));
        s.setDomStorageEnabled(!SuiteSettings.incognito(this));
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSafeBrowsingEnabled(true);
        if (SuiteSettings.desktopUa(this)) s.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/140 Safari/537.36");
        if (SuiteSettings.incognito(this)) { CookieManager.getInstance().setAcceptCookie(false); web.clearHistory(); web.clearCache(true); } else CookieManager.getInstance().setAcceptCookie(true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                record(request, -1, "", "");
                return null;
            }

            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                address.setText(url);
            }

            @Override public void onPageFinished(WebView view, String url) {
                address.setText(url);
                setTitle(view.getTitle() == null ? "ReqLens Browser" : view.getTitle());
            }

            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                record(request, response.getStatusCode(), response.getMimeType(), "HTTP " + response.getStatusCode());
                errorCount++;
                updateStats();
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    errorCount++;
                    Toast.makeText(BrowserActivity.this, String.valueOf(error.getDescription()), Toast.LENGTH_SHORT).show();
                }
                updateStats();
            }
        });
    }

    private void record(WebResourceRequest request, int status, String mime, String error) {
        BrowserRequest item = new BrowserRequest();
        item.timestamp = System.currentTimeMillis();
        item.method = request.getMethod() == null ? "GET" : request.getMethod();
        item.url = request.getUrl().toString();
        item.statusCode = status;
        item.mimeType = mime == null ? "" : mime;
        item.mainFrame = request.isForMainFrame();
        item.error = error == null ? "" : error;
        for (Map.Entry<String, String> e : request.getRequestHeaders().entrySet()) item.headers.put(e.getKey(), e.getValue());
        BrowserHistoryRepository.add(item);
        requestCount++;
        runOnUiThread(this::updateStats);
    }

    private void navigate(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return;
        if (!value.contains("://")) {
            if (value.contains(".") && !value.contains(" ")) value = "https://" + value;
            else value = "https://www.google.com/search?q=" + Uri.encode(value);
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("https://") || lower.startsWith("http://"))) {
            Toast.makeText(this, "Only HTTP/HTTPS URLs are supported", Toast.LENGTH_SHORT).show();
            return;
        }
        address.setText(value);
        web.loadUrl(value);
    }

    private void updateStats() {
        stats.setText("Requests: " + requestCount + "   Errors: " + errorCount + "   Saved: " + BrowserHistoryRepository.size() + "   •   " + (SuiteSettings.incognito(this)?"Incognito • ":"") + (SuiteSettings.desktopUa(this)?"Desktop UA • ":"") + "Standard TLS validation");
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()) { web.goBack(); return true; }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onDestroy() {
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
