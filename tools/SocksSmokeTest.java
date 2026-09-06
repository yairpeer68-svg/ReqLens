import com.reqlens.app.LocalSocks5Server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class SocksSmokeTest {
    public static void main(String[] args) throws Exception {
        ServerSocket tcpEcho = new ServerSocket(0, 20, InetAddress.getLoopbackAddress());
        Thread tcpThread = new Thread(() -> {
            try (Socket s = tcpEcho.accept()) {
                byte[] b = new byte[1024]; int n = s.getInputStream().read(b);
                s.getOutputStream().write(b,0,n); s.getOutputStream().flush();
            } catch (Exception e) { throw new RuntimeException(e); }
        }); tcpThread.setDaemon(true); tcpThread.start();

        DatagramSocket udpEcho = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(),0));
        Thread udpThread = new Thread(() -> {
            try { byte[] b=new byte[2048]; DatagramPacket p=new DatagramPacket(b,b.length); udpEcho.receive(p); udpEcho.send(new DatagramPacket(p.getData(),p.getLength(),p.getSocketAddress())); }
            catch(Exception e){ throw new RuntimeException(e); }
        }); udpThread.setDaemon(true); udpThread.start();

        AtomicInteger tcpOpened=new AtomicInteger(), udpSeen=new AtomicInteger();
        LocalSocks5Server server = new LocalSocks5Server(new LocalSocks5Server.Protector() {
            public boolean protect(Socket s){ return true; }
            public boolean protect(DatagramSocket s){ return true; }
        }, InetAddress::getByName, new LocalSocks5Server.Observer() {
            public void onTcpOpened(long id,String host,String ip,int port){ tcpOpened.incrementAndGet(); }
            public void onTcpClientData(long id,byte[] data,int length){}
            public void onTcpBytes(long id,long bytes){}
            public void onUdpDatagram(long id,String host,String ip,int port,byte[] payload,int length,boolean outbound){ if(outbound) udpSeen.incrementAndGet(); }
        });
        int socksPort=server.start();

        try (Socket s=new Socket(InetAddress.getLoopbackAddress(),socksPort)) {
            InputStream in=s.getInputStream(); OutputStream out=s.getOutputStream();
            out.write(new byte[]{5,1,0}); out.flush(); require(read(in)==5 && read(in)==0,"greeting");
            byte[] addr=InetAddress.getLoopbackAddress().getAddress();
            ByteArrayOutputStream req=new ByteArrayOutputStream(); req.write(5);req.write(1);req.write(0);req.write(addr.length==4?1:4);req.write(addr);req.write(tcpEcho.getLocalPort()>>8);req.write(tcpEcho.getLocalPort());
            out.write(req.toByteArray());out.flush(); readReply(in);
            byte[] msg="tcp-ok".getBytes(StandardCharsets.US_ASCII); out.write(msg);out.flush(); byte[] got=readN(in,msg.length); require(Arrays.equals(msg,got),"tcp echo");
        }

        try (Socket control=new Socket(InetAddress.getLoopbackAddress(),socksPort); DatagramSocket client=new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(),0))) {
            InputStream in=control.getInputStream(); OutputStream out=control.getOutputStream();
            out.write(new byte[]{5,1,0});out.flush();require(read(in)==5&&read(in)==0,"udp greeting");
            out.write(new byte[]{5,3,0,1,0,0,0,0,0,0});out.flush(); InetSocketAddress relay=readReply(in);
            byte[] payload="udp-ok".getBytes(StandardCharsets.US_ASCII); byte[] dst=InetAddress.getLoopbackAddress().getAddress();
            ByteArrayOutputStream frame=new ByteArrayOutputStream(); frame.write(0);frame.write(0);frame.write(0);frame.write(dst.length==4?1:4);frame.write(dst);frame.write(udpEcho.getLocalPort()>>8);frame.write(udpEcho.getLocalPort());frame.write(payload);
            client.setSoTimeout(3000); client.send(new DatagramPacket(frame.toByteArray(),frame.size(),relay));
            byte[] rb=new byte[2048]; DatagramPacket rp=new DatagramPacket(rb,rb.length); client.receive(rp); byte[] extracted=udpPayload(rb,rp.getLength()); require(Arrays.equals(payload,extracted),"udp echo");
        }

        server.stop(); tcpEcho.close(); udpEcho.close();
        require(tcpOpened.get()==1,"tcp observer"); require(udpSeen.get()>=1,"udp observer");
        System.out.println("SOCKS_SMOKE_PASS tcp="+tcpOpened.get()+" udp="+udpSeen.get());
    }
    static int read(InputStream in)throws Exception{int v=in.read();if(v<0)throw new EOFException();return v;}
    static byte[] readN(InputStream in,int n)throws Exception{byte[] b=new byte[n];int p=0;while(p<n){int r=in.read(b,p,n-p);if(r<0)throw new EOFException();p+=r;}return b;}
    static InetSocketAddress readReply(InputStream in)throws Exception{require(read(in)==5,"reply ver");int rep=read(in);require(rep==0,"reply code "+rep);read(in);int atyp=read(in);byte[] a;if(atyp==1)a=readN(in,4);else if(atyp==4)a=readN(in,16);else throw new IOException("atyp");int port=(read(in)<<8)|read(in);return new InetSocketAddress(InetAddress.getByAddress(a),port);}
    static byte[] udpPayload(byte[] b,int n)throws Exception{int p=0;require(b[p++]==0&&b[p++]==0,"udp rsv");p++;int atyp=b[p++]&255;if(atyp==1)p+=4;else if(atyp==4)p+=16;else if(atyp==3)p+=1+(b[p]&255);else throw new IOException("atyp");p+=2;return Arrays.copyOfRange(b,p,n);}
    static void require(boolean ok,String m){if(!ok)throw new AssertionError(m);}
}
