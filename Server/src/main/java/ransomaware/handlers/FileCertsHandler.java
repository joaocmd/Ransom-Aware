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
import java.util.Map;
import java.util.logging.Logger;

public class FileCertsHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(FileCertsHandler.class.getName());

    public FileCertsHandler(RansomAware server, String method, boolean requireAuth) {
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
        LOGGER.info(String.format("user %s requested certs for file %s/%s", user, owner,filename));

        try {
            Map<String, String> certs = server.getEncryptCertificates(new StoredFile(owner, filename), user);
            JsonObject response = JsonParser.parseString("{}").getAsJsonObject();

            JsonObject jsonCerts = JsonParser.parseString("{}").getAsJsonObject();
            certs.forEach(jsonCerts::addProperty);

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
