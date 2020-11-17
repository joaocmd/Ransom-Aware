package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SessionInfo;

import java.io.Console;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;

public class LoginCommand extends AbstractCommand {

    private final SessionInfo info;

    public LoginCommand(SessionInfo info) {
        this.info = info;
    }

    @Override
    public void run(HttpClient client) {
        Console console = System.console();
        String username = console.readLine("username: ");
        String password = new String(console.readPassword("password: "));

        // Create JSON
        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("username", username);
        jsonRoot.addProperty("password", password);

        // Send request
        JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/login", jsonRoot, client);
        if (response.get("status").getAsInt() == HttpURLConnection.HTTP_OK) {
            info.setUsername(username);
            info.setLogged(true);
        } else {
            Utils.handleError(response);
        }
    }
}