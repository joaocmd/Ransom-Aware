package ransomaware;

import ransomaware.commands.*;

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

public class RansomAwareClient {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final CookieManager cm = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private HttpClient client = HttpClient.newBuilder().cookieHandler(cm).executor(executor).build();
    private final SessionInfo sessionInfo;

    private final Map<String, Function<String[], Optional<Command>>> parsers = new HashMap<>();

    public RansomAwareClient(String encryptKeyPath, String signKeyPath) {
        this.sessionInfo = new SessionInfo(encryptKeyPath, signKeyPath);
    }

    private void populateParsers() {
        parsers.put("get", this::parseGet);
        parsers.put("save", this::parseSaveNoKeys);
        parsers.put("save-renew", this::parseSaveWithKeys);
        parsers.put("grant", this::parseGrant);
        parsers.put("revoke", this::parseRevoke);
        parsers.put("rollback", this::parseRollback);
        parsers.put("list", this::parseList);
        parsers.put("list-permissions", this::parseListPermissions);
        parsers.put("create", this::parseCreate);
        parsers.put("logout", this::parseLogout);
        parsers.put("help", this::parseHelp);
        parsers.put("exit", this::parseExit);
        parsers.put("clear", this::parseClear);
    }


    public void start() {
        this.populateParsers();
        Console console = System.console();
        String input;

        register();
        login();

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

    private void register() {
        System.out.println("Register? [Yy]");
        Console console = System.console();
        String answer = console.readLine();
        if (answer.toLowerCase().startsWith("y")) {
            RegisterCommand register = new RegisterCommand(sessionInfo);
            while (!sessionInfo.isLogged()) {
                register.run(client);
            }
        }
    }

    private void login() {
        if (!sessionInfo.isLogged()) {
            LoginCommand login = new LoginCommand(sessionInfo);
            while (!sessionInfo.isLogged()) {
                login.run(client);
            }
        }
    }

    private Optional<Command> parseLogout(String[] args) {
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

    private Optional<Command> parseGet(String[] args) {
        Runnable showUsage = () -> {
            System.err.println("get <file> usage: fetches file from the server");
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

        return Optional.of(new GetFileCommand(sessionInfo, file[0], file[1]));
    }

    private Optional<Command> parseListPermissions(String[] args) {
        Runnable showUsage = () -> {
            System.err.println("list-permissions <file> usage: fetches users with file permissions from the server");
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

        return Optional.of(new ListFilePermissionsCommand(sessionInfo, file[0], file[1]));
    }

    private Optional<Command> parseSaveWithKeys(String[] args) {
        return parseSave(args, true);
    }

    private Optional<Command> parseSaveNoKeys(String[] args) {
        return parseSave(args, false);
    }

    private Optional<Command> parseSave(String[] args, boolean generateNewKeys) {
        Runnable showUsage = () -> {
            System.err.println("save <file> usage: sends the file to the remote server");
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

        return Optional.of(new SendFileCommand(sessionInfo, file[0], file[1], generateNewKeys));
    }

    private Optional<Command> parseCreate(String[] args) {
        Runnable showUsage = () -> {
            System.err.println("create <file> usage: creates a file locally");
            System.err.println("    file: file name, can be user/file or just file, user must be current user");
        };
        if (args.length != 2) {
            showUsage.run();
            return Optional.empty();
        }

        String[] file = parseFileName(args[1]);
        if (file == null || !file[0].equals(sessionInfo.getUsername())) {
            showUsage.run();
            return  Optional.empty();
        }

        return Optional.of(new CreateFileCommand(file[0], file[1]));
    }

    private Optional<Command> parseGrant(String[] args) {
        Runnable showUsage = () -> {
            System.err.println("grant <file> <user> usage:");
            System.err.println("    file: file name, can be user/file or just file");
            System.err.println("    user: user name, to grant permissions");
        };
        if (args.length != 3) {
            showUsage.run();
            return Optional.empty();
        }

        String[] file = parseFileName(args[1]);
        if (file == null) {
            showUsage.run();
            return  Optional.empty();
        }

        return Optional.of(new GrantCommand(sessionInfo, file[0], file[1], args[2]));
    }

    private Optional<Command> parseRevoke(String[] args) {
        Runnable showUsage = () -> {
            System.err.println("revoke <file> <user> usage:");
            System.err.println("    file: file name, can be user/file or just file");
            System.err.println("    user: user name, to revoke permissions");
        };
        if (args.length != 3) {
            showUsage.run();
            return Optional.empty();
        }

        String[] file = parseFileName(args[1]);
        if (file == null) {
            showUsage.run();
            return  Optional.empty();
        }

        return Optional.of(new RevokeCommand(sessionInfo, file[0], file[1], args[2]));
    }

    private Optional<Command> parseRollback(String[] args) {
        Runnable showUsage = () -> {
            System.err.println("rollback <file> <n> usage:");
            System.err.println("    file: file name, can be user/file or just file");
            System.err.println("    n: number of versions to rollback");
        };
        if (args.length != 3) {
            showUsage.run();
            return Optional.empty();
        }

        String[] file = parseFileName(args[1]);
        if (file == null) {
            showUsage.run();
            return  Optional.empty();
        }

        try {
            return Optional.of(new RollbackCommand(sessionInfo, file[0], file[1], Integer.parseInt(args[2])));
        } catch (NumberFormatException e) {
            showUsage.run();
            return  Optional.empty();
        }
    }

    private Optional<Command> parseList(String[] args) {
        Runnable showUsage = () -> System.err.println("list usage: no args");

        if (args.length != 1) {
            showUsage.run();
            return Optional.empty();
        }
        return Optional.of(new ListFilesCommand(sessionInfo));
    }

    private Optional<Command> parseHelp(String[] args) {
        Runnable showUsage = () -> System.err.println("help usage: no args");

        if (args.length != 1) {
            showUsage.run();
            return Optional.empty();
        }
        return Optional.of(new HelpCommand());
    }

    private Optional<Command> parseExit(String[] args) {
        if (sessionInfo.isLogged()) {
            return  Optional.of(new LogoutCommand(sessionInfo));
        }
        return Optional.empty();
    }

    private Optional<Command> parseClear(String[] args) {
        Runnable showUsage = () -> System.err.println("clear usage: no args");

        if(args.length != 1) {
            showUsage.run();
            return Optional.empty();
        }

        return Optional.of(new ClearCommand());
    }
}
