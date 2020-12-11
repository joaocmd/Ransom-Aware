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
    static RansomAware server;
    static TestUser testUser1;
    static TestUser testUser2;
    static TestUser testUser3;
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

        String username3 = "diogo";
        String password3 = generateRandomString(10, true, true);
        SessionManager.register(username3, password3, "fake-cert", "fake-cert-2");
        testUser3 = new TestUser(username3, password3);

        // Get valid file
        testFile1 = loadFile(testFileName);
    }

    @AfterAll
    public static void oneTimeTearDown() {
    }

    // initialization and clean-up for each test

    @BeforeEach
    public void setUp() {
        testFile1 = loadFile(testFileName);
        server = new RansomAware(9999, true, true);
    }

    @AfterEach
    public void tearDown() {
        if (server != null) server.shutdown();
    }

    // tests

    /**
     * Upload tests
     */
    @Test
    @DisplayName("Upload file with invalid filename")
    void uploadInvalidFilename() {
        StoredFile fileSent = new StoredFile(testFile1.owner, invalidFilename, testFile1.data, testFile1.fileInfo);
        assertThrows(InvalidFileNameException.class, () -> server.uploadFile(testUser1.username, fileSent));
    }

    @Test
    @DisplayName("Upload file with wrong author")
    void uploadWrongAuthor() {
        JsonObject fileInfoChanged = testFile1.fileInfo;
        fileInfoChanged.addProperty("author", testUser2.username);

        StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, fileInfoChanged);
        assertThrows(IllegalArgumentException.class, () -> server.uploadFile(testUser1.username, fileSent));
    }

    @Test
    @DisplayName("Reupload file with wrong permissions")
    void reuploadWrongPermissions() {
        try {
            // First upload file
            StoredFile fileSent1 = new StoredFile(testUser1.username, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent1);

            // Prepare reupload with wrong permissions
            JsonObject fileInfoChanged = testFile1.fileInfo;
            JsonObject keys = fileInfoChanged.getAsJsonObject("keys");
            keys.addProperty(testUser2.username, "fake-key");
            fileInfoChanged.add("keys", keys);

            StoredFile fileSent2 = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, fileInfoChanged);
            assertThrows(IllegalArgumentException.class, () -> server.uploadFile(testUser1.username, fileSent2));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            StoredFile sendingFile = new StoredFile(testFile1.owner, testFile1.filename);
            assertThrows(UnauthorizedException.class, () -> server.getFile(testUser2.username, sendingFile));
        } catch (Exception e) {
            fail("Should not have thrown exception.");
        }
    }

    @Test
    @DisplayName("Get file that does not exist")
    void getNonExistentFile() {
        try {
            // Get file non-existent
            StoredFile file = new StoredFile(testUser2.username, "file-does-not-exist");
            assertThrows(NoSuchFileException.class, () -> server.getFile(testUser2.username, file));
        } catch (Exception e) {
            fail("Should not have thrown exception.");
        }
    }

    /**
     * Grant permissions tests
     */

    @Test
    @DisplayName("Owner grants permissions and user2 can get")
    void ownerGrantUserCanGet() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            // Verify that user2 can't get file
            StoredFile file2 = new StoredFile(testFile1.owner, testFile1.filename);
            assertThrows(UnauthorizedException.class, () -> server.getFile(testUser2.username, file2));

            // User1 grants permission to user 2
            server.grantPermission(testUser1.username, testUser2.username, fileSent);

            // User2 can get file
            server.getFile(testUser2.username, new StoredFile(testFile1.owner, testFile1.filename));
        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("User2 tries to grant, but is not the owner and does not have access")
    void userTriesGrantButShouldNot() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            StoredFile fileToGet = new StoredFile(testFile1.owner, testFile1.filename);

            // Verify that user2 can't get file
            assertThrows(UnauthorizedException.class, () -> server.getFile(testUser2.username, fileToGet));

            // User2 tries to grant permission to user3
            assertThrows(UnauthorizedException.class, () -> server.grantPermission(testUser2.username, testUser3.username, fileToGet));

            // User2 can't get file
            assertThrows(UnauthorizedException.class, () -> server.getFile(testUser2.username, fileToGet));

            // User3 can't get file
            assertThrows(UnauthorizedException.class, () -> server.getFile(testUser3.username, fileToGet));
        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("User2 tries to grant, but is not the owner and has access")
    void userTriesGrantButShouldNot2() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            StoredFile fileToGet = new StoredFile(testFile1.owner, testFile1.filename);

            // Give access to user2
            server.grantPermission(testUser1.username, testUser2.username, fileToGet);

            // Verify that user2 can get file
            server.getFile(testUser2.username, fileToGet);

            // User2 tries to grant permission to user3
            assertThrows(UnauthorizedException.class, () -> server.grantPermission(testUser2.username, testUser3.username, fileToGet));

            // User3 can't get file
            assertThrows(UnauthorizedException.class, () -> server.getFile(testUser3.username, fileToGet));
        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("User tries to grant file that does not exist")
    void userTriesGrantNonExistent() {
        try {
            String nonExistentFilename = "IDontExist";
            StoredFile file = new StoredFile(testUser2.username, nonExistentFilename);

            // Try to grant permission of file that does not exist
            assertThrows(NoSuchFileException.class, () -> server.grantPermission(testUser2.username, testUser3.username,
                    file));

            // Try to get file that does not exist
            assertThrows(NoSuchFileException.class, () -> server.getFile(testUser2.username, file));

        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("User1 tries to grant permissions to itself")
    void userTriesGrantToItself() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            // Give access to user1
            assertThrows(AlreadyGrantedException.class, () -> server.grantPermission(testUser1.username, testUser1.username, fileSent));
        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("User1 tries to grant to user2 two times")
    void userTriesGrantTwice() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            // Give access to user2
            server.grantPermission(testUser1.username, testUser2.username, fileSent);

            // Verify that user2 can get file
            server.getFile(testUser2.username, new StoredFile(testFile1.owner, testFile1.filename));

            // Give access to user2 again
            assertThrows(AlreadyGrantedException.class, () -> server.grantPermission(testUser1.username, testUser2.username, fileSent));

        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    /**
     * Revoke permissions tests
     */

    @Test
    @DisplayName("User1 having grant permission to user2, revokes it")
    void revokeSuccess() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            StoredFile fileUsed = new StoredFile(testFile1.owner, testFile1.filename);

            // Give access to user2
            server.grantPermission(testUser1.username, testUser2.username, fileUsed);

            // Verify that user2 can get file
            server.getFile(testUser2.username, fileUsed);

            // Revoke user2 permission
            server.revokePermission(testUser1.username, testUser2.username, fileUsed);

            // Verify that user2 can't get the file
            assertThrows(UnauthorizedException.class, () -> server.getFile(testUser2.username, fileUsed));

        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("User2 tries to revoke permission when it is not the owner but has access")
    void revokeNotOwner() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            StoredFile fileUsed = new StoredFile(testFile1.owner, testFile1.filename);

            // Give access to user2
            server.grantPermission(testUser1.username, testUser2.username, fileUsed);

            // Verify that user2 can get file
            server.getFile(testUser2.username, fileUsed);

            // User2 tries to revoke permission to user1
            assertThrows(UnauthorizedException.class, () -> server.revokePermission(testUser2.username, testUser1.username, fileUsed));

        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("User2 tries to revoke permission when it is does not have access")
    void revokeWithoutAccess() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            StoredFile fileUsed = new StoredFile(testFile1.owner, testFile1.filename);

            // Verify that user2 cant get file
            assertThrows(UnauthorizedException.class, () -> server.getFile(testUser2.username, fileUsed));

            // User2 tries to revoke permission to user1
            assertThrows(UnauthorizedException.class, () -> server.revokePermission(testUser2.username, testUser1.username, fileUsed));

        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("User1 tries to revoke permission to user2 when user2 already does not have permission")
    void ownerRevokeAlreadyWithoutAccess() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            StoredFile fileUsed = new StoredFile(testFile1.owner, testFile1.filename);

            // Verify that user2 cant get file
            assertThrows(UnauthorizedException.class, () -> server.getFile(testUser2.username, fileUsed));

            // User1 tries to revoke permission to user2
            assertThrows(AlreadyRevokedException.class, () -> server.revokePermission(testUser1.username, testUser2.username, fileUsed));

        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("User1 tries to revoke permission to user2 on file non existent")
    void userRevokeNonExistentFile() {
        try {
            StoredFile fileUsed = new StoredFile(testFile1.owner, "IDontExistForReal");

            // Verify that user1 cant get file because it does not exist
            assertThrows(NoSuchFileException.class, () -> server.getFile(testUser1.username, fileUsed));

            // User1 tries to revoke permission to user2
            assertThrows(NoSuchFileException.class, () -> server.revokePermission(testUser1.username, testUser2.username, fileUsed));

        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("User1 tries to revoke permission to non existent user on existent file")
    void userRevokeNonExistentUser() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            StoredFile fileUsed = new StoredFile(testFile1.owner, testFile1.filename);

            // Verify that user1 can get file because it exists
            server.getFile(testUser1.username, fileUsed);

            // User1 tries to revoke permission to non existent user
            assertThrows(NoSuchUserException.class, () -> server.revokePermission(testUser1.username, "antonio-bambuino", fileUsed));

        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("User1 tries to revoke permission to itself, being owner")
    void ownerRevokeOwner() {
        try {
            // Upload file with user1
            StoredFile fileSent = new StoredFile(testFile1.owner, testFile1.filename, testFile1.data, testFile1.fileInfo);
            server.uploadFile(testUser1.username, fileSent);

            StoredFile fileUsed = new StoredFile(testFile1.owner, testFile1.filename);

            // Verify that user1 can get file because it exists
            server.getFile(testUser1.username, fileUsed);

            // User1 tries to revoke permission to itself
            assertThrows(UnauthorizedException.class, () -> server.revokePermission(testUser1.username, testUser1.username, fileUsed));

        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    /**
     * owner revoking owner
     * Revoke
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

