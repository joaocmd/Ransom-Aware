package ransomaware;

import ransomaware.domain.StoredFile;
import ransomaware.exceptions.*;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class RansomAware {

    private final ConcurrentHashMap<String, Set<String>> userFiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> usersWithAccess = new ConcurrentHashMap<>();

    public RansomAware(int port, boolean firstTime) {
        if (!firstTime) {
            spinUp();
        }

        Server.start(this, port);
    }

    private boolean isOwner(String user, StoredFile file) {
        return user.equals(file.getOwner());
    }

    private boolean hasAccessToFile(String user, StoredFile file) {
        // isOwner is used because we don't have permissions yet
        String filename = file.getFileName();
        boolean hasBeenGranted = userFiles.containsKey(user) && userFiles.get(user).contains(filename) &&
                usersWithAccess.containsKey(filename) && usersWithAccess.get(filename).contains(user);
        return isOwner(user, file) || hasBeenGranted;
    }

    public void uploadFile(String user, StoredFile file) {
        String fileName = file.getFileName();

        // Check if file name is valid
        String name = file.getName();
        if (name.matches("[.]*") || name.contains("/")) {
            throw new InvalidFileNameException();
        }

        if (hasAccessToFile(user, file)) {
            // Check if permissions are correct with server's
            if (!usersWithAccess.get(fileName).equals(file.getUsersWithAccess())) {
                throw new IllegalArgumentException();
            }

            FileManager.saveFile(file);

            userFiles.putIfAbsent(user, new HashSet<>());
            userFiles.get(user).add(fileName);

            // draft for getting the certificates for all users with access later
            usersWithAccess.putIfAbsent(fileName, new HashSet<>());
            usersWithAccess.get(fileName).add(user);
        } else {
            throw new UnauthorizedException();
        }
    }

    public StoredFile getFile(String user, StoredFile file) {
        String fileName = file.getFileName();
        String owner = file.getOwner();

        if (!(userFiles.containsKey(owner) && userFiles.get(owner).contains(fileName))) {
            throw new NoSuchFileException();
        }
        if (hasAccessToFile(user, file)) {
            return FileManager.getFile(file);
        } else {
            throw new UnauthorizedException();
        }
    }

    public Map<String, String> getEncryptCertificates(StoredFile file, String username) {
        String filename = file.getFileName();

        // Check if file exists
        if (!usersWithAccess.containsKey(filename)) {
            throw new NoSuchFileException();
        }

        // Check if has access
        if (!hasAccessToFile(username, file)) {
            throw new UnauthorizedException();
        }

        // Get all certificates
        Map<String, String> certs = new HashMap<>();
        usersWithAccess.get(filename).forEach(user -> certs.put(user, SessionManager.getEncryptCertificate(user)));

        return certs;
    }

    public Stream<String> listFiles(String user) {
        if (!this.userFiles.containsKey(user)) {
            return Stream.empty();
        }
        return this.userFiles.get(user).stream();
    }

    public void grantPermission(String userGranting, String userToGrant, StoredFile file) {
        String filename = file.getFileName();

        // Check if user exists
        SessionManager.hasUser(userToGrant);

        // Check if file exists
        if (!(userFiles.containsKey(userGranting) && userFiles.get(userGranting).contains(filename))) {
            throw new NoSuchFileException();
        }

        // Check if it is owner to grant permissions
        if (!isOwner(userGranting, file)) {
            throw new UnauthorizedException();
        }

        // Check if already has permissions
        if (hasAccessToFile(userToGrant, file)) {
            throw new AlreadyGrantedException();
        }

        // Add permissions to user file list and users with access list
        userFiles.putIfAbsent(userToGrant, new HashSet<>());
        userFiles.get(userToGrant).add(filename);

        usersWithAccess.putIfAbsent(filename, new HashSet<>());
        usersWithAccess.get(filename).add(userToGrant);

    }

    public void revokePermission(String userRevoking, String userToRevoke, StoredFile file) {
        String filename = file.getFileName();

        // Check if user exists
        SessionManager.hasUser(userToRevoke);

        // Check if file exists
        if (!(userFiles.containsKey(userRevoking) && userFiles.get(userRevoking).contains(filename))) {
            throw new NoSuchFileException();
        }

        // Check if it is owner to revoke permissions
        if (!isOwner(userRevoking, file)) {
            throw new UnauthorizedException();
        }

        // Check if already don't permissions
        if (!hasAccessToFile(userToRevoke, file)) {
            throw new AlreadyRevokedException();
        }

        // Revoke permissions to user file list and users with access list
        if (userFiles.containsKey(userToRevoke)) {
            userFiles.get(userToRevoke).remove(filename);
        } else {
            throw new AlreadyGrantedException();
        }

        if (usersWithAccess.containsKey(filename)) {
            usersWithAccess.get(filename).remove(userToRevoke);
        } else {
            throw new AlreadyGrantedException();
        }
    }

    public void logout(int sessionToken) {
        SessionManager.logout(sessionToken);
    }
    
    private void spinUp(){
        FileManager.dropDB();

        File folder = new File(ServerVariables.FILES_PATH);
        folder.mkdirs();
        File[] users =  folder.listFiles(File::isDirectory);

        for(File user : users) {
            userFiles.putIfAbsent(user.getName(), new HashSet<>());

            File[] files = user.listFiles(File::isDirectory);

            for(File file : files) {
                String fileName = FileManager.getFileName(user.getName(), file.getName());
                
                userFiles.get(user.getName()).add(fileName);
                usersWithAccess.putIfAbsent(fileName, new HashSet<>());
                usersWithAccess.get(fileName).add(user.getName());

                // TODO: Read permissions and add to maps


                File[] versions = file.listFiles();
                int mostRecent = versions.length;
            
                FileManager.saveNewFileVersion(fileName, mostRecent);
            }
        }
    }
}
