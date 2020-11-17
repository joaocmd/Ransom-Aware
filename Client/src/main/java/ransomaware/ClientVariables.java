package ransomaware;

public class ClientVariables {
    public static String WORKSPACE;
    public static String FS_PATH;
    public static String URL;

    public static final long SESSION_DURATION = 60*20;
    public static String KEYSTORE;
    public static final String SSL_STOREPASS = "simulator";
    public static final String SSL_KEYPASS = "simulator";

    public  static void init(String path, String url) {
        FS_PATH = path;
        WORKSPACE = path + "/workspace";
        KEYSTORE = String.format("%s/server.keystore", FS_PATH);
        URL = url;
    }
}
