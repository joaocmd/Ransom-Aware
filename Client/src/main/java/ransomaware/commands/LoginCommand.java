package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;

import java.io.Console;
import java.net.http.HttpClient;

public class LoginCommand extends AbstractCommand {
    int sessionToken;

    public LoginCommand() {
        super();
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
        System.out.println("Response: " + response);
        return true;
    }

    @Override
    public int getSessionToken() {
        return sessionToken;
    }
}