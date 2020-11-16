package ransomaware.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ransomaware.ClientVariables;
import ransomaware.SessionInfo;

import java.io.Console;
import java.io.File;
import java.net.http.HttpClient;

public class LogoutCommand extends AbstractCommand {

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
    }
}