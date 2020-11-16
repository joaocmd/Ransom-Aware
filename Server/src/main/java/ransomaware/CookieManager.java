package ransomaware;

import java.net.HttpCookie;

public class CookieManager {

    private CookieManager(){

    }

    public static HttpCookie createCookie(String name, String value) {
        HttpCookie cookie = new HttpCookie(name, value);
        cookie.setMaxAge(ServerVariables.SESSION_DURATION);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        return cookie;
    }

    public static void renew(HttpCookie cookie){
        cookie.setMaxAge(ServerVariables.SESSION_DURATION);
    }

    public static String toString(HttpCookie cookie) {
        return cookie.getName() + "=" + cookie.getValue() + ";" + ";" + cookie.getMaxAge()
    }
}
