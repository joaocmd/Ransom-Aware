package ransomaware;

public class SessionInfo {
    private String username;
    private int sessionToken;
    private boolean isLogged = false;

    public void setSessionToken(int sessionToken) {
                                                this.sessionToken = sessionToken;
                                                                                 }

    public void setUsername(String username) {
                                           this.username = username;
                                                                    }

    public String getUsername() {
                              return username;
                                              }

    public int getSessionToken() {
                               return sessionToken;
                                                   }

    public boolean isLogged() {
                            return isLogged;
                                            }

    public void setLogged(boolean logged) {
            isLogged = logged;
        }
}
