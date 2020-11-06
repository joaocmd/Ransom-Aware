package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpsExchange;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;

public class LoginHandler extends AbstractHandler {

    public LoginHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    public void handle(HttpsExchange exchange) {
        super.handle(exchange);
        JsonObject body = getBodyAsJSON();

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();
        try {
            int sessionToken = SessionManager.login(username, password);
            sendResponse(HttpURLConnection.HTTP_OK, Integer.toString(sessionToken));
        } catch (UnauthorizedException e) {
            super.sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "Invalid credentials");
        }
    }
}
