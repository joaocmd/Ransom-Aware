package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SecurityUtils;
import ransomaware.SessionInfo;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;

public class RollbackCommand implements Command {

    private final String owner;
    private final String filename;
    private final SessionInfo sessionInfo;
    private final int n;

    public RollbackCommand(SessionInfo sessionInfo, String owner, String filename, int n) {
        this.owner = owner;
        this.filename = filename;
        this.sessionInfo = sessionInfo;
        this.n = n;
    }

    @Override
    public void run(HttpClient client) {
        try {
            JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
            jsonRoot.addProperty("n", n);

            JsonObject file = JsonParser.parseString("{}").getAsJsonObject();
            file.addProperty("owner", owner);
            file.addProperty("name", filename);
            jsonRoot.add("file", file);



            JsonObject response = Utils.requestPostFromURL(ClientVariables.URL + "/rollback", jsonRoot, client);
            if (response.get("status").getAsInt() == HttpURLConnection.HTTP_OK) {
                System.out.println("Successfully rolled back, fetching new version");
                new GetFileCommand(sessionInfo, owner, filename).run(client);
            } else {
                Utils.handleError(response, this.sessionInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
