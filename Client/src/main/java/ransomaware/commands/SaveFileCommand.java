package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;

public class SaveFileCommand extends AbstractCommand {

    private final int sessionToken;
    private final String owner;
    private final String filename;

    public SaveFileCommand(int sessionToken, String owner, String filename) {
        this.sessionToken = sessionToken;
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

            byte[] data = Files.readAllBytes(Path.of(filePath));
            String encodedData = SecurityUtils.getBase64(data);

            JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
            jsonRoot.addProperty("data", encodedData);
            JsonObject jsonInfo = JsonParser.parseString("{}").getAsJsonObject();
            jsonInfo.addProperty("user", owner);
            jsonInfo.addProperty("name", filename);
            jsonRoot.add("info", jsonInfo);
            Utils.addLoginToken(jsonRoot, sessionToken);

            JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/save", jsonRoot, client);
            if (response.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
                Utils.handleError(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}