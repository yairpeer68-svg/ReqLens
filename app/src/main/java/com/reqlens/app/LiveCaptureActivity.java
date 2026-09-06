package com.reqlens.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class LiveCaptureActivity extends Activity {
    private final ArrayList<FlowRecord> visible = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ArrayAdapter<FlowRecord> adapter;
    private EditText filter;
    private TextView summary;
    private final Runnable refresh = new Runnable() {
        @Override public void run() { applyFilter(); handler.postDelayed(this, 1000); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildUi());
    }
    @Override protected void onStart() { super.onStart(); handler.post(refresh); }
    @Override protected void onStop() { handler.removeCallbacks(refresh); super.onStop(); }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12),dp(12),dp(12),dp(12));
        TextView title = new TextView(this); title.setText("Live Connections"); title.setTextSize(22); root.addView(title, mw());
        summary = new TextView(this); root.addView(summary, mw());
        filter = new EditText(this); filter.setHint("Filter app / host / IP / protocol / port"); filter.setSingleLine(true); root.addView(filter, mw());
        LinearLayout actions = new LinearLayout(this);
        Button apply = new Button(this); apply.setText("FILTER"); apply.setOnClickListener(v -> applyFilter()); actions.addView(apply,new LinearLayout.LayoutParams(0,dp(48),1));
        Button export = new Button(this); export.setText("EXPORT"); export.setOnClickListener(v -> chooseExport()); actions.addView(export,new LinearLayout.LayoutParams(0,dp(48),1));
        Button clear = new Button(this); clear.setText("CLEAR"); clear.setOnClickListener(v -> { CaptureRuntime.FLOWS.clear(); applyFilter(); }); actions.addView(clear,new LinearLayout.LayoutParams(0,dp(48),1));
        root.addView(actions,mw());
        ListView list = new ListView(this);
        adapter = new ArrayAdapter<FlowRecord>(this, android.R.layout.simple_list_item_2, android.R.id.text1, visible) {
            @Override public View getView(int pos, View convert, ViewGroup parent) {
                View v=super.getView(pos,convert,parent); FlowRecord r=getItem(pos);
                TextView a=v.findViewById(android.R.id.text1), b=v.findViewById(android.R.id.text2);
                String app=r.appLabel.isEmpty() ? (r.appPackage.isEmpty()?"Unknown app":r.appPackage) : r.appLabel;
                String host=!r.tlsSni.isEmpty()?r.tlsSni:(!r.host.isEmpty()?r.host:r.destinationIp);
                a.setText(app + "  •  " + r.protocolName + "  •  " + host + ":" + r.destinationPort);
                b.setText(r.destinationIp + "  •  " + r.packets + " packets  •  " + r.bytes + " B" + (r.quic?"  •  QUIC":""));
                return v;
            }
        };
        list.setAdapter(adapter); list.setOnItemClickListener((p,v,pos,id)->showDetails(visible.get(pos)));
        root.addView(list,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        return root;
    }

    private void applyFilter() {
        String q=filter==null?"":filter.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<FlowRecord> all=CaptureRuntime.FLOWS.snapshotNewestFirst(); visible.clear();
        for (FlowRecord r:all) {
            String hay=(r.appLabel+" "+r.appPackage+" "+r.protocolName+" "+r.host+" "+r.tlsSni+" "+r.destinationIp+" "+r.destinationPort).toLowerCase(Locale.ROOT);
            if (q.isEmpty() || hay.contains(q)) visible.add(r);
        }
        if (adapter!=null) adapter.notifyDataSetChanged();
        if (summary!=null) { SessionStats st=SessionStats.from(all); summary.setText("Flows: "+st.flows+"  •  Apps: "+st.apps+"  •  Packets: "+st.packets+"  •  Bytes: "+st.bytes+"  •  QUIC: "+st.quicFlows); }
    }

    private void showDetails(FlowRecord r) {
        StringBuilder details=new StringBuilder();
        details.append("App: ").append(r.appLabel).append("\nPackage: ").append(r.appPackage).append("\nUID: ").append(r.uid)
                .append("\nProtocol: ").append(r.protocolName).append("\nSource: ").append(r.sourceIp).append(":").append(r.sourcePort)
                .append("\nDestination: ").append(r.destinationIp).append(":").append(r.destinationPort)
                .append("\nDNS host: ").append(r.host).append("\nTLS SNI: ").append(r.tlsSni).append("\nALPN: ").append(r.tlsAlpn)
                .append("\nTLS: ").append(r.tlsVersion).append("\nQUIC: ").append(r.quic).append("\nPackets: ").append(r.packets)
                .append("\nBytes: ").append(r.bytes).append("\nFirst: ").append(DateFormat.getDateTimeInstance().format(new Date(r.firstSeen)));
        java.util.List<String> signals=ProtectionSignals.infer(r);
        if(!signals.isEmpty()){ details.append("\n\nSignals:"); for(String signal:signals) details.append("\n• ").append(signal); }
        String text=details.toString();
        new AlertDialog.Builder(this).setTitle("Flow details").setMessage(text).setPositiveButton("OK",null).show();
    }

    private void chooseExport() {
        new AlertDialog.Builder(this).setTitle("Export flows")
                .setItems(new String[]{"Safe JSON (redacted)", "CSV"}, (d, which) -> {
                    if (which == 0) exportJsonSafe(); else exportCsv();
                }).show();
    }

    private void exportJsonSafe() {
        try {
            File f=ExportManager.writeJson(this, CaptureRuntime.FLOWS.snapshotNewestFirst(), true);
            startActivity(Intent.createChooser(ExportManager.share(this, f, "application/json"), "Share ReqLens export"));
        } catch(Exception e) { Toast.makeText(this,"Export failed: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }

    private void exportCsv() {
        try {
            File f=ExportManager.writeCsv(this, CaptureRuntime.FLOWS.snapshotNewestFirst());
            startActivity(Intent.createChooser(ExportManager.share(this, f, "text/csv"), "Share ReqLens export"));
        } catch(Exception e) { Toast.makeText(this,"Export failed: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }
    private LinearLayout.LayoutParams mw(){return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);} private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
