package ransomaware;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import ransomaware.handlers.LoginHandler;
import ransomaware.handlers.RegisterHandler;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

public class Server {

    private static HttpsServer prepareHttpsServer(RansomAware domain, int port) {
        SSLContext context = null;
        HttpsServer server = null;
        try {
            server = HttpsServer.create(new InetSocketAddress(port), 0);
            context = SSLContext.getInstance("TLS");
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream(ServerVariables.SSL_KEYSTORE);
            ks.load(fis, ServerVariables.SSL_STOREPASS.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, ServerVariables.SSL_KEYPASS.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        registerEndpoints(domain, server);

        HttpsConfigurator configurator = new HttpsConfigurator(context) {
            public void configure(HttpsParameters params) {
                SSLContext c = getSSLContext();
                SSLParameters sslParameters = c.getDefaultSSLParameters();
                params.setSSLParameters(sslParameters);
            }
        };
        server.setHttpsConfigurator(configurator);
        return  server;
    }

    private static void registerEndpoints(RansomAware domain, HttpsServer server) {
        server.createContext("/register", new RegisterHandler(domain, "POST", false));
        server.createContext("/login", new LoginHandler(domain, "POST", false));
//        server.createContext("/create", new CreateFileHandler(domain, "POST", true));
//        server.createContext("/list", new ListFileHandler(domain, "GET", true));
//        server.createContext("/get", new GetFileHandler(domain, "GET", true));
//        server.createContext("/save", new SaveFileHandler(domain, "POST", true));
//        server.createContext("/grant", new GrantHandler(domain, "POST", false));
//        server.createContext("/revoke", new GrantHandler(domain, "POST", false));

    }

    public static void start(RansomAware domain, int port) {
        HttpsServer server = prepareHttpsServer(domain, port);
        server.start();
        System.out.println("Server started successfully");
    }
}
