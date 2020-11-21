package ransomaware;

import ransomaware.exceptions.CertificateInvalidException;
import ransomaware.exceptions.CertificateNotFoundException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

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

    public static String signFile(String user, byte[] data) {
        try {
            //FIXME should private key's location be static or should it's path be input
            String keyPath = ClientVariables.FS_PATH + "/daniel/sign.key";
            RSAPrivateKey privKey = readPrivateKey(keyPath);
            Signature sign =  Signature.getInstance("SHA256withRSA");

            sign.initSign(privKey);
            sign.update(data);

            byte[] signature = sign.sign();

            return getBase64(signature);

        } catch (NoSuchAlgorithmException | IOException
                | InvalidKeyException | SignatureException | InvalidKeySpecException e) {
            e.printStackTrace();
            System.exit(1);
            return "";
        }
    }

    public static boolean validSignature(String signature, byte[] data, byte[] certificate) {
        try {
            X509Certificate cert = convertToCertificate(certificate);
            PublicKey pubKey = cert.getPublicKey();

            Signature sign = Signature.getInstance("SHA256withRSA");

            sign.initVerify(pubKey);
            sign.update(data);
            return sign.verify(decodeBase64(signature));
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | CertificateException e) {
            e.printStackTrace();
            System.exit(1);
            return false;
        }
    }

    private static RSAPrivateKey readPrivateKey(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        FileInputStream privateFIS = new FileInputStream(path);
        byte[] keyBytes = new byte[privateFIS.available()];
        privateFIS.read(keyBytes);
        privateFIS.close();

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);

    }

    private static X509Certificate convertToCertificate(byte[] cert) throws CertificateException {
        InputStream in = new ByteArrayInputStream(cert);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(in);
    }

    //FIXME: may not be needed
    private static RSAPublicKey readPublicKey(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        FileInputStream publicFIS = new FileInputStream(path);
        byte[] keyBytes = new byte[publicFIS.available()];
        publicFIS.read(keyBytes);
        publicFIS.close();

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf =  KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
