package ransomaware;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;


public class BaseIT {

    private static final String TEST_PROP_FILE = "/test.properties";
    protected static Properties testProps;
    private static MongoClient mongoClient;
    private static String dbName;
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
        ServerVariables.init("ransom-aware", "mongodb://localhost:27017", "localhost:rsync/");
    }

    @AfterAll
    public static void cleanup() {
        // Comment next line to keep testing data
        mongoClient.dropDatabase(dbName);

        mongoClient.getDB(dbName);
        System.gc();
    }

    // Helper methods

    String generateRandomString(int length, boolean useLetters, boolean useNumbers) {
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }

}

