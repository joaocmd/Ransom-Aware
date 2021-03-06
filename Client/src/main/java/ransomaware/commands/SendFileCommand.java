package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;
import ransomaware.SessionInfo;
import ransomaware.exceptions.CertificateInvalidException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;

import static ransomaware.commands.Utils.getMetadataPath;

public class SendFileCommand implements Command {

    private final String owner;
    private final String filename;
    private final SessionInfo sessionInfo;
    private final String filePath;
    private final boolean generateNewKeys;

    public SendFileCommand(SessionInfo sessionInfo, String owner, String filename, boolean generateNewKeys) {
        this(sessionInfo, Utils.getFilePath(owner, filename), generateNewKeys);
    }

    public SendFileCommand(SessionInfo sessionInfo, String filePath, boolean generateNewKeys) {
        String[] args = filePath.split("/");

        this.owner = args[args.length - 2];
        this.filename = args[args.length - 1];
        this.sessionInfo = sessionInfo;
        this.filePath = filePath;
        this.generateNewKeys = generateNewKeys;
    }

    @Override
    public void run(HttpClient client) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("File not found locally.");
                return;
            }

            byte[] data = Files.readAllBytes(Path.of(filePath));
            String encodedData = SecurityUtils.getBase64(data);

            JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();

            FileInfo cryptoInfo = getFileKeys(client);

            if (cryptoInfo == null) {
                return;
            }

            SecretKey key = cryptoInfo.getKey();
            IvParameterSpec iv = cryptoInfo.getIv();
            JsonObject info = cryptoInfo.getInfo();

            JsonObject jsonFile = JsonParser.parseString("{}").getAsJsonObject();
            jsonFile.addProperty("data", encodedData);
            jsonFile.add("info", info);

            PrivateKey signingKey = SecurityUtils.readPrivateKey(sessionInfo.getSignKeyPath());
            byte[] signature = SecurityUtils.sign(signingKey, jsonFile.toString().getBytes());
            jsonFile.addProperty("signature", SecurityUtils.getBase64(signature));

            byte[] encryptedData = SecurityUtils.aesCipher(Cipher.ENCRYPT_MODE, jsonFile.toString().getBytes(), key, iv);
            String decodedEncryptedData = SecurityUtils.getBase64(encryptedData);
            jsonRoot.addProperty("file", decodedEncryptedData);
            jsonRoot.add("info", info);

            JsonObject requestInfo = JsonParser.parseString("{}").getAsJsonObject();
            requestInfo.addProperty("user", owner);
            requestInfo.addProperty("name", filename);
            jsonRoot.add("requestInfo", requestInfo);

            JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/save", jsonRoot, client);
            if (response.get("status").getAsInt() == HttpURLConnection.HTTP_OK) {
                Files.write(getMetadataPath(filePath), info.toString().getBytes());
                System.out.println("File successfully saved");
            } else {
                Utils.handleError(response, this.sessionInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FileInfo getFileKeys(HttpClient client) throws IOException {
        try {
            if (generateNewKeys) {
                return getFileInfoServer(client);
            } else {
                return getFileInfoFile(client);
            }
        } catch (CertificateInvalidException e) {
            System.err.println("Certificates for users with permission are not valid");
            return null;
        }
    }

    private FileInfo getFileInfoServer(HttpClient client) {
        SecretKey key = SecurityUtils.generateAesKey();
        IvParameterSpec iv = SecurityUtils.generateIV();
        byte[] secretKey = key.getEncoded();

        JsonObject response = Utils.requestGetFromURL(ClientVariables.URL + "/files/certs/" + owner + '/' + filename, client);
        int responseStatus = response.get("status").getAsInt();

        JsonObject root = JsonParser.parseString("{}").getAsJsonObject();
        JsonObject keys = JsonParser.parseString("{}").getAsJsonObject();
        if (responseStatus == HttpURLConnection.HTTP_OK) {
            response.getAsJsonObject("certs").entrySet().forEach(entry -> {
                byte[] decodedCert = SecurityUtils.decodeBase64(entry.getValue().getAsString());
                X509Certificate cert = SecurityUtils.getCertFromBytes(decodedCert);
                PublicKey userKey = SecurityUtils.getKeyFromCert(SecurityUtils.getCertFromBytes(decodedCert));
                try {
                    String encryptedKey = SecurityUtils.getBase64(SecurityUtils.rsaCipher(Cipher.ENCRYPT_MODE, secretKey, userKey));

                    if (!SecurityUtils.isCertificateValid(cert)) {
                        throw new CertificateInvalidException();
                    }
                    keys.addProperty(entry.getKey(), encryptedKey);
                } catch (BadPaddingException e) {
                    System.err.println("Got bad certificate from server, exiting");
                    e.printStackTrace();
                    System.exit(1);
                }
            });
        } else if (responseStatus == HttpURLConnection.HTTP_NOT_FOUND) {
            // File not created yet, just get my certificate
            response = Utils.requestGetFromURL(ClientVariables.URL + "/users/certs/" + sessionInfo.getUsername(), client);
            byte[] decodedCert = SecurityUtils.decodeBase64(response.getAsJsonObject("certs").get("encrypt").getAsString());
            X509Certificate cert = SecurityUtils.getCertFromBytes(decodedCert);
            PublicKey userKey = SecurityUtils.getKeyFromCert(cert);
            try {
                String encryptedKey = SecurityUtils.getBase64(SecurityUtils.rsaCipher(Cipher.ENCRYPT_MODE, secretKey, userKey));

                if (!SecurityUtils.isCertificateValid(cert)) {
                    throw new CertificateInvalidException();
                }
                keys.addProperty(sessionInfo.getUsername(), encryptedKey);
            } catch (BadPaddingException e) {
                System.err.println("Got bad certificate from server, exiting");
                e.printStackTrace();
                System.exit(1);
            }
        }

        root.add("keys", keys);
        root.addProperty("iv", SecurityUtils.getBase64(iv.getIV()));
        root.addProperty("author", sessionInfo.getUsername());
        root.addProperty("timestamp", Instant.now().toString());

        return new FileInfo(iv, key, root);
    }

    private FileInfo getFileInfoFile(HttpClient client) throws IOException {
        Path metadataPath = getMetadataPath(filePath);

        try {
            byte[] data = Files.readAllBytes(metadataPath);
            JsonObject metadata = JsonParser.parseString(new String(data)).getAsJsonObject();
            metadata.addProperty("author", sessionInfo.getUsername());
            metadata.addProperty("timestamp", Instant.now().toString());

            byte[] ivBytes = SecurityUtils.decodeBase64(metadata.get("iv").getAsString());
            byte[] keyBytes = SecurityUtils.decodeBase64(metadata.getAsJsonObject("keys").get(sessionInfo.getUsername()).getAsString());
            PrivateKey privKey = SecurityUtils.readPrivateKey(sessionInfo.getEncryptKeyPath());
            SecretKey key = SecurityUtils.getKeyFromBytes(SecurityUtils.rsaCipher(Cipher.DECRYPT_MODE, keyBytes, privKey));

            return new FileInfo(new IvParameterSpec(ivBytes), key, metadata);
        } catch (NoSuchFileException | BadPaddingException e) {
            return getFileInfoServer(client);
        }
    }

    private static class FileInfo {
        private final IvParameterSpec iv;
        private final SecretKey key;
        private final JsonObject info;

        public FileInfo(IvParameterSpec iv, SecretKey key, JsonObject info) {
            this.iv = iv;
            this.key = key;
            this.info = info;
        }

        public SecretKey getKey() {
            return key;
        }

        public IvParameterSpec getIv() {
            return iv;
        }

        public JsonObject getInfo() {
            return info;
        }
    }
}
