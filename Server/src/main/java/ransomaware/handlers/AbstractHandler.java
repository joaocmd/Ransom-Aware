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

public abstract class AbstractHandler implements HttpHandler {

    protected final Server server;
    private final String method;
    private final boolean requireAuth;
    private JsonObject body;

    public AbstractHandler(Server server, String method, boolean requireAuth) {
        this.server = server;
        this.method = method;
        this.requireAuth = requireAuth;
    }

    public void handle(HttpExchange exchange) {
        if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
            throw new InvalidMethodException();
        }
        if (requireAuth) {
            Integer token = getBodyAsJSON(exchange).get("login-token").getAsInt();
            switch (SessionManager.isValidSession(token)) {
                case INVALID:
                    throw new UnauthorizedException();
                case EXPIRED:
                    throw new SessionExpiredException();
                default:
            }
        }
    }

    protected JsonObject getBodyAsJSON(HttpExchange exchange) {
        if (this.body != null) {
            return this.body;
        }

        try {
            String bodyString = new String(exchange.getRequestBody().readAllBytes());
            JsonObject bodyJson = JsonParser.parseString(bodyString).getAsJsonObject();
            this.body = bodyJson;
            return bodyJson;
        } catch (IOException e) {
            System.err.println("Error on reading request body");
        }
        return null;
    }
}
