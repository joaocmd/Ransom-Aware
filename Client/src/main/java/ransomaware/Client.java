package ransomaware;

import java.time.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpClient.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;
import java.security.cert.Certificate;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import com.google.gson.*;
import java.util.concurrent.*;

import javax.net.ssl.*;

public class Client {
    static ExecutorService executor = Executors.newSingleThreadExecutor();
    static HttpClient client = HttpClient.newBuilder().executor(executor).build();
    static int sessionToken;

    public static void start() {
        Console console = System.console();
        String command = "";

        do {
            command = console.readLine("> ");
            String[] args = command.split(" ");

            // TODO: Commands
            switch (args[0]) {
                // list
                case ("list"):
                    if (args.length > 1) {
                        System.out.println("List: Too many arguments.\nExample: list");
                        break;
                    }
                    String response = requestGetFromURL(ClientVariables.URL + "/list");
                    System.out.print(response);
                    break;
                // get user/file.txt
                case ("get"):
                    getFile(args);
                    break;
                // send file.txt
                case ("save"):
                    saveFile(args);
                    break;
                // login
                case ("login"):
                    login(args);
                    break;
                case ("help"):
                    // TODO: Show commands
                    break;
                case ("exit"):
                    break;
                default:
                    System.out.println("Command not found.");
            }

        } while (!command.equals("exit"));

        // Shutdown HTTP Client
        executor.shutdownNow();
        client = null;
        System.gc();
    }

    /**
     * getFile
     * @param args - for example: ['get','a.txt'] or ['get','masterzeus','a.txt']
     */
    public static void getFile(String[] args) {
        if (args.length == 1 || args.length > 2) {
            System.out.println("get: Too many arguments.\nExample: get a.txt");
            return;
        }
        String file[] = args[1].split("/"), user = "", filename = "";
        if (file.length == 1) { user = ""; filename = file[0]; }
        else if (file.length == 2) { user = file[0]; filename = file[1]; }

        try {
            // TODO: Send request to get file
            // TODO: Write file

        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println(user + " - " + filename);
    }

    /**
     * saveFile
     * @param args - for example: ['save','a.txt'] or ['save','masterzeus','a.txt']
     */
    public static void saveFile(String[] args) {
        if (args.length == 1 || args.length > 2) {
            System.out.println("save: Too many arguments.\nExample: save a.txt");
            return;
        }
        String givenFile[] = args[1].split("/"), user = "", filename = "";
        if (givenFile.length == 1) { user = ""; filename = givenFile[0]; }
        else if (givenFile.length == 2) { user = givenFile[0]; filename = givenFile[1]; }

        try {
            String filePath = ClientVariables.FS_PATH + '/' + user + '/' + filename;

            // Check if file exists
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("File not found.");
                return;
            }

            // Read file to bytes
            byte[] data = Files.readAllBytes(Path.of(filePath));

            // Pass string file to base64
            String encodedData = SecurityUtils.getBase64(data);;

            // Create JSON
            JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
            jsonRoot.addProperty("data", encodedData);
            JsonObject jsonInfo = JsonParser.parseString("{}").getAsJsonObject();
            jsonInfo.addProperty("user", user);
            jsonInfo.addProperty("name", filename);
            jsonRoot.add("info", jsonInfo);

            System.out.println(jsonRoot);

            // Send request
            String response = requestPostFromURL(ClientVariables.URL + "/save", jsonRoot);

            System.out.println("Response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * login
     * @param args
     */
    public static void login(String[] args) {
        if (args.length != 1) {
            System.out.println("login: Too many arguments.\nExample: login");
            return;
        }

        Console console = System.console();
        String user = console.readLine("user: ");
        String password = new String(console.readPassword("password: "));

        // Create JSON
        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("username", user);
        jsonRoot.addProperty("password", password);

        // Send request
        String response = requestPostFromURL(ClientVariables.URL + "/login", jsonRoot);

        // TODO: Store session token or check if error
        System.out.println("Response: " + response);

    }

    // ----

    public static String requestGetFromURL(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response =
                    client.send(request, BodyHandlers.ofString());

            return response.body();
        } catch (Exception e) {
            // FIXME: UGLY
            e.printStackTrace();
            System.exit(1);
        }

        return "";
    }

    public static String requestPostFromURL(String url, JsonObject jsonObject) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(jsonObject.toString()))
                    .build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            return response.body();
        } catch (Exception e) {
            // FIXME: UGLY
            e.printStackTrace();
            System.exit(1);
        }

        return "";
    }
}
