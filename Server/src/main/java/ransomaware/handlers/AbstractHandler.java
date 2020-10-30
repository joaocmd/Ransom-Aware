package ransomaware.handlers;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.JsonObject;

import ransomaware.Server;
import ransomaware.SessionManager;
import ransomaware.exceptions.InvalidMethodException;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractHandler implements HttpHandler {

    protected final Server server;
    private final String method;
    private final boolean requireAuth;
    private JsonObject body;
    private HttpExchange exchange;

    public AbstractHandler(Server server, String method, boolean requireAuth) {
        this.server = server;
        this.method = method;
        this.requireAuth = requireAuth;
    }

    public void handle(HttpExchange exchange) {
        this.exchange = exchange;
        if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
            throw new InvalidMethodException();
        }
        if (requireAuth) {
            Integer token = getBodyAsJSON(exchange).get("login-token").getAsInt();
            switch (SessionManager.getSessionSate(token)) {
                case INVALID:
                    throw new UnauthorizedException();
                case EXPIRED:
                    throw new SessionExpiredException();
                default:
            }
        }
    }

    protected JsonObject getBodyAsJSON() {
        if (this.body != null) {
            return this.body;
        }

        try (InputStream is = exchange.getRequestBody()) {
            String bodyString = new String(is.readAllBytes());
            JsonObject bodyJson = JsonParser.parseString(bodyString).getAsJsonObject();
            this.body = bodyJson;
            return bodyJson;
        } catch (IOException e) {
            System.err.println("Error on reading request body");
        }
        return null;
    }

    protected void sendResponse(int statusCode, String message) {
        try (OutputStream os = this.exchange.getResponseBody()) {
            this.exchange.sendResponseHeaders(statusCode, message.length());
            os.write(message.getBytes());
            os.flush();
        } catch (IOException e) {
            System.err.println("Error on writing response body");
        }
    }
}
