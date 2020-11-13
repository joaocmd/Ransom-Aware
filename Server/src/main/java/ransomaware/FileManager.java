package ransomaware;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import java.io.File;
import java.io.FileOutputStream;
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

    private static int getNewFileVersion(String fileName) {
        MongoClient client = getMongoClient();
        var query = new BasicDBObject("_id", fileName);
        DBObject file = client.getDB(ServerVariables.FS_PATH).getCollection("files").findOne(query);
        client.close();

        if (file != null) {
            return (Integer)file.get("version") + 1;
        } else {
            return 0;
        }
    }

    private static void saveNewFileVersion(String fileName, int version) {
        MongoClient client = getMongoClient();
        var query = new BasicDBObject("_id", fileName);
        var update = new BasicDBObject("version", version);
        client.getDB(ServerVariables.FS_PATH).getCollection("files").update(query, update, true, false);
        client.close();
    }

    public static void saveFile(String fileName, byte[] data) {
        String fileDir = ServerVariables.FILES_PATH + '/' + fileName;
        var dir = new File(fileDir);

        var a = dir.mkdirs();

        // TODO: validate file here (what verifications though?)

        int newVersion = getNewFileVersion(fileName);
        String filePath = String.format("%s/%d", fileDir, newVersion);
        System.out.println(filePath);
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
