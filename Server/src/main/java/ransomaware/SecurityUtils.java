package ransomaware;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SecurityUtils {

    private SecurityUtils() {}

    public static byte[] getDigest(String text) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(text.getBytes());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Couldn't get SHA-256 instance.");
            System.exit(1);
            return new byte[0];
        }
    }

    public static String getBase64(byte[] src) {
        return new String(Base64.getEncoder().encode(src));
    }

    public static byte[] decodeBase64(String data) {
        return Base64.getDecoder().decode(data.getBytes());
    }
}
