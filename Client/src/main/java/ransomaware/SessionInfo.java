package ransomaware;

public class SessionInfo {
    private String username;
    private boolean isLogged = false;
    private String privateKeyPath;

    public void setUsername(String username) {
        this.username = username;
        privateKeyPath = ClientVariables.FS_PATH + "/" + username + ".key";
    }

    public String getUsername() {
                              return username;
                                              }

    public boolean isLogged() {
                            return isLogged;
                                            }

    public void setLogged(boolean logged) {
            isLogged = logged;
        }

    public void logOff(){
        setLogged(false);
        setUsername("");
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }
}
