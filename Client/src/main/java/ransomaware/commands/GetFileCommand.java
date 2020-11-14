package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;

import java.io.File;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.FileWriter;
import java.nio.file.Path;

public class GetFileCommand extends AbstractCommand {

    public GetFileCommand() {
        super();
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
            //TODO
            jsonRoot.addProperty("login-token", null);

            String response = requestPostFromURL(ClientVariables.URL + "/get", jsonRoot, client);
            
            JsonObject json = JsonParser.parse(response).asJsonObject();

            Path path = new Path(filename);
            byte[] data = SecurityUtils.decodeBase64(json.get("data").getAsString());

            Files.write(path, data);
            Files.close();
        } catch (Exception e) {
            // FIXME:
            e.printStackTrace();
        }

        return true;
    }
}