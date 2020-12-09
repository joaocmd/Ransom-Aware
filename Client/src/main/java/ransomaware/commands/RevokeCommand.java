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
    private final String owner;
    private final String filename;
    private final String userToRevoke;

    public RevokeCommand(SessionInfo sessionInfo, String owner, String filename, String userToRevoke) {
        this.sessionInfo = sessionInfo;
        this.owner = owner;
        this.filename = filename;
        this.userToRevoke = userToRevoke;
    }

    @Override
    public void run(HttpClient client) {
        String user = sessionInfo.getUsername();

        // Verify if revoking permissions to the owner
        if (owner.equals(userToRevoke)) {
            System.err.println("You can't revoke permission to owner");
            return;
        }

        // Verify if revoking permissions to user itself
        if (user.equals(userToRevoke)) {
            System.err.println("You can't revoke permission to yourself");
            return;
        }

        // Make sure tmp folder is created
        File dir = new File(ClientVariables.TMP_PATH);
        dir.mkdirs();

        // Get file to temporary folder
        GetFileCommand getCommand = new GetFileCommand(sessionInfo, user, filename, ClientVariables.TMP_PATH);
        getCommand.run(client);
        if (!getCommand.hasSuccess()) {
            return;
        }
        String filePath = getCommand.getOutputFilePath();

        // Send revoke request, adding certificate to new save
        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("user", userToRevoke);
        jsonRoot.addProperty("file", user + '/' + filename);

        JsonObject responseRevoke = Utils.requestPostFromURL(ClientVariables.URL + "/revoke", jsonRoot, client);
        if (responseRevoke.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
            Utils.handleError(responseRevoke, this.sessionInfo);
            return;
        }

        // Save file to server
        SendFileCommand saveCommand = new SendFileCommand(sessionInfo, filePath, true);
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
