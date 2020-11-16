package ransomaware.handlers;

import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;

public class LogoutHandler extends AbstractHandler {

    public LogoutHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            super.handle(exchange);
        } catch (UnauthorizedException | SessionExpiredException ignored) {
            return;
        }

        server.logout(this.getSessionToken());
        sendResponse(HttpURLConnection.HTTP_OK, "OK");
    }
}
