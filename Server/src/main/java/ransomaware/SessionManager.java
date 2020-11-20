package ransomaware;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import ransomaware.exceptions.DuplicateUsernameException;
import ransomaware.exceptions.InvalidUserNameException;
import ransomaware.exceptions.UnauthorizedException;

import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final ConcurrentHashMap<Integer, SessionObject> sessions = new ConcurrentHashMap<>();
    
    public enum SessionState {
        VALID,
        INVALID,
        EXPIRED
    }

    public static SessionState getSessionSate(int sessionToken) {
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
        // TODO: else should never happen but should still be checked
        return null;
    }

    public static void register(String username, String password, String encryptCert) {
        MongoClient client = getMongoClient();

        var query = new BasicDBObject("_id", username);
        var collection = client.getDB(ServerVariables.FS_PATH).getCollection("users");
        var user = collection.findOne(query);

        if(username.contains("/")){ 
            throw new InvalidUserNameException();
        }

        if (user == null) {
            String passwordDigest = SecurityUtils.getBase64(SecurityUtils.getDigest(password));
            var obj = new BasicDBObject("_id", username)
                    .append("password", passwordDigest)
                    .append("encryptCert", encryptCert);
            collection.insert(obj);
            client.close();
            System.out.println("Registered: " + username);
        } else {
            client.close();
            throw new DuplicateUsernameException();
        }

    }

    public static int login(String username, String password) {
        MongoClient client = getMongoClient();

        var query = new BasicDBObject("_id", username);
        DBObject user = client.getDB(ServerVariables.FS_PATH).getCollection("users").findOne(query);
        client.close();

        if (user != null) {
            String passwordDigest = SecurityUtils.getBase64(SecurityUtils.getDigest(password));
            if (user.get("password").equals(passwordDigest)) {
                SecureRandom rand = new SecureRandom();
                int token = rand.nextInt();
                sessions.put(token, new SessionObject(username, Instant.now().plusSeconds(ServerVariables.SESSION_DURATION)));
                System.out.println("Logged in: " + username);
                return token;
            }
        }
        throw new UnauthorizedException();
    }

    public static void logout(int sessionToken) {
        try {
            String username = sessions.remove(sessionToken).username;
            System.out.println("Logged out: " + username);
        } catch (NullPointerException ignored) { }
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

    public static String createSessionCookie(int token) {
        StringBuilder c = new StringBuilder()
                .append("login-token=")
                .append(Integer.toString(token))
                .append("; HttpOnly; Secure; Version=1; max-age=")
                .append(Long.toString(ServerVariables.SESSION_DURATION));
        return c.toString();
    }
}
