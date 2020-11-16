package ransomaware;

public class SessionInfo {
    private String username;
    private boolean isLogged = false;

    public void setUsername(String username) {
                                           this.username = username;
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
}
