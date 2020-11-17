package ransomaware.commands;

import java.net.http.HttpClient;

public abstract class AbstractCommand {

    public abstract void run(HttpClient client);
}