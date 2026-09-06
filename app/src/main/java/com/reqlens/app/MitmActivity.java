package com.reqlens.app;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.OutputStream;
import java.security.cert.X509Certificate;

public final class MitmActivity extends Activity {
    private static final int CREATE_CA_FILE = 4101;
    private TextView status;

    @Override public void onCreate(Bundle b) { super.onCreate(b); setContentView(ui()); refresh(); }

    private View ui() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView t = new TextView(this); t.setText("Authorized HTTPS MITM"); t.setTextSize(22); r.addView(t);
        TextView n = new TextView(this);
        n.setText("For traffic you own or are explicitly authorized to test. ReqLens creates a local CA and decrypts only clients that explicitly trust that CA. Android requires CA certificates to be installed manually from Settings. ReqLens does not bypass certificate pinning or mTLS.");
        n.setPadding(0, dp(8), 0, dp(12)); r.addView(n);
        status = new TextView(this); r.addView(status);

        Button export = new Button(this); export.setText("1. EXPORT REQLENS CA (.CER)"); export.setOnClickListener(v -> exportCa()); r.addView(export, new LinearLayout.LayoutParams(-1, dp(52)));
        Button settings = new Button(this); settings.setText("2. OPEN CERTIFICATE SETTINGS"); settings.setOnClickListener(v -> openCertificateSettings()); r.addView(settings, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView installHint = new TextView(this);
        installHint.setText("In Settings choose: Install certificate / CA certificate, then select ReqLens-Authorized-MITM-CA.cer from Downloads.");
        installHint.setPadding(0, dp(4), 0, dp(10)); r.addView(installHint);

        Button start = new Button(this); start.setText("3. ENABLE MITM FOR REQLENS BROWSER"); start.setOnClickListener(v -> enable()); r.addView(start, new LinearLayout.LayoutParams(-1, dp(52)));
        Button stop = new Button(this); stop.setText("DISABLE MITM"); stop.setOnClickListener(v -> disable()); r.addView(stop, new LinearLayout.LayoutParams(-1, dp(52)));
        Button hist = new Button(this); hist.setText("DECRYPTED HTTPS HISTORY"); hist.setOnClickListener(v -> startActivity(new Intent(this, MitmHistoryActivity.class))); r.addView(hist, new LinearLayout.LayoutParams(-1, dp(52)));
        Button browser = new Button(this); browser.setText("OPEN REQLENS BROWSER"); browser.setOnClickListener(v -> startActivity(new Intent(this, BrowserActivity.class))); r.addView(browser, new LinearLayout.LayoutParams(-1, dp(52)));
        return r;
    }

    private void exportCa() {
        try {
            new MitmCaManager(this).ensureCa();
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/x-x509-ca-cert");
            i.putExtra(Intent.EXTRA_TITLE, "ReqLens-Authorized-MITM-CA.cer");
            startActivityForResult(i, CREATE_CA_FILE);
            status.setText("Choose Downloads and save the ReqLens CA file.");
        } catch (Exception e) {
            status.setText("CA generation failed: " + e.getClass().getSimpleName() + ": " + safe(e.getMessage()));
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CREATE_CA_FILE) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) { status.setText("CA export cancelled."); return; }
        Uri uri = data.getData();
        try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
            if (out == null) throw new IllegalStateException("Cannot open selected destination");
            out.write(new MitmCaManager(this).caDer());
            out.flush();
            status.setText("CA exported successfully. Now open Certificate Settings and install it as a CA certificate.\nSaved: " + uri);
        } catch (Exception e) {
            status.setText("CA export failed: " + e.getClass().getSimpleName() + ": " + safe(e.getMessage()));
        }
    }

    private void openCertificateSettings() {
        Intent[] candidates = new Intent[]{
                new Intent("android.settings.CREDENTIAL_STORAGE"),
                new Intent(Settings.ACTION_SECURITY_SETTINGS),
                new Intent(Settings.ACTION_SETTINGS)
        };
        for (Intent i : candidates) {
            try { startActivity(i); status.setText("Android Settings opened. Search for 'Install certificate' or 'CA certificate' if the exact page is not shown."); return; }
            catch (Exception ignored) { }
        }
        status.setText("Could not open Settings automatically.");
    }

    private void enable() {
        try {
            MitmProxyController.setEnabled(this, true);
            if (!MitmProxyController.proxyOverrideSupported()) { status.setText("WebView proxy override is not supported by this WebView build."); return; }
            int port = MitmProxyController.ensureStarted(this);
            MitmProxyController.applyToWebViewProcess(this, () -> runOnUiThread(this::refresh));
            status.setText("MITM proxy starting on 127.0.0.1:" + port + ". The ReqLens CA must already be installed/trusted in Android Settings.");
        } catch (Exception e) {
            MitmProxyController.setEnabled(this, false);
            status.setText("MITM start failed: " + e.getClass().getSimpleName() + ": " + safe(e.getMessage()));
        }
    }

    private void disable() { MitmProxyController.setEnabled(this, false); MitmProxyController.clear(() -> {}); MitmProxyController.stop(this); refresh(); }

    private void refresh() {
        MitmProxyServer s = MitmProxyServer.get(this);
        X509Certificate cert = new MitmCaManager(this).loadStoredCertificate();
        status.setText("Enabled: " + MitmProxyController.enabled(this) + "\nProxy: " + (s.isRunning() ? "127.0.0.1:" + s.port() : "stopped") + "\nWebView proxy override: " + MitmProxyController.proxyOverrideSupported() + "\nCA generated: " + (cert != null) + "\nCA installation: verify in Android Settings (apps cannot confirm user-CA trust reliably)" + "\nDecrypted records: " + MitmHistoryRepository.size() + (s.lastError().isEmpty() ? "" : "\nLast proxy error: " + s.lastError()));
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private static String safe(String s) { return s == null ? "unknown" : s; }
}
