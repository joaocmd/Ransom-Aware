package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SessionInfo;
import ransomaware.exceptions.ConnectionException;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

public class Utils {

    private Utils() {}

    public static void handleError(JsonObject err, SessionInfo info) {
        if(err.get("status").getAsInt() == HttpURLConnection.HTTP_UNAUTHORIZED && err.get("body").getAsString().contains("token")) {
            info.logOff();
        }
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
        } catch (ConnectException e) {
            throw new ConnectionException();
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
        } catch (ConnectException e) {
            throw new ConnectionException();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getFilePath(String owner, String filename) {
        return ClientVariables.WORKSPACE + '/' + owner + '/' + filename;
    }

    public static Path getMetadataPath(String filePath) {
        Path path = Path.of(filePath);
        String fileName = '.' + path.getFileName().toString() + ".metadata";
        return path.resolveSibling(fileName);
    }

    public static void clearWorkspace(File dir) {
        if(dir == null) {
            return;
        }
        File[] files = dir.listFiles();
        if(files != null) {
            for (File file : files) {
                clearWorkspace(file);
            }
        }
        dir.delete();
    }

    public static String getUserDirectory(String user) {
        return ClientVariables.WORKSPACE + "/" + user;
    }
}
