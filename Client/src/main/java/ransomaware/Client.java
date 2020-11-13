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
            String[] args;

            // TODO: Commands
            switch (command) {
                case ("list"):
                    String response = requestGetFromURL(ClientVariables.URL + "/list");
                    System.out.print(response);
                    break;
                case ()
                default:
                    System.out.println("Command not found.");
            }

        } while (!command.equals("exit"));
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
