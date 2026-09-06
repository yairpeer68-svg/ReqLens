package com.reqlens.app;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.VpnService;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import com.google.android.material.card.MaterialCardView;
import java.util.*;

public final class CaptureActivity extends Activity {
    private static final int VPN_REQUEST=301;
    private boolean waitingForVpnPermission=false;
    private long vpnRequestStartedAt=0L;
    private final ArrayList<AppEntry> all=new ArrayList<>(), visible=new ArrayList<>();
    private AppAdapter adapter; private TextView selectedText,status; private CheckBox mitm;

    private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){String m=i.getStringExtra(CaptureVpnService.EXTRA_MESSAGE);if(status!=null&&m!=null)status.setText(m);}};

    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(ui());loadApps();}
    @Override protected void onStart(){super.onStart();IntentFilter f=new IntentFilter(CaptureVpnService.ACTION_STATE);if(android.os.Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);}
    @Override protected void onResume(){super.onResume();if(waitingForVpnPermission){Intent check=VpnService.prepare(this);if(check==null){waitingForVpnPermission=false;status.setText("VPN permission granted. Starting session…");startServiceNow();}else if(System.currentTimeMillis()-vpnRequestStartedAt>1200){status.setText("Android returned without granting VPN permission. Tap START SESSION again; if no system dialog appears, open Android Settings > VPN and remove any stale ReqLens VPN entry, then retry.");}}}
    @Override protected void onStop(){try{unregisterReceiver(receiver);}catch(Exception ignored){}super.onStop();}

    private View ui(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(16),dp(16),dp(16));
        TextView title=text("App Session",28,true);root.addView(title);
        TextView sub=text("Pick one target app. Only that app is routed through ReqLens.",14,false);sub.setAlpha(.7f);sub.setPadding(0,0,0,dp(14));root.addView(sub);

        MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(22));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(16),dp(16),dp(16));
        box.addView(text("CURRENT TARGET",12,true)); selectedText=text("No app selected",19,true);selectedText.setPadding(0,dp(6),0,dp(8));box.addView(selectedText);
        mitm=new CheckBox(this);mitm.setText("Decrypt HTTPS for this app when it trusts the ReqLens CA");mitm.setChecked(getSharedPreferences("reqlens_capture",MODE_PRIVATE).getBoolean("app_mitm",false));box.addView(mitm);
        TextView note=text("HTTPS decryption is app-specific. Apps that reject user CAs, use certificate pinning, or require mTLS will not decrypt; ReqLens does not bypass those protections.",12,false);note.setAlpha(.68f);box.addView(note);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button ca=new Button(this);ca.setText("CA SETUP");ca.setOnClickListener(v->startActivity(new Intent(this,MitmActivity.class)));actions.addView(ca,new LinearLayout.LayoutParams(0,dp(52),1));
        Button start=new Button(this);start.setText("START SESSION");start.setOnClickListener(v->prepareVpn());actions.addView(start,new LinearLayout.LayoutParams(0,dp(52),1));box.addView(actions);
        LinearLayout actions2=new LinearLayout(this);actions2.setOrientation(LinearLayout.HORIZONTAL);
        Button stop=new Button(this);stop.setText("STOP");stop.setOnClickListener(v->stopCapture());actions2.addView(stop,new LinearLayout.LayoutParams(0,dp(48),1));
        Button live=new Button(this);live.setText("LIVE TRAFFIC");live.setOnClickListener(v->startActivity(new Intent(this,LiveCaptureActivity.class)));actions2.addView(live,new LinearLayout.LayoutParams(0,dp(48),1));box.addView(actions2);
        status=text("Idle",13,false);status.setPadding(0,dp(8),0,0);box.addView(status);card.addView(box);root.addView(card,new LinearLayout.LayoutParams(-1,-2));

        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Search installed apps");search.setPadding(dp(12),dp(8),dp(12),dp(8));root.addView(search,new LinearLayout.LayoutParams(-1,dp(54)));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){filter(s.toString());}public void afterTextChanged(Editable e){}});
        TextView pick=text("SELECT TARGET APP",12,true);pick.setAlpha(.65f);pick.setPadding(0,dp(8),0,dp(6));root.addView(pick);
        ListView list=new ListView(this);adapter=new AppAdapter();list.setAdapter(adapter);list.setDividerHeight(0);list.setOnItemClickListener((p,v,pos,id)->select(visible.get(pos)));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        return root;
    }

    private void select(AppEntry hit){for(AppEntry a:all)a.selected=a.packageName.equals(hit.packageName);persistSelection(hit);adapter.notifyDataSetChanged();refreshSelected();}
    private void persistSelection(AppEntry a){HashSet<String> one=new HashSet<>();one.add(a.packageName);getSharedPreferences("reqlens_capture",MODE_PRIVATE).edit().putStringSet("packages",one).putString("selected_package",a.packageName).putString("selected_label",a.label).apply();}
    private void refreshSelected(){AppEntry s=selected();selectedText.setText(s==null?"No app selected":s.label+"\n"+s.packageName);}
    private AppEntry selected(){for(AppEntry a:all)if(a.selected)return a;return null;}
    private void filter(String q){String x=q==null?"":q.trim().toLowerCase(Locale.ROOT);visible.clear();for(AppEntry a:all)if(x.isEmpty()||a.label.toLowerCase(Locale.ROOT).contains(x)||a.packageName.toLowerCase(Locale.ROOT).contains(x))visible.add(a);if(adapter!=null)adapter.notifyDataSetChanged();}

    private void prepareVpn(){AppEntry s=selected();if(s==null){new AlertDialog.Builder(this).setMessage("Select one target app first.").setPositiveButton("OK",null).show();return;}boolean decrypt=mitm.isChecked();getSharedPreferences("reqlens_capture",MODE_PRIVATE).edit().putBoolean("app_mitm",decrypt).apply();if(decrypt&&new MitmCaManager(this).loadStoredCertificate()==null){new AlertDialog.Builder(this).setTitle("ReqLens CA required").setMessage("Generate/export and install the ReqLens CA first. Then return and start the app session.").setPositiveButton("OPEN CA SETUP",(d,w)->startActivity(new Intent(this,MitmActivity.class))).setNegativeButton("CANCEL",null).show();return;}try{Intent i=VpnService.prepare(this);if(i==null){status.setText("VPN permission already granted. Starting session…");startServiceNow();return;}waitingForVpnPermission=true;vpnRequestStartedAt=System.currentTimeMillis();status.setText("Waiting for Android VPN confirmation…");startActivityForResult(i,VPN_REQUEST);}catch(Exception e){waitingForVpnPermission=false;status.setText("Could not open Android VPN confirmation: "+e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));}}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=VPN_REQUEST)return;Intent check=VpnService.prepare(this);if(result==RESULT_OK||check==null){waitingForVpnPermission=false;status.setText("VPN permission granted. Starting session…");startServiceNow();}else{waitingForVpnPermission=false;long elapsed=System.currentTimeMillis()-vpnRequestStartedAt;status.setText("Android VPN confirmation returned without permission (result="+result+", "+elapsed+" ms). No session was started.");}}
    private void startServiceNow(){status.setText("Starting app-only VPN session…");Intent i=new Intent(this,CaptureVpnService.class).setAction(CaptureVpnService.ACTION_START);startForegroundService(i);}
    private void stopCapture(){Intent i=new Intent(this,CaptureVpnService.class).setAction(CaptureVpnService.ACTION_STOP);startService(i);status.setText("Stopping…");}

    private void loadApps(){all.clear();String wanted=getSharedPreferences("reqlens_capture",MODE_PRIVATE).getString("selected_package","");PackageManager pm=getPackageManager();List<ApplicationInfo> installed=android.os.Build.VERSION.SDK_INT>=33?pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0)):pm.getInstalledApplications(0);for(ApplicationInfo ai:installed){if(ai.packageName.equals(getPackageName())||pm.getLaunchIntentForPackage(ai.packageName)==null)continue;String label=String.valueOf(pm.getApplicationLabel(ai));all.add(new AppEntry(label,ai.packageName,ai.uid,ai.packageName.equals(wanted)));}Collections.sort(all);visible.clear();visible.addAll(all);adapter.notifyDataSetChanged();refreshSelected();}

    private final class AppAdapter extends BaseAdapter{
        public int getCount(){return visible.size();}public Object getItem(int p){return visible.get(p);}public long getItemId(int p){return visible.get(p).uid;}
        public View getView(int p,View reuse,ViewGroup parent){AppEntry a=visible.get(p);LinearLayout row=new LinearLayout(CaptureActivity.this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(9),dp(10),dp(9));ImageView icon=new ImageView(CaptureActivity.this);try{Drawable d=getPackageManager().getApplicationIcon(a.packageName);icon.setImageDrawable(d);}catch(Exception ignored){}row.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));LinearLayout txt=new LinearLayout(CaptureActivity.this);txt.setOrientation(LinearLayout.VERTICAL);txt.setPadding(dp(12),0,0,0);TextView l=text((a.selected?"✓  ":"")+a.label,16,a.selected);TextView pkg=text(a.packageName,12,false);pkg.setAlpha(.58f);txt.addView(l);txt.addView(pkg);row.addView(txt,new LinearLayout.LayoutParams(0,-2,1));return row;}
    }
    private TextView text(String s,int z,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);if(b)t.setTypeface(Typeface.DEFAULT_BOLD);return t;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
