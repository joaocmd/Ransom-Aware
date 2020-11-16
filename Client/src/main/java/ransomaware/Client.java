package ransomaware;

import ransomaware.commands.*;

import javax.swing.text.html.Option;
import java.io.Console;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class Client {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private CookieManager cm = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private HttpClient client = HttpClient.newBuilder().cookieHandler(cm).executor(executor).build();
    private SessionInfo sessionInfo = new SessionInfo();

    private final Map<String, Function<String[], Optional<AbstractCommand>>> parsers = new HashMap<>();

    private void populateParsers() {
        parsers.put("register", this::parseRegister);
        parsers.put("login", this::parseLogin);
        parsers.put("logout", this::parseLogout);
        parsers.put("get", this::parseGet);
        parsers.put("save", this::parseSave);
        parsers.put("list", this::parseList);
        parsers.put("exit", this::parseExit);
        parsers.put("clear", this::parseClear);
    }


    public void start() {
        this.populateParsers();
        Console console = System.console();
        String input;

//        register();
//        login();

        do {
            input = console.readLine("> ");
            String[] args = input.split(" ");

            if (parsers.containsKey(args[0])) {
                parsers.get(args[0]).apply(args).ifPresent(c -> c.run(client));

            } else {
                System.err.println("Command not found, use 'help'");
            }
        } while (!input.equals("exit"));

        // Shutdown HTTP Client
        executor.shutdownNow();
        client = null;
        System.gc();
    }

//    private void register() {
//        System.out.println("Register? [Yy]");
//        Console console = System.console();
//        String answer = console.readLine();
//        if (answer.toLowerCase().startsWith("y")) {
//            RegisterCommand register = new RegisterCommand();
//            boolean success = false;
//            while (!success) {
//                success = register.run(new String[]{"register"}, client);
//            }
//        }
//    }

//    private void login() {
//        System.out.println("Login:");
//        LoginCommand login = new LoginCommand();
//        while (username == null) {
//            boolean success = login.run(new String[]{"login"}, client);
//            if (success) {
//                 username = login.getUsername();
//                 sessionToken = login.getSessionToken();
//            }
//        }
//    }

    private Optional<AbstractCommand> parseRegister(String[] args) {
        Runnable showUsage = () -> System.err.println("register usage: no args");

        if (args.length != 1) {
            showUsage.run();
            return Optional.empty();
        }
        return Optional.of(new RegisterCommand());
    }

    private Optional<AbstractCommand> parseLogin(String[] args) {
        Runnable showUsage = () -> System.err.println("login usage: no args");

        if (args.length != 1) {
            showUsage.run();
            return Optional.empty();
        }
        if (sessionInfo.isLogged())  {
            System.err.println("Already logged in");
            return  Optional.empty();
        }

        return Optional.of(new LoginCommand(sessionInfo));
    }

    private Optional<AbstractCommand> parseLogout(String[] args) {
        Runnable showUsage = () -> System.err.println("logout usage: no args");

        if (args.length != 1) {
            showUsage.run();
            return Optional.empty();
        }
        if (!sessionInfo.isLogged()) {
            System.err.println("Not currently logged in");
            return Optional.empty();
        }

        return Optional.of(new LogoutCommand(sessionInfo));
    }

    private String[] parseFileName(String name) {
        String[] parts = name.split("/");
        if (parts.length > 2) {
            return null;
        }

        String user;
        String file;
        if (parts.length == 1) {
            user = sessionInfo.getUsername();
            file = parts[0];
        } else {
            user = parts[0];
            file = parts[1];
        }

        return new String[]{user, file};
    }

    private Optional<AbstractCommand> parseGet(String[] args) {
        Runnable showUsage = () -> {
            System.err.println("get <file> usage:");
            System.err.println("    file: file name, can be user/file or just file");
        };
        if (args.length != 2) {
            showUsage.run();
            return Optional.empty();
        }

        String[] file = parseFileName(args[1]);
        if (file == null) {
            showUsage.run();
            return Optional.empty();
        }

        return Optional.of(new GetFileCommand(file[0], file[1]));
    }

    private Optional<AbstractCommand> parseSave(String[] args) {
        Runnable showUsage = () -> {
            System.err.println("save <file> usage:");
            System.err.println("    file: file name, can be user/file or just file");
        };
        if (args.length != 2) {
            showUsage.run();
            return Optional.empty();
        }

        String[] file = parseFileName(args[1]);
        if (file == null) {
            showUsage.run();
            return  Optional.empty();
        }

        return Optional.of(new SaveFileCommand(file[0], file[1]));
    }

    private Optional<AbstractCommand> parseList(String[] args) {
        Runnable showUsage = () -> System.err.println("login usage: no args");

        if (args.length != 1) {
            showUsage.run();
            return Optional.empty();
        }
        return Optional.of(new ListFilesCommand());
    }

    private Optional<AbstractCommand> parseExit(String[] args) {
        if (sessionInfo.isLogged()) {
            return  Optional.of(new LogoutCommand(sessionInfo));
        }
        return Optional.empty();
    }

    private Optional<AbstractCommand> parseClear(String[] args) {
        Runnable showUsage = () -> System.err.println("clear usage: no args");

        if(args.length != 1) {
            showUsage.run();
            return Optional.empty();
        }

        return Optional.of(new ClearCommand());
    }
}
