package ransomaware;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import ransomaware.exceptions.DuplicateUsernameException;
import ransomaware.exceptions.UnauthorizedException;

import java.io.File;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final ConcurrentHashMap<Integer, SessionObject> sessions = new ConcurrentHashMap<>();

    public enum SessionState {
        VALID,
        INVALID,
        EXPIRED
    }

    public static SessionState getSessionSate(Integer sessionToken) {
        if (sessions.containsKey(sessionToken)) {
            if (sessions.get(sessionToken).expirationMoment.isAfter(Instant.now())) {
                return SessionState.VALID;
            } else {
                sessions.remove(sessionToken);
                return SessionState.EXPIRED;
            }
        }
        return SessionState.INVALID;
    }

    public static String getUsername(Integer sessionToken) {
        if (sessions.containsKey(sessionToken)) {
            return sessions.get(sessionToken).username;
        }
        return null;
    }

    public static void register(String username, String password, String userKey) throws UnknownHostException {
        MongoClient client = getMongoClient();

        var query = new BasicDBObject("name", username);
        var collection = client.getDB(ServerVariables.NAME).getCollection("users");
        var user = collection.findOne(query);
        if (user == null) {
            String passwordDigest = SecurityUtils.getBase64(SecurityUtils.getDigest(password));
            var obj = new BasicDBObject("name", username)
                    .append("password", passwordDigest)
                    .append("key", userKey);
            collection.insert(obj);
            client.close();

            // FIXME: don't allow weird usernames ('joao/david/')
            File userFolder = new File(String.format("%s/%s", ServerVariables.FS_PATH, username));
            userFolder.mkdir();
        } else {
            client.close();
            throw new DuplicateUsernameException();
        }

    }

    public static int login(String username, String password) {
        MongoClient client = getMongoClient();

        var query = new BasicDBObject("name", username);
        DBObject user = client.getDB(ServerVariables.NAME).getCollection("users").findOne(query);
        client.close();

        if (user != null) {
            String passwordDigest = SecurityUtils.getBase64(SecurityUtils.getDigest(password));
            if (user.get("password").equals(passwordDigest)) {
                SecureRandom rand = new SecureRandom();
                Integer token = rand.nextInt();
                sessions.put(token, new SessionObject(username, Instant.now().plusSeconds(ServerVariables.SESSION_DURATION)));
                return rand.nextInt();
            }
        }
        throw new UnauthorizedException();
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

    private static class SessionObject {
        private final String username;
        private final Instant expirationMoment;

        public SessionObject(String username, Instant expirationMoment) {
            this.username = username;
            this.expirationMoment = expirationMoment;
        }
    }
}
