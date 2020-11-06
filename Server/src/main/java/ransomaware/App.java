package ransomaware;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

public class App implements Callable<Integer> {

    @Option(names = {"-n", "--name"}, description = "Instance name.")
    private String name;

    @Option(names = {"-p", "--port"}, description = "Server port to bind to.")
    private int port;

    @Option(names = {"-s", "--set-up"}, negatable = true, description = "Run first time setup if specified.")
    private boolean firstTime;

    @Option(names = {"-db", "--db-url"}, description = "MongoDB URL")
    private String mongoUrl = "mongodb://localhost:27017";

    public Integer call() throws Exception {
        Server server = new Server(name, port, firstTime);
        ServerVariables.init(name, mongoUrl);
        server.start();
        return 0;
    }

    public static void Main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
