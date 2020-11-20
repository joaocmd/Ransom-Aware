package ransomaware.handlers;

import com.google.gson.JsonArray;
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
import java.util.Map;
import java.util.logging.Logger;

public class CertsHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(GetFileHandler.class.getName());

    public CertsHandler(RansomAware server, String method, boolean requireAuth) {
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
        if (parts.length != 5) {
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid file path");
            return;
        }

        String owner = parts[3];
        String filename = parts[4];

        String user = SessionManager.getUsername(getSessionToken());
        LOGGER.info(String.format("user %s requested file %s/%s", user, owner,filename));

        try {
            // TODO: Get certs from all users with permissions

            // For now, the only certificate sent is the owner's
            StoredFile file = server.getFile(user, new StoredFile(owner, filename));

            Map<String, String> certs = server.getEncryptCertificates(file);
            JsonObject response = JsonParser.parseString("{}").getAsJsonObject();

            JsonArray jsonCerts = new JsonArray();

            certs.forEach((key, value) -> {
                JsonObject obj = JsonParser.parseString("{}").getAsJsonObject();
                obj.addProperty(key, value);
                jsonCerts.add(obj);
            });

            response.add("certs", jsonCerts);

            sendResponse(HttpURLConnection.HTTP_OK, response);
        } catch (NoSuchFileException e) {
            sendResponse(HttpURLConnection.HTTP_NOT_FOUND, "No such file");
        } catch (UnauthorizedException e) {
            sendResponse(HttpURLConnection.HTTP_FORBIDDEN, "You don't have permission to see this file");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something unexpected happened");
        }
    }
}
