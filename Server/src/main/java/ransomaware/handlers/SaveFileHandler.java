package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SecurityUtils;
import ransomaware.SessionManager;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;
import java.util.logging.Logger;

public class SaveFileHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(SaveFileHandler.class.getName());

    public SaveFileHandler(RansomAware server, String method, boolean requireAuth) {
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

        String user = SessionManager.getUsername(this.getSessionToken());
        String username = body.getAsJsonObject("info").get("user").getAsString();
        String fileName = body.getAsJsonObject("info").get("name").getAsString();

        LOGGER.info(String.format("user %s uploading file %s/%s", user, username, fileName));

        // TODO: no reason to decode base 64 if we're sending it encrypted.....
        // This is here now just so we can see the files are being sent correctly
        byte[] data = SecurityUtils.decodeBase64(body.get("data").getAsString());
        try {
            server.uploadFile(user, username, fileName, data);
            sendResponse(HttpURLConnection.HTTP_OK, "OK");
        } catch (UnauthorizedException e) {
            sendResponse(HttpURLConnection.HTTP_FORBIDDEN, "Unauthorized access to resource");
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            e.printStackTrace();
            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something unexpected happened");
        }
    }
}
