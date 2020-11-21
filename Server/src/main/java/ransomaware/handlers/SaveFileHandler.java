package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SecurityUtils;
import ransomaware.SessionManager;
import ransomaware.domain.StoredFile;
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
        String owner = body.getAsJsonObject("requestInfo").get("user").getAsString();
        String fileName = body.getAsJsonObject("requestInfo").get("name").getAsString();
        String data = body.get("file").getAsString();

        JsonObject fileInfo = body.getAsJsonObject("info");

        LOGGER.info(String.format("user %s uploading file %s/%s", user, owner, fileName));
        try {
            StoredFile file = new StoredFile(owner, fileName, data, fileInfo);
            server.uploadFile(user, file);
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
