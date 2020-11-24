package ransomaware;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import ransomaware.handlers.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Scanner;
import java.util.logging.Logger;

public class Server {

    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private Server() {}

    private static HttpsServer prepareHttpsServer(RansomAware domain, int port) {
        SSLContext context = null;
        HttpsServer server = null;
        try {
            server = HttpsServer.create(new InetSocketAddress(port), 0);
            context = SSLContext.getInstance("TLS");
            KeyStore ks = KeyStore.getInstance("pkcs12");
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
            @Override
            public void configure(HttpsParameters params) {
                SSLContext c = getSSLContext();
                SSLParameters sslParameters = c.getDefaultSSLParameters();
                params.setSSLParameters(sslParameters);
            }
        };
        server.setHttpsConfigurator(configurator);
        return server;
    }

    private static void registerEndpoints(RansomAware domain, HttpServer server) {
        server.createContext("/register", new RegisterHandler(domain, "POST", false));
        server.createContext("/login", new LoginHandler(domain, "POST", false));
        server.createContext("/logout", new LogoutHandler(domain, "POST", true));
        server.createContext("/list", new ListFileHandler(domain, "GET", true));
        server.createContext("/users/certs/", new UserCertsHandler(domain, "GET", true));
        server.createContext("/files", new GetFileHandler(domain, "GET", true));
        server.createContext("/files/certs", new FileCertsHandler(domain, "GET", true));
        server.createContext("/save", new SaveFileHandler(domain, "POST", true));
        server.createContext("/grant", new GrantHandler(domain, "POST", true));
        server.createContext("/revoke", new RevokeHandler(domain, "POST", true));

    }

    public static void start(RansomAware domain, int port) {
        HttpsServer server = prepareHttpsServer(domain, port);
        LOGGER.info("Server starting");
        server.start();

        // Create new thread where we wait for user to end the server
        new Thread(() -> {
            LOGGER.info("<Press enter to shutdown>");
            new Scanner(System.in).nextLine();

            server.stop(0);
        }).start();
    }
}
