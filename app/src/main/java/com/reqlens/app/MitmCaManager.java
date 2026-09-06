package com.reqlens.app;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public final class MitmCaManager {
    private static final String ALIAS = "reqlens_mitm_ca";
    private static final String PREF = "reqlens_mitm";
    private static final String CERT = "ca_cert";
    private static final SecureRandom RNG = new SecureRandom();
    private final Context app;
    private final ConcurrentHashMap<String, HostIdentity> cache = new ConcurrentHashMap<>();

    public static final class HostIdentity {
        public final PrivateKey key;
        public final X509Certificate cert;
        HostIdentity(PrivateKey key, X509Certificate cert) { this.key = key; this.cert = cert; }
    }

    public MitmCaManager(Context context) { app = context.getApplicationContext(); }

    public synchronized X509Certificate ensureCa() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        PrivateKey key = (PrivateKey) ks.getKey(ALIAS, null);
        X509Certificate cert = loadStoredCertificate();
        if (key != null && cert != null) return cert;

        if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS);
        KeyPairGenerator gen = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
        gen.initialize(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setKeySize(2048)
                .build());
        KeyPair kp = gen.generateKeyPair();
        cert = buildCaCertificate(kp.getPublic(), kp.getPrivate());
        app.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString(CERT, Base64.encodeToString(cert.getEncoded(), Base64.NO_WRAP)).apply();
        return cert;
    }

    public synchronized PrivateKey caPrivateKey() throws Exception {
        ensureCa();
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        return (PrivateKey) ks.getKey(ALIAS, null);
    }

    public synchronized HostIdentity identityFor(String host) throws Exception {
        String key = host == null ? "" : host.trim().toLowerCase(java.util.Locale.ROOT);
        HostIdentity hit = cache.get(key);
        if (hit != null) return hit;
        X509Certificate ca = ensureCa();
        PrivateKey caKey = caPrivateKey();
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair leaf = gen.generateKeyPair();
        X509Certificate cert = buildLeafCertificate(key, leaf.getPublic(), ca, caKey);
        HostIdentity out = new HostIdentity(leaf.getPrivate(), cert);
        cache.put(key, out);
        if (cache.size() > 128) cache.clear();
        return out;
    }

    public byte[] caDer() throws Exception { return ensureCa().getEncoded(); }

    public X509Certificate loadStoredCertificate() {
        try {
            String raw = app.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(CERT, "");
            if (raw == null || raw.isEmpty()) return null;
            byte[] der = Base64.decode(raw, Base64.DEFAULT);
            java.security.cert.CertificateFactory f = java.security.cert.CertificateFactory.getInstance("X.509");
            return (X509Certificate) f.generateCertificate(new java.io.ByteArrayInputStream(der));
        } catch (Exception e) { return null; }
    }

    private static X509Certificate buildCaCertificate(PublicKey pub, PrivateKey key) throws Exception {
        long now = System.currentTimeMillis();
        X500Name dn = new X500Name("CN=ReqLens Authorized MITM CA,O=ReqLens");
        JcaX509v3CertificateBuilder b = new JcaX509v3CertificateBuilder(dn, serial(), new Date(now - 60000L), new Date(now + 315360000000L), dn, pub);
        b.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(key);
        X509CertificateHolder holder = b.build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    private static X509Certificate buildLeafCertificate(String host, PublicKey pub, X509Certificate ca, PrivateKey caKey) throws Exception {
        long now = System.currentTimeMillis();
        X500Name issuer = new X500Name(ca.getSubjectX500Principal().getName());
        X500Name subject = new X500Name("CN=" + host);
        JcaX509v3CertificateBuilder b = new JcaX509v3CertificateBuilder(issuer, serial(), new Date(now - 60000L), new Date(now + 604800000L), subject, pub);
        b.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        b.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.dNSName, host)));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(caKey);
        return new JcaX509CertificateConverter().getCertificate(b.build(signer));
    }

    private static BigInteger serial() { return new BigInteger(160, RNG).abs().add(BigInteger.ONE); }
}
