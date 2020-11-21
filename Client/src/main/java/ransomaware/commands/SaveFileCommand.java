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

public class SaveFileCommand extends AbstractCommand {

    private final String owner;
    private final String filename;
    private SessionInfo info;

    public SaveFileCommand(String owner, String filename, SessionInfo info) {
        this.owner = owner;
        this.filename = filename;
        this.info = info;
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
            byte[] encryptedData = SecurityUtils.AesCipher(Cipher.ENCRYPT_MODE, data, key, iv);


            System.out.println(new String(encryptedData));


            String encodedData = SecurityUtils.getBase64(encryptedData);
            String signature = SecurityUtils.signFile(info.getPrivateKeyPath(), encryptedData);

            JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();

            JsonObject jsonFile = JsonParser.parseString("{}").getAsJsonObject();
            jsonFile.addProperty("data", encodedData);
            jsonFile.addProperty("signature", signature);

            JsonObject fileInfo = JsonParser.parseString("{}").getAsJsonObject();
            fileInfo.addProperty("key", SecurityUtils.getBase64(key.getEncoded()));
            fileInfo.addProperty("iv", SecurityUtils.getBase64(iv.getIV()));
            fileInfo.addProperty("by", info.getUsername());
            jsonFile.add("info", fileInfo);
            jsonRoot.add("file", jsonFile);

            JsonObject requestInfo = JsonParser.parseString("{}").getAsJsonObject();
            requestInfo.addProperty("user", owner);
            requestInfo.addProperty("name", filename);
            jsonRoot.add("info", requestInfo);

            JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/save", jsonRoot, client);
            if (response.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
                Utils.handleError(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}