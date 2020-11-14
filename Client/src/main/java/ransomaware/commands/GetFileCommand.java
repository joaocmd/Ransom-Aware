package ransomaware.commands;

import java.net.http.HttpClient;

public class GetFileCommand extends AbstractCommand {

    public GetFileCommand() {
        super();
    }

    /**
     * run
     * @param args - for example: ['get','a.txt'] or ['get','masterzeus','a.txt']
     * @param client - the HttpClient
     * @return if the commands has succeeded
     */
    public boolean run(String[] args, HttpClient client) {
        if (args.length == 1 || args.length > 2) {
            System.out.println("get: Too many arguments.\nExample: get a.txt");
            return false;
        }
        String[] file = args[1].split("/");
        String user = "";
        String filename = "";
        if (file.length == 1) { user = ""; filename = file[0]; }
        else if (file.length == 2) { user = file[0]; filename = file[1]; }

        try {
            // TODO: Send request to get file
            // TODO: Write file

        } catch (Exception e) {
            // FIXME:
            e.printStackTrace();
        }

        return true;
    }
}