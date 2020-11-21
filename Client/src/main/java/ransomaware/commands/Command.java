package ransomaware.commands;

import java.net.http.HttpClient;

public interface Command {

    public abstract void run(HttpClient client);
}