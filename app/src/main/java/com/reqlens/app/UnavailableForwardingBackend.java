package com.reqlens.app;

import android.net.VpnService;
import java.util.Set;

public final class UnavailableForwardingBackend implements ForwardingBackend {
    @Override public boolean isAvailable() { return false; }
    @Override public String name() { return "No embedded userspace TCP/IP backend"; }
    @Override public String diagnostic() {
        return "TUN establishment is intentionally disabled until a real userspace TCP/IP forwarding engine is bundled.";
    }
    @Override public void start(VpnService service, Set<String> packages, Observer observer) {
        throw new IllegalStateException(diagnostic());
    }
    @Override public void stop() { }
}
