package ransomaware;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

public class App implements Callable<Integer> {

    @Option(names = {"-p", "--path"}, required = false, description = "Instance path.")
    private String path = "workspace";

    @Option(names = {"-s", "--set-up"}, negatable = true, description = "Run first time setup if specified.")
    private boolean firstTime;

    @Option(names = {"-u", "--url"}, description = "Server url to connect to. P.e.: localhost:8843")
    private String url = "localhost:8443";

    public Integer call() throws Exception {
        RansomAware ransomAware = new RansomAware(path, url);
        return 0;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new App()).execute(args);
//        System.exit(exitCode);
    }
}
