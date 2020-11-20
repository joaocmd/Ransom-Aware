package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;

import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;


public class RegisterCommand extends AbstractCommand{

    @Override
    public void run(HttpClient client) {
        Console console = System.console();
        String user = console.readLine("username: ");
        String password = new String(console.readPassword("password: "));

        // Get user's certificate
        String certificatePath = ClientVariables.FS_PATH + "/" + user + ".pem";
        Optional<X509Certificate> certificate = SecurityUtils.getCertificate(certificatePath);
        Optional<byte[]> cert = SecurityUtils.getCertificateToSend(certificatePath);
        if (certificate.isEmpty() || cert.isEmpty()) {
            System.err.println("Certificate could not be read");
            return;
        }

        // Check if certificate is given to user
        if (!SecurityUtils.checkCertificateUser(certificate.get(), user)) {
            System.err.println("Certificate doesn't correspond to user.");
            return;
        }

        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        jsonRoot.addProperty("username", user);
        jsonRoot.addProperty("password", password);
        jsonRoot.addProperty("certificate", SecurityUtils.getBase64(cert.get()));

        System.out.println(jsonRoot.toString());

        JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/register", jsonRoot, client);
        if (response.get("status").getAsInt() != HttpURLConnection.HTTP_OK) {
            Utils.handleError(response);
        }
    }
}
