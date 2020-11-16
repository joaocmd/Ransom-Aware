package ransomaware.commands;

import ransomaware.ClientVariables;
import ransomaware.SessionInfo;

import java.io.File;
import java.net.http.HttpClient;

public class ClearCommand extends AbstractCommand{

    public ClearCommand(){}

    @Override
    public void run(HttpClient client) {
        Utils.clearWorkspace(new File(ClientVariables.WORKSPACE));
    }
}
