package com.reqlens.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public final class BrowserHistoryActivity extends Activity {
    private final ArrayList<BrowserRequest> items = new ArrayList<>();
    private ArrayAdapter<BrowserRequest> adapter;
    private EditText filter;

    @Override public void onCreate(Bundle state) { super.onCreate(state); RequestAnnotationRepository.init(this); setContentView(buildUi()); refresh(); }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(10),dp(10),dp(10),dp(10));
        TextView title = new TextView(this); title.setText("Browser History"); title.setTextSize(22); title.setTypeface(Typeface.DEFAULT_BOLD); root.addView(title);
        TextView note = new TextView(this); note.setText("Requests visible to Android WebView. HTTPS payloads are not decrypted or bypassed."); root.addView(note);
        LinearLayout row = new LinearLayout(this);
        filter = new EditText(this); filter.setSingleLine(true); filter.setHint("Filter URL / method / status / MIME"); row.addView(filter,new LinearLayout.LayoutParams(0,dp(48),1));
        Button refresh = new Button(this); refresh.setText("FILTER"); refresh.setOnClickListener(v->refresh()); row.addView(refresh,new LinearLayout.LayoutParams(dp(100),dp(48))); root.addView(row);
        ListView list = new ListView(this);
        adapter = new ArrayAdapter<BrowserRequest>(this,android.R.layout.simple_list_item_2,android.R.id.text1,items){
            @Override public View getView(int p, View c, ViewGroup g){View v=super.getView(p,c,g);BrowserRequest x=getItem(p);RequestAnnotationRepository.Annotation an=RequestAnnotationRepository.get(x.method,x.url);((TextView)v.findViewById(android.R.id.text1)).setText((an.pinned?"★ ":"")+x.toString());String host="";try{host=new URL(x.url).getHost();}catch(Exception ignored){}String meta=(x.mainFrame?"PAGE • ":"RESOURCE • ")+host+(x.mimeType.isEmpty()?"":" • "+x.mimeType)+(an.tags.isEmpty()?"":" • "+an.tags);((TextView)v.findViewById(android.R.id.text2)).setText(meta);return v;}
        };
        list.setAdapter(adapter); list.setOnItemClickListener((a,v,p,id)->show(items.get(p))); list.setOnItemLongClickListener((a,v,p,id)->{annotate(items.get(p));return true;}); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bottom=new LinearLayout(this);
        Button browser=new Button(this);browser.setText("BROWSER");browser.setOnClickListener(v->startActivity(new Intent(this,BrowserActivity.class)));bottom.addView(browser,new LinearLayout.LayoutParams(0,dp(48),1));
        Button clear=new Button(this);clear.setText("CLEAR");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Clear browser history?").setMessage("This clears the in-app browser request list.").setNegativeButton("CANCEL",null).setPositiveButton("CLEAR",(d,w)->{BrowserHistoryRepository.clear();refresh();}).show());bottom.addView(clear,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(bottom);
        return root;
    }

    private void refresh(){String q=filter==null?"":filter.getText().toString().trim().toLowerCase(Locale.ROOT);items.clear();for(BrowserRequest x:BrowserHistoryRepository.snapshotNewestFirst()){String hay=(x.method+" "+x.statusCode+" "+x.url+" "+x.mimeType+" "+x.error).toLowerCase(Locale.ROOT);if(q.isEmpty()||hay.contains(q))items.add(x);}if(adapter!=null)adapter.notifyDataSetChanged();}

    private void show(BrowserRequest x){StringBuilder h=new StringBuilder();for(Map.Entry<String,String>e:x.headers.entrySet())h.append(e.getKey()).append(": ").append(e.getValue()).append('\n');String detail="Method: "+x.method+"\nURL: "+x.url+"\nStatus: "+(x.statusCode<0?"unknown":x.statusCode)+"\nMain frame: "+x.mainFrame+"\nMIME: "+x.mimeType+"\n\nHeaders:\n"+h;new AlertDialog.Builder(this).setTitle("Browser request").setMessage(detail).setPositiveButton("REPEATER",(d,w)->{RepeaterActivity.pending=x.toRequestRecord();startActivity(new Intent(this,RepeaterActivity.class));}).setNeutralButton("COPY URL",(d,w)->copy(x.url)).setNegativeButton("CLOSE",null).show();}

    private void annotate(BrowserRequest x){RequestAnnotationRepository.Annotation a=RequestAnnotationRepository.get(x.method,x.url);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);CheckBox pin=new CheckBox(this);pin.setText("Pin request");pin.setChecked(a.pinned);box.addView(pin);EditText tags=new EditText(this);tags.setHint("Tags, e.g. auth, api, upload");tags.setText(a.tags);box.addView(tags);EditText note=new EditText(this);note.setHint("Notes");note.setMinLines(3);note.setText(a.note);box.addView(note);new AlertDialog.Builder(this).setTitle("Request annotation").setView(box).setPositiveButton("SAVE",(d,w)->{RequestAnnotationRepository.save(x.method,x.url,pin.isChecked(),tags.getText().toString().trim(),note.getText().toString());refresh();}).setNegativeButton("CANCEL",null).show();}
    private void copy(String s){((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("ReqLens URL",s));Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
