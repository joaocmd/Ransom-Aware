package ransomaware;

import ransomaware.commands.*;

import java.io.Console;
import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private HttpClient client = HttpClient.newBuilder().executor(executor).build();
    private String sessionToken;
    private String username = null;

    public void start() {
        Console console = System.console();
        String command;

        register();
        login();

        do {
            command = console.readLine("> ");
            String[] args = command.split(" ");

            switch (args[0]) {
                case "register":
                    AbstractCommand register = new RegisterCommand();
                    register.run(args, client);
                    break;
                case "list":
                    AbstractCommand list = new ListFilesCommand(sessionToken);
                    list.run(args, client);
                    break;
                case "get":
                    AbstractCommand get = new GetFileCommand(sessionToken, username);
                    get.run(args, client);
                    break;
                case "save":
                    AbstractCommand save = new SaveFileCommand(sessionToken, username);
                    save.run(args, client);
                    break;
                case "login":
                    // FIXME: handle logout and session expired
                    System.err.println("Already logged in");
                    break;
                case "help":
                    AbstractCommand help = new HelpCommand();
                    help.run(args, client);
                    break;
                case "exit":
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

    private void register() {
        System.out.println("Register? [Yy]");
        Console console = System.console();
        String answer = console.readLine();
        if (answer.toLowerCase().startsWith("y")) {
            RegisterCommand register = new RegisterCommand();
            boolean success = false;
            while (!success) {
                success = register.run(new String[]{"register"}, client);
            }
        }
    }

    private void login() {
        System.out.println("Login:");
        LoginCommand login = new LoginCommand();
        while (username == null) {
            boolean success = login.run(new String[]{"login"}, client);
            if (success) {
                 username = login.getUsername();
                 sessionToken = login.getSessionToken();
            }
        }
    }
}
