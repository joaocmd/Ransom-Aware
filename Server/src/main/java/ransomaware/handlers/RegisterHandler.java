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
        String encodedEncryptCert = body.get("encrypt-certificate").getAsString();
        String encodedSignCert = body.get("sign-certificate").getAsString();

        LOGGER.info(String.format("register request: %s password: [REDACTED]", username));

        if (!validateCertificate(username, encodedEncryptCert, "Encrypting")) {
            return;
        }
        if (!validateCertificate(username, encodedSignCert, "Signing")) {
            return;
        }

        try {
            SessionManager.register(username, password, encodedEncryptCert, encodedSignCert);
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

    private boolean validateCertificate(String username, String encodedCert, String certName) {
        Optional<X509Certificate> cert = SecurityUtils.getCertificate(SecurityUtils.decodeBase64(encodedCert));
        if (cert.isEmpty()) {
            String message = String.format("%s certificate could not be read", certName);
            LOGGER.info(message);
            sendResponse(HttpURLConnection.HTTP_BAD_REQUEST, message);
            return false;
        }

        try {
            if (!SecurityUtils.isCertificateOfUser(cert.get(), username)) {
                String message = String.format("%s certificate does not belong to the user", certName);
                LOGGER.info(message);
                sendResponse(HttpURLConnection.HTTP_FORBIDDEN, message);
                return false;
            }
        } catch (CertificateInvalidException e) {
            String message = String.format("%s certificate is invalid", certName);
            LOGGER.info(message);
            sendResponse(HttpURLConnection.HTTP_FORBIDDEN, message);
            return false;
        }
        return true;
    }
}
