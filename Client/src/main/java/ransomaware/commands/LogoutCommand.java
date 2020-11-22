package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SessionInfo;

import java.io.Console;
import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.http.HttpClient;
import java.util.Optional;

public class LogoutCommand implements Command {

    private final SessionInfo info;

    public LogoutCommand(SessionInfo info) {
        this.info = info;
    }

    @Override
    public void run(HttpClient client) {
        Console console = System.console();
        String clear = console.readLine(" Clear workspace? [y/n]: ");
        if (clear.toLowerCase().startsWith("y")) {
            Utils.clearWorkspace(new File(ClientVariables.WORKSPACE));
        }

        JsonObject jsonRoot = JsonParser.parseString("{}").getAsJsonObject();

        Utils.requestPostFromURL(ClientVariables.URL + "/logout", jsonRoot, client);
        info.logOff();

        Optional<CookieHandler> ch = client.cookieHandler();
        if(ch.isPresent()) {
            CookieManager cm = (CookieManager) ch.get();
            cm.getCookieStore().removeAll();
        }
    }
}