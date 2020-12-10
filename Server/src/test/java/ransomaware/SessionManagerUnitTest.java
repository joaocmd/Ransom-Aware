package ransomaware;

import org.junit.jupiter.api.*;
import ransomaware.exceptions.DuplicateUsernameException;
import ransomaware.exceptions.InvalidUserNameException;
import ransomaware.exceptions.UnauthorizedException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Test Register and Login functions")
class SessionManagerUnitTest extends BaseIT {

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
        try {
            registeredUsername = generateRandomString(10, true, true);
            registeredPassword = generateRandomString(10, true, true);
            SessionManager.register(registeredUsername, registeredPassword, "fake-cert", "fake-cert-2");
        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("Register the previous one already registered")
    public void TryRegisterAlreadyRegistered() {
        assertThrows(DuplicateUsernameException.class, () -> {
            SessionManager.register(registeredUsername, registeredPassword, "fake-cert", "fake-cert-2");
        });
    }

    @Test
    @DisplayName("Register with illegal characters in name")
    public void RegisterIllegalChars() {
        assertThrows(InvalidUserNameException.class, () -> {
            SessionManager.register(illegalUsername, registeredPassword, "fake-cert", "fake-cert-2");
        });
    }

    /**
     * Login Tests
     */
    @Test
    @DisplayName("Login with registered user")
    public void LoginRegistered() {
        try {
            loggedSessionToken = SessionManager.login(registeredUsername, registeredPassword);
        } catch (Exception e) {
            fail("Should not have thrown exception");
        }
    }

    @Test
    @DisplayName("Login with registered user but wrong password")
    public void LoginWrongPassword() {
        assertThrows(UnauthorizedException.class, () -> {
            SessionManager.login(registeredUsername, "NotThisOneForSure,AmIRight?");
        });
    }

    @Test
    @DisplayName("Login with non existent user")
    public void LoginNonExistentUser() {
        assertThrows(UnauthorizedException.class, () -> {
            SessionManager.login("zeus", "NotThisOneForSure,AmIRight?");
        });
    }
}

