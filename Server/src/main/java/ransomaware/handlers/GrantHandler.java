package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.domain.StoredFile;
import ransomaware.exceptions.*;

import java.net.HttpURLConnection;
import java.util.logging.Logger;

public class GrantHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(GrantHandler.class.getName());

    public GrantHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            super.handle(exchange);
        } catch (UnauthorizedException | SessionExpiredException ignored) {
            return;
        }

        JsonObject body = getBodyAsJSON();

        String file = body.get("file").getAsString();
        String[] parts = file.split("/");
        if (parts.length != 2) {
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "File name is invalid");
            return;
        }
        String userToGrant = body.get("user").getAsString();

        String owner = SessionManager.getUsername(getSessionToken());

        try {
            server.grantPermission(owner, userToGrant, new StoredFile(parts[0], parts[1]));
            LOGGER.info(String.format("User '%s' granted permissions of File '%s' to '%s'", owner, file, userToGrant));
            sendResponse(HttpURLConnection.HTTP_OK, "OK");
        } catch (NoSuchFileException e) {
            sendResponse(HttpURLConnection.HTTP_NOT_FOUND, "No such file");
        } catch (UnauthorizedException e) {
            sendResponse(HttpURLConnection.HTTP_FORBIDDEN, "You don't have permission to see this file");
        } catch (AlreadyGrantedException e) {
            sendResponse(HttpURLConnection.HTTP_FORBIDDEN, "Permission already granted");
        } catch (NoSuchUserException e) {
            sendResponse(HttpURLConnection.HTTP_NOT_FOUND, "User does not exist");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something unexpected happened");
        }
    }
}
