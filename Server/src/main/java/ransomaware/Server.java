package ransomaware;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Server {

    private String name;
    private int port;
    private MongoClient mongoClient;

    public Server(String name, int port, boolean firstTime, String mongoUrl) throws UnknownHostException {
        mongoClient = new MongoClient(new MongoClientURI(mongoUrl));
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

        server.createContext("/login", new LoginHandler("POST"));
        server.createContext("/create", new CreateFileHandler("POST"));
        server.createContext("/list", new ListFileHandler("GET"));
        server.createContext("/save", new SaveFileHandler("POST"));
        server.createContext("/get", new GetFileHandler("GET"));
    }
}
