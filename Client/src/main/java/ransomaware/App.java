package ransomaware;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

public class App implements Callable<Integer> {

    @Option(names = {"-p", "--path"}, required = false, description = "Instance path.")
    private String path = "ransom-aware/workspace";

    @Option(names = {"-u", "--url"}, description = "Server url to connect to. P.e.: localhost:8843")
    private String url = "https://localhost:8443";

    @Option(names = {"-d", "--decrypt-key"}, required = true, description = "Path to key used to decrypt files")
    private String decryptKeyPath;

    @Option(names = {"-s", "--signing-key"}, required = true, description = "Path to key used to sign files")
    private String signKeyPath;

    public Integer call() {
        ClientVariables.init(path, url);
        RansomAwareClient client = new RansomAwareClient(decryptKeyPath, signKeyPath);
        client.start();
        return 0;
    }

    public static void main(String[] args) {
       new CommandLine(new App()).execute(args);
    }
}
