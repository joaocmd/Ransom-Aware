package ransomaware;

import ransomaware.exceptions.NoSuchFileException;
import ransomaware.exceptions.UnauthorizedException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.util.stream.Stream;

public class RansomAware {

    private final ConcurrentHashMap<String, Set<String>> userFiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> usersWithAccess = new ConcurrentHashMap<>();

    public RansomAware(String path, int port, boolean firstTime) {
        if (!firstTime) {
            spinUp();
        }

        Server.start(this, port);
    }

    private boolean isOwner(String user, String fileName) {
        return user.equals(fileName.split("/")[0]);
    }

    private boolean hasAccessToFile(String user, String fileName) {
        return isOwner(user, fileName);
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
        String fileName = FileManager.getFileName(owner, file);

        if (!(userFiles.containsKey(owner) && userFiles.get(owner).contains(fileName))) {
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

    public void logout(int sessionToken) {
        SessionManager.logout(sessionToken);
    }
    
    private void spinUp(){
        FileManager.dropDB();

        File folder = new File(ServerVariables.FILES_PATH);
        File[] users =  folder.listFiles(File::isDirectory);

        for(File user : users) {
            userFiles.putIfAbsent(user.getName(), new HashSet<>());

            File[] files = user.listFiles(File::isDirectory);

            for(File file : files) {
                String fileName = FileManager.getFileName(user.getName(), file.getName());
                
                userFiles.get(user.getName()).add(fileName);
                usersWithAccess.putIfAbsent(fileName, new HashSet<>());
                usersWithAccess.get(fileName).add(user.getName());


                File[] versions = file.listFiles();
                int mostRecent = versions.length;
            
                FileManager.saveNewFileVersion(fileName, mostRecent);
            }
        }
    }
}
