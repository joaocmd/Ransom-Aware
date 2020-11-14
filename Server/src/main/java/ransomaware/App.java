package ransomaware;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import ransomaware.exceptions.DuplicateUsernameException;

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

    public Integer call() {
        ServerVariables.init(path, mongoUrl);
        RansomAware ransomAware = new RansomAware(path, port, firstTime);
        try {
            SessionManager.register("joao", "pass");
        } catch (DuplicateUsernameException ignored) { }
        int token = SessionManager.login("joao", "pass");
        ransomAware.uploadFile(token, "o_meu_primeiro_ficheiro.txt", "POR FAVOR SO QUERO SER AMADO".getBytes());
        ransomAware.uploadFile(token, "outro.txt", "Ola colegas".getBytes());
        ransomAware.uploadFile(token, "o_meu_primeiro_ficheiro.txt", "Apaguei os meus segredos tinha vergonha".getBytes());
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
//        System.exit(exitCode);
    }
}
