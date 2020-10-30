package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.Server;
import ransomaware.SessionManager;
import ransomaware.exceptions.UnauthorizedException;

public class LoginHandler extends AbstractHandler {

    public LoginHandler(Server server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    public void handle(HttpExchange exchange) {
        super.handle(exchange);
        JsonObject body = getBodyAsJSON(exchange);

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();


        try {
            String sessionToken = SessionManager.login(username, password);
        } catch (UnauthorizedException e) {
        }
    }
}
