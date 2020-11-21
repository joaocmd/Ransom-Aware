package ransomaware;

public class ClientVariables {
    public static String WORKSPACE;
    public static String FS_PATH;
    public static final String TMP_PATH = "/tmp/Ransom-Aware-Client/";
    public static String URL;

    public static final long SESSION_DURATION = 60*20;
    public static final String SSL_STOREPASS = "simulator";
    public static final String SSL_KEYPASS = "simulator";

    private ClientVariables() {}

    public  static void init(String path, String url) {
        FS_PATH = path;
        WORKSPACE = path + "/workspace";
        URL = url;
    }
}
