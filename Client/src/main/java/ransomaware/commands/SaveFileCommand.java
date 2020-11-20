package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;

public class SaveFileCommand extends AbstractCommand {

    private final String owner;
    private final String filename;

    public SaveFileCommand(String owner, String filename) {
        this.owner = owner;
        this.filename = filename;
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
            info.addProperty("key", SecurityUtils.getBase64(key.getEncoded()));
            info.addProperty("iv", SecurityUtils.getBase64(iv.getIV()));

            JsonObject jsonFile = JsonParser.parseString("{}").getAsJsonObject();
            jsonFile.addProperty("data", encodedData);
            jsonFile.add("info", info);


            byte[] encryptedData = SecurityUtils.AesCipher(Cipher.ENCRYPT_MODE, jsonFile.toString().getBytes(), key, iv);
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
}