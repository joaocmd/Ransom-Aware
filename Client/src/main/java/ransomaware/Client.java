package ransomaware.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.io.*;

import javax.net.ssl.*;

public class Client {

    public static void requestGetFromURL(String url) {
        try {
            System.setProperty("javax.net.ssl.trustStore", ClientVariables.KEYSTORE);
            System.setProperty("javax.net.ssl.trustStorePassword", ClientVariables.SSL_STOREPASS);
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

            while ((inputLine = br.readLine()) != null) {
                System.out.println(inputLine);
            }

            br.close();
        } catch (Exception e) {
            //FIXME: UGLY
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}
