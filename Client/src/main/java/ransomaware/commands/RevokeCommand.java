package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SessionInfo;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RevokeCommand implements Command {

    private final SessionInfo sessionInfo;
    private final String filename;
    private final String userToRevoke;

    public RevokeCommand(SessionInfo sessionInfo, String filename, String userToRevoke) {
        this.sessionInfo = sessionInfo;
        this.filename = filename;
        this.userToRevoke = userToRevoke;
    }

    @Override
    public void run(HttpClient client) {
        String owner = sessionInfo.getUsername();

        // Verify if revoking permissions to self
        if (owner.equals(userToRevoke)) {
            System.err.println("You shouldn't revoke permissions from yourself....");
            return;
        }

        // Make sure tmp folder is created
        File dir = new File(ClientVariables.TMP_PATH);
        dir.mkdirs();

        // Get file to temporary folder
        GetFileCommand getCommand = new GetFileCommand(sessionInfo, owner, filename, ClientVariables.TMP_PATH);
        getCommand.run(client);
        if (!getCommand.hasSuccess()) {
            return;
        }
        String filePath = getCommand.getOutputFilePath();

        // Send revoke request, adding certificate to new save
        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("user", userToRevoke);
        jsonRoot.addProperty("file", owner + '/' + filename);

        JsonObject responseRevoke = Utils.requestPostFromURL(ClientVariables.URL + "/revoke", jsonRoot, client);
        if (responseRevoke.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
            Utils.handleError(responseRevoke);
            return;
        }

        // Save file to server
        SaveFileCommand saveCommand = new SaveFileCommand(sessionInfo, filePath);
        saveCommand.run(client);

        // Remove file
        try {
            Path fileToDeletePath = Paths.get(filePath);
            Files.delete(fileToDeletePath);
        } catch (IOException ignored) {
            // ignored since we created it first, and if already erased it is intended.
        }

        System.out.println("Permissions successfully revoked");
    }
}
