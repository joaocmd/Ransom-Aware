package ransomaware;

import ransomaware.exceptions.CertificateInvalidException;
import ransomaware.exceptions.CertificateNotFoundException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

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
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static SecretKey getKeyFromBytes(byte[] bytes) {
        return new SecretKeySpec(bytes, 0, bytes.length, "AES");
    }

    public static byte[] aesCipher(int opmode, byte[] data, SecretKey key, IvParameterSpec iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(opmode, key, iv);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return new byte[0];
    }

    public static byte[] rsaCipher(int opmode, byte[] data, Key key) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(opmode, key);
            return cipher.doFinal(data);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return new byte[0];
    }

    public static boolean checkCertificateUser(X509Certificate certificate, String username) {
        try {
            certificate.checkValidity();

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
        }
    }

    public static X509Certificate readCertificate(String path) {
        try (BufferedInputStream buf = new BufferedInputStream(new FileInputStream(path))) {
            byte[] bytes = buf.readAllBytes();
            return getCertFromBytes(bytes);
        } catch (IOException e) {
            throw new CertificateNotFoundException();
        }
    }

    public static byte[] sign(PrivateKey key, byte[] data) {
        try {
            Signature signature =  Signature.getInstance("SHA256withRSA");
            signature.initSign(key);

            signature.update(data);
            return signature.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return new byte[0];
    }

    public static boolean verifySignature(byte[] signature, byte[] data, X509Certificate cert) {
        // TODO: Should verify if the certificate is valid to root (path)
        try {
            PublicKey pubKey = cert.getPublicKey();
            Signature sign = Signature.getInstance("SHA256withRSA");

            sign.initVerify(pubKey);
            sign.update(data);
            return sign.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return false;
    }

    public static PrivateKey readPrivateKey(String path) {
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] keyBytes = fis.readAllBytes();

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static X509Certificate getCertFromBytes(byte[] cert) {
        try (InputStream in = new ByteArrayInputStream(cert)) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(in);
        } catch (IOException | CertificateException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    //FIXME: may not be needed
    private static RSAPublicKey readPublicKey(String path) {
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] keyBytes = fis.readAllBytes();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static PublicKey getKeyFromCert(X509Certificate cert) {
        return cert.getPublicKey();
    }

    public static boolean isCertificateValid(X509Certificate cert) {
        System.out.println("Hello biatch");
        TrustManagerFactory tmfactory = null;
        try {
            tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            tmfactory.init((KeyStore) null);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        for (TrustManager trustManager : tmfactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                try {
                    ((X509TrustManager) trustManager).checkClientTrusted(new X509Certificate[]{cert}, "RSA");
                    return true;
                } catch (CertificateException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
