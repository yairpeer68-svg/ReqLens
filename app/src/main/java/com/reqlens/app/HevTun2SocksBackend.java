package com.reqlens.app;

import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import com.zaneschepke.hevtunnel.TProxyService;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class HevTun2SocksBackend implements ForwardingBackend {
    private static final int MTU = 1500;
    private static final String VPN4 = "198.18.0.1";
    private static final String VPN6 = "fd00:1:fd00:1::1";
    private ParcelFileDescriptor tun;
    private LocalSocks5Server socks;
    private volatile boolean started;
    private volatile String availabilityError = "";

    @Override public boolean isAvailable() {
        try {
            Class.forName("com.zaneschepke.hevtunnel.TProxyService", true, HevTun2SocksBackend.class.getClassLoader());
            availabilityError = "";
            return true;
        } catch (Throwable t) {
            availabilityError = t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
            return false;
        }
    }
    @Override public String name() { return "HEV tun2socks + protected local SOCKS5"; }
    @Override public String diagnostic() {
        if (!availabilityError.isEmpty()) return "HEV native backend unavailable on this device: " + availabilityError;
        return "Embedded HEV userspace TCP/IP forwarding is enabled. TCP and UDP exit through a local SOCKS5 relay whose upstream sockets are protected from VPN recursion.";
    }

    @Override public synchronized void start(VpnService service, Set<String> packages, Observer observer) throws Exception {
        if (started) throw new IllegalStateException("Backend already running");
        if (packages == null || packages.isEmpty()) throw new IllegalArgumentException("No selected apps");
        if (packages.contains(service.getPackageName())) throw new IllegalArgumentException("ReqLens cannot capture itself");

        ConnectivityManager cm = (ConnectivityManager) service.getSystemService(VpnService.CONNECTIVITY_SERVICE);
        Network underlying = cm == null ? null : cm.getActiveNetwork();
        LinkProperties links = (cm == null || underlying == null) ? null : cm.getLinkProperties(underlying);

        LocalSocks5Server.Resolver resolver = host -> {
            InetAddress[] all;
            if (underlying != null) all = underlying.getAllByName(host);
            else all = InetAddress.getAllByName(host);
            if (all.length == 0) throw new java.net.UnknownHostException(host);
            return all[0];
        };
        socks = new LocalSocks5Server(new LocalSocks5Server.Protector() {
            @Override public boolean protect(java.net.Socket socket) { return service.protect(socket); }
            @Override public boolean protect(java.net.DatagramSocket socket) { return service.protect(socket); }
        }, resolver, new LocalSocks5Server.Observer() {
            @Override public void onTcpOpened(long id, String host, String ip, int port) { observer.onTcpOpened(id, host, ip, port); }
            @Override public void onTcpClientData(long id, byte[] data, int length) { observer.onTcpClientData(id, data, length); }
            @Override public void onTcpBytes(long id, long bytes) { observer.onTcpBytes(id, bytes); }
            @Override public void onUdpDatagram(long id, String host, String ip, int port, byte[] payload, int length, boolean outbound) {
                observer.onUdpDatagram(id, host, ip, port, payload, length, outbound);
            }
        });
        int socksPort = socks.start();

        try {
            VpnService.Builder b = service.new Builder()
                    .setSession("ReqLens App Capture")
                    .setMtu(MTU)
                    .addAddress(VPN4, 32)
                    .addRoute("0.0.0.0", 0);
            if (underlying != null) b.setUnderlyingNetworks(new Network[]{underlying});
            try { b.addAddress(VPN6, 128).addRoute("::", 0); } catch (IllegalArgumentException ignored) { }
            if (links != null) {
                for (InetAddress dns : links.getDnsServers()) {
                    try { b.addDnsServer(dns); } catch (IllegalArgumentException ignored) { }
                }
            }
            int allowed = 0;
            for (String pkg : packages) {
                if (pkg == null || pkg.isEmpty() || pkg.equals(service.getPackageName())) continue;
                try { b.addAllowedApplication(pkg); allowed++; }
                catch (PackageManager.NameNotFoundException ignored) { }
            }
            if (allowed == 0) throw new IllegalStateException("None of the selected packages are installed");
            tun = b.establish();
            if (tun == null) throw new IllegalStateException("VpnService.Builder.establish() returned null");

            File config = writeHevConfig(service.getCacheDir(), socksPort);
            boolean ok = TProxyService.TProxyStartService(config.getAbsolutePath(), tun.getFd());
            if (!ok) throw new IllegalStateException("HEV tun2socks refused to start");
            started = true;
        } catch (Exception e) {
            cleanup();
            throw e;
        }
    }

    @Override public synchronized void stop() { cleanup(); }

    private void cleanup() {
        try { if (TProxyService.TProxyIsRunning()) TProxyService.TProxyStopService(); } catch (Throwable ignored) { }
        if (tun != null) try { tun.close(); } catch (Exception ignored) { }
        tun = null;
        if (socks != null) try { socks.stop(); } catch (Exception ignored) { }
        socks = null;
        started = false;
    }

    private static File writeHevConfig(File cacheDir, int socksPort) throws Exception {
        String yaml = "misc:\n" +
                "  task-stack-size: 24576\n" +
                "  tcp-buffer-size: 16384\n" +
                "  max-session-count: 1024\n" +
                "tunnel:\n" +
                "  mtu: " + MTU + "\n" +
                "  ipv4: '" + VPN4 + "'\n" +
                "  ipv6: '" + VPN6 + "'\n" +
                "socks5:\n" +
                "  address: '127.0.0.1'\n" +
                "  port: " + socksPort + "\n" +
                "  udp: 'udp'\n";
        File f = new File(cacheDir, "reqlens-hev.yml");
        try (FileOutputStream out = new FileOutputStream(f, false)) {
            out.write(yaml.getBytes(StandardCharsets.UTF_8));
        }
        return f;
    }
}
