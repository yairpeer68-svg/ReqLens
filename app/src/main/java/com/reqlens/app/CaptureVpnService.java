package com.reqlens.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class CaptureVpnService extends VpnService {
    public static final String ACTION_START = "com.reqlens.app.capture.START";
    public static final String ACTION_STOP = "com.reqlens.app.capture.STOP";
    public static final String ACTION_STATE = "com.reqlens.app.capture.STATE";
    public static final String EXTRA_MESSAGE = "message";
    private static final int NOTIFICATION_ID = 2102;
    private static final String CHANNEL = "capture";

    private ForwardingBackend backend = new HevTun2SocksBackend();
    private ConnectionOwnerResolver ownerResolver;
    private AppAttribution attribution;
    private FlowRepository flowRepository;
    private long observedPackets;
    private CaptureSession session;
    private Set<String> activePackages = Collections.emptySet();
    private String singlePackage = "";

    @Override public void onCreate() {
        super.onCreate();
        ownerResolver = new ConnectionOwnerResolver(this);
        attribution = new AppAttribution(this);
        flowRepository = new FlowRepository(this);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "" : intent.getAction();
        if (ACTION_STOP.equals(action)) { shutdown("Stopped"); return START_NOT_STICKY; }
        if (!ACTION_START.equals(action)) return START_NOT_STICKY;

        createChannel();
        startForeground(NOTIFICATION_ID, notification("Starting capture backend"));
        Set<String> packages = new HashSet<>(getSharedPreferences("reqlens_capture", MODE_PRIVATE)
                .getStringSet("packages", Collections.emptySet()));
        packages.remove(getPackageName());
        if (packages.isEmpty()) { shutdown("No apps selected"); return START_NOT_STICKY; }
        activePackages = Collections.unmodifiableSet(new HashSet<>(packages));
        singlePackage = packages.size() == 1 ? packages.iterator().next() : "";

        if (!backend.isAvailable()) {
            shutdown("Full capture is safely blocked: " + backend.name() + ". " + backend.diagnostic());
            return START_NOT_STICKY;
        }
        try {
            session = new CaptureSession(java.util.UUID.randomUUID().toString(), System.currentTimeMillis());
            CaptureRuntime.FLOWS.clear();
            backend.start(this, packages, backendObserver);
            updateNotification("Capturing " + packages.size() + " app(s)");
            broadcast("Capture running with " + backend.name() + " for " + packages.size() + " app(s)");
            return START_STICKY;
        } catch (Throwable e) {
            shutdown("Capture start failed safely: " + e.getClass().getSimpleName() + ": " + safe(e.getMessage()));
            return START_NOT_STICKY;
        }
    }

    private final ForwardingBackend.Observer backendObserver = new ForwardingBackend.Observer() {
        @Override public void onPacket(byte[] data, int length) { observePacket(data, length); }
        @Override public void onTcpOpened(long flowId, String host, String ip, int port) { observeTcpOpen(flowId, host, ip, port); }
        @Override public void onTcpClientData(long flowId, byte[] data, int length) { observeTcpClientData(flowId, data, length); }
        @Override public void onTcpBytes(long flowId, long bytes) { observeTcpBytes(flowId, bytes); }
        @Override public void onUdpDatagram(long associationId, String host, String ip, int port, byte[] payload, int length, boolean outbound) {
            observeUdp(associationId, host, ip, port, payload, length, outbound);
        }
    };

    public void observePacket(byte[] data, int length) {
        PacketParser.ParsedPacket p = PacketParser.parse(data, length);
        if (p == null) return;
        long now = System.currentTimeMillis();
        FlowRecord r = new FlowRecord();
        r.firstSeen = now; r.lastSeen = now; r.ipVersion = p.ipVersion; r.protocol = p.protocol;
        r.protocolName = PacketParser.protocolName(p.protocol, p.quic);
        r.sourceIp = p.sourceIp; r.sourcePort = p.sourcePort; r.destinationIp = p.destinationIp; r.destinationPort = p.destinationPort;
        r.host = p.dnsName; r.tlsSni = p.tlsSni; r.tlsAlpn = p.tlsAlpn; r.tlsVersion = p.tlsVersion; r.quic = p.quic;
        r.bytes = length; r.packets = 1;
        int uid = ownerResolver == null ? -1 : ownerResolver.resolve(p); r.uid = uid;
        if (attribution != null) { r.appPackage = attribution.packageForUid(uid); r.appLabel = attribution.labelForPackage(r.appPackage); }
        store(r);
    }

    private void observeTcpOpen(long id, String host, String ip, int port) {
        FlowRecord r = baseProxyFlow("tcp:" + id, 6, host, ip, port);
        r.protocolName = "TCP"; r.packets = 1;
        store(r);
        ProxyHistoryRepository.addTcp(id, host, ip, port, r.appLabel, r.appPackage);
    }

    private void observeTcpClientData(long id, byte[] data, int length) {
        TlsClientHelloInspector.Result tls = TlsClientHelloInspector.inspect(data, length);
        if (!tls.clientHello) return;
        FlowRecord r = baseProxyFlow("tcp:" + id, 6, "", "", -1);
        r.protocolName = "TCP/TLS"; r.tlsSni = tls.sni; r.tlsAlpn = tls.alpn; r.tlsVersion = tls.tlsVersion;
        if (!tls.sni.isEmpty()) r.host = tls.sni;
        r.packets = 0; r.bytes = 0;
        store(r);
        ProxyHistoryRepository.updateTls(id, tls);
    }

    private void observeTcpBytes(long id, long bytes) {
        if (bytes <= 0) return;
        FlowRecord r = baseProxyFlow("tcp:" + id, 6, "", "", -1);
        r.protocolName = "TCP"; r.packets = 1; r.bytes = bytes;
        store(r);
        ProxyHistoryRepository.addBytes(id, bytes);
    }

    private void observeUdp(long associationId, String host, String ip, int port, byte[] payload, int length, boolean outbound) {
        if (length < 0) return;
        String tag = "udp:" + associationId + ":" + ip + ":" + port;
        FlowRecord r = baseProxyFlow(tag, 17, host, ip, port);
        r.protocolName = "UDP"; r.packets = 1; r.bytes = length;
        if (outbound && port == 53) {
            String q = DnsInspector.questionName(payload, length);
            if (!q.isEmpty()) { r.host = q; r.protocolName = "DNS/UDP"; }
        }
        if ((port == 443 || port == 784 || port == 8853) && length > 0 && (payload[0] & 0x40) != 0) {
            r.quic = true; r.protocolName = "QUIC/UDP";
        }
        store(r);
        if (outbound) ProxyHistoryRepository.addUdp(r.host, r.destinationIp, r.destinationPort, length, r.protocolName, r.appLabel, r.appPackage);
    }

    private FlowRecord baseProxyFlow(String tag, int protocol, String host, String ip, int port) {
        long now = System.currentTimeMillis();
        FlowRecord r = new FlowRecord();
        r.flowTag = tag; r.firstSeen = now; r.lastSeen = now; r.protocol = protocol;
        r.destinationIp = ip == null ? "" : ip; r.destinationPort = port;
        r.ipVersion = r.destinationIp.contains(":") ? 6 : (r.destinationIp.isEmpty() ? 0 : 4);
        r.host = host == null ? "" : host;
        if (!singlePackage.isEmpty() && attribution != null) {
            r.appPackage = singlePackage; r.appLabel = attribution.labelForPackage(singlePackage); r.uid = attribution.uidForPackage(singlePackage);
        } else if (activePackages.size() > 1) {
            r.appLabel = activePackages.size() + " selected apps";
        }
        return r;
    }

    private void store(FlowRecord r) {
        CaptureRuntime.FLOWS.observe(r);
        if (++observedPackets % 100 == 0 && flowRepository != null)
            flowRepository.save(CaptureRuntime.FLOWS.snapshotNewestFirst());
    }

    @Override public void onDestroy() { try { backend.stop(); } catch (Throwable ignored) { } super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return super.onBind(intent); }

    private void shutdown(String message) {
        if (session != null && session.isRunning()) { session.stoppedAt = System.currentTimeMillis(); session.stopReason = message; }
        try { backend.stop(); } catch (Throwable ignored) { }
        if (flowRepository != null) flowRepository.save(CaptureRuntime.FLOWS.snapshotNewestFirst());
        broadcast(message);
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
        stopSelf();
    }

    private Notification notification(String text) {
        Intent open = new Intent(this, CaptureActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL).setContentTitle("ReqLens App Capture").setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download_done).setContentIntent(pi).setOngoing(true).build();
    }
    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIFICATION_ID, notification(text));
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "ReqLens Capture", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
    private void broadcast(String message) {
        Intent i = new Intent(ACTION_STATE).setPackage(getPackageName()); i.putExtra(EXTRA_MESSAGE, message); sendBroadcast(i);
    }
    private static String safe(String s) { return s == null ? "unknown error" : s; }
}
