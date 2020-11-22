package ransomaware.commands;

import ransomaware.ClientVariables;

import java.io.File;
import java.net.http.HttpClient;

public class ClearCommand implements Command {

    @Override
    public void run(HttpClient client) {
        Utils.clearWorkspace(new File(ClientVariables.WORKSPACE));
    }
}
