package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;

import java.io.Console;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;

public class LoginCommand extends AbstractCommand {
    private String username;

    public LoginCommand() {
        super("");
    }

    /**
     * run
     * @param args - ['login'] at most
     * @param client - the Http Client
     * @return if the commands has succeeded
     */

    public boolean run(String[] args, HttpClient client) {
        if (args.length != 1) {
            System.out.println("login: Too many arguments.\nExample: login");
            return false;
        }

        Console console = System.console();
        username = console.readLine("user: ");
        String password = new String(console.readPassword("password: "));

        // Create JSON
        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("username", username);
        jsonRoot.addProperty("password", password);

        // Send request
        String response = super.requestPostFromURL(ClientVariables.URL + "/login", jsonRoot, client);

        // TODO: Store session token or check if error
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        switch (jsonResponse.get("status").getAsInt()) {
            case HttpURLConnection.HTTP_OK:
                sessionToken = jsonResponse.get("login-token").getAsString();
                return true;
            default:
                handleError(jsonResponse);
                return false;
        }
    }

    public String getUsername() {
        return username;
    }
}