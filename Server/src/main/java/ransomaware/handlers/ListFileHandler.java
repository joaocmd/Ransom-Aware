package ransomaware.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;

public class ListFileHandler extends AbstractHandler {

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
        server.listFiles(getSessionToken()).forEachOrdered(files::add);

        JsonObject response = JsonParser.parseString("{}").getAsJsonObject();
        response.add("files", files);

        super.sendResponse(HttpURLConnection.HTTP_OK, response);
    }
}
