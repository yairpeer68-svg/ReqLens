package com.reqlens.app;

public final class TextDiff {
    private TextDiff() {}
    public static String summarize(String a,String b){ if(a==null)a=""; if(b==null)b=""; if(a.equals(b))return "No differences."; String[]x=a.split("\\R",-1),y=b.split("\\R",-1);StringBuilder s=new StringBuilder();int max=Math.max(x.length,y.length),shown=0;for(int i=0;i<max&&shown<120;i++){String l=i<x.length?x[i]:null,r=i<y.length?y[i]:null;if(l==null||r==null||!l.equals(r)){s.append("@@ line ").append(i+1).append(" @@\n");if(l!=null)s.append("- ").append(l).append('\n');if(r!=null)s.append("+ ").append(r).append('\n');shown++;}}if(shown>=120)s.append("[diff truncated]\n");return s.toString();}
}
