package ransomaware.handlers;

import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;
import ransomaware.Server;
import ransomaware.ServerVariables;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;

public class ListFileHandler extends AbstractHandler {

    public ListFileHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        super.handle(exchange);
        System.out.println("Handling list GET");

        // Get list of files
        // TODO: Get files of user
        String message = "Your files:\n" + listFiles(ServerVariables.FS_PATH);

        super.sendResponse(HttpURLConnection.HTTP_OK, message);
    }

    private String listFiles(String startDir) {
        File dir = new File(startDir);
        File[] files = dir.listFiles();
        StringBuilder message = new StringBuilder();

        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    message.append("|-- ").append(file.getName()).append("/\n");
                    message.append("    ").append(listFiles(file.getAbsolutePath()));
                } else {
                    message.append("|-- ").append(file.getName()).append(" (size in bytes: ")
                            .append(file.length()).append(")").append('\n');
                }
            }
        }
        return message.toString();
    }
}
