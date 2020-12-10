package ransomaware;

public class ServerVariables {
    public static String MONGO_URI = "mongodb://localhost:27017";

    public static String DB_NAME;
    public static String FS_PATH;
    public static String FILES_PATH;

    public static final long SESSION_DURATION = 60*20;
    public static String SSL_KEYSTORE;
    public static final String SSL_STOREPASS = "changeme";
    public static final String SSL_KEYPASS = "changeme";

    public static final String PASSWORD_SALT = "joca-o-maior";
    public static final String DB_COLLECTION_USERS = "users";
    public static final String DB_COLLECTION_FILES = "files";
    public static final String DB_COLLECTION_SALTS = "salts";

    public static String RSYNC_SERVER = "localhost:rsync/";
    public static String RSYNC_KEY = "id_rsync";

    private  ServerVariables() {}

    public static void init(String path, String mongoUri, String rsyncUri) {
        MONGO_URI = mongoUri;
        DB_NAME = path;
        FS_PATH = path;
        FILES_PATH = path + "/files";
        SSL_KEYSTORE = String.format("%s/server-ssl.p12", FS_PATH);
        RSYNC_SERVER = rsyncUri;
        RSYNC_KEY = String.format("%s/id_rsync", FS_PATH);
    }

    public static void init(String path, String mongoUri, String rsyncUri, String dbName) {
        init(path, mongoUri, rsyncUri);
        DB_NAME = dbName;
    }
}
