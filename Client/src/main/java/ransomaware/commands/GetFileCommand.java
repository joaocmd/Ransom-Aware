package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;

public class GetFileCommand extends AbstractCommand {

    public GetFileCommand(String sessionToken) {
        super(sessionToken);
    }

    /**
     * run
     * @param args - for example: ['get','a.txt'] or ['get','masterzeus','a.txt']
     * @param client - the HttpClient
     * @return if the commands has succeeded
     */
    public boolean run(String[] args, HttpClient client) {
        if (args.length == 1 || args.length > 2) {
            System.out.println("get: Too many arguments.\nExample: get a.txt");
            return false;
        }
        String[] file = args[1].split("/");
        String user = "";
        String filename = "";
        if (file.length == 1) { user = ""; filename = file[0]; }
        else if (file.length == 2) { user = file[0]; filename = file[1]; }

        try {
            JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
            jsonRoot.addProperty("user", user);
            jsonRoot.addProperty("name", filename);
            // FIXME: this should be in all methods
            jsonRoot.addProperty("login-token", Integer.valueOf(super.getSessionToken()));

            String response = requestPostFromURL(ClientVariables.URL + "/files", jsonRoot, client);
            
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            byte[] data = SecurityUtils.decodeBase64(json.get("data").getAsString());

            Files.write(Path.of(ClientVariables.FS_PATH + filename), data);
        } catch (Exception e) {
            // FIXME:
            e.printStackTrace();
        }

        return true;
    }
}