package ransomaware.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;
import java.util.logging.Logger;

public class ListFileHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(ListFileHandler.class.getName());

    public ListFileHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            super.handle(exchange);
        } catch (UnauthorizedException | SessionExpiredException ignored) {
            return;
        }

        JsonArray files = new JsonArray();

        String user = SessionManager.getUsername(this.getSessionToken());
        LOGGER.info(String.format("list request: %s", user));

        server.listFiles(user).forEachOrdered(files::add);

        JsonObject response = JsonParser.parseString("{}").getAsJsonObject();
        response.add("files", files);
        sendResponse(HttpURLConnection.HTTP_OK, response);
    }
}
