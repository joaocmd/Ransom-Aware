package ransomaware;

public class ClientVariables {
    public static String WORKSPACE;
    public static final String TMP_PATH = "/tmp/Ransom-Aware-Client/";
    public static String URL;

    private ClientVariables() {}

    public  static void init(String path, String url) {
        WORKSPACE = path;
        URL = url;
    }
}
