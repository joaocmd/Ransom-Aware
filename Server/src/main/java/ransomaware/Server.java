package ransomaware;

import com.mongodb.MongoClient;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import ransomaware.handlers.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

public class Server {

    private String name;
    private int port;
    private MongoClient mongoClient;

    public Server(String name, int port, boolean firstTime) throws UnknownHostException {
        this.port = port;
        this.name = name;
        if (firstTime) {
            firstTimeSetup();
            // createFS()
        } else {
            // validateFS()
        }
    }

    private void firstTimeSetup() {
    }

    private HttpsServer prepareHttpsServer(int port) {
        SSLContext context = null;
        HttpsServer server = null;
        try {
            server = HttpsServer.create(new InetSocketAddress(port), 0);
            context = SSLContext.getInstance("TLS");
            char[] password = ServerVariables.SSL_PASS.toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream(ServerVariables.SSL_KEYSTORE);
            ks.load(fis, password);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        registerEndpoints(server);

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

    private void registerEndpoints(HttpsServer server) {
        server.createContext("/register", new RegisterHandler(this, "POST", false));
        server.createContext("/login", new LoginHandler(this, "POST", false));
//        server.createContext("/create", new CreateFileHandler(this, "POST", true));
//        server.createContext("/list", new ListFileHandler(this, "GET", true));
//        server.createContext("/get", new GetFileHandler(this, "GET", true));
//        server.createContext("/save", new SaveFileHandler(this, "POST", true));
//        server.createContext("/grant", new GrantHandler(this, "POST", false));
//        server.createContext("/revoke", new GrantHandler(this, "POST", false));

    }

    public void start() {
        HttpsServer server = prepareHttpsServer(port);
        server.start();
        System.out.println("Server started successfully");
    }
}
