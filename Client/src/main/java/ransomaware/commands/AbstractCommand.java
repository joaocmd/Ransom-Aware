package ransomaware.commands;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public abstract class AbstractCommand {

    public AbstractCommand() {
    }

    public abstract boolean run(String[] args, HttpClient client);

    public int getSessionToken() {
        return 0;
    }

    String requestGetFromURL(String url, HttpClient client) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();
        } catch (Exception e) {
            // FIXME: UGLY
            e.printStackTrace();
            System.exit(1);
        }

        return "";
    }

    String requestPostFromURL(String url, JsonObject jsonObject, HttpClient client) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
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
}