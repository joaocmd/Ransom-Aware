package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpCookie;
import java.net.HttpURLConnection;

public class LoginHandler extends AbstractHandler {

    public LoginHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        super.handle(exchange);
        JsonObject body = getBodyAsJSON();

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();

        try {
            int sessionToken = SessionManager.login(username, password);
            String cookie = SessionManager.createSessionCookie(sessionToken);
            JsonObject resp = JsonParser.parseString("{}").getAsJsonObject();
            resp.addProperty("status", Integer.toString(sessionToken));

            sendResponse(HttpURLConnection.HTTP_OK, "Successful login", cookie);
        } catch (UnauthorizedException e) {
            super.sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "Invalid credentials");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something unexpected happened");
        }
    }
}
