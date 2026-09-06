package com.reqlens.app;

public final class ParserSmokeTest {
    public static void main(String[] args) {
        byte[] dns = new byte[64];
        dns[0]=0x45; dns[9]=17; dns[12]=10; dns[15]=2; dns[16]=8; dns[17]=8; dns[18]=8; dns[19]=8;
        dns[20]=0x30; dns[21]=0x39; dns[22]=0; dns[23]=53; dns[32]=0; dns[33]=1;
        int q=40; dns[q++]=3; dns[q++]='a'; dns[q++]='p'; dns[q++]='i'; dns[q++]=7;
        for(byte b:"example".getBytes()) dns[q++]=b; dns[q++]=3; for(byte b:"com".getBytes()) dns[q++]=b; dns[q++]=0;
        PacketParser.ParsedPacket p = PacketParser.parse(dns, q);
        if (p == null || !"api.example.com".equals(p.dnsName)) throw new AssertionError("DNS parse failed");
        System.out.println("PASS");
    }
}
