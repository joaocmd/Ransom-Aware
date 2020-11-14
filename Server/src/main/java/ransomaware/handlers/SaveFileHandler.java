package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SecurityUtils;
import ransomaware.SessionManager;
import ransomaware.exceptions.DuplicateUsernameException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;
import java.net.UnknownHostException;

public class SaveFileHandler extends AbstractHandler {

    public SaveFileHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        super.handle(exchange);
        JsonObject body = getBodyAsJSON();

        String username = body.getAsJsonObject("info").get("user").getAsString();
        String fileName = body.getAsJsonObject("info").get("name").getAsString();
        // TODO: no reason to decode base 64 if we're sending it encrypted.....
        // This is here now just so we can see the files are being sent correctly
        byte[] data = SecurityUtils.decodeBase64(body.get("data").getAsString());
        try {
            if (!username.equals("")) {
                server.uploadFile(username, fileName, data);
            } else {
                server.uploadFile(this.getSessionToken(), fileName, data);
            }
            sendResponse(HttpURLConnection.HTTP_OK, "OK");
        } catch (UnauthorizedException e) {
            sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "Unauthorized access to resource.");
        }
    }
}
