package com.reqlens.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

public final class AppAttribution {
    private final PackageManager pm;
    private final Map<Integer, String> packageCache = new HashMap<>();
    private final Map<String, String> labelCache = new HashMap<>();

    public AppAttribution(Context context) { pm = context.getPackageManager(); }

    public synchronized String packageForUid(int uid) {
        if (uid < 0) return "";
        if (packageCache.containsKey(uid)) return packageCache.get(uid);
        String[] names = pm.getPackagesForUid(uid);
        String result = names == null || names.length == 0 ? "" : names[0];
        packageCache.put(uid, result);
        return result;
    }

    public synchronized int uidForPackage(String pkg) {
        if (pkg == null || pkg.isEmpty()) return -1;
        try {
            ApplicationInfo ai;
            if (android.os.Build.VERSION.SDK_INT >= 33) ai = pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0));
            else { /*noinspection deprecation*/ ai = pm.getApplicationInfo(pkg, 0); }
            return ai.uid;
        } catch (Exception ignored) { return -1; }
    }

    public synchronized String labelForPackage(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "";
        if (labelCache.containsKey(pkg)) return labelCache.get(pkg);
        String label = pkg;
        try {
            ApplicationInfo ai;
            if (android.os.Build.VERSION.SDK_INT >= 33) ai = pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0));
            else { /*noinspection deprecation*/ ai = pm.getApplicationInfo(pkg, 0); }
            label = String.valueOf(pm.getApplicationLabel(ai));
        } catch (Exception ignored) { }
        labelCache.put(pkg, label);
        return label;
    }
}
