package ransomaware;

import org.junit.jupiter.api.*;

@DisplayName("Test Register and Login functions")
public class RansomAwareUnitTest extends BaseIT {

    // static members
    static String registeredUsername;
    static String registeredPassword;
    static final String illegalUsername = "daniel/gon";
    static String loggedSessionToken;


    // one-time initialization and clean-up
    @BeforeAll
    public static void oneTimeSetUp(){
        ServerVariables.init("ransom-aware", "mongodb://localhost:27017", "localhost:rsync/");
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
     *
     */
    @Test
    @DisplayName("")
    public void sample() {
    }

    /**
     * Upload
     * Get
     * grantPermission
     * revokePermission
     */
}

