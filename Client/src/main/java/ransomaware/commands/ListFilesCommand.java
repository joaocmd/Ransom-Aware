package ransomaware.commands;

import ransomaware.ClientVariables;

import java.net.http.HttpClient;

public class ListFilesCommand extends AbstractCommand {

    public ListFilesCommand() {
        super();
    }

    /**
     * run
     * @param args - ['list'] at most
     * @param client - the Http Client
     * @return if the commands has succeeded
     */
    public boolean run(String[] args, HttpClient client) {
        if (args.length > 1) {
            System.out.println("List: Too many arguments.\nExample: list");
            return false;
        }
        String response = super.requestGetFromURL(ClientVariables.URL + "/list", client);
        System.out.print(response);

        return true;
    }
}