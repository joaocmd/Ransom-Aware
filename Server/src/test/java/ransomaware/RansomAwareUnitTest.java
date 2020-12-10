package ransomaware;

import org.junit.jupiter.api.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.domain.StoredFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test Register and Login functions")
class RansomAwareUnitTest extends BaseIT {

    private static class TestUser {
        String username;
        String password;

        public TestUser(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    private static class TestFile {
        String owner;
        String filename;
        String data;
        JsonObject fileInfo;

        public TestFile(String owner, String filename, String data, JsonObject fileInfo) {
            this.owner = owner;
            this.filename = filename;
            this.data = data;
            this.fileInfo = fileInfo;
        }
    }

    // static members
    // FIXME: Port wrong
    static RansomAware server = new RansomAware(0, true);
    static TestUser testUser1;
    static TestUser testUser2;
    static String testFileName = "/test-file1.txt";
    static TestFile testFile1;


    // one-time initialization and clean-up
    @BeforeAll
    public static void oneTimeSetUp(){
        // Get registered users
        String username1 = "daniel";
        String password1 = generateRandomString(10, true, true);
        SessionManager.register(username1, password1, "fake-cert", "fake-cert-2");
        testUser1 = new TestUser(username1, password1);

        String username2 = "joao";
        String password2 = generateRandomString(10, true, true);
        SessionManager.register(username2, password2, "fake-cert", "fake-cert-2");
        testUser2 = new TestUser(username2, password2);

        // Get valid file
        testFile1 = getFile(testFileName);
    }

    @AfterAll
    public static void oneTimeTearDown() {
        server.shutdown();
    }

    // initialization and clean-up for each test

    @BeforeEach
    public void setUp() {

    }

    @AfterEach
    public void tearDown() {

    }

    // tests

    /**
     * Upload and Get tests
     */
    @Test
    @DisplayName("Upload valid file with owner and get it after, checking if they are equal")
    void uploadAndGetValidFile() {
        try {
            // Upload file
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            // Get file
            StoredFile fileReceived = server.getFile(testUser1.username, new StoredFile(testFile1.owner, testFile1.filename));

            // Check if equal
            assertEquals(fileReceived.toString(), fileSent.toString());
        } catch (Exception e) {
            fail("Should not have thrown exception.");
        }
    }

    /**
     * Upload
     * Get
     * grantPermission
     * revokePermission
     */

    // helper
    private static TestFile getFile(String name) {
        try {
            String fileBody = new String(BaseIT.class.getResourceAsStream(name).readAllBytes());
            JsonObject body = JsonParser.parseString(fileBody).getAsJsonObject();

            String owner = body.getAsJsonObject("requestInfo").get("user").getAsString();
            String fileName = body.getAsJsonObject("requestInfo").get("name").getAsString();
            String data = body.get("file").getAsString();
            JsonObject fileInfo = body.getAsJsonObject("info");

            return new TestFile(owner, fileName, data, fileInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}

