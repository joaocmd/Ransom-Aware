package ransomaware.commands;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public abstract class AbstractCommand {

    String sessionToken;

    public AbstractCommand() { }

    public AbstractCommand(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public abstract boolean run(String[] args, HttpClient client);

    public String getSessionToken() {
        return sessionToken;
    }
    String requestPostFromURL(String url, JsonObject jsonObject, HttpClient client) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Cache-Control", "no-store")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();
        } catch (Exception e) {
            // FIXME: UGLY
            e.printStackTrace();
            System.exit(1);
        }

        return "";
    }

    protected static void handleError(JsonObject err) {
        System.err.println(err.get("status").getAsString() + ": " + err.get("body").getAsString());
    }
}