package ransomaware.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.SecurityUtils;
import ransomaware.SessionManager;
import ransomaware.exceptions.CertificateInvalidException;
import ransomaware.exceptions.DuplicateUsernameException;
import ransomaware.exceptions.InvalidUserNameException;

import java.net.HttpURLConnection;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import java.util.Optional;

public class RegisterHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(RegisterHandler.class.getName());

    public RegisterHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        super.handle(exchange);
        JsonObject body = getBodyAsJSON();

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();
        String certificateBase64 = body.get("certificate").getAsString();

        LOGGER.info(String.format("register request: %s password: [REDACTED]", username));

        Optional<X509Certificate> cert = SecurityUtils.getCertificate(SecurityUtils.decodeBase64(certificateBase64));
        if (cert.isEmpty()) {
            LOGGER.info("Certificate could not be read.");
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Certificate could not be read.");
            return;
        }

        try {
            if (!SecurityUtils.isCertificateOfUser(cert.get(), username)) {
                LOGGER.info("Certificate is not of user registered.");
                sendResponse(HttpURLConnection.HTTP_FORBIDDEN, "Certificate is not of user registered.");
                return;
            }
        } catch (CertificateInvalidException e) {
            LOGGER.info("Certificate is invalid.");
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Certificate is invalid.");
            return;
        }

        try {
            SessionManager.register(username, password, certificateBase64);
            sendResponse(HttpURLConnection.HTTP_OK, "OK");
        } catch (DuplicateUsernameException e) {
            sendResponse(HttpURLConnection.HTTP_CONFLICT, "Username already registered");
        } catch(InvalidUserNameException e) {
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, "Username must be alphanumeric");
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            e.printStackTrace();
            sendResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "Something unexpected happened");
        }
    }
}
