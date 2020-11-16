package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SecurityUtils;
import ransomaware.SessionManager;
import ransomaware.exceptions.NoSuchFileException;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;

public class GetFileHandler extends AbstractHandler {

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
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid file path");
            return;
        }

        String owner = parts[2];
        String filename = parts[3];

        try {
            String file = SecurityUtils.getBase64(server.getFile(getSessionToken(), owner, filename));
            JsonObject response = JsonParser.parseString("{}").getAsJsonObject();
            response.addProperty("file", file);
            sendResponse(HttpURLConnection.HTTP_OK, response);
        } catch (NoSuchFileException e) {
            sendResponse(HttpURLConnection.HTTP_NOT_FOUND, "No such file");
        } catch (UnauthorizedException e) {
            sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "You don't have permission to see this file");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something unexpected happened");
        }
    }
}
