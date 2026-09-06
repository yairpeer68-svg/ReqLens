package com.reqlens.app;
import android.app.*;import android.graphics.Typeface;import android.os.Bundle;import android.view.*;import android.widget.*;import java.util.*;
public final class WebSocketHistoryActivity extends Activity{
 private final ArrayList<WebSocketHistoryRepository.Event>items=new ArrayList<>();private ArrayAdapter<WebSocketHistoryRepository.Event>a;
 @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(ui());refresh();}
 private View ui(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(16,16,16,16);TextView t=new TextView(this);t.setText("WebSocket History");t.setTextSize(22);t.setTypeface(Typeface.DEFAULT_BOLD);r.addView(t);TextView n=new TextView(this);n.setText("Tracks WebSocket upgrade handshakes visible to ReqLens. Encrypted third-party frame contents are not decrypted by passive capture.");r.addView(n);Button ref=new Button(this);ref.setText("REFRESH");ref.setOnClickListener(v->refresh());r.addView(ref);ListView l=new ListView(this);a=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,items);l.setAdapter(a);r.addView(l,new LinearLayout.LayoutParams(-1,0,1));return r;}
 private void refresh(){items.clear();items.addAll(WebSocketHistoryRepository.snapshot());Collections.reverse(items);if(a!=null)a.notifyDataSetChanged();}
}
