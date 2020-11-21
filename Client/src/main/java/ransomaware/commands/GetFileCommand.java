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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Optional;

public class GetFileCommand extends AbstractCommand {

    private final SessionInfo sessionInfo;
    private final String owner;
    private final String filename;
    private final String outputPath;
    private boolean success;

    public GetFileCommand(SessionInfo sessionInfo, String owner, String filename, String outputPath) {
        this.sessionInfo = sessionInfo;
        this.owner = owner;
        this.filename = filename;
        this.outputPath = outputPath;
        this.success = false;
    }

    public GetFileCommand(SessionInfo sessionInfo, String owner, String filename) {
        this(sessionInfo, owner, filename, ClientVariables.WORKSPACE);
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
//            String signature = file.get("signature").getAsString();
//            byte[] certificate = SecurityUtils.decodeBase64(response.get("certificate").getAsString());

            byte[] keyBytes = getCorrectKey(info.getAsJsonObject("keys"));
            PrivateKey privKey = SecurityUtils.readPrivateKey(sessionInfo.getEncryptKeyPath());
            SecretKey key = SecurityUtils.getKeyFromBytes(SecurityUtils.rsaCipher(Cipher.DECRYPT_MODE, keyBytes, privKey));
            byte[] iv = SecurityUtils.decodeBase64(info.get("iv").getAsString());

            byte[] unencryptedData = SecurityUtils.aesCipher(Cipher.DECRYPT_MODE, encryptedFile, key, new IvParameterSpec(iv));
            JsonObject fileJson = JsonParser.parseString(new String(unencryptedData)).getAsJsonObject();
//            if(!SecurityUtils.validSignature(signature, unencryptedData, certificate)) {
//                System.err.println("Bad signature");
//                return;
//            }

            if (!fileJson.getAsJsonObject("info").equals(info)) {
                System.out.println(fileJson.getAsJsonObject("info").toString());
                System.out.println(info.toString());
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

    private byte[] getCorrectKey(JsonObject keys) {
        Optional<String> encodedKey = keys.entrySet().stream()
                .filter(e -> e.getKey().equals(sessionInfo.getUsername()))
                .map(e -> e.getValue().getAsString())
                .findFirst();
        if (encodedKey.isPresent()) {
            return SecurityUtils.decodeBase64(encodedKey.get());
        } else {
            System.err.println("This should not have happened.");
            return new byte[0];
        }
    }

    String getOutputFilePath() {
        return this.outputPath + '/' + owner + '/' + filename;
    }

    boolean hasSuccess() {
        return this.success;
    }
}