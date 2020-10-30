package ransomaware;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import ransomaware.exceptions.DuplicateUsernameException;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static ConcurrentHashMap<Integer, SessionObject> sessions = new ConcurrentHashMap<>();

    public static boolean validateSession(String sessionToken) {

    }

    public static void register(String username, String password) {
        MongoClient client = new MongoClient(new MongoClientURI(ServerVariables.mongoUri));
        var query = new BasicDBObject("name", username);

        var collection = client.getDB(ServerVariables.name).getCollection("users");
        var user = collection.findOne(query);
        if (user == null) {
            String passwordDigest = SecurityUtils.getBase64(SecurityUtils.getDigest(password));
            var obj = new BasicDBObject("name", username).append("password", passwordDigest);
            collection.insert(obj);
        } else {
            client.close();
            throw new DuplicateUsernameException();
        }

    }

    public static int login(String username, String password) {
        MongoClient client = new MongoClient(new MongoClientURI(ServerVariables.mongoUri));

        var query = new BasicDBObject("name", username);
        DBObject user = client.getDB(ServerVariables.name).getCollection("users").findOne(query);
        client.close();

        if (user != null) {
            String passwordDigest = SecurityUtils.getBase64(SecurityUtils.getDigest(password));
            if (((String)user.get("password")).equals(passwordDigest)) {
                SecureRandom rand = new SecureRandom();
                Integer token = rand.nextInt();
                sessions.put(token, new SessionObject(username, Instant.now().plusSeconds(ServerVariables.SESSION_DURATION)));
                return rand.nextInt();
            }
        }
    }

    private static class SessionObject {
        private String username;
        private Instant expirationMoment;

        public SessionObject(String username, Instant expirationMoment) {
            this.username = username;
            this.expirationMoment = expirationMoment;
        }
    }
}
