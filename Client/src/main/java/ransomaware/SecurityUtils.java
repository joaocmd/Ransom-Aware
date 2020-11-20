package ransomaware;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import ransomaware.exceptions.CertificateInvalidException;
import ransomaware.exceptions.CertificateNotFoundException;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
            e.printStackTrace();
            System.exit(1);
        }
        return new byte[0];
    }

    public static String getBase64(byte[] src) {
        return new String(Base64.getEncoder().encode(src));
    }

    public static byte[] decodeBase64(String src) {
        return Base64.getDecoder().decode(src);
    }

    public static IvParameterSpec generateIV()  {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public static SecretKey generateAesKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Could not get AES key generator.");
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static SecretKey getKeyFromBytes(byte[] bytes) {
        return new SecretKeySpec(bytes, 0, bytes.length, "AES");
    }

    public static byte[] AesCipher(int opmode, byte[] data, SecretKey key, IvParameterSpec iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(opmode, key, iv);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
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

    public static boolean checkCertificateUser(String path, String username) {
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(path));
            X509Certificate certificate = (X509Certificate)
                    CertificateFactory.getInstance("X.509").generateCertificate(buf);

            certificate.checkValidity();
            buf.close();

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
        } catch (InvalidNameException | CertificateException e) {
            throw new CertificateInvalidException();
        } catch (IOException e) {
            throw new CertificateNotFoundException();
        }
    }

    public static byte[] getCertificateToSend(String path) {
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(path));
            return buf.readAllBytes();
        } catch (IOException e) {
            throw new CertificateNotFoundException();
        }
    }
}
