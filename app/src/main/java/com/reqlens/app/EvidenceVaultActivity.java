package com.reqlens.app;

import android.app.*;import android.os.Bundle;import android.view.*;import android.widget.*;import java.util.*;
public final class EvidenceVaultActivity extends Activity{
 private ArrayList<EvidenceRepository.Item>items=new ArrayList<>();private ArrayAdapter<EvidenceRepository.Item>a;
 @Override public void onCreate(Bundle b){super.onCreate(b);EvidenceRepository.init(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(16,16,16,16);TextView t=new TextView(this);t.setText("Evidence Vault");t.setTextSize(24);r.addView(t);ListView l=new ListView(this);a=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,items);l.setAdapter(a);l.setOnItemClickListener((p,v,pos,id)->show(items.get(pos)));r.addView(l,new LinearLayout.LayoutParams(-1,0,1));Button clear=new Button(this);clear.setText("CLEAR VAULT");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Clear evidence vault?").setMessage("This removes locally saved research evidence.").setNegativeButton("CANCEL",null).setPositiveButton("CLEAR",(d,w)->{EvidenceRepository.clear();refresh();}).show());r.addView(clear);setContentView(r);refresh();}
 private void refresh(){items.clear();items.addAll(EvidenceRepository.snapshotNewest());if(a!=null)a.notifyDataSetChanged();}
 private void show(EvidenceRepository.Item x){new AlertDialog.Builder(this).setTitle(x.title).setMessage("SHA-256: "+x.hash+"\n\n"+x.body).setPositiveButton("CLOSE",null).show();}
}
