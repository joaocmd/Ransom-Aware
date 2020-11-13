package ransomaware;

import java.io.File;

public class RansomAware {

    public RansomAware(String path, String url) {
//        System.setProperty("javax.net.ssl.trustStore", ClientVariables.KEYSTORE);
//        System.setProperty("javax.net.ssl.trustStorePassword", ClientVariables.SSL_STOREPASS);

        ClientVariables.init(path, url);
        Client.start();
    }
}
