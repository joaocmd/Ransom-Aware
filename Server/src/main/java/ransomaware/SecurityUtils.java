package ransomaware;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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

}
