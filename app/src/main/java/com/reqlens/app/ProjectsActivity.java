package com.reqlens.app;
import android.app.*;import android.graphics.Typeface;import android.os.Bundle;import android.view.*;import android.widget.*;import java.util.*;
public final class ProjectsActivity extends Activity{
 private final ArrayList<ProjectRepository.Project>items=new ArrayList<>();private ArrayAdapter<ProjectRepository.Project>a;
 @Override public void onCreate(Bundle b){super.onCreate(b);ProjectRepository.init(this);setContentView(ui());refresh();}
 private View ui(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(16,16,16,16);TextView t=new TextView(this);t.setText("Projects");t.setTextSize(22);t.setTypeface(Typeface.DEFAULT_BOLD);r.addView(t);TextView active=new TextView(this);active.setId(12345);r.addView(active);Button add=new Button(this);add.setText("NEW PROJECT");add.setOnClickListener(v->newProject());r.addView(add);ListView l=new ListView(this);a=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,items);l.setAdapter(a);l.setOnItemClickListener((p,v,pos,id)->{ProjectRepository.activate(items.get(pos).id);refresh();});r.addView(l,new LinearLayout.LayoutParams(-1,0,1));return r;}
 private void newProject(){EditText e=new EditText(this);e.setHint("Project name");new AlertDialog.Builder(this).setTitle("New project").setView(e).setPositiveButton("CREATE",(d,w)->{String n=e.getText().toString().trim();if(n.isEmpty())n="Project "+System.currentTimeMillis();ProjectRepository.add(n);refresh();}).setNegativeButton("CANCEL",null).show();}
 private void refresh(){items.clear();items.addAll(ProjectRepository.snapshot());if(a!=null)a.notifyDataSetChanged();TextView t=findViewById(12345);if(t!=null)t.setText("Active: "+ProjectRepository.active().name);}
}
