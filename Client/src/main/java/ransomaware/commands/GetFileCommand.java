package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import ransomaware.Client;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;

public class GetFileCommand extends AbstractCommand {

    private String username;

    public GetFileCommand(String sessionToken, String username) {
        super(sessionToken);
        this.username = username;
    }

    /**
     * run
     * @param args - for example: ['get','a.txt'] or ['get','masterzeus','a.txt']
     * @param client - the HttpClient
     * @return if the commands has succeeded
     */
    public boolean run(String[] args, HttpClient client) {
        if (args.length != 2) {
            System.out.println("get: Too many arguments.\nExample: get a.txt");
            return false;
        }
        String[] file = args[1].split("/");
        String user;
        String filename;

        if(file.length == 0 || file.length > 2) {
            System.out.println("bad file-name: expected <user>/<file> or simply <file>.");
            return false;
        }

        if (file.length == 1) {
            user = this.username;
            filename = file[0];
        } else {
            user = file[0];
            filename = file[1];
        }

        try {
            JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
            jsonRoot.addProperty("user", user);
            jsonRoot.addProperty("name", filename);


            // FIXME: this should be in all methods
            jsonRoot.addProperty("login-token", Integer.valueOf(super.getSessionToken()));

            String response = requestPostFromURL(ClientVariables.URL + "/files", jsonRoot, client);

            JsonObject json =  JsonParser.parseString(response).getAsJsonObject();

            switch (json.get("status").getAsInt()) {
                case HttpURLConnection.HTTP_OK:
                    byte[] data = SecurityUtils.decodeBase64(json.get("file").getAsString());
                    File dir = new File(ClientVariables.FS_PATH + '/' + user);
                    dir.mkdirs();
                    Files.write(Path.of(ClientVariables.FS_PATH + '/' + user + '/' + filename), data);
                    return true;
                default:
                    handleError(json);
                    return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  false;
    }
}