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
import java.util.Optional;


public class RegisterCommand extends AbstractCommand{

    private SessionInfo sessionInfo;

    public RegisterCommand(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    @Override
    public void run(HttpClient client) {
        Console console = System.console();
        String user = console.readLine("username: ");
        String password = new String(console.readPassword("password: "));

        // Get user's certificate
        String certificatePath = ClientVariables.FS_PATH + "/" + user + ".pem";
        byte[] cert;

        // Check if certificate is given to user
        try {
            cert = SecurityUtils.getCertificateToSend(certificatePath);

            if (!SecurityUtils.checkCertificateUser(certificatePath, user)) {
                System.err.println("Certificate does not correspond to the user.");
                return;
            }
        } catch (CertificateNotFoundException e) {
            System.err.println("Certificate cannot be found.");
            return;
        } catch (CertificateInvalidException e) {
            System.err.println("Certificate is invalid.");
            return;
        }

        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("username", user);
        jsonRoot.addProperty("password", password);
        jsonRoot.addProperty("certificate", SecurityUtils.getBase64(cert));

        JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/register", jsonRoot, client);
        if (response.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
            Utils.handleError(response);
        }
    }
}
