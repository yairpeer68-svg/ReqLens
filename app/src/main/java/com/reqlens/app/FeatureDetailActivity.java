package com.reqlens.app;

import android.app.*;import android.content.*;import android.graphics.Typeface;import android.os.Bundle;import android.text.InputType;import android.view.*;import android.widget.*;

public final class FeatureDetailActivity extends Activity{
 private int id;private EditText input;private TextView out;
 @Override public void onCreate(Bundle b){super.onCreate(b);id=getIntent().getIntExtra("featureId",1);id=Math.max(1,Math.min(80,id));EvidenceRepository.init(this);setContentView(ui());run();}
 private View ui(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(16),dp(16),dp(16));TextView title=new TextView(this);title.setText(FeatureCatalog.NAMES[id-1]);title.setTextSize(24);title.setTypeface(Typeface.DEFAULT_BOLD);root.addView(title);TextView cat=new TextView(this);cat.setText(FeatureCatalog.group(id)+" • Feature "+id+"/80");cat.setAlpha(.65f);root.addView(cat);
  input=new EditText(this);input.setHint("Optional input / query / schema / comparison data");input.setMinLines(2);input.setGravity(Gravity.TOP);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);root.addView(input,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout row=new LinearLayout(this);Button run=new Button(this);run.setText("RUN ANALYSIS");run.setOnClickListener(v->run());row.addView(run,new LinearLayout.LayoutParams(0,dp(50),1));Button save=new Button(this);save.setText("SAVE FINDING");save.setOnClickListener(v->save());row.addView(save,new LinearLayout.LayoutParams(0,dp(50),1));Button copy=new Button(this);copy.setText("COPY");copy.setOnClickListener(v->copy());row.addView(copy,new LinearLayout.LayoutParams(0,dp(50),1));root.addView(row);
  out=new TextView(this);out.setTextIsSelectable(true);out.setTypeface(Typeface.MONOSPACE);out.setTextSize(13);out.setPadding(0,dp(12),0,dp(20));ScrollView sc=new ScrollView(this);sc.addView(out);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));return root;}
 private void run(){try{out.setText(FeatureEngine.report(this,id,input.getText().toString()));}catch(Throwable e){out.setText("Analysis failed safely: "+e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));}}
 private void save(){String body=out.getText().toString();EvidenceRepository.Item x=EvidenceRepository.add(id,FeatureCatalog.NAMES[id-1],body);Toast.makeText(this,"Saved evidence • "+x.hash.substring(0,Math.min(12,x.hash.length())),Toast.LENGTH_LONG).show();}
 private void copy(){android.content.ClipboardManager c=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);c.setPrimaryClip(android.content.ClipData.newPlainText("ReqLens analysis",out.getText()));Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show();}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
