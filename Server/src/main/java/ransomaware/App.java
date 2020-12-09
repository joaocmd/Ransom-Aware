package ransomaware;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

public class App implements Callable<Integer> {

    @Option(names = {"-p", "--path"}, required = false, description = "Instance path.")
    private String path = "ransom-aware";

    @Option(names = {"-P", "--port"}, description = "Server port to bind to.")
    private int port = 8443;

    @Option(names = {"-s", "--set-up"}, negatable = true, description = "Run first time setup if specified.")
    private boolean firstTime;

    @Option(names = {"-db", "--db-url"}, description = "MongoDB URL")
    private String mongoUrl = "mongodb://localhost:27017";

    @Option(names = {"-r", "--rsync-uri"}, description = "Rsync server ssh uri")
    private String rsyncUri = "localhost:rsync/";

    public Integer call() {
        ServerVariables.init(path, mongoUrl, rsyncUri);
        new RansomAware(port, firstTime);
        return 0;
    }

    public static void main(String[] args) {
        new CommandLine(new App()).execute(args);
    }
}
