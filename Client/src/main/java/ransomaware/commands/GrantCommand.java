package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SessionInfo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GrantCommand extends AbstractCommand {

    private final SessionInfo sessionInfo;
    private final String filename;
    private final String userToGrant;

    public GrantCommand(SessionInfo sessionInfo, String filename, String userToGrant) {
        this.sessionInfo = sessionInfo;
        this.filename = filename;
        this.userToGrant = userToGrant;
    }

    @Override
    public void run(HttpClient client) {
        String owner = sessionInfo.getUsername();
        // Verify if logged in // FIXME this will be general and will be done in the parser
        if (!sessionInfo.isLogged()) {
            System.err.println("Login first.");
            return;
        }

        // Verify if giving permissions to self
        if (owner.equals(userToGrant)) {
            System.err.println("You shouldn't give permissions to yourself....");
            return;
        }

        // Get file to temporary folder
        GetFileCommand getCommand = new GetFileCommand(sessionInfo, owner, filename, ClientVariables.TMP_PATH);
        getCommand.run(client);
        if (!getCommand.hasSuccess()) {
            return;
        }
        String filePath = getCommand.getOutputFilePath();

        // Get certificates
        JsonObject responseCert = Utils.requestGetFromURL(ClientVariables.URL + "/users/certs/" + userToGrant, client);
        if (responseCert.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
            Utils.handleError(responseCert);
            return;
        }

        JsonObject certs = responseCert.getAsJsonObject("certs");
        String encryptCert = certs.get("encrypt").getAsString();

        // Add certificate to save command

        // Save file to server
        // TODO: Don't forget that the file is in a temporary folder
        // SaveFileCommand saveCommand = new SaveFileCommand(owner, filename);
        // saveCommand.run(client);

        // Remove file
        try {
            Path fileToDeletePath = Paths.get(filePath);
            Files.delete(fileToDeletePath);
        } catch (IOException ignored) {
            // ignored since we created it first, and if already erased it is intended.
        }

        // Send grant request
        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("user", userToGrant);
        jsonRoot.addProperty("file", owner + '/' + filename);

        JsonObject responseGrant = Utils.requestPostFromURL(ClientVariables.URL + "/grant", jsonRoot, client);
        if (responseGrant.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
            Utils.handleError(responseGrant);
            return;
        }

        System.out.println("Permissions successfully granted");
    }
}