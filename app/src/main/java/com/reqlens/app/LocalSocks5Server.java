package com.reqlens.app;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalSocks5Server {
    public interface Protector {
        boolean protect(Socket socket);
        boolean protect(DatagramSocket socket);
    }
    public interface Resolver { InetAddress resolve(String host) throws IOException; }
    public interface Observer {
        void onTcpOpened(long id, String host, String ip, int port);
        void onTcpClientData(long id, byte[] data, int length);
        void onTcpBytes(long id, long bytes);
        void onUdpDatagram(long associationId, String host, String ip, int port,
                           byte[] payload, int length, boolean outbound);
    }

    private final Protector protector;
    private final Resolver resolver;
    private final Observer observer;
    private final int mitmPort;
    private final ExecutorService workers;
    private final AtomicLong ids = new AtomicLong(1);
    private final Set<Socket> activeClients = ConcurrentHashMap.newKeySet();
    private volatile boolean running;
    private ServerSocket server;
    private Thread acceptThread;

    public LocalSocks5Server(Protector protector, Resolver resolver, Observer observer) {
        this(protector, resolver, observer, -1);
    }

    public LocalSocks5Server(Protector protector, Resolver resolver, Observer observer, int mitmPort) {
        this.protector = protector;
        this.resolver = resolver;
        this.observer = observer;
        this.mitmPort = mitmPort;
        this.workers = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicLong n = new AtomicLong();
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ReqLens-SOCKS-" + n.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
    }

    public synchronized int start() throws IOException {
        if (running) return server.getLocalPort();
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(loopback4(), 0));
        running = true;
        acceptThread = new Thread(this::acceptLoop, "ReqLens-SOCKS-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return server.getLocalPort();
    }

    public synchronized void stop() {
        running = false;
        close(server);
        if (acceptThread != null) acceptThread.interrupt();
        for (Socket socket : activeClients) close(socket);
        activeClients.clear();
        workers.shutdownNow();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = server.accept();
                client.setTcpNoDelay(true);
                activeClients.add(client);
                workers.execute(() -> handleClient(client));
            } catch (SocketException e) {
                if (running) sleepQuiet(50);
            } catch (IOException e) {
                if (running) sleepQuiet(50);
            }
        }
    }

    private void handleClient(Socket client) {
        try (Socket c = client) {
            InputStream in = c.getInputStream();
            OutputStream out = c.getOutputStream();
            negotiate(in, out);
            Request req = readRequest(in);
            if (req.command == 1) handleConnect(c, in, out, req);
            else if (req.command == 3) handleUdpAssociate(c, in, out);
            else sendReply(out, 7, null, 0);
        } catch (Exception ignored) { }
        finally { activeClients.remove(client); }
    }

    private void negotiate(InputStream in, OutputStream out) throws IOException {
        int version = readU8(in), methods = readU8(in);
        if (version != 5 || methods < 1) throw new IOException("Invalid SOCKS greeting");
        boolean noAuth = false;
        for (int i = 0; i < methods; i++) if (readU8(in) == 0) noAuth = true;
        out.write(new byte[]{5, (byte)(noAuth ? 0 : 0xff)}); out.flush();
        if (!noAuth) throw new IOException("SOCKS no-auth not offered");
    }

    private Request readRequest(InputStream in) throws IOException {
        if (readU8(in) != 5) throw new IOException("Invalid SOCKS request version");
        int command = readU8(in); readU8(in);
        Address address = readAddress(in);
        int port = readU16(in);
        return new Request(command, address, port);
    }

    private void handleConnect(Socket client, InputStream clientIn, OutputStream clientOut, Request req) throws IOException {
        if (mitmPort > 0 && req.port == 443) {
            handleMitmConnect(client, clientIn, clientOut, req);
            return;
        }
        InetAddress remoteAddress = resolve(req.address);
        long id = ids.getAndIncrement();
        Socket remote = new Socket();
        boolean successReplySent = false;
        try {
            if (!protector.protect(remote)) throw new IOException("VpnService.protect(TCP) failed");
            remote.setTcpNoDelay(true);
            remote.connect(new InetSocketAddress(remoteAddress, req.port), 10000);
            sendReply(clientOut, 0, remote.getLocalAddress(), remote.getLocalPort());
            successReplySent = true;
            observer.onTcpOpened(id, req.address.display(), remoteAddress.getHostAddress(), req.port);
            InputStream remoteIn = remote.getInputStream();
            OutputStream remoteOut = remote.getOutputStream();
            workers.execute(() -> pumpRemoteToClient(id, remoteIn, clientOut, client));
            pumpClientToRemote(id, clientIn, remoteOut);
        } catch (IOException e) {
            if (!successReplySent) try { sendReply(clientOut, 5, null, 0); } catch (Exception ignored) { }
            throw e;
        } finally { close(remote); }
    }


    private void handleMitmConnect(Socket client, InputStream clientIn, OutputStream clientOut, Request req) throws IOException {
        long id = ids.getAndIncrement();
        String host = req.address.display();
        Socket proxy = new Socket();
        boolean successReplySent = false;
        try {
            proxy.setTcpNoDelay(true);
            proxy.connect(new InetSocketAddress(loopback4(), mitmPort), 5000);
            OutputStream proxyOut = proxy.getOutputStream();
            InputStream proxyIn = proxy.getInputStream();
            String authority = host + ":" + req.port;
            proxyOut.write(("CONNECT " + authority + " HTTP/1.1\r\nHost: " + authority + "\r\nProxy-Connection: keep-alive\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            proxyOut.flush();
            String response = readHttpHeader(proxyIn, 16384);
            if (!response.startsWith("HTTP/1.1 200") && !response.startsWith("HTTP/1.0 200"))
                throw new IOException("MITM proxy CONNECT failed: " + response.split("\r?\n", 2)[0]);
            sendReply(clientOut, 0, loopback4(), 0);
            successReplySent = true;
            observer.onTcpOpened(id, host, "127.0.0.1", req.port);
            workers.execute(() -> pumpRemoteToClient(id, proxyIn, clientOut, client));
            pumpClientToRemote(id, clientIn, proxyOut);
        } catch (IOException e) {
            if (!successReplySent) try { sendReply(clientOut, 5, null, 0); } catch (Exception ignored) { }
            throw e;
        } finally { close(proxy); }
    }

    private static String readHttpHeader(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int state = 0;
        while (out.size() < max) {
            int x = in.read();
            if (x < 0) break;
            out.write(x);
            state = (state == 0 && x == '\r') ? 1 : (state == 1 && x == '\n') ? 2 : (state == 2 && x == '\r') ? 3 : (state == 3 && x == '\n') ? 4 : 0;
            if (state == 4) break;
        }
        return out.toString(StandardCharsets.ISO_8859_1.name());
    }

    private void pumpClientToRemote(long id, InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[32768];
        ByteArrayOutputStream inspect = new ByteArrayOutputStream();
        boolean inspectionDone = false;
        int n;
        while (running && (n = in.read(buf)) >= 0) {
            if (n == 0) continue;
            if (!inspectionDone && inspect.size() < 65536) {
                int take = Math.min(n, 65536 - inspect.size());
                inspect.write(buf, 0, take);
                byte[] snapshot = inspect.toByteArray();
                observer.onTcpClientData(id, snapshot, snapshot.length);
                TlsClientHelloInspector.Result tls = TlsClientHelloInspector.inspect(snapshot, snapshot.length);
                if (tls.clientHello || inspect.size() >= 65536) inspectionDone = true;
            }
            out.write(buf, 0, n); out.flush();
            observer.onTcpBytes(id, n);
        }
    }

    private void pumpRemoteToClient(long id, InputStream in, OutputStream out, Socket client) {
        byte[] buf = new byte[32768];
        try {
            int n;
            while (running && (n = in.read(buf)) >= 0) {
                if (n == 0) continue;
                out.write(buf, 0, n); out.flush();
                observer.onTcpBytes(id, n);
            }
        } catch (IOException ignored) { }
        finally { close(client); }
    }

    private void handleUdpAssociate(Socket control, InputStream controlIn, OutputStream controlOut) throws IOException {
        long associationId = ids.getAndIncrement();
        DatagramSocket udp = new DatagramSocket(new InetSocketAddress(0));
        if (!protector.protect(udp)) { udp.close(); throw new IOException("VpnService.protect(UDP) failed"); }
        sendReply(controlOut, 0, loopback4(), udp.getLocalPort());
        Thread relay = new Thread(() -> udpLoop(associationId, udp), "ReqLens-SOCKS-UDP-" + associationId);
        relay.setDaemon(true); relay.start();
        try {
            while (running && controlIn.read() != -1) { /* control channel stays open */ }
        } finally { udp.close(); relay.interrupt(); }
    }

    private void udpLoop(long associationId, DatagramSocket udp) {
        byte[] buf = new byte[65535];
        SocketAddress clientAddress = null;
        while (running && !udp.isClosed()) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                udp.receive(p);
                boolean socksFrame = p.getLength() >= 4 && buf[0] == 0 && buf[1] == 0;
                if (socksFrame && (clientAddress == null || p.getSocketAddress().equals(clientAddress) || p.getAddress().isLoopbackAddress())) {
                    clientAddress = p.getSocketAddress();
                    UdpRequest req = parseUdpRequest(buf, p.getOffset(), p.getLength());
                    if (req == null || req.fragment != 0) continue;
                    InetAddress dst = resolve(req.address);
                    byte[] payload = Arrays.copyOfRange(buf, req.payloadOffset, p.getOffset() + p.getLength());
                    observer.onUdpDatagram(associationId, req.address.display(), dst.getHostAddress(), req.port, payload, payload.length, true);
                    udp.send(new DatagramPacket(payload, payload.length, dst, req.port));
                } else if (clientAddress != null) {
                    byte[] framed = encodeUdpResponse(p.getAddress(), p.getPort(), p.getData(), p.getOffset(), p.getLength());
                    observer.onUdpDatagram(associationId, p.getAddress().getHostAddress(), p.getAddress().getHostAddress(), p.getPort(),
                            p.getData(), p.getLength(), false);
                    udp.send(new DatagramPacket(framed, framed.length, clientAddress));
                }
            } catch (SocketException e) {
                if (running && !udp.isClosed()) sleepQuiet(20);
            } catch (Exception ignored) { }
        }
    }

    private UdpRequest parseUdpRequest(byte[] b, int off, int len) throws IOException {
        int end = off + len, p = off;
        if (p + 4 > end || b[p] != 0 || b[p + 1] != 0) return null;
        p += 2; int frag = b[p++] & 255; int atyp = b[p++] & 255;
        ParsedAddress parsed = parseAddressBytes(b, p, end, atyp); p = parsed.next;
        if (p + 2 > end) return null;
        int port = ((b[p] & 255) << 8) | (b[p + 1] & 255); p += 2;
        return new UdpRequest(frag, parsed.address, port, p);
    }

    private byte[] encodeUdpResponse(InetAddress address, int port, byte[] data, int off, int len) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(len + 32);
        out.write(0); out.write(0); out.write(0);
        byte[] a = address.getAddress(); out.write(a.length == 4 ? 1 : 4); out.write(a);
        out.write((port >>> 8) & 255); out.write(port & 255); out.write(data, off, len);
        return out.toByteArray();
    }

    private Address readAddress(InputStream in) throws IOException {
        int atyp = readU8(in);
        if (atyp == 1) return Address.ip(readExact(in, 4));
        if (atyp == 4) return Address.ip(readExact(in, 16));
        if (atyp == 3) {
            int n = readU8(in); if (n < 1 || n > 253) throw new IOException("Invalid domain length");
            return Address.host(new String(readExact(in, n), StandardCharsets.US_ASCII));
        }
        throw new IOException("Unsupported address type");
    }

    private ParsedAddress parseAddressBytes(byte[] b, int p, int end, int atyp) throws IOException {
        if (atyp == 1) { if (p + 4 > end) throw new EOFException(); return new ParsedAddress(Address.ip(Arrays.copyOfRange(b,p,p+4)), p+4); }
        if (atyp == 4) { if (p + 16 > end) throw new EOFException(); return new ParsedAddress(Address.ip(Arrays.copyOfRange(b,p,p+16)), p+16); }
        if (atyp == 3) {
            if (p >= end) throw new EOFException(); int n = b[p++] & 255;
            if (n < 1 || p + n > end) throw new EOFException();
            return new ParsedAddress(Address.host(new String(b,p,n,StandardCharsets.US_ASCII)), p+n);
        }
        throw new IOException("Unsupported UDP address type");
    }

    private InetAddress resolve(Address address) throws IOException {
        if (address.ip != null) return InetAddress.getByAddress(address.ip);
        return resolver.resolve(address.host);
    }

    private void sendReply(OutputStream out, int code, InetAddress address, int port) throws IOException {
        if (address == null) address = InetAddress.getByName("0.0.0.0");
        byte[] a = address.getAddress();
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.write(5); b.write(code); b.write(0); b.write(a.length == 4 ? 1 : 4); b.write(a);
        b.write((port >>> 8) & 255); b.write(port & 255); out.write(b.toByteArray()); out.flush();
    }

    private static InetAddress loopback4() throws IOException { return InetAddress.getByAddress(new byte[]{127,0,0,1}); }
    private static int readU8(InputStream in) throws IOException { int v = in.read(); if (v < 0) throw new EOFException(); return v; }
    private static int readU16(InputStream in) throws IOException { return (readU8(in) << 8) | readU8(in); }
    private static byte[] readExact(InputStream in, int n) throws IOException {
        byte[] out = new byte[n]; int p = 0;
        while (p < n) { int r = in.read(out, p, n-p); if (r < 0) throw new EOFException(); p += r; }
        return out;
    }
    private static void close(java.io.Closeable c) { if (c != null) try { c.close(); } catch (Exception ignored) { } }
    private static void sleepQuiet(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    private static final class Request { final int command, port; final Address address; Request(int c, Address a, int p){command=c;address=a;port=p;} }
    private static final class UdpRequest { final int fragment,port,payloadOffset; final Address address; UdpRequest(int f,Address a,int p,int o){fragment=f;address=a;port=p;payloadOffset=o;} }
    private static final class ParsedAddress { final Address address; final int next; ParsedAddress(Address a,int n){address=a;next=n;} }
    private static final class Address {
        final byte[] ip; final String host;
        private Address(byte[] i,String h){ip=i;host=h;}
        static Address ip(byte[] i){return new Address(i,null);} static Address host(String h){return new Address(null,h);}
        String display(){ try { return ip != null ? InetAddress.getByAddress(ip).getHostAddress() : host; } catch(Exception e){ return host==null?"":host; } }
    }
}
