package ransomaware;

import ransomaware.exceptions.NoSuchFileException;
import ransomaware.exceptions.UnauthorizedException;

import java.util.*;
import java.util.stream.Stream;

public class RansomAware {

    private final HashMap<String, Set<String>> userFiles = new HashMap<>();
    private final HashMap<String, Set<String>> usersWithAccess = new HashMap<>();

    public RansomAware(String path, int port, boolean firstTime) {
        // TODO: Validate FS and populate database
//        if (!firstTime) {
//             validateFS(path);
//        }

        Server.start(this, port);
    }

    private boolean isOwner(String user, String fileName) {
        System.out.println(user);
        System.out.println(fileName);
        return user.equals(fileName.split("/")[0]);
    }

    private boolean hasAccessToFile(String user, String fileName) {
        return isOwner(user, fileName);
    }

    public void uploadFile(int sessionToken, String file, byte[] data) {
        String owner = SessionManager.getUsername(sessionToken);
        uploadFile(sessionToken, owner, file, data);
    }

    public void uploadFile(int sessionToken, String owner, String file, byte[] data) {
        String fileName = FileManager.getFileName(owner, file);
        String user = SessionManager.getUsername(sessionToken);
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

    public byte[] getFile(int sessionToken, String owner, String file) {
        String user = SessionManager.getUsername(sessionToken);
        if (owner.equals("")){
            owner = user;
        }

        String fileName = FileManager.getFileName(owner, file);

        if (!userFiles.containsKey(owner) || !userFiles.get(owner).contains(fileName)) {
            throw new NoSuchFileException();
        }
        if (hasAccessToFile(user, fileName)) {
            return FileManager.getFile(fileName);
        } else {
            throw new UnauthorizedException();
        }
    }

    public Stream<String> listFiles(int sessionToken) {
        String user = SessionManager.getUsername(sessionToken);
        if (!this.userFiles.containsKey(user)) {
            return Stream.empty();
        }
        return this.userFiles.get(user).stream();
    }
}
