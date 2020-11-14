package ransomaware;

import ransomaware.commands.*;

import java.io.Console;
import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    HttpClient client = HttpClient.newBuilder().executor(executor).build();
    int sessionToken;
    boolean hasSessionToken = false;

    public void start() {
        Console console = System.console();
        String command;

        do {
            command = console.readLine("> ");
            String[] args = command.split(" ");

            // TODO: Commands
            switch (args[0]) {
                // list
                case ("list"):
                    AbstractCommand list = new ListFilesCommand();
                    list.run(args, client);
                    break;
                // get user/file.txt
                case ("get"):
                    AbstractCommand get = new GetFileCommand();
                    get.run(args, client);
                    break;
                // send file.txt
                case ("save"):
                    AbstractCommand save = new SaveFileCommand();
                    save.run(args, client);
                    // saveFile(args);
                    break;
                // login
                case ("login"):
                    AbstractCommand login = new LoginCommand();
                    hasSessionToken = login.run(args, client);;
                    sessionToken = login.getSessionToken();
                    break;
                case ("help"):
                    AbstractCommand help = new HelpCommand();
                    help.run(args, client);;
                    break;
                case ("exit"):
                    break;
                default:
                    System.out.println("Command not found.");
            }

        } while (!command.equals("exit"));

        // Shutdown HTTP Client
        executor.shutdownNow();
        client = null;
        System.gc();
    }
}
