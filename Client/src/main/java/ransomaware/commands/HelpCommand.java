package ransomaware.commands;

import java.net.http.HttpClient;

public class HelpCommand implements Command {

    @Override
    public void run(HttpClient client) {
        String help = "List of commands:\n" +
                "list - list files with granted access\n" +
                "get - download file to workspace \n" +
                "list-permissions - list users with file permissions \n" +
                "save - save file to server\n" +
                "grant - grant permissions of read/edit file to user\n" +
                "revoke - grant permissions of read/edit file from user\n" +
                "clear - clear workspace\n";

        System.out.println(help);
    }
}