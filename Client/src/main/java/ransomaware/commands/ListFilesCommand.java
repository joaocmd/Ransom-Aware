package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;

import java.net.http.HttpClient;

public class ListFilesCommand extends AbstractCommand {

    private final int sessionToken;

    public ListFilesCommand(int sessionToken) {
        this.sessionToken = sessionToken;
    }

    @Override
    public void run(HttpClient client) {
        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        Utils.addLoginToken(jsonRoot, sessionToken);

        JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/list", jsonRoot, client);
        System.out.println(response.toString());
    }
}