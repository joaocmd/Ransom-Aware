package ransomaware.commands;

import com.google.gson.JsonObject;
import ransomaware.ClientVariables;
import ransomaware.SessionInfo;

import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

public class ListFilePermissionsCommand implements Command {

    private final SessionInfo info;
    private final String owner;
    private final String filename;

    public ListFilePermissionsCommand(SessionInfo info, String owner, String filename) {
        this.info = info;
        this.owner = owner;
        this.filename = filename;
    }

    @Override
    public void run(HttpClient client) {
        JsonObject response = Utils.requestGetFromURL(ClientVariables.URL + "/files/certs/"  + owner + '/' + filename, client);
        if (response.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
            Utils.handleError(response, this.info);
            return;
        }

        List<String> users = new ArrayList<>();
        System.out.println("- " + owner + " (Owner)");
        response.getAsJsonObject("certs").keySet().forEach(user -> {
            if (!user.equals(owner)) {
                users.add(user);
            }
        });

        if (!users.isEmpty()) {
            users.stream().sorted().forEachOrdered(e -> System.out.println("- " + e));
        }
    }
}