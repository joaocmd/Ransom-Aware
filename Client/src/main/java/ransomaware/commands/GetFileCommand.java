package ransomaware.commands;

import com.google.gson.JsonObject;
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

    public GetFileCommand(String owner, String filename) {
        this.owner = owner;
        this.filename = filename;
    }

    @Override
    public void run(HttpClient client) {
        try {
            JsonObject response = Utils.requestGetFromURL(ClientVariables.URL + "/files" + '/' + owner + '/' + filename, client);
            if (response.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
                Utils.handleError(response);
                return;
            }

            JsonObject fileJson = response.getAsJsonObject("file");
            byte[] data = SecurityUtils.decodeBase64(fileJson.get("data").getAsString());

            JsonObject info = fileJson.getAsJsonObject("info");
            String signature = info.get("signature").getAsString();
            byte[] certificate = SecurityUtils.decodeBase64(response.get("certificate").getAsString());
            if(!SecurityUtils.validSignature(signature, data, certificate)) {
                System.err.println("Bad signature");
                return;
            }

            byte[] keyBytes = SecurityUtils.decodeBase64(info.get("key").getAsString());
            SecretKey key = SecurityUtils.getKeyFromBytes(keyBytes);

            byte[] iv = SecurityUtils.decodeBase64(info.get("iv").getAsString());

            byte[] unencryptedData = SecurityUtils.AesCipher(Cipher.DECRYPT_MODE, data, key, new IvParameterSpec(iv));

            File dir = new File(ClientVariables.WORKSPACE + '/' + owner);
            dir.mkdirs();
            Files.write(Path.of(ClientVariables.WORKSPACE + '/' + owner + '/' + filename), unencryptedData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}