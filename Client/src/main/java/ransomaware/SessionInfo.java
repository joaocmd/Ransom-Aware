package ransomaware;

public class SessionInfo {
    private String username;
    private boolean isLogged = false;
    private String encryptKeyPath;
    private String signKeyPath;

    public String getUsername() {
                              return username;
                                              }

    public boolean isLogged() {
                            return isLogged;
                                            }

    public void login(String username) {
        isLogged = true;
        this.username = username;
        encryptKeyPath = ClientVariables.FS_PATH + '/' + username + '/' + username + ".pkcs8";
        // FIXME: good paths
        signKeyPath = ClientVariables.FS_PATH + '/' + username + '/' + username + ".pkcs8";
    }

    public void logOff(){
        isLogged = false;
        username = "";
    }

    public String getEncryptKeyPath() {
        return encryptKeyPath;
    }

    public String getSignKeyPath() {
        return signKeyPath;
    }
}
