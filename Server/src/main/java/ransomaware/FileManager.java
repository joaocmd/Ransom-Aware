package ransomaware;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import ransomaware.exceptions.NoSuchFileException;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileManager {

    private FileManager() {}

    public static String getFileName(String user, String file) {
        return user + '/' + file;
    }

    private static int getFileVersion(String fileName) {
        MongoClient client = getMongoClient();
        var query = new BasicDBObject("_id", fileName);
        DBObject file = client.getDB(ServerVariables.FS_PATH).getCollection("files").findOne(query);
        client.close();

        if (file != null) {
            return (Integer)file.get("version");
        } else {
            return 0;
        }
    }

    public static void dropDB(){
        MongoClient client = getMongoClient();
        client.getDB(ServerVariables.FS_PATH).getCollection("files").drop();
        client.close();
    }

    public static void saveNewFileVersion(String fileName, int version) {
        MongoClient client = getMongoClient();
        var query = new BasicDBObject("_id", fileName);
        var update = new BasicDBObject("version", version);
        client.getDB(ServerVariables.FS_PATH).getCollection("files").update(query, update, true, false);
        client.close();
    }

    public static void saveFile(String fileName, byte[] data) {
        String fileDir = ServerVariables.FILES_PATH + '/' + fileName;
        var dir = new File(fileDir);

        dir.mkdirs();

        // TODO: validate file here (what verifications though?)

        int newVersion = getFileVersion(fileName) + 1;
        String filePath = String.format("%s/%d", fileDir, newVersion);
        Path file = Paths.get(filePath);
        try {
            Files.write(file, data);
            saveNewFileVersion(fileName, newVersion);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error writing to file");
            System.exit(1);
        }
    }

    public static byte[] getFile(String fileName) {
        String fileDir = ServerVariables.FILES_PATH + '/' + fileName + '/' + getFileVersion(fileName);
        Path path = Paths.get(fileDir);
        try {
            System.out.println(fileDir);
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new NoSuchFileException();
        }
    }

    private static MongoClient getMongoClient() {
        MongoClient client = null;
        try {
            client = new MongoClient(new MongoClientURI(ServerVariables.MONGO_URI));
        } catch (UnknownHostException e) {
            System.err.println("Can't establish connection to the database.");
            System.exit(1);
        }
        return client;
    }
}
