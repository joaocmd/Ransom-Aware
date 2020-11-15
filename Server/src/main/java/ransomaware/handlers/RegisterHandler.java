package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.exceptions.DuplicateUsernameException;
import ransomaware.exceptions.InvalidUserNameException;

import java.net.HttpURLConnection;

public class RegisterHandler extends AbstractHandler {

    public RegisterHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        super.handle(exchange);
        JsonObject body = getBodyAsJSON();

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();
        try {
            SessionManager.register(username, password);
            sendResponse(HttpURLConnection.HTTP_OK, "Successfully registered");
        } catch (DuplicateUsernameException e) {
            sendResponse(HttpURLConnection.HTTP_CONFLICT, "Username already registered");
        } catch(InvalidUserNameException e) {
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Bad format: Username must be alfanumeric.");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something unexpected happened");
        }
    }
}
