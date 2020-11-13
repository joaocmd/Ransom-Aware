package ransomaware;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import com.google.gson.*;

import javax.net.ssl.*;

public class Client {

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
                case ("help"):
                    // TODO: Show commands
                    break;
                default:
                    System.out.println("Command not found.");
            }

        } while (!command.equals("exit"));
    }

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

            System.out.println(jsonRoot);

            // Send file
            // TODO: Send request

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String requestGetFromURL(String url) {
        try {
            // TODO: use custom keystore if desired
            // System.setProperty("javax.net.ssl.trustStore", ClientVariables.KEYSTORE);
            // System.setProperty("javax.net.ssl.trustStorePassword", ClientVariables.SSL_STOREPASS);
            URL myUrl = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) myUrl.openConnection();

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String inputLine;
            StringBuilder output = new StringBuilder();

            while ((inputLine = br.readLine()) != null) {
                output.append(inputLine + '\n');
            }

            br.close();

            return output.toString();
        } catch (Exception e) {
            //FIXME: UGLY
            System.out.println(e.getMessage());
            System.exit(1);
        }

        return "";
    }
}
