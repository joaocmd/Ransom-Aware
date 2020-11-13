package ransomaware;

import ransomaware.exceptions.UnauthorizedException;

import java.util.*;
import java.util.stream.Stream;

public class RansomAware {

    private final HashMap<String, Set<String>> userFiles = new HashMap<>();
    private final HashMap<String, Set<String>> usersWithAccess = new HashMap<>();

    public RansomAware(String path, int port, boolean firstTime) {
//        if (!firstTime) {
//             validateFS(path);
//        }

        Server.start(this, port);
    }

    private boolean isOwner(String user, String fileName) {
        return user.equals(fileName.split("/")[0]);
    }

    private boolean hasAccessToFile(String user, String fileName) {
        return isOwner(user, fileName);
    }

    public void uploadFile(int sessionToken, String file, byte[] data) {
        String user = SessionManager.getUsername(sessionToken);
        uploadFile(user, file, data);
    }

    public void uploadFile(String user, String file, byte[] data) {
        String fileName = FileManager.getFileName(user, file);
        if (hasAccessToFile(user, fileName)) {
            FileManager.saveFile(fileName, data);

            userFiles.putIfAbsent(user, new HashSet<>());
            userFiles.get(user).add(fileName);

            // draft for getting the certificates for all users with access later
            usersWithAccess.putIfAbsent(fileName, new HashSet<>());
            usersWithAccess.get(fileName).add(user);
        } else {
            throw new UnauthorizedException();
        }
    }

    public Stream<String> listFiles(int sessionToken) {
        String user = SessionManager.getUsername(sessionToken);
        return this.userFiles.get(user).stream();
    }
}
