package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;
import ransomaware.SessionInfo;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;

import static ransomaware.commands.Utils.getMetadataPath;

public class GetFileCommand implements Command {

    private final SessionInfo sessionInfo;
    private final String owner;
    private final String filename;
    private final String outputPath;
    private final boolean rollback;
    private boolean success;

    public GetFileCommand(SessionInfo sessionInfo, String owner, String filename, String outputPath, boolean rollback) {
        this.sessionInfo = sessionInfo;
        this.owner = owner;
        this.filename = filename;
        this.outputPath = outputPath;
        this.success = false;
        this.rollback = rollback;
    }

    public GetFileCommand(SessionInfo sessionInfo, String owner, String filename, String outputPath) {
        this(sessionInfo, owner, filename, outputPath, false);
    }

    public GetFileCommand(SessionInfo sessionInfo, String owner, String filename) {
        this(sessionInfo, owner, filename, ClientVariables.WORKSPACE, false);
    }

    public GetFileCommand(SessionInfo sessionInfo, String owner, String filename, boolean rollback) {
        this(sessionInfo, owner, filename, ClientVariables.WORKSPACE, rollback);
    }

    @Override
    public void run(HttpClient client) {
        try {
            JsonObject response = Utils.requestGetFromURL(ClientVariables.URL + "/files" + '/' + owner + '/' + filename, client);
            if (response.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
                Utils.handleError(response, this.sessionInfo);
                return;
            }

            JsonObject file = response.getAsJsonObject("file");
            byte[] encryptedFile = SecurityUtils.decodeBase64(file.get("data").getAsString());

            JsonObject info = file.getAsJsonObject("info");

            byte[] keyBytes = getCorrectKey(info.getAsJsonObject("keys"));
            PrivateKey privKey = SecurityUtils.readPrivateKey(sessionInfo.getEncryptKeyPath());

            byte[] unencryptedData = getUnencryptedData(encryptedFile, info, keyBytes, privKey);
            JsonObject fileJson = JsonParser.parseString(new String(unencryptedData)).getAsJsonObject();

            String encodedSignature = fileJson.get("signature").getAsString();
            fileJson.remove("signature");

            if (!fileJson.getAsJsonObject("info").equals(info)) {
                System.out.println(fileJson.getAsJsonObject("info").toString());
                System.out.println(info.toString());
                System.out.println("ERROR: Signed info did not match public info, aborting");
                return;
            }

            X509Certificate cert = getUserCert(fileJson.getAsJsonObject("info").get("author").getAsString(), client);
            if (cert == null) {
                System.err.println("ERROR: Could not get certificate for author, aborting");
                return;
            }
            if (!SecurityUtils.isCertificateValid(cert)) {
                System.err.println("ERROR: Certificate is not trusted, aborting");
            }
            if (!SecurityUtils.verifySignature(SecurityUtils.decodeBase64(encodedSignature), fileJson.toString().getBytes(), cert)) {
                System.err.println("ERROR: File contains bad signature, aborting");
                return;
            }

            if (!rollback && !fileIsFresh(info)) {
                System.err.println("WARNING: File received is not fresh, this might be intentional, resuming");
            }

            byte[] fileData = SecurityUtils.decodeBase64(fileJson.get("data").getAsString());
            byte[] metadata = info.toString().getBytes();

            File dir = new File(this.outputPath + '/' + owner);
            dir.mkdirs();

            Files.write(Path.of(this.outputPath + '/' + owner + '/' + filename), fileData);
            Files.write(Path.of(this.outputPath + '/' + owner + "/." + filename + ".metadata"), metadata);

            this.success = true;
            System.out.println("File successfully fetched");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] getUnencryptedData(byte[] encryptedFile, JsonObject info, byte[] keyBytes, PrivateKey privKey) {
        try {
            SecretKey key = SecurityUtils.getKeyFromBytes(SecurityUtils.rsaCipher(Cipher.DECRYPT_MODE, keyBytes, privKey));
            byte[] iv = SecurityUtils.decodeBase64(info.get("iv").getAsString());
            return SecurityUtils.aesCipher(Cipher.DECRYPT_MODE, encryptedFile, key, new IvParameterSpec(iv));
        } catch (BadPaddingException e) {
            System.err.println("Got bad key from server, exiting");
            System.exit(1);
        }
        return new byte[0];
    }

    private boolean fileIsFresh(JsonObject info) {
        Instant receivedTs = Instant.parse(info.get("timestamp").getAsString());
        try {
            byte[] data = Files.readAllBytes(getMetadataPath(getOutputFilePath()));
            JsonObject metadata = JsonParser.parseString(new String(data)).getAsJsonObject();
            Instant localTs = Instant.parse(metadata.get("timestamp").getAsString());
            if (localTs.isAfter(receivedTs)) {
                return false;
            }
        } catch (IOException e) {
            // Ignored, file not found
        }
        return true;
    }

    private byte[] getCorrectKey(JsonObject keys) {
        String encodedKey = keys.get(sessionInfo.getUsername()).getAsString();
        if (encodedKey != null) {
            return SecurityUtils.decodeBase64(encodedKey);
        } else {
            System.err.println("This should not have happened.");
            return new byte[0];
        }
    }

    private X509Certificate getUserCert(String user, HttpClient client) {
        JsonObject response = Utils.requestGetFromURL(ClientVariables.URL + "/users/certs/" + user, client);
        if (response.get("status").getAsInt() == HttpURLConnection.HTTP_OK) {
            byte[] cert =  SecurityUtils.decodeBase64(response.getAsJsonObject("certs").get("sign").getAsString());
            return SecurityUtils.getCertFromBytes(cert);
        } else {
            Utils.handleError(response, this.sessionInfo);
        }
        return null;
    }

    String getOutputFilePath() {
        return this.outputPath + '/' + owner + '/' + filename;
    }

    boolean hasSuccess() {
        return this.success;
    }
}