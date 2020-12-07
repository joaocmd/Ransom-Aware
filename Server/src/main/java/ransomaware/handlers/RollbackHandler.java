package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.domain.StoredFile;
import ransomaware.exceptions.NoSuchFileException;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;
import java.security.InvalidParameterException;
import java.util.logging.Logger;

public class RollbackHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(RollbackHandler.class.getName());

    public RollbackHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            super.handle(exchange);
        } catch (UnauthorizedException | SessionExpiredException ignored) {
            return;
        }

        try {
            String sessionToken = this.getSessionToken();
            String username = SessionManager.getUsername(sessionToken);

            JsonObject bodyAsJson = getBodyAsJSON();
            int n = bodyAsJson.get("n").getAsInt();
            JsonObject fileJson = bodyAsJson.getAsJsonObject("file");
            StoredFile file = new StoredFile(fileJson.get("owner").getAsString(), fileJson.get("name").getAsString());

            LOGGER.info(String.format("rollback %s,%d request: %s", file.getFileName(), n, SessionManager.getUsername(sessionToken)));

            server.rollback(username, file, n);
            sendResponse(HttpURLConnection.HTTP_OK, "OK");
        } catch (UnauthorizedException e) {
            sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "You don't have permissions to rollback this file");
        } catch (NoSuchFileException e) {
            sendResponse(HttpURLConnection.HTTP_NOT_FOUND, "File was not found on the server");
        } catch (InvalidParameterException e) {
            sendResponse(HttpURLConnection.HTTP_FORBIDDEN, "This file does not have that many versions");
        } catch (Exception e) {
            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something unexpected went wrong");
        }
    }
}
