package ransomaware;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

public class App implements Callable<Integer> {

    @Option(names = {"-p", "--path"}, required = false, description = "Instance path.")
    private String path = "ransom-aware";

    @Option(names = {"-u", "--url"}, description = "Server url to connect to. P.e.: localhost:8843")
    private String url = "https://localhost:8443";

    // FIXME: Get private key and certificate paths and workspace folder

    public Integer call() throws Exception {
        ClientVariables.init(path, url);
        RansomAwareClient client = new RansomAwareClient();
        client.start();
        return 0;
    }

    public static void main(String[] args) {
       new CommandLine(new App()).execute(args);
    }
}
