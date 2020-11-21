package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;

public class GetFileCommand extends AbstractCommand {

    private final String owner;
    private final String filename;
    private final String outputPath;
    private boolean success;

    public GetFileCommand(String owner, String filename, String outputPath) {
        this.owner = owner;
        this.filename = filename;
        this.outputPath = outputPath;
        this.success = false;
    }

    public GetFileCommand(String owner, String filename) {
        this(owner, filename, ClientVariables.WORKSPACE);
    }

    @Override
    public void run(HttpClient client) {
        try {
            JsonObject response = Utils.requestGetFromURL(ClientVariables.URL + "/files" + '/' + owner + '/' + filename, client);
            if (response.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
                Utils.handleError(response);
                return;
            }

            JsonObject file = response.getAsJsonObject("file");
            byte[] encryptedFile = SecurityUtils.decodeBase64(file.get("data").getAsString());

            JsonObject info = file.getAsJsonObject("info");
            byte[] keyBytes = SecurityUtils.decodeBase64(info.get("key").getAsString());
            SecretKey key = SecurityUtils.getKeyFromBytes(keyBytes);
            byte[] iv = SecurityUtils.decodeBase64(info.get("iv").getAsString());

            byte[] unencryptedData = SecurityUtils.AesCipher(Cipher.DECRYPT_MODE, encryptedFile, key, new IvParameterSpec(iv));
            JsonObject fileJson = JsonParser.parseString(new String(unencryptedData)).getAsJsonObject();

            if (!fileJson.getAsJsonObject("info").equals(info)) {
                System.out.println("WARNING: Signed info did not match public info");
                // TODO: prompt continue
            }

            byte[] fileData = SecurityUtils.decodeBase64(fileJson.get("data").getAsString());

            File dir = new File(this.outputPath + '/' + owner);
            dir.mkdirs();
            Files.write(Path.of(this.outputPath + '/' + owner + '/' + filename), fileData);

            this.success = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String getOutputFilePath() {
        return this.outputPath + '/' + owner + '/' + filename;
    }

    boolean hasSuccess() {
        return this.success;
    }
}