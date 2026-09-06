package com.reqlens.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

public final class MainActivity extends Activity {
    private TextView targetText;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ProjectRepository.init(this);
        RequestAnnotationRepository.init(this);
        BrowserHistoryRepository.init(this);
        ProxyHistoryRepository.init(this);
        setContentView(buildUi());
    }

    @Override protected void onResume() { super.onResume(); refreshTarget(); }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        scroll.addView(root);

        TextView brand = text("ReqLens", 30, true);
        root.addView(brand);
        TextView sub = text("Mobile traffic lab • app-focused capture", 14, false);
        sub.setAlpha(.72f); sub.setPadding(0, 0, 0, dp(18)); root.addView(sub);

        MaterialCardView target = card();
        LinearLayout targetBox = vertical(16);
        TextView targetTitle = text("TARGET APP", 12, true); targetTitle.setAlpha(.65f); targetBox.addView(targetTitle);
        targetText = text("No app selected", 20, true); targetText.setPadding(0, dp(6), 0, dp(10)); targetBox.addView(targetText);
        TextView targetNote = text("Choose one app. ReqLens routes only that app through the local VPN session.", 13, false); targetNote.setAlpha(.74f); targetBox.addView(targetNote);
        Button choose = button("SELECT APP", CaptureActivity.class); targetBox.addView(choose);
        target.addView(targetBox); root.addView(target, matchWrap(0, dp(10)));

        MaterialCardView inspect = card();
        LinearLayout box = vertical(16);
        box.addView(text("INSPECT", 12, true));
        LinearLayout row1 = row();
        row1.addView(buttonWeight("LIVE", LiveCaptureActivity.class));
        row1.addView(buttonWeight("HISTORY", ProxyHistoryActivity.class));
        row1.addView(buttonWeight("REPEATER", RepeaterActivity.class));
        box.addView(row1);
        LinearLayout row2 = row();
        row2.addView(buttonWeight("INTERCEPT", InterceptActivity.class));
        row2.addView(buttonWeight("RULES", RulesActivity.class));
        row2.addView(buttonWeight("MITM", MitmActivity.class));
        box.addView(row2);
        inspect.addView(box); root.addView(inspect, matchWrap(0, dp(10)));

        MaterialCardView workspace = card();
        LinearLayout w = vertical(16);
        w.addView(text("WORKSPACE", 12, true));
        LinearLayout wr1=row(); wr1.addView(buttonWeight("BROWSER", BrowserActivity.class)); wr1.addView(buttonWeight("DASHBOARD", DashboardActivity.class)); w.addView(wr1);
        LinearLayout wr2=row(); wr2.addView(buttonWeight("PROJECTS", ProjectsActivity.class)); wr2.addView(buttonWeight("TOOLS", ToolsActivity.class)); w.addView(wr2);
        LinearLayout wr3=row(); wr3.addView(buttonWeight("RESEARCH 80", ResearchSuiteActivity.class)); wr3.addView(buttonWeight("EVIDENCE", EvidenceVaultActivity.class)); w.addView(wr3);
        workspace.addView(w); root.addView(workspace, matchWrap(0, dp(10)));

        TextView footer = text("Authorized testing only • no pinning or mTLS bypass", 12, false);
        footer.setGravity(Gravity.CENTER); footer.setAlpha(.55f); footer.setPadding(0, dp(12), 0, 0); root.addView(footer);
        refreshTarget();
        return scroll;
    }

    private void refreshTarget() {
        if (targetText == null) return;
        SharedPreferences p=getSharedPreferences("reqlens_capture", MODE_PRIVATE);
        String label=p.getString("selected_label", "");
        String pkg=p.getString("selected_package", "");
        boolean mitm=p.getBoolean("app_mitm", false);
        if (pkg==null || pkg.isEmpty()) targetText.setText("No app selected");
        else targetText.setText((label==null||label.isEmpty()?pkg:label) + (mitm?"  •  HTTPS decrypt ON":""));
    }

    private MaterialCardView card(){ MaterialCardView c=new MaterialCardView(this); c.setRadius(dp(22)); c.setCardElevation(dp(1)); c.setUseCompatPadding(false); return c; }
    private LinearLayout vertical(int pad){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(pad),dp(pad),dp(pad),dp(pad)); return l; }
    private LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private TextView text(String s,int size,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); if(bold)t.setTypeface(Typeface.DEFAULT_BOLD); return t; }
    private Button button(String s,Class<?> cls){ Button b=new Button(this); b.setText(s); b.setOnClickListener(v->open(cls)); return b; }
    private Button buttonWeight(String s,Class<?> cls){ Button b=button(s,cls); b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(52),1)); return b; }
    private void open(Class<?> cls){ if(cls==RepeaterActivity.class) RepeaterActivity.pending=new RequestRecord(); startActivity(new Intent(this,cls)); }
    private LinearLayout.LayoutParams matchWrap(int top,int bottom){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(top),0,dp(bottom)); return p; }
    private int dp(int n){ return Math.round(n*getResources().getDisplayMetrics().density); }
}
