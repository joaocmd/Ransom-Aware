package ransomaware.commands;

import ransomaware.ClientVariables;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;

public class CreateFileCommand implements Command {

    private final String owner;
    private final String filename;

    public CreateFileCommand(String owner, String filename) {
        this.owner = owner;
        this.filename = filename;
    }

    @Override
    public void run(HttpClient client) {
        try {
            File dir = new File(ClientVariables.WORKSPACE + '/' + owner);
            dir.mkdirs();
            dir = new File(ClientVariables.WORKSPACE + '/' + owner + '/' + filename);
            dir.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}