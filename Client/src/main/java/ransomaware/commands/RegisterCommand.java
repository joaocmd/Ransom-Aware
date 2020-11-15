package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;

import java.io.Console;
import java.net.http.HttpClient;


public class RegisterCommand extends AbstractCommand{

    public boolean run(String[] args, HttpClient client) {
        if (args.length != 1) {
            System.out.println("Too many arguments.");
            return false;
        }

        Console console = System.console();
        String user = console.readLine("user: ");
        String password = new String(console.readPassword("password: "));

        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("username", user);
        jsonRoot.addProperty("password", password);

        String response = super.requestPostFromURL(ClientVariables.URL + "/register", jsonRoot, client);
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        int status = jsonResponse.get("status").getAsInt();
        if (status != 200) {
            // FIXME: Check for unauthorized, etc.
            System.out.println("Error logging in.");
            return false;
        }
        return true;
    }
}
