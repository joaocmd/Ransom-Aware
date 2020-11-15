package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;

public class GetFileCommand extends AbstractCommand {

    private final String owner;
    private final String filename;
    private final int sessionToken;

    public GetFileCommand(int sessionToken, String owner, String filename) {
        this.owner = owner;
        this.filename = filename;
        this.sessionToken = sessionToken;
    }

    @Override
    public void run(HttpClient client) {
        try {
            JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
            jsonRoot.addProperty("user", owner);
            jsonRoot.addProperty("name", filename);
            Utils.addLoginToken(jsonRoot, sessionToken);

            JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/files", jsonRoot, client);
            if (response.get("status").getAsInt() == HttpURLConnection.HTTP_OK) {
                byte[] data = SecurityUtils.decodeBase64(response.get("file").getAsString());
                File dir = new File(ClientVariables.FS_PATH + '/' + owner);
                dir.mkdirs();
                Files.write(Path.of(ClientVariables.FS_PATH + '/' + owner + '/' + filename), data);
            } else {
                Utils.handleError(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}