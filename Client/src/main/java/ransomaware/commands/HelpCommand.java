package ransomaware.commands;

import java.net.http.HttpClient;

public class HelpCommand extends AbstractCommand {

    @Override
    public void run(HttpClient client) {
        String help = "List of commands:\n" +
                "login - \n" +
                "list - \n" +
                "get - \n" +
                "save - \n" +
                "register - \n";

        System.out.println(help);
    }
}