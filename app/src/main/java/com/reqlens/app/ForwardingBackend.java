package com.reqlens.app;

import android.net.VpnService;
import java.util.Set;

public interface ForwardingBackend {
    interface Observer {
        void onPacket(byte[] data, int length);
        void onTcpOpened(long flowId, String destinationHost, String destinationIp, int destinationPort);
        void onTcpClientData(long flowId, byte[] data, int length);
        void onTcpBytes(long flowId, long bytes);
        void onUdpDatagram(long associationId, String destinationHost, String destinationIp,
                           int destinationPort, byte[] payload, int length, boolean outbound);
    }

    boolean isAvailable();
    String name();
    String diagnostic();
    void start(VpnService service, Set<String> packages, Observer observer) throws Exception;
    void stop();
}
