package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;

import java.net.http.HttpClient;

public class ListFilesCommand extends AbstractCommand {

    public ListFilesCommand() {
    }

    @Override
    public void run(HttpClient client) {
        JsonObject response = Utils.requestGetFromURL(ClientVariables.URL + "/list", client);
        System.out.println(response.toString());
    }
}