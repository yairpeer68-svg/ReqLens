package com.reqlens.app;

public final class CaptureEngineState {
    private CaptureEngineState() {}
    public static final boolean FORWARDER_READY = true;
    public static final String STATUS = "Embedded app forwarding backend ready (HEV tun2socks + protected local SOCKS5).";
    public static final String CAPABILITIES = "Live per-app VPN routing, TCP/UDP forwarding, IPv4/IPv6, DNS metadata, TLS ClientHello SNI/ALPN/version, QUIC candidates, live flow aggregation, encrypted local HTTP history, safe JSON/CSV export, diagnostics and protection signals.";
    public static final String SAFETY = "No TLS interception is performed by Capture Mode. Upstream sockets are protected from VPN recursion, and startup fails closed if forwarding cannot be established.";
}
