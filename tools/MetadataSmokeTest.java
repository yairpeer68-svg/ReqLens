import com.reqlens.app.TlsClientHelloInspector;
import com.reqlens.app.DnsInspector;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class MetadataSmokeTest {
    public static void main(String[] args) throws Exception {
        byte[] hello = clientHello("api.example.com");
        TlsClientHelloInspector.Result r = TlsClientHelloInspector.inspect(hello, hello.length);
        require(r.clientHello, "client hello");
        require("api.example.com".equals(r.sni), "sni=" + r.sni);
        require(r.alpn.contains("h2"), "alpn=" + r.alpn);
        require("TLS 1.3".equals(r.tlsVersion), "version=" + r.tlsVersion);

        byte[] dns = dnsQuery("api.example.com");
        require("api.example.com".equals(DnsInspector.questionName(dns, dns.length)), "dns");
        System.out.println("METADATA_SMOKE_PASS sni=" + r.sni + " alpn=" + r.alpn + " tls=" + r.tlsVersion);
    }

    static byte[] clientHello(String host) throws Exception {
        ByteArrayOutputStream exts = new ByteArrayOutputStream();
        byte[] h = host.getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream sni = new ByteArrayOutputStream();
        int listLen = 1 + 2 + h.length; u16(sni, listLen); sni.write(0); u16(sni, h.length); sni.write(h);
        ext(exts, 0, sni.toByteArray());
        ByteArrayOutputStream alpn = new ByteArrayOutputStream(); u16(alpn, 3); alpn.write(2); alpn.write('h'); alpn.write('2'); ext(exts,16,alpn.toByteArray());
        ext(exts,43,new byte[]{2,3,4});

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(3); body.write(3); body.write(new byte[32]); body.write(0); u16(body,2); body.write(0x13); body.write(0x01); body.write(1); body.write(0); u16(body,exts.size()); body.write(exts.toByteArray());
        ByteArrayOutputStream hs = new ByteArrayOutputStream(); hs.write(1); u24(hs,body.size()); hs.write(body.toByteArray());
        ByteArrayOutputStream rec = new ByteArrayOutputStream(); rec.write(22); rec.write(3); rec.write(1); u16(rec,hs.size()); rec.write(hs.toByteArray()); return rec.toByteArray();
    }
    static byte[] dnsQuery(String host) throws Exception {
        ByteArrayOutputStream o=new ByteArrayOutputStream(); o.write(new byte[]{0x12,0x34,1,0,0,1,0,0,0,0,0,0});
        for(String part:host.split("\\.")){byte[] b=part.getBytes(StandardCharsets.US_ASCII);o.write(b.length);o.write(b);} o.write(0);o.write(0);o.write(1);o.write(0);o.write(1);return o.toByteArray();
    }
    static void ext(ByteArrayOutputStream o,int type,byte[] data)throws Exception{u16(o,type);u16(o,data.length);o.write(data);} static void u16(ByteArrayOutputStream o,int v){o.write((v>>>8)&255);o.write(v&255);} static void u24(ByteArrayOutputStream o,int v){o.write((v>>>16)&255);o.write((v>>>8)&255);o.write(v&255);} static void require(boolean v,String m){if(!v)throw new AssertionError(m);}
}
