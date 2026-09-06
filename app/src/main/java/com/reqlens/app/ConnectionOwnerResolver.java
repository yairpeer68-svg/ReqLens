package com.reqlens.app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.system.OsConstants;

import java.net.InetSocketAddress;

public final class ConnectionOwnerResolver {
    private final ConnectivityManager cm;
    public ConnectionOwnerResolver(Context context) { cm = context.getSystemService(ConnectivityManager.class); }

    public int resolve(PacketParser.ParsedPacket p) {
        if (Build.VERSION.SDK_INT < 29 || p == null || p.sourcePort < 0 || p.destinationPort < 0) return -1;
        try {
            int proto = p.protocol == 6 ? OsConstants.IPPROTO_TCP : p.protocol == 17 ? OsConstants.IPPROTO_UDP : -1;
            if (proto < 0) return -1;
            InetSocketAddress local = new InetSocketAddress(p.sourceIp, p.sourcePort);
            InetSocketAddress remote = new InetSocketAddress(p.destinationIp, p.destinationPort);
            return cm.getConnectionOwnerUid(proto, local, remote);
        } catch (Exception ignored) { return -1; }
    }
}
