package ransomaware;

import com.mongodb.MongoClient;
import com.sun.net.httpserver.HttpServer;
import ransomaware.handlers.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Server {

    private String name;
    private int port;
    private MongoClient mongoClient;

    public Server(String name, int port, boolean firstTime) throws UnknownHostException {
        this.port = port;
        this.name = name;
        if (firstTime) {
            // createFS()
        } else {
            // validateFS()
        }
    }

    public void start() throws IOException {
        InetSocketAddress socketAddress = new InetSocketAddress(port);
        HttpServer server;
        server = HttpServer.create(socketAddress, 0);

        server.createContext("/login", new LoginHandler(this, "POST", false));
        server.createContext("/create", new CreateFileHandler(this, "POST", true));
        server.createContext("/list", new ListFileHandler(this, "GET", true));
        server.createContext("/save", new SaveFileHandler(this, "POST", true));
        server.createContext("/get", new GetFileHandler(this, "GET", true));
    }

}
