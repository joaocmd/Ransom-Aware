package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;

import java.io.Console;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;


public class RegisterCommand extends AbstractCommand{

    @Override
    public void run(HttpClient client) {
        Console console = System.console();
        String user = console.readLine("user: ");
        String password = new String(console.readPassword("password: "));

        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("username", user);
        jsonRoot.addProperty("password", password);

        JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/register", jsonRoot, client);
        if (response.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
            Utils.handleError(response);
        }
    }
}
