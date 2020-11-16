package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ransomaware.CookieManager;
import ransomaware.RansomAware;
import ransomaware.SessionManager;
import ransomaware.exceptions.InvalidMethodException;
import ransomaware.exceptions.SessionExpiredException;
import ransomaware.exceptions.UnauthorizedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.HttpCookie;
import java.util.List;
import java.util.Optional;

public abstract class AbstractHandler implements HttpHandler {

    protected final RansomAware server;
    private final String method;
    private final boolean requireAuth;
    private JsonObject body;
    private HttpExchange exchange;
    private int sessionToken;

    public AbstractHandler(RansomAware server, String method, boolean requireAuth) {
        this.server = server;
        this.method = method;
        this.requireAuth = requireAuth;
    }

    @Override
    public void handle(HttpExchange exchange) {
        this.exchange = exchange;
        if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
            throw new InvalidMethodException();
        }

        if(exchange.getRequestMethod().equals("POST")){
            convertBodyToJSON();
        }

        if (requireAuth) {
            Optional<String> loginCookie = exchange.getRequestHeaders().get("Cookie").stream().filter(s -> s.startsWith("login-token")).findFirst();

            if (!loginCookie.isPresent()) {
                sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "Invalid session token");
                return;
            }

            HttpCookie cookie = HttpCookie.parse(loginCookie.get()).get(0);
            int tokenVal = Integer.valueOf(cookie.getValue());

            this.sessionToken = tokenVal;
            switch (SessionManager.getSessionSate(cookie)) {
                case INVALID:
                    sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "Invalid session token");
                    throw new UnauthorizedException();
                case EXPIRED:
                    sendResponse(HttpURLConnection.HTTP_UNAUTHORIZED, "Session token expired");
                    throw new SessionExpiredException();
                default:
            }
        }
    }


    protected void convertBodyToJSON() {
        try (InputStream is = exchange.getRequestBody()) {
            String bodyString = new String(is.readAllBytes());
            JsonObject bodyJson = JsonParser.parseString(bodyString).getAsJsonObject();
            this.body = bodyJson;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error on reading request body");
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Malformed request body");
        }
    }

    protected JsonObject getBodyAsJSON() {
        return this.body;
    }

    protected void sendResponse(int statusCode, String message) {
        JsonObject responseObj = JsonParser.parseString("{}").getAsJsonObject();
        responseObj.addProperty("body", message);
        sendResponse(statusCode, responseObj);
    }

    protected void sendResponse(int statusCode, JsonObject object) {
        try (OutputStream os = this.exchange.getResponseBody()) {
            object.addProperty("status", statusCode);
            String message = object.toString();

            this.exchange.getResponseHeaders().set("Content-Type", "application/json");
            this.exchange.sendResponseHeaders(statusCode, message.length());
            os.write(message.getBytes());
            os.flush();
        } catch (IOException e) {
            System.err.println("Error on writing response body");
        }
    }

    protected void sendResponse(int statusCode, String message, HttpCookie cookie) {
        JsonObject responseObj = JsonParser.parseString("{}").getAsJsonObject();
        responseObj.addProperty("body", message);
        sendResponse(statusCode, responseObj, cookie);
    }

    protected void sendResponse(int statusCode, JsonObject object, HttpCookie cookie) {
        try (OutputStream os = this.exchange.getResponseBody()) {
            object.addProperty("status", statusCode);
            String message = object.toString();

            this.exchange.getResponseHeaders().set("Content-Type", "application/json");
            this.exchange.getResponseHeaders().set("Set-Cookie", cookie.toString());
            this.exchange.sendResponseHeaders(statusCode, message.length());
            os.write(message.getBytes());
            os.flush();
        } catch (IOException e) {
            System.err.println("Error on writing response body");
        }
    }

    protected int getSessionToken() {
        return this.sessionToken;
    }
}
