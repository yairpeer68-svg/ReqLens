package com.reqlens.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CaptureActivity extends Activity {
    private static final int VPN_REQUEST = 301;
    private final ArrayList<AppEntry> apps = new ArrayList<>();
    private ArrayAdapter<AppEntry> adapter;
    private TextView status;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (status != null) status.setText(intent.getStringExtra(CaptureVpnService.EXTRA_MESSAGE));
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildUi());
        loadApps();
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter(CaptureVpnService.ACTION_STATE);
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, f);
    }

    @Override protected void onStop() {
        try { unregisterReceiver(receiver); } catch (Exception ignored) { }
        super.onStop();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView title = new TextView(this);
        title.setText("App Capture Mode");
        title.setTextSize(22);
        root.addView(title, matchWrap());

        status = new TextView(this);
        status.setText(CaptureEngineState.STATUS + "\n" + CaptureEngineState.CAPABILITIES);
        status.setPadding(0, dp(8), 0, dp(8));
        root.addView(status, matchWrap());

        LinearLayout buttons = new LinearLayout(this);
        Button start = new Button(this);
        start.setText("START CAPTURE");
        start.setOnClickListener(v -> prepareVpn());
        buttons.addView(start, new LinearLayout.LayoutParams(0, dp(50), 1));
        Button stop = new Button(this);
        stop.setText("STOP");
        stop.setOnClickListener(v -> stopCapture());
        buttons.addView(stop, new LinearLayout.LayoutParams(0, dp(50), 1));
        root.addView(buttons, matchWrap());

        Button live = new Button(this);
        live.setText("LIVE CONNECTIONS");
        live.setOnClickListener(v -> startActivity(new Intent(this, LiveCaptureActivity.class)));
        root.addView(live, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        Button diagnostics = new Button(this);
        diagnostics.setText("CAPTURE DIAGNOSTICS");
        diagnostics.setOnClickListener(v -> startActivity(new Intent(this, DiagnosticsActivity.class)));
        root.addView(diagnostics, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        TextView hint = new TextView(this);
        hint.setText("Choose apps to route through ReqLens. For exact app attribution, select one app at a time; multi-app capture is supported but flows are grouped when Android cannot reliably recover the original UID after tun2socks translation.");
        root.addView(hint, matchWrap());

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<AppEntry>(this, android.R.layout.simple_list_item_1, apps);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            AppEntry app = apps.get(pos);
            app.selected = !app.selected;
            saveSelection();
            adapter.notifyDataSetChanged();
        });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    private void prepareVpn() {
        if (selectedPackages().isEmpty()) {
            new AlertDialog.Builder(this).setMessage("Select at least one app first.").setPositiveButton("OK", null).show();
            return;
        }
        Intent intent = VpnService.prepare(this);
        if (intent != null) startActivityForResult(intent, VPN_REQUEST);
        else startCaptureService();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST && resultCode == RESULT_OK) startCaptureService();
        else if (requestCode == VPN_REQUEST) status.setText("VPN permission was not granted.");
    }

    private void startCaptureService() {
        getSharedPreferences("reqlens_capture", MODE_PRIVATE).edit()
                .putStringSet("packages", selectedPackages()).apply();
        Intent i = new Intent(this, CaptureVpnService.class).setAction(CaptureVpnService.ACTION_START);
        startForegroundService(i);
    }

    private void stopCapture() {
        Intent i = new Intent(this, CaptureVpnService.class).setAction(CaptureVpnService.ACTION_STOP);
        startService(i);
    }

    private void loadApps() {
        apps.clear();
        Set<String> selected = getSharedPreferences("reqlens_capture", MODE_PRIVATE)
                .getStringSet("packages", Collections.emptySet());
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installed;
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            installed = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0));
        } else {
            //noinspection deprecation
            installed = pm.getInstalledApplications(0);
        }
        for (ApplicationInfo ai : installed) {
            if (ai.packageName.equals(getPackageName())) continue;
            Intent launch = pm.getLaunchIntentForPackage(ai.packageName);
            if (launch == null) continue;
            String label = String.valueOf(pm.getApplicationLabel(ai));
            apps.add(new AppEntry(label, ai.packageName, ai.uid, selected.contains(ai.packageName)));
        }
        Collections.sort(apps);
        adapter.notifyDataSetChanged();
    }

    private Set<String> selectedPackages() {
        HashSet<String> out = new HashSet<>();
        for (AppEntry app : apps) if (app.selected) out.add(app.packageName);
        return out;
    }

    private void saveSelection() {
        getSharedPreferences("reqlens_capture", MODE_PRIVATE).edit().putStringSet("packages", selectedPackages()).apply();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
