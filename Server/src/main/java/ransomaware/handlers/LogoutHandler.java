package ransomaware.handlers;

import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.HttpURLConnection;
import java.util.logging.Logger;

public class LogoutHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(LogoutHandler.class.getName());

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

        String sessionToken = this.getSessionToken();
        LOGGER.info(String.format("logout request: %s", SessionManager.getUsername(sessionToken)));
        server.logout(sessionToken);
        sendResponse(HttpURLConnection.HTTP_OK, "OK");
    }
}
