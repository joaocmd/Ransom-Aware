package ransomaware.commands;

import com.google.gson.JsonObject;
import ransomaware.ClientVariables;
import ransomaware.SessionInfo;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

public class ListFilesCommand implements Command {

    private SessionInfo info;

    public ListFilesCommand(SessionInfo info) {
        this.info = info;
    }

    @Override
    public void run(HttpClient client) {
        JsonObject response = Utils.requestGetFromURL(ClientVariables.URL + "/list", client);

        List<String> userFiles = new ArrayList<>();
        List<String> otherFiles = new ArrayList<>();
        response.getAsJsonArray("files").forEach(e -> {
            String[] split = e.getAsString().split("/");
            if (split[0].equals(info.getUsername())) {
                userFiles.add(split[1]);
            } else {
                otherFiles.add(e.getAsString());
            }
        });

        prettyPrint("My files", userFiles);
        System.out.println();
        prettyPrint("Other files", otherFiles);
    }

    private void prettyPrint(String name, List<String> list) {
        System.out.println(name + ':');
        if (list.isEmpty()) {
            System.out.println("No files found here");
        } else {
            list.stream().sorted().forEachOrdered(e -> System.out.println("- " + e));
        }
    }
}