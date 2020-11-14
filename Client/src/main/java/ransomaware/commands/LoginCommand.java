package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;

import java.io.Console;
import java.net.http.HttpClient;

public class LoginCommand extends AbstractCommand {
    String sessionToken;

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
        String user = console.readLine("user: ");
        String password = new String(console.readPassword("password: "));

        // Create JSON
        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("username", user);
        jsonRoot.addProperty("password", password);

        // Send request
        String response = super.requestPostFromURL(ClientVariables.URL + "/login", jsonRoot, client);

        // TODO: Store session token or check if error
        // Check if error
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        int status = jsonResponse.get("status").getAsInt();
        if (status != 200) {
            // FIXME: Check for unauthorized, etc.
            System.out.println("Error logging in.");
            return false;
        }
        sessionToken = jsonResponse.get("login-token").getAsString();
        System.out.println("Login successful!");
        return true;
    }

    @Override
    public String getSessionToken() {
        return sessionToken;
    }
}