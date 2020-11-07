package ransomaware.handlers;

import com.sun.net.httpserver.HttpExchange;
import ransomaware.RansomAware;

import java.net.HttpURLConnection;

public class ListFileHandler extends AbstractHandler {

    public ListFileHandler(RansomAware server, String method, boolean requireAuth) {
        super(server, method, requireAuth);
    }

    @Override
    public void handle(HttpExchange exchange) {
        super.handle(exchange);
        System.out.println("Handling list GET");
        super.sendResponse(HttpURLConnection.HTTP_OK, "<h1>list</h1>");
    }
}
