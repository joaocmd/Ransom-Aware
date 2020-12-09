package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;
import java.util.logging.Logger;

public class LoginHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(LoginHandler.class.getName());

    public LoginHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        super.handle(exchange);
        JsonObject body = getBodyAsJSON();

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();

        LOGGER.info(String.format("login request: %s password: [REDACTED]", username));

        try {
            String sessionToken = SessionManager.login(username, password);
            String cookie = SessionManager.createSessionCookie(sessionToken);
            sendResponse(HttpURLConnection.HTTP_OK, "Successful login", cookie);
        } catch (UnauthorizedException e) {
            super.sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "Invalid credentials");
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            e.printStackTrace();
            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something unexpected happened");
        }
    }
}
