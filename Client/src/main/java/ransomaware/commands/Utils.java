package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Utils {

    private Utils() {}

    public static void handleError(JsonObject err) {
        System.err.println(err.get("status").getAsString() + ": " + err.get("body").getAsString());
    }

    public static void addLoginToken(JsonObject obj, int token) {
        obj.addProperty("login-token", token);
    }

    public static JsonObject requestPostFromURL(String url, JsonObject jsonObject, HttpClient client) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Cache-Control", "no-store")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getFilePath(String owner, String filename) {
        return ClientVariables.FS_PATH + '/' + owner + '/' + filename;
    }

    public static String getUserDirectory(String user) {
        return ClientVariables.FS_PATH + '/' + user;
    }
}
