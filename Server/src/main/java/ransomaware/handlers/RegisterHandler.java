package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpsExchange;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.exceptions.DuplicateUsernameException;

import java.net.HttpURLConnection;
import java.net.UnknownHostException;

public class RegisterHandler extends AbstractHandler {

    public RegisterHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    public void handle(HttpsExchange exchange) {
        super.handle(exchange);
        JsonObject body = getBodyAsJSON();

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();
        String encodedPublicKey = body.get("publicKey").getAsString();
        try {
            SessionManager.register(username, password, encodedPublicKey);
            sendResponse(HttpURLConnection.HTTP_OK, "Successfully registered");
        } catch (DuplicateUsernameException e) {
            super.sendResponse(HttpURLConnection.HTTP_CONFLICT, "Username already registered");
        } catch (UnknownHostException e) {
            super.sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something went wrong, we'll look into it");
        }
    }
}
