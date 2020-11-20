package ransomaware;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;

public class SecurityUtils {
    public static byte[] getDigest(String text) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(text.getBytes());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Couldn't get SHA-256 instance.");
            System.exit(1);
            // FIXME: UGLY
            return new byte[0];
        }
    }

    public static String getBase64(byte[] src) {
        return new String(Base64.getEncoder().encode(src));
    }

    public static byte[] decodeBase64(String src) {
        return Base64.getDecoder().decode(src);
    }

    public static Optional<X509Certificate> getCertificate(String path) {
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(path));
            X509Certificate certificate = (X509Certificate)
                    CertificateFactory.getInstance("X.509").generateCertificate(buf);

            certificate.checkValidity();
            buf.close();
            return Optional.of(certificate);
        } catch (CertificateException | IOException e) {
            System.err.println("Certificate could not be read.");

            return Optional.empty();
        }
    }

    public static boolean checkCertificateUser(X509Certificate cert, String username) {
        try {
            String dn = cert.getSubjectDN().getName();
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
            System.err.println("Certificate has bad format.");
            return false;
        }
    }

    public static Optional<byte[]> getCertificateToSend(String path) {
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(path));
            return Optional.of(buf.readAllBytes());
        } catch (IOException e) {
            System.err.println("Certificate could not be read.");

            return Optional.empty();
        }
    }
}
