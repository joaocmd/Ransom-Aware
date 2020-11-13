package ransomaware;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

public class App implements Callable<Integer> {

    @Option(names = {"-p", "--path"}, required = true, description = "Instance path.")
    private String path;

    @Option(names = {"-P", "--port"}, description = "Server port to bind to.")
    private int port = 8443;

    @Option(names = {"-s", "--set-up"}, negatable = true, description = "Run first time setup if specified.")
    private boolean firstTime;

    @Option(names = {"-db", "--db-url"}, description = "MongoDB URL")
    private String mongoUrl = "mongodb://localhost:27017";

    public Integer call() {
        ServerVariables.init(path, mongoUrl);
//        RansomAware ransomAware = new RansomAware(path, port, firstTime);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        FileManager.saveFile("a/teste", "test123".getBytes());
//        System.exit(exitCode);
    }
}
