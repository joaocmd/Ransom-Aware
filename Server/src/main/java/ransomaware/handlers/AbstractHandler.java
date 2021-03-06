package ransomaware.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.exceptions.InvalidMethodException;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

public abstract class AbstractHandler implements HttpHandler {

    private static final Logger LOGGER = Logger.getLogger(AbstractHandler.class.getName());

    protected final RansomAware server;
    private final String method;
    private final boolean requireAuth;
    private JsonObject body;
    private HttpExchange exchange;
    private String sessionToken;

    public AbstractHandler(RansomAware server, String method, boolean requireAuth) {
        this.server = server;
        this.method = method;
        this.requireAuth = requireAuth;
    }

    @Override
    public void handle(HttpExchange exchange) {
        this.exchange = exchange;
        if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
            sendResponse(HttpURLConnection.HTTP_BAD_METHOD, "Bad method");
            throw new InvalidMethodException();
        }

        if(exchange.getRequestMethod().equals("POST")){
            convertBodyToJSON();
        }

        if (requireAuth) {
            try {
                Optional<String> loginCookie = Stream.of(exchange.getRequestHeaders().get("Cookie").get(0).split(";"))
                        .map(String::trim)
                        .filter(s -> s.startsWith("login-token"))
                        .findFirst();

                if (loginCookie.isEmpty()) {
                    sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "No session token given");
                    return;
                }

                HttpCookie cookie = HttpCookie.parse(loginCookie.get()).get(0);
                this.sessionToken = cookie.getValue();
                switch (SessionManager.getSessionSate(this.sessionToken)) {
                    case INVALID:
                        sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "Invalid session token");
                        throw new UnauthorizedException();
                    case EXPIRED:
                        sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "Session token expired");
                        throw new SessionExpiredException();
                    default:
                }
            }catch(NullPointerException e) {
                sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "No session token given");
                throw new SessionExpiredException();
            }
        }
    }


    protected void convertBodyToJSON() {
        try (InputStream is = exchange.getRequestBody()) {
            String bodyString = new String(is.readAllBytes());
            JsonElement jsonParsed = JsonParser.parseString(bodyString);
            if (!jsonParsed.isJsonNull()) {
                this.body = jsonParsed.getAsJsonObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.severe("Error on reading request body");
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Malformed request body");
        }
    }

    protected JsonObject getBodyAsJSON() {
        return this.body;
    }

    protected void sendResponse(int statusCode, String message) {
        sendResponse(statusCode, message, null);
    }

    protected void sendResponse(int statusCode, JsonObject object) {
        sendResponse(statusCode, object, null);
    }

    protected void sendResponse(int statusCode, String message, String cookie) {
        JsonObject responseObj = JsonParser.parseString("{}").getAsJsonObject();
        responseObj.addProperty("body", message);
        sendResponse(statusCode, responseObj, cookie);
    }

    protected void sendResponse(int statusCode, JsonObject object, String cookie) {
        this.exchange.getResponseHeaders().set("Content-Type", "application/json");
        if (cookie != null) {
            this.exchange.getResponseHeaders().set("Set-Cookie", cookie);
        }

        object.addProperty("status", statusCode);
        String message = object.toString();
        try {
            this.exchange.sendResponseHeaders(statusCode, message.length());
        } catch (IOException e) {
            LOGGER.severe("Error on writing response headers");
            e.printStackTrace();
            return;
        }
        try (OutputStream os = this.exchange.getResponseBody()) {
            os.write(message.getBytes());
            os.flush();
        } catch (IOException e) {
            LOGGER.severe("Error on writing response body");
        }
    }

    protected String getSessionToken() {
        return this.sessionToken;
    }
}
