package ransomaware;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Test Register and Login functions")
public class RegisterLoginTest extends BaseIT {

    // static members
    static String registeredUsername;
    static String registeredPassword;
    static final String illegalUsername = "daniel/gon";
    static String loggedSessionToken;


    // one-time initialization and clean-up
    @BeforeAll
    public static void oneTimeSetUp(){
    }

    @AfterAll
    public static void oneTimeTearDown() {

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
     * Register Tests
     * Note: all usernames in these tests have at least 10 characters
     */
    @Test
    @DisplayName("Register with random user and password")
    public void RegisterRandom() {
        JsonObject jsonRequest = JsonParser.parseString("{}").getAsJsonObject();
        registeredUsername = generateRandomString(10, true, true);
        registeredPassword = generateRandomString(10, true, true);
        jsonRequest.addProperty("username", registeredUsername);
        jsonRequest.addProperty("password", registeredPassword);

        try {
            String response = super.requestPostFromURL(baseUrl + "/register", jsonRequest);
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            int statusCode = jsonResponse.get("status").getAsInt();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                fail("Failed to register with code " + statusCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("Register the previous one already registered")
    public void TryRegisterAlreadyRegistered() {
        JsonObject jsonRequest = formRegisterReq(registeredUsername, registeredPassword);

        try {
            String response = super.requestPostFromURL(baseUrl + "/register", jsonRequest);
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            int statusCode = jsonResponse.get("status").getAsInt();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                fail("The server accepted to register an already registered user");
            } else if (statusCode != HttpURLConnection.HTTP_CONFLICT) {
                fail("The server had problems trying to register an already registered used with" +
                        " error " + statusCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("Register with illegal characters in name")
    public void RegisterIllegalChars() {
        JsonObject jsonRequest = formRegisterReq(illegalUsername);

        try {
            String response = super.requestPostFromURL(baseUrl + "/register", jsonRequest);
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            int statusCode = jsonResponse.get("status").getAsInt();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                fail("Server accepted illegal username.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            fail("Should not have thrown exception");
        }
    }

    /**
     * Login Tests
     */
    @Test
    @DisplayName("Login with registered user")
    public void LoginRegistered() {
        JsonObject jsonRequest = formRegisterReq(registeredUsername, registeredPassword);

        try {
            String response = super.requestPostFromURL(baseUrl + "/login", jsonRequest);
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            loggedSessionToken = jsonResponse.get("login-token").getAsString();

            int statusCode = jsonResponse.get("status").getAsInt();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                fail("The server didn't accept a registered user with status " + statusCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("Login with registered user but wrong password")
    public void LoginWrongPassword() {
        JsonObject jsonRequest = formRegisterReq(registeredUsername, "NotThisOneForSure,AmIRight?");

        try {
            String response = super.requestPostFromURL(baseUrl + "/login", jsonRequest);
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

            int statusCode = jsonResponse.get("status").getAsInt();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                fail("The server accepted a wrong password");
            } else if (statusCode != HttpURLConnection.HTTP_UNAUTHORIZED) {
                fail("The server had problems with login with wrong password");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("Login with non existent user")
    public void LoginNonExistentUser() {
        JsonObject jsonRequest = formRegisterReq("zeus");

        try {
            String response = super.requestPostFromURL(baseUrl + "/login", jsonRequest);
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

            int statusCode = jsonResponse.get("status").getAsInt();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                fail("The server accepted a non existent user");
            } else if (statusCode != HttpURLConnection.HTTP_UNAUTHORIZED) {
                fail("The server had problems with login with a non existent user");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            fail("Should not have thrown exception");
        }
    }

    // Helper
    JsonObject formRegisterReq(String username, String password) {
        JsonObject jsonRequest = JsonParser.parseString("{}").getAsJsonObject();
        jsonRequest.addProperty("username", username);
        jsonRequest.addProperty("password", password);
        return jsonRequest;
    }

    JsonObject formRegisterReq(String username) {
        return formRegisterReq(username, generateRandomString(10, true, true));
    }
}

