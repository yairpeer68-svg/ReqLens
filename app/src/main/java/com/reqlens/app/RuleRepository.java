package com.reqlens.app;

import java.util.ArrayList;import java.util.List;
public final class RuleRepository {
 private static final ArrayList<InterceptRule> RULES=new ArrayList<>(); private RuleRepository(){}
 public static synchronized List<InterceptRule> snapshot(){return new ArrayList<>(RULES);}
 public static synchronized void add(InterceptRule r){RULES.add(r);}
 public static synchronized void remove(int i){if(i>=0&&i<RULES.size())RULES.remove(i);}
 public static synchronized void toggle(int i){if(i>=0&&i<RULES.size())RULES.get(i).enabled=!RULES.get(i).enabled;}
 public static synchronized void move(int i,int delta){int j=i+delta;if(i<0||i>=RULES.size()||j<0||j>=RULES.size())return;InterceptRule x=RULES.remove(i);RULES.add(j,x);}
 public static synchronized RequestRecord apply(RequestRecord input){RequestRecord r=RequestExecutor.cloneRequest(input);for(InterceptRule x:RULES)x.apply(r);return r;}
}
