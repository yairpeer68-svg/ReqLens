package com.reqlens.app;

public final class AppEntry implements Comparable<AppEntry> {
    public final String label;
    public final String packageName;
    public final int uid;
    public boolean selected;

    public AppEntry(String label, String packageName, int uid, boolean selected) {
        this.label = label;
        this.packageName = packageName;
        this.uid = uid;
        this.selected = selected;
    }

    @Override public int compareTo(AppEntry other) {
        int c = label.compareToIgnoreCase(other.label);
        return c != 0 ? c : packageName.compareToIgnoreCase(other.packageName);
    }

    @Override public String toString() {
        return (selected ? "✓ " : "□ ") + label + "\n" + packageName + "  •  UID " + uid;
    }
}
