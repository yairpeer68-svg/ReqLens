package com.reqlens.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public final class DiagnosticsActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView title = new TextView(this); title.setText("Protection / Capture Diagnostics"); title.setTextSize(22); root.addView(title, mw());
        TextView report = new TextView(this); report.setText(buildReport()); report.setTextIsSelectable(true); report.setPadding(0, dp(12), 0, 0); root.addView(report, mw());
        setContentView(root);
    }

    private String buildReport() {
        List<FlowRecord> flows = CaptureRuntime.FLOWS.snapshotNewestFirst();
        int quic=0,tls=0,noSni=0,unknown=0,dns=0;
        for (FlowRecord r:flows) {
            if (r.quic) quic++;
            if (!r.tlsVersion.isEmpty() || r.destinationPort==443) tls++;
            if (r.destinationPort==443 && r.tlsSni.isEmpty()) noSni++;
            if (r.appPackage.isEmpty()) unknown++;
            if (!r.host.isEmpty()) dns++;
        }
        return "Backend: " + (CaptureEngineState.FORWARDER_READY ? "ready" : "not installed") +
                "\nCapabilities: " + CaptureEngineState.CAPABILITIES +
                "\n\nObserved flows: " + flows.size() +
                "\nQUIC candidates: " + quic +
                "\nTLS/443 flows: " + tls +
                "\nTLS flows without visible SNI: " + noSni +
                "\nDNS names observed: " + dns +
                "\nUnknown app attribution: " + unknown +
                "\n\nInterpretation:\n" +
                "• QUIC/HTTP3 can reduce what a TCP-only inspector sees.\n" +
                "• Missing SNI may be normal (for example ECH) and does not prove pinning.\n" +
                "• Certificate pinning, mTLS and app-layer encryption cannot be proven from passive metadata alone.\n" +
                "• Authorized Debug Mode should instrument or configure apps you own rather than silently disabling third-party protections.";
    }
    private LinearLayout.LayoutParams mw(){return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);} private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
