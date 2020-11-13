package ransomaware;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.io.*;

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
                case ("send"):
                    sendFile(args);
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
        if (file.length == 1) { user = "me"; filename = file[0]; }
        else if (file.length == 2) { user = file[0]; filename = file[1]; }

        System.out.println(user + " - " + filename);
    }

    public static void sendFile(String[] args) {
        if (args.length == 1 || args.length > 2) {
            System.out.println("send: Too many arguments.\nExample: send a.txt");
            return;
        }
        String file[] = args[1].split("/"), user = "", filename = "";
        if (file.length == 1) { user = "me"; filename = file[0]; }
        else if (file.length == 2) { user = file[0]; filename = file[1]; }

        System.out.println(user + " - " + filename);
    }

    public static String requestGetFromURL(String url) {
        try {
            // TODO: use custom keystore if desired
//            System.setProperty("javax.net.ssl.trustStore", ClientVariables.KEYSTORE);
//            System.setProperty("javax.net.ssl.trustStorePassword", ClientVariables.SSL_STOREPASS);
            URL myUrl = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) myUrl.openConnection();

            // We bypass the hostname verifier, since we don't use a root CA
            conn.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String inputLine;
            StringBuilder output = new StringBuilder();

            while ((inputLine = br.readLine()) != null) {
                // System.out.println(inputLine);
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
