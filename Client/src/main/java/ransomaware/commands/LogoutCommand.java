package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SessionInfo;

import java.io.File;
import java.net.http.HttpClient;

public class LogoutCommand extends AbstractCommand {

    private final SessionInfo info;

    public LogoutCommand(SessionInfo info) {
        this.info = info;
    }

    @Override
    public void run(HttpClient client) {
        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();
        Utils.addLoginToken(jsonRoot, info.getSessionToken());

        Utils.requestPostFromURL(ClientVariables.URL + "/login", jsonRoot, client);
        info.setUsername(null);
        info.setLogged(false);

        // TODO: delete workspace folder
//        File file = new File(ClientVariables.WORKSPACE);
//        file.delete();
    }
}