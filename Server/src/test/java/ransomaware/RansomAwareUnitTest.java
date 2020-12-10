package ransomaware;

import org.junit.jupiter.api.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.domain.StoredFile;
import ransomaware.exceptions.*;

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
    static RansomAware server = new RansomAware(9999, true);
    static TestUser testUser1;
    static TestUser testUser2;
    static String testFileName = "/test-file1.txt";
    static String invalidFilename = "fake/joao.txt";
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
        testFile1 = loadFile(testFileName);
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
     * Upload tests
     */
    @Test
    @DisplayName("Upload file with invalid filename")
    void uploadInvalidFilename() {
        StoredFile fileSent = new StoredFile(testFile1.owner, invalidFilename, testFile1.data, testFile1.fileInfo);
        assertThrows(InvalidFileNameException.class, () -> {
            server.uploadFile(testUser1.username, fileSent);
        });
    }

    @Test
    @DisplayName("Upload file with wrong author")
    void uploadWrongAuthor() {
        JsonObject fileInfoChanged = testFile1.fileInfo;
        fileInfoChanged.addProperty("author", testUser2.username);

        StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, fileInfoChanged);
        assertThrows(IllegalArgumentException.class, () -> {
            server.uploadFile(testUser1.username, fileSent);
        });
    }

    @Test
    @DisplayName("Reupload file with wrong permissions")
    void reuploadWrongPermissions() {
        // First upload file
        StoredFile fileSent1 = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
        server.uploadFile(testUser1.username, fileSent1);

        // Prepare reupload with wrong permissions
        JsonObject fileInfoChanged = testFile1.fileInfo;
        JsonObject keys = fileInfoChanged.getAsJsonObject("keys");
        keys.addProperty(testUser2.username, "fake-key");
        fileInfoChanged.add("keys", keys);

        StoredFile fileSent2 = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, fileInfoChanged);
        assertThrows(IllegalArgumentException.class, () -> {
            server.uploadFile(testUser1.username, fileSent2);
        });
    }

    /**
     * Get tests
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

    @Test
    @DisplayName("Upload valid file with owner and get with user without access")
    void getUnauthorizedFile() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            // Get file with user 2
            assertThrows(UnauthorizedException.class, () -> {
                server.getFile(testUser2.username, new StoredFile(testFile1.owner, testFile1.filename));
            });
        } catch (Exception e) {
            fail("Should not have thrown exception.");
        }
    }

    @Test
    @DisplayName("Get file that does not exist")
    void getNonExistentFile() {
        try {
            // Get file non-existent
            assertThrows(NoSuchFileException.class, () -> {
                server.getFile(testUser2.username, new StoredFile(testUser2.username, "file-does-not-exist"));
            });
        } catch (Exception e) {
            fail("Should not have thrown exception.");
        }
    }

    /**
     * Grant and revoke permissions tests
     */


    // helper
    private static TestFile loadFile(String name) {
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

