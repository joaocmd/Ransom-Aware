package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;

import java.io.File;
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

    public static JsonObject requestGetFromURL(String url, HttpClient client) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cache-Control", "no-store")
                .GET()
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
        return ClientVariables.WORKSPACE + '/' + owner + '/' + filename;
    }

    public static void clearWorkspace(File dir) {
        if(dir == null)
            return;
        File[] files = dir.listFiles();
        if(files != null) {
            for (File file : files) {
                clearWorkspace(file);
            }
        }
        dir.delete();
    }

    public static String getUserDirectory(String user) {
        return ClientVariables.WORKSPACE + '/' + user;
    }
}
