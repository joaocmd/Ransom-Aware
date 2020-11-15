package ransomaware;

import com.google.gson.JsonObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.RandomStringUtils;


public class BaseIT {

    private static final String TEST_PROP_FILE = "/test.properties";
    protected static Properties testProps;
    private static MongoClient mongoClient;
    private static String dbName;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static HttpClient client = HttpClient.newBuilder().executor(executor).build();
    static String baseUrl;

    @BeforeAll
    public static void oneTimeSetup() throws IOException {
        testProps = new Properties();

        try {
            testProps.load(BaseIT.class.getResourceAsStream(TEST_PROP_FILE));
            // System.out.println("Test properties:" + testProps);
        } catch (IOException e) {
            System.out.printf("Could not load properties file {}", TEST_PROP_FILE);
            throw e;
        }

        try {
            String dbHost = testProps.getProperty("db.host");
            String dbPort = testProps.getProperty("db.port");
            dbName = testProps.getProperty("db.name");
            mongoClient = new MongoClient(new MongoClientURI("mongodb://" + dbHost + ":" + dbPort));
            List<String> databases = mongoClient.getDatabaseNames();
            if (databases.contains(dbName)) {
                mongoClient.dropDatabase(dbName);
                mongoClient.getDB(dbName);
            }
        } catch (UnknownHostException e) {
            System.out.print("Could not connect to MongoDB database");
            throw e;
        }


        String HOST = testProps.getProperty("server.host");
        String PORT = testProps.getProperty("server.port");
        baseUrl = "https://" + HOST + ":" + PORT;
    }

    @AfterAll
    public static void cleanup() {
        mongoClient.dropDatabase(dbName);
        mongoClient.getDB(dbName);
        executor.shutdownNow();
        client = null;
        System.gc();
    }

    // Helper methods
    String requestPostFromURL(String url, JsonObject jsonObject) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Cache-Control", "no-store")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    String requestGetFromURL(String url, HttpClient client) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    String generateRandomString(int length, boolean useLetters, boolean useNumbers) {
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }

}

