package ransomaware;

public class SessionInfo {
    private String username;
    private boolean isLogged = false;
    private String encryptKeyPath;
    private String signKeyPath;

    public SessionInfo(String encryptKeyPath, String signKeyPath) {
        this.encryptKeyPath = encryptKeyPath;
        this.signKeyPath = signKeyPath;
    }

    public String getUsername() {
        return username;
    }

    public boolean isLogged() {
        return isLogged;
    }

    public void login(String username) {
        isLogged = true;
        this.username = username;
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
