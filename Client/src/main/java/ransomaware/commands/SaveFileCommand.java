package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;
import ransomaware.SessionInfo;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;

public class SaveFileCommand extends AbstractCommand {

    private final String owner;
    private final String filename;
    private final SessionInfo sessionInfo;

    public SaveFileCommand(SessionInfo sessionInfo, String owner, String filename) {
        this.owner = owner;
        this.filename = filename;
        this.sessionInfo = sessionInfo;
    }

    @Override
    public void run(HttpClient client) {
        try {
            String filePath = Utils.getFilePath(owner, filename);

            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("File not found locally.");
                return;
            }

            SecretKey key = SecurityUtils.generateAesKey();
            IvParameterSpec iv = SecurityUtils.generateIV();

            byte[] data = Files.readAllBytes(Path.of(filePath));
            String encodedData = SecurityUtils.getBase64(data);

            JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();

            JsonObject info = JsonParser.parseString("{}").getAsJsonObject();

            JsonObject keys = getFileKeys(client, key.getEncoded());

            info.add("keys", keys);
            info.addProperty("iv", SecurityUtils.getBase64(iv.getIV()));
            info.addProperty("author", sessionInfo.getUsername());

            JsonObject jsonFile = JsonParser.parseString("{}").getAsJsonObject();
            jsonFile.addProperty("data", encodedData);
            jsonFile.add("info", info);
//            String signature = SecurityUtils.signFile(sessionInfo.getPrivateKeyPath(), jsonFile.toString().getBytes());
//            jsonRoot.addProperty("fileSignature", signature);


            byte[] encryptedData = SecurityUtils.aesCipher(Cipher.ENCRYPT_MODE, jsonFile.toString().getBytes(), key, iv);
            String decodedEncryptedData = SecurityUtils.getBase64(encryptedData);
            jsonRoot.addProperty("file", decodedEncryptedData);
            jsonRoot.add("info", info);

            JsonObject requestInfo = JsonParser.parseString("{}").getAsJsonObject();
            requestInfo.addProperty("user", owner);
            requestInfo.addProperty("name", filename);
            jsonRoot.add("requestInfo", requestInfo);

            JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/save", jsonRoot, client);
            if (response.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
                Utils.handleError(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JsonObject getFileKeys(HttpClient client, byte[] secretKey) {
        JsonObject response = Utils.requestGetFromURL(ClientVariables.URL + "/files/certs/" + owner + '/' + filename, client);

        JsonObject result = JsonParser.parseString("{}").getAsJsonObject();

        response.getAsJsonObject("certs").entrySet().forEach(entry -> {
                byte[] decodedCert = SecurityUtils.decodeBase64(entry.getValue().getAsString());
                PublicKey userKey = SecurityUtils.getKeyFromCert(SecurityUtils.getCertFromBytes(decodedCert));
                String encryptedKey = SecurityUtils.getBase64(SecurityUtils.rsaCipher(Cipher.ENCRYPT_MODE, secretKey, userKey));

                result.addProperty(entry.getKey(), encryptedKey);
            });

        return result;
    }
}