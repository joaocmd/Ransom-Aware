package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;

import java.net.http.HttpClient;

public class ListFilesCommand extends AbstractCommand {

    public ListFilesCommand(String sessionToken) {
        super(sessionToken);
    }

    /**
     * run
     * @param args - ['list'] at most
     * @param client - the Http Client
     * @return if the commands has succeeded
     */
    public boolean run(String[] args, HttpClient client) {
        if (args.length > 1) {
            System.out.println("List: Too many arguments.\nExample: list");
            return false;
        }

        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        // FIXME: this should be in all methods
        jsonRoot.addProperty("login-token", Integer.valueOf(super.getSessionToken()));

        String response = super.requestPostFromURL(ClientVariables.URL + "/list", jsonRoot, client);
        System.out.print(response);

        return true;
    }
}