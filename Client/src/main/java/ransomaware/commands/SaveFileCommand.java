package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;

import java.io.File;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;

public class SaveFileCommand extends AbstractCommand {

    private String username;

    public SaveFileCommand(String sessionToken, String username) {
        super(sessionToken);
        this.username = username;
    }

    /**
     * run
     * @param args - for example: ['save','a.txt'] or ['save','masterzeus','a.txt']
     * @param client - the HttpClient
     * @return if the commands has succeeded
     */
    public boolean run(String[] args, HttpClient client) {
        if (args.length != 2) {
            System.out.println("save: Too many arguments.\nExample: save a.txt");
            return false;
        }

        String[] givenFile = args[1].split("/");
        String user;
        String filename;

        if(givenFile.length == 0 || givenFile.length > 2) {
            System.out.println("bad file-name: expected <user>/<file> or simply <file>.");
            return false;
        }

        if (givenFile.length == 1) {
            user = this.username;
            filename = givenFile[0];
        } else {
            user = givenFile[0];
            filename = givenFile[1];
        }

        try {
            String filePath = ClientVariables.FS_PATH + '/' + user + '/' + filename;
            System.out.println(filePath);

            // Check if file exists
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("File not found.");
                return false;
            }

            // Read file to bytes
            byte[] data = Files.readAllBytes(Path.of(filePath));

            // Pass string file to base64
            String encodedData = SecurityUtils.getBase64(data);

            // Create JSON
            JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
            jsonRoot.addProperty("data", encodedData);
            JsonObject jsonInfo = JsonParser.parseString("{}").getAsJsonObject();
            jsonInfo.addProperty("user", user);
            jsonInfo.addProperty("name", filename);
            jsonRoot.add("info", jsonInfo);
            // FIXME: this should be in all methods
            jsonRoot.addProperty("login-token", Integer.valueOf(super.getSessionToken()));

            System.out.println(jsonRoot);

            // Send request
            String response = super.requestPostFromURL(ClientVariables.URL + "/save", jsonRoot, client);

            System.out.println("Response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}