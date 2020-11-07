package ransomaware;

public class ServerVariables {
    public static String MONGO_URI = "mongodb://localhost:27017";

    public static String FS_PATH;

    public static final long SESSION_DURATION = 60*20;
    public static String KEYSTORE;
    public static final String SSL_STOREPASS = "simulator";
    public static final String SSL_KEYPASS = "simulator";

    public  static void init(String path, String mongoUri) {
        MONGO_URI = mongoUri;
        FS_PATH = path;
        KEYSTORE = String.format("%s/server.keystore", FS_PATH);
    }
}
