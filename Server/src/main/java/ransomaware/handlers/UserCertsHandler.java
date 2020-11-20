package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.exceptions.NoSuchFileException;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;
import java.util.logging.Logger;

public class UserCertsHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(UserCertsHandler.class.getName());

    public UserCertsHandler(RansomAware server, String method, boolean requireAuth) {
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
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid file path");
            return;
        }

        String userToFetch = parts[3];

        String user = SessionManager.getUsername(getSessionToken());
        LOGGER.info(String.format("user %s requested %s's certificates", user, userToFetch));

        try {
            // Get user's encrypt certificate
            String encryptCert = SessionManager.getEncryptCertificate(user);

            JsonObject jsonCerts = JsonParser.parseString("{}").getAsJsonObject();
            jsonCerts.addProperty("encrypt", encryptCert);
            // TODO: Also fetch user's signing certificate
            // jsonCerts.addProperty("sign", "");
            JsonObject response = JsonParser.parseString("{}").getAsJsonObject();
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
