package ransomaware;

import com.github.fracpete.processoutput4j.output.CollectingProcessOutput;
import com.github.fracpete.rsync4j.Binaries;
import com.github.fracpete.rsync4j.RSync;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import ransomaware.domain.StoredFile;
import ransomaware.exceptions.NoSuchFileException;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class FileManager {

    private static final Logger LOGGER = Logger.getLogger(FileManager.class.getName());

    private FileManager() {}

    public static String getFileName(String user, String file) {
        return user + '/' + file;
    }

    public static int getFileVersion(String fileName) {
        MongoClient client = getMongoClient();
        var query = new BasicDBObject("_id", fileName);
        DBObject file = client.getDB(ServerVariables.DB_NAME)
                .getCollection(ServerVariables.DB_COLLECTION_FILES)
                .findOne(query);
        client.close();

        if (file != null) {
            return (Integer)file.get("version");
        } else {
            return 0;
        }
    }

    public static void dropDB(){
        MongoClient client = getMongoClient();
        client.getDB(ServerVariables.DB_NAME).getCollection(ServerVariables.DB_COLLECTION_FILES).drop();
        client.close();
    }

    public static void saveNewFileVersion(String fileName, int version) {
        MongoClient client = getMongoClient();
        var query = new BasicDBObject("_id", fileName);
        var update = new BasicDBObject("version", version);
        client.getDB(ServerVariables.DB_NAME)
                .getCollection(ServerVariables.DB_COLLECTION_FILES)
                .update(query, update, true, false);
        client.close();
    }

    public static void saveFile(StoredFile file) {
        String fileName = file.getFileName();
        String fileDir = ServerVariables.FILES_PATH + '/' + fileName;
        var dir = new File(fileDir);

        dir.mkdirs();

        int newVersion = getFileVersion(fileName) + 1;
        String filePath = String.format("%s/%d", fileDir, newVersion);

        try {
            Path path = Paths.get(filePath);
            Files.write(path, file.toString().getBytes());
            sendToBackupServer(filePath);
            saveNewFileVersion(fileName, newVersion);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.severe("Error writing to file");
            System.exit(1);
        }
    }

    private static void sendToBackupServer(String localPath) {
        RSync rsync = new RSync()
                .recursive(true)
                .relative(true)
                .dirs(true)
                .times(true)
                .source(localPath)
                .destination(ServerVariables.RSYNC_SERVER)
                .rsh("ssh -i " + ServerVariables.RSYNC_KEY);

        try {
            CollectingProcessOutput output = rsync.execute();
            if (!output.hasSucceeded()) {
                LOGGER.severe(output.getStdErr());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static StoredFile getFile(StoredFile file) {
        String fileName = file.getFileName();
        String fileDir = ServerVariables.FILES_PATH + '/' + fileName + '/' + getFileVersion(fileName);
        Path path = Paths.get(fileDir);
        try {
            return new StoredFile(file, new String(Files.readAllBytes(path)));
        } catch (IOException e) {
            throw new NoSuchFileException();
        }
    }

    private static MongoClient getMongoClient() {
        MongoClient client = null;
        try {
            client = new MongoClient(new MongoClientURI(ServerVariables.MONGO_URI));
        } catch (UnknownHostException e) {
            LOGGER.severe("Can't establish connection to the database.");
            System.exit(1);
        }
        return client;
    }

    public static void rollBack(StoredFile file, int n) {
        String fileName = file.getFileName();
        int currentVersion = getFileVersion(fileName);
        int newVersion = currentVersion - n;

        String fileFolder = ServerVariables.FILES_PATH + '/' + fileName + '/';
        for (int i = newVersion + 1; i <= currentVersion; i++) {
            try {
                Files.delete(Path.of(fileFolder + i));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        deleteFromBackupServer(fileFolder);

        MongoClient client = getMongoClient();
        var query = new BasicDBObject("_id", fileName);
        var update = new BasicDBObject("version", newVersion);
        client.getDB(ServerVariables.DB_NAME)
                .getCollection(ServerVariables.DB_COLLECTION_FILES)
                .update(query, update);
        client.close();
    }

    private static void deleteFromBackupServer(String localPath) {
        RSync rsync = new RSync()
                .archive(true)
                .delete(true)
                .times(true)
                .source(localPath)
                .destination(ServerVariables.RSYNC_SERVER + localPath)
                .rsh("ssh -i " + ServerVariables.RSYNC_KEY);

        try {
            CollectingProcessOutput output = rsync.execute();
            if (!output.hasSucceeded()) {
                LOGGER.severe(output.getStdErr());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
