package ransomaware;

import ransomaware.domain.StoredFile;
import ransomaware.exceptions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class RansomAware {

    private static final Logger LOGGER = Logger.getLogger(RansomAware.class.getName());

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

        // Check if already has lost permissions
        if (!hasAccessToFile(userToRevoke, file)) {
            throw new AlreadyRevokedException();
        }

        // Check if it is revoking permissions of owner
        if (isOwner(userToRevoke, file)) {
            throw new UnauthorizedException();
        }

        // Revoke permissions to user file list and users with access list
        if (userFiles.containsKey(userToRevoke)) {
            userFiles.get(userToRevoke).remove(filename);
        } else {
            throw new AlreadyRevokedException();
        }

        if (usersWithAccess.containsKey(filename)) {
            usersWithAccess.get(filename).remove(userToRevoke);
        } else {
            throw new AlreadyRevokedException();
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

        LOGGER.info("Reading files stored");

        for(File user : users) {
            userFiles.putIfAbsent(user.getName(), new HashSet<>());

            File[] files = user.listFiles(File::isDirectory);

            for(File file : files) {
                String fileName = FileManager.getFileName(user.getName(), file.getName());
                
                userFiles.get(user.getName()).add(fileName);
                usersWithAccess.putIfAbsent(fileName, new HashSet<>());
                usersWithAccess.get(fileName).add(user.getName());

                Optional<File> mostRecent = Stream.of(file.listFiles())
                        .sorted(Comparator.comparing(File::getName).reversed())
                        .findFirst();
                if (mostRecent.isEmpty()) {
                    LOGGER.severe("Inconsistent file system");
                    System.exit(1);
                }
            
                FileManager.saveNewFileVersion(fileName, Integer.parseInt(mostRecent.get().getName()));

                // Read permissions and add to maps
                try {
                    // Get file to read
                    String mostRecentPath = mostRecent.get().getAbsolutePath();
                    StoredFile fileWithOwner = new StoredFile(user.getName(), file.getName());
                    fileWithOwner.toString();
                    String data = Files.readString(Path.of(mostRecentPath));
                    StoredFile storedFile = new StoredFile(fileWithOwner, data);

                    // Add permissions to users that are not the owner
                    for (String userWithAccess: storedFile.getUsersWithAccess()) {
                        // Ignore if owner
                        if (userWithAccess.equals(user.getName())) continue;

                        // Add permission
                        userFiles.putIfAbsent(userWithAccess, new HashSet<>());
                        userFiles.get(userWithAccess).add(fileName);
                        usersWithAccess.putIfAbsent(fileName, new HashSet<>());
                        usersWithAccess.get(fileName).add(userWithAccess);
                    }
                } catch (IOException e) {
                    // FIXME: Should the server be prepared for problems reading the file?
                    e.printStackTrace();
                }

            }
        }
    }
}
