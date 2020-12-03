package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;
import ransomaware.SessionInfo;
import ransomaware.exceptions.CertificateInvalidException;
import ransomaware.exceptions.CertificateNotFoundException;

import java.io.Console;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;


public class RegisterCommand implements Command{

    private final SessionInfo sessionInfo;

    public RegisterCommand(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    @Override
    public void run(HttpClient client) {
        Console console = System.console();
        String user = console.readLine("Username: ");
        String password = new String(console.readPassword("Password: "));

        X509Certificate encryptionCert = null;
        while (encryptionCert == null) {
            encryptionCert = readCertificate(user, "encryption");
        }

        X509Certificate signingCert = null;
        while (signingCert == null) {
            signingCert = readCertificate(user, "signing");
        }


        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("username", user);
        jsonRoot.addProperty("password", password);
        try {
            jsonRoot.addProperty("encrypt-cert", SecurityUtils.getBase64(encryptionCert.getEncoded()));
            jsonRoot.addProperty("sign-cert", SecurityUtils.getBase64(signingCert.getEncoded()));
        } catch (CertificateEncodingException e) {
            System.err.println("Could not encode certificates");
            return;
        }

        JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/register", jsonRoot, client);
        if (response.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
            Utils.handleError(response, this.sessionInfo);
            return;
        }

        jsonRoot.remove("encrypt-cert");
        jsonRoot.remove("sign-cert");
        response = Utils.requestPostFromURL(ClientVariables.URL + "/login", jsonRoot, client);
        if (response.get("status").getAsInt() == HttpURLConnection.HTTP_OK) {
            sessionInfo.login(user);
            System.out.println("Registered and logged in successfully");
        } else {
            Utils.handleError(response, this.sessionInfo);
        }
    }

    private X509Certificate readCertificate(String username, String name) {
        Console console = System.console();

        String path = console.readLine(String.format("Path to %s certificate: ", name));

        X509Certificate cert = null;
        try {
            cert = SecurityUtils.readCertificate(path);

            if (!SecurityUtils.checkCertificateUser(cert, username)) {
                System.err.println("Certificate does not correspond to the user");
                return null;
            }
        } catch (CertificateNotFoundException e) {
            System.err.println("Certificate not found");
        } catch (CertificateInvalidException e) {
            System.err.println("Certificate is invalid");
        }
        return cert;
    }
}
