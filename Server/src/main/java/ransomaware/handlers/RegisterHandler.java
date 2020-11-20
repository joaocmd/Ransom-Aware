package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SecurityUtils;
import ransomaware.SessionManager;
import ransomaware.exceptions.DuplicateUsernameException;
import ransomaware.exceptions.InvalidUserNameException;

import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.util.Optional;

public class RegisterHandler extends AbstractHandler {

    public RegisterHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        super.handle(exchange);
        JsonObject body = getBodyAsJSON();

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();
        String certificateRaw = body.get("certificate").getAsString();
        Optional<X509Certificate> cert = SecurityUtils.getCertificate(SecurityUtils.decodeBase64(certificateRaw));
        if (cert.isEmpty()) {
            System.err.println("Certificate could not be read.");
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Certificate could not be read.");
            return;
        }

        // FIXME: Check if certificate is from user

        // TODO: Store certificate

        try {
            SessionManager.register(username, password);
            sendResponse(HttpURLConnection.HTTP_OK, "OK");
        } catch (DuplicateUsernameException e) {
            sendResponse(HttpURLConnection.HTTP_CONFLICT, "Username already registered");
        } catch(InvalidUserNameException e) {
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Username must be alphanumeric");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something unexpected happened");
        }
    }
}
