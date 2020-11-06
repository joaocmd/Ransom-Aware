package ransomaware;

public class ServerVariables {
    public static String MONGO_URI = "mongodb://localhost:27017";
    public static String NAME = "ransom-aware";

    public static String FS_PATH;

    public static final long SESSION_DURATION = 60*20;
    public static final String SSL_PASS = "changeme";
    public static String SSL_KEYSTORE;

    public  static void init(String name, String mongoUri) {
        NAME = name;
        MONGO_URI = mongoUri;
        FS_PATH = String.format("/var/lib/%s", name);
        SSL_KEYSTORE = String.format("%s/ssl_keystore", FS_PATH);
    }
}
