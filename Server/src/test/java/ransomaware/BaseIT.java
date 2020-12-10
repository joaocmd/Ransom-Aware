package ransomaware;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;


public class BaseIT {

    private static final String TEST_PROP_FILE = "/test.properties";
    protected static Properties testProps;
    private static MongoClient mongoClient;
    private static String dbName;
    private static File resourcesDirectory;
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
            dbName = testProps.getProperty("db.test");
            mongoClient = new MongoClient(new MongoClientURI("mongodb://" + dbHost + ":" + dbPort));
            List<String> databases = mongoClient.getDatabaseNames();
            if (databases.contains(dbName)) {
                mongoClient.dropDatabase(dbName);
            }

            mongoClient.getDB(dbName);

            resourcesDirectory = new File("src/test/resources");

            ServerVariables.init(resourcesDirectory.getAbsolutePath() + "/ransom-aware-test", "mongodb://" + dbHost + ":" + dbPort, "localhost:rsync/", dbName);
        } catch (UnknownHostException e) {
            System.out.print("Could not connect to MongoDB database");
            throw e;
        }


        String HOST = testProps.getProperty("server.host");
        String PORT = testProps.getProperty("server.port");
        baseUrl = "https://" + HOST + ":" + PORT;
    }

    @AfterAll
    public static void cleanup() throws IOException {
        // Comment next line to keep testing data
        mongoClient.dropDatabase(dbName);

        System.gc();

        // Delete files
        File filesFolder = new File(resourcesDirectory.getAbsolutePath() + "/" + dbName + "/files");
        FileUtils.deleteDirectory(filesFolder);
    }

    // Helper methods

    static String generateRandomString(int length, boolean useLetters, boolean useNumbers) {
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }

}

