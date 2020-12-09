package ransomaware;

import ransomaware.exceptions.CertificateInvalidException;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.Base64;
import java.util.Optional;

public class SecurityUtils {

    private SecurityUtils() {}

    public static byte[] getDigest(String text) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(text.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return new byte[0];
    }

    public static String getBase64(byte[] src) {
        return new String(Base64.getEncoder().encode(src));
    }

    public static byte[] decodeBase64(String data) {
        return Base64.getDecoder().decode(data.getBytes());
    }

    public static Optional<X509Certificate> getCertificate(byte[] certificateRaw) {
        try {
            X509Certificate certificate = (X509Certificate)
                    CertificateFactory.getInstance("X.509")
                            .generateCertificate(new ByteArrayInputStream(certificateRaw));

            certificate.checkValidity();
            return Optional.of(certificate);
        } catch (CertificateException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static boolean isCertificateOfUser(X509Certificate certificate, String username) {
        try {
            String dn = certificate.getSubjectDN().getName();
            LdapName ln = new LdapName(dn);
            String cn = "";

            for (Rdn rdn : ln.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    cn = (String) rdn.getValue();
                    break;
                }
            }
            return cn.equals(username);
        } catch (InvalidNameException e) {
            throw new CertificateInvalidException();
        }
    }

    public static boolean isCertificateValid(X509Certificate cert) {
        TrustManagerFactory tmfactory = null;
        try {
            tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmfactory.init((KeyStore) null);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
            System.exit(1);
        }

        for (TrustManager trustManager : tmfactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                try {
                    ((X509TrustManager) trustManager).checkClientTrusted(new X509Certificate[]{cert}, "RSA");
                    return true;
                } catch (CertificateException e) {
                    //ignore
                }
            }
        }
        return false;
    }
}
