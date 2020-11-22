package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.domain.StoredFile;
import ransomaware.exceptions.NoSuchFileException;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;
import java.util.logging.Logger;

public class GetFileHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(GetFileHandler.class.getName());

    public GetFileHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            super.handle(exchange);
        } catch (UnauthorizedException | SessionExpiredException ignored) {
            return;
        }


        String[] parts = exchange.getRequestURI().getPath().split("/");
        if (parts.length != 4) {
            StringBuilder msg = new StringBuilder("Invalid file path: ");
            for (String part: parts) {
                msg.append("/").append(part);
            }
            LOGGER.info(msg.toString());

            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid file path");
            return;
        }

        String owner = parts[2];
        String filename = parts[3];
        String user = SessionManager.getUsername(getSessionToken());
        LOGGER.info(String.format("user %s requested file %s/%s", user, owner,filename));

        try {
            StoredFile file = server.getFile(user, new StoredFile(owner, filename));
            JsonObject response = JsonParser.parseString("{}").getAsJsonObject();
            response.add("file", file.getAsJsonObject());

            // FIXME: Should the certificate of the owner be sent?
            response.addProperty("certificate", SessionManager.getEncryptCertificate(file.getOwner()));
            sendResponse(HttpURLConnection.HTTP_OK, response);
        } catch (NoSuchFileException e) {
            sendResponse(HttpURLConnection.HTTP_NOT_FOUND, "No such file");
        } catch (UnauthorizedException e) {
            sendResponse(HttpURLConnection.HTTP_FORBIDDEN, "You don't have permission to see this file");
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            e.printStackTrace();
            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something unexpected happened");
        }
    }
}
