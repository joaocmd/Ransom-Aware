package ransomaware.commands;

import java.net.http.HttpClient;

public class HelpCommand extends AbstractCommand {

    public HelpCommand() {
        super("");
    }

    /**
     * run
     * @param args - ['help'] or ['help', 'list'] p.e.
     * @param client - the Http Client
     * @return if the commands has succeeded
     */
    public boolean run(String[] args, HttpClient client) {
        if (args.length > 2) {
            System.out.println("List: Too many arguments.\nExample: help\t or help list");
            return false;
        }
        else if (args.length == 1) {
            // TODO: Improve
            String help = "List of commands:\n" +
                    "login - \n" +
                    "list - \n" +
                    "get - \n" +
                    "save - \n";
        }

        return true;
    }
}